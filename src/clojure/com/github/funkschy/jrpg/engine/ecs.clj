(ns com.github.funkschy.jrpg.engine.ecs
  (:require [clojure.set :refer [intersection]])
  (:import [java.util UUID]))

(defn create-uuid []
  (UUID/randomUUID))

(defrecord SystemData [system-fn component-types])

(defprotocol EcsSystem
  (execute [system ecs data delta]))

(defn create-ecs [now]
  {:last-timestamp     now
   :entity-components  {}   ; entity         -> {component-type -> instance}
   :component-entities {}   ; component-type -> set of entities
   :systems            []}) ; systems, either normal (once per entity) or batch

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
    (assert (get-in ecs [:entity-components entity]) "trying to add components to non existing entity")
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

(defn component-of [ecs entity component-type]
  (get-in ecs [:entity-components entity component-type]))

(defn add-system [ecs system]
  (update ecs :systems conj system))

(defn- run-system [ecs data delta {:keys [system-fn component-types]}]
  (map (fn [e]
         (let [comps  (get-in ecs [:entity-components e])
               result (system-fn (vec (map comps component-types)) data delta)]
           [e component-types result]))
       (entities-with-components ecs (set component-types))))

(defn- apply-update [ecs e ty instance]
  (if instance
    (assoc-in ecs [:entity-components e ty] instance)
    (remove-component ecs e ty)))

(defn- apply-updates [ecs updates]
  (reduce (fn [ecs [e types instances]]
            (reduce (fn [ecs [ty instance]]
                      (apply-update ecs e ty instance))
                    ecs
                    (map vector types instances)))
          ecs
          updates))

(extend-protocol EcsSystem
  clojure.lang.AFn
  (execute [system ecs data delta]
    (system ecs data delta))
  SystemData
  (execute [system ecs data delta]
    (apply-updates ecs (run-system ecs data delta system))))

(defn assoc-component
  ([ecs e component-type instance]
   (apply-update ecs e component-type instance))
  ([ecs e component-type k value]
   (let [instance (component-of ecs e component-type)
         updated  (assoc instance k value)]
     (assoc-component ecs e component-type updated))))

(defn assoc-in-component [ecs e component-type ks value]
  (let [instance (component-of ecs e component-type)
        updated  (assoc-in instance ks value)]
    (assoc-component ecs e component-type updated)))

(defn update-component [ecs e component-type k f & args]
  (let [instance (component-of ecs e component-type)
        updated  (apply update instance k f args)]
    (assoc-component ecs e component-type updated)))

(defn run-systems [ecs data timestamp]
  (let [delta   (double (/ (- timestamp (:last-timestamp ecs)) 1000))
        updated (reduce #(execute %2 %1 data delta)
                        ecs
                        (:systems ecs))]
    (assoc updated :last-timestamp timestamp)))

(defmacro def-batchsystem [system-name arglist component-types & exprs]
  (assert (= 4 (count arglist)) "arglist should be [ecs data delta entities]")
  `(def ~system-name
     (fn [ecs# data# delta#]
       (let [entities# (entities-with-components ecs# ~(set component-types))]
         (or (and (not-empty entities#) ; only execute system, if it's relevant to some entity
                  ((fn ~arglist ~@exprs) ecs# data# delta# entities#))
                                        ; if the function returned nil, we just return the old ecs
             ecs#)))))

(defmacro defsystem [system-name arglist component-types & exprs]
  (assert (= 3 (count arglist)) "arglist should be [components data delta]")
  `(def ~system-name
     (SystemData. (fn ~arglist ~@exprs) ~component-types)))
