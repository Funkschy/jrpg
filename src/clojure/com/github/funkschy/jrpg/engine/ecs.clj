(ns com.github.funkschy.jrpg.engine.ecs
  (:require [clojure.set :refer [difference subset? intersection]])
  (:import [java.util UUID]))

(defn create-uuid []
  (UUID/randomUUID))

(defn create-ecs [now]
  {:last-timestamp     now
   :entity-components  {}                                   ; entity         -> [set of component instances, set of component types]
   :component-entities {}                                   ; component-type -> set of entities
   :systems            []})

(defn create-entity []
  (create-uuid))

(defn components [ecs entity]
  (get-in ecs [:entity-components entity 0]))

(defn component-types [ecs entity]
  (get-in ecs [:entity-components entity 1]))

(defn add-entity [ecs entity]
  (assoc ecs :entity-components (assoc (:entity-components ecs) entity [#{} #{}])))

(defn remove-entity [ecs entity]
  (let [comp-types (component-types ecs entity)]
    (-> ecs
        (assoc :entity-components (dissoc (:entity-components ecs) entity))
        (assoc :component-entities
               (reduce (fn [cs->es component]
                         (update cs->es component #(disj % entity)))
                       (ecs :component-entities) comp-types)))))

(defn- update-component-entities [transient-ecs entity component-types]
  (assoc! transient-ecs
          :component-entities
          (persistent!
            (reduce (fn [cs->es c]
                      (let [t (type c)]
                        (assoc! cs->es t (conj (or (cs->es t) #{}) entity))))
                    (transient (transient-ecs :component-entities))
                    component-types))))

(defn add-components [ecs entity & components]
  (let [[old-inst old-types] (get-in ecs [:entity-components entity])
        new-inst (persistent! (reduce conj! (transient old-inst) components))
        new-types (persistent! (reduce conj! (transient old-types) (map type components)))]
    (-> (transient ecs)
        (assoc! :entity-components (assoc (:entity-components ecs) entity [new-inst new-types]))
        (update-component-entities entity components)
        (persistent!))))

(defn remove-component [ecs entity component]
  (let [new-instances (disj (components ecs entity) component)
        old-types (component-types ecs entity)
        new-types (into #{} (map type new-instances))
        removed-types (difference old-types new-types)]
    (-> ecs
        (assoc-in [:entity-components entity 0] new-instances)
        (assoc-in [:entity-components entity 1] new-types)
        (assoc :component-entities
               (reduce (fn [cs->es component-type]
                         (update cs->es component-type #(disj % entity)))
                       (:component-entities ecs)
                       removed-types)))))

(defn update-components [ecs entity component-type k f & args]
  (let [instances (into [] (components ecs entity))]
    (->> (reduce
           (fn [new-instances [i component]]
             (if (= (type component) component-type)
               (assoc! new-instances i (apply update component k f args))
               new-instances))
           (transient instances)
           (map vector (range) instances))
         (persistent!)
         (into #{})
         (assoc-in ecs [:entity-components entity 0]))))

(defn assoc-components [ecs entity component-type k value]
  (update-components ecs entity component-type k (constantly value)))

(defn entities-with-components [ecs component-type-set]
  (let [es->cs (ecs :entity-components)
        cs->es (ecs :component-entities)]
    (filter (fn [e] (subset? component-type-set (second (es->cs e))))
            (reduce intersection (map cs->es component-type-set)))))

(defn has-component-of? [ecs entity component-type]
  (contains? (component-types ecs entity)
             component-type))

(defn component-of [ecs entity component-type]
  (let [[h & t] (filter #(= (type %) component-type) (components ecs entity))]
    (assert (nil? t) (str "Multiple components of type " component-type ", use 'components-of'"))
    h))

(defn get-components [ecs entity & component-types]
  (map (partial component-of ecs entity) component-types))

(defn components-of [ecs entity component-type]
  (filter #(= (type %) component-type) (components ecs entity)))

(defn add-system [ecs system-fn]
  (update ecs :systems #(conj % system-fn)))

(defn run-systems [ecs data timestamp]
  (let [delta (double (/ (- timestamp (:last-timestamp ecs)) 1000))]
    (assoc (reduce (fn [ecs system] (system ecs data delta)) ecs (:systems ecs))
      :last-timestamp
      timestamp)))

(defmacro defsystem [system-name component-types arglist & exprs]
  (assert (= 4 (count arglist)) "arglist should be [ecs data delta entities]")
  `(def ~system-name
     (fn [ecs# data# delta#]
       (let [entities# (entities-with-components ecs# ~component-types)]
         (or (and (not-empty entities#)                     ; only execute system, if it's relevant to some entity
                  ((fn ~arglist ~@exprs) ecs# data# delta# entities#))
             ; if the function returned nil, we just return the old ecs
             ecs#)))))

