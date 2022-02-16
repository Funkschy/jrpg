(ns com.github.funkschy.jrpg.engine.ecs
  (:require [clojure.set :refer [difference subset? intersection]])
  (:import [java.util UUID]))

(defn create-uuid []
  (UUID/randomUUID))

(defrecord SystemData [system-fn component-types])

(defn create-ecs [now]
  {:last-timestamp     now
   :entity-components  {} ; entity         -> {component-type -> instance}
   :component-entities {} ; component-type -> set of entities
   :systems            []})

(defn create-entity []
  (create-uuid))

(defn components [ecs entity]
  (vals (get-in ecs [:entity-components entity])))

(defn component-types [ecs entity]
  (keys (get-in ecs [:entity-components entity])))

(defn add-entity [ecs entity]
  (assoc-in ecs [:entity-components entity] {}))

(defn remove-entity [ecs entity]
  (let [comp-types (component-types ecs entity)]
    (-> ecs
        (update :entity-components dissoc entity)
        (assoc :component-entities
               (reduce (fn [cs->es component]
                         (update cs->es component disj entity))
                       (ecs :component-entities) comp-types)))))

(defn- update-component-entities [ecs entity component-types]
  (assoc ecs
         :component-entities
         (persistent!
           (reduce (fn [cs->es c]
                     (let [t (type c)]
                       (assoc! cs->es t (conj (or (cs->es t) #{}) entity))))
                   (transient (ecs :component-entities))
                   component-types))))

(defn add-components [ecs entity & components]
  (let [old-comps (get-in ecs [:entity-components entity])
        new-comps (persistent! (reduce #(assoc! %1 (type %2) %2) (transient old-comps) components))]
    (-> ecs
        (assoc-in [:entity-components entity] new-comps)
        (update-component-entities entity components))))

(defn remove-component [ecs entity component-type]
  (-> ecs
      (update-in [:entity-components entity] dissoc component-type)
      (update-in [:component-entities component-type] disj entity)))

(defn entities-with-components [ecs component-type-set]
  (reduce intersection
          (map (ecs :component-entities) component-type-set)))

(defn add-system [ecs system]
  (update ecs :systems conj system))

(defn- run-system [ecs data delta {:keys [system-fn component-types]}]
  (map (fn [e]
         (let [comps  (get-in ecs [:entity-components e])
               result (system-fn (vec (map comps component-types)) data delta)]
           [e component-types result]))
       (entities-with-components ecs (set component-types))))

(defn- apply-update [ecs e ty instance]
  (assoc! ecs :entity-components (assoc-in (:entity-components ecs) [e ty] instance)))

(defn- apply-updates [ecs updates]
  (reduce (fn [ecs [e types instances]]
            (reduce (fn [ecs [ty instance]]
                      (apply-update ecs e ty instance))
                    ecs
                    (map vector types instances)))
          ecs
          updates))

(defn run-systems [ecs data timestamp]
  (let [delta (double (/ (- timestamp (:last-timestamp ecs)) 1000))
        updated (reduce (fn [ecs system] (apply-updates ecs (run-system ecs data delta system)))
                        (transient ecs)
                        (:systems ecs))]
    (persistent! (assoc! updated :last-timestamp timestamp))))

(defmacro defsystem [system-name component-types arglist & exprs]
  (assert (= 3 (count arglist)) "arglist should be [components data delta]")
  `(def ~system-name
     (SystemData. (fn ~arglist ~@exprs) ~component-types)))

