(ns com.github.funkschy.jrpg.physics
  (:require
   [com.github.funkschy.jrpg.components]
   [com.github.funkschy.jrpg.engine.ecs :as s :refer [def-batchsystem defsystem]]
   [com.github.funkschy.jrpg.engine.math.vector :refer [add normalized scale]]
   [com.github.funkschy.jrpg.engine.aabb :refer [collisions]])
  (:import
   [com.github.funkschy.jrpg.components Velocity Transform Hitbox InteractionHitbox InteractionContent Input]))


(defn- resolve-collision
  [{a-pos :position :as a-trans} {a-aabb :aabb} {b-pos :position} {b-aabb :aabb}]
  (let [a-abs-pos (add a-pos (:rel-pos a-aabb))
        b-abs-pos (add b-pos (:rel-pos b-aabb))
        [h-coll v-coll] (collisions a-abs-pos a-aabb b-abs-pos b-aabb)]
    (if (<= h-coll v-coll)
      (let [op (if (< (:x a-abs-pos) (:x b-abs-pos)) - +)]
        (update-in a-trans [:position :x] op h-coll))
      (let [op (if (< (:y a-abs-pos) (:y b-abs-pos)) - +)]
        (update-in a-trans [:position :y] op v-coll)))))

(defn- resolve-collisions-with [a ecs b]
  (if (not= a b)
    (let [a-transform    (s/component-of ecs a Transform)
          a-hitbox       (s/component-of ecs a Hitbox)
          b-transform    (s/component-of ecs b Transform)
          b-hitbox       (s/component-of ecs b Hitbox)
          updated        (resolve-collision a-transform a-hitbox b-transform b-hitbox)]
      (s/assoc-component ecs a Transform updated))
    ecs))

(def-batchsystem resolve-collisions
  [ecs _ _ entities]
  [Transform Hitbox Velocity]
  (let [hitbox-ents (s/entities-with-components ecs #{Hitbox Transform})]
    (reduce (fn [ecs a] (reduce (partial resolve-collisions-with a) ecs hitbox-ents))
            ecs
            entities)))

(defsystem update-position
  [[{:keys [dir speed] :as vel} transform] _ delta]
  [Velocity Transform]
  [vel (update transform :position add (scale (normalized dir) (* speed delta)))])

