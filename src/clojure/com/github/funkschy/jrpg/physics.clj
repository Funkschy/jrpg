(ns com.github.funkschy.jrpg.physics
  (:require [com.github.funkschy.jrpg.engine.ecs :as s :refer [defsystem def-batchsystem]]
            [com.github.funkschy.jrpg.components]

            [com.github.funkschy.jrpg.engine.math.vector :refer [add scale normalized]])
  (:import [com.github.funkschy.jrpg.components Velocity Transform Hitbox]
           [com.github.funkschy.jrpg.engine.math.vector Vec2]))


(defrecord AABB [^Vec2 rel-pos ^Vec2 size])

(defn- collisions [a-abs-pos a-aabb b-abs-pos b-aabb]
  (let [size-a (:size a-aabb)
        size-b (:size b-aabb)
        a-1 a-abs-pos
        a-2 (add a-1 size-a)
        b-1 b-abs-pos
        b-2 (add b-1 size-b)]
    [(max 0 (- (min (:x a-2) (:x b-2)) (max (:x a-1) (:x b-1))))
     (max 0 (- (min (:y a-2) (:y b-2)) (max (:y a-1) (:y b-1))))]))

(defn- resolve-collision
  [{a-pos :position :as a-trans} {a-aabb :aabb} {b-pos :position} {b-aabb :aabb}]
  (let [a-abs-pos (add a-pos (:rel-pos a-aabb))
        b-abs-pos (add b-pos (:rel-pos b-aabb))
        [h-coll v-coll] (collisions a-abs-pos a-aabb b-abs-pos b-aabb)]
    (if (< h-coll v-coll)
      (let [op (if (< (:x a-abs-pos) (:x b-abs-pos)) - +)]
        (update-in a-trans [:position :x] op h-coll))
      (let [op (if (< (:y a-abs-pos) (:y b-abs-pos)) - +)]
        (update-in a-trans [:position :y] op v-coll)))))

(defn- resolve-collisions-with [a ecs b]
  (if (not= a b)
    (let [a-transform (s/component-of ecs a Transform)
          a-hitbox    (s/component-of ecs a Hitbox)
          b-transform (s/component-of ecs b Transform)
          b-hitbox    (s/component-of ecs b Hitbox)]
      (s/update-component ecs a Transform (resolve-collision a-transform a-hitbox b-transform b-hitbox)))
    ecs))

(def-batchsystem resolve-collisions
  [Transform Hitbox Velocity]
  [ecs _ _ entities]
  (let [hitbox-ents (s/entities-with-components ecs #{Hitbox Transform})]
    (reduce (fn [ecs a] (reduce (partial resolve-collisions-with a) ecs hitbox-ents))
            ecs
            entities)))

(defsystem update-position
  [Velocity Transform]
  [[{:keys [dir speed] :as vel} transform] _ delta]
  [vel (update transform :position add (scale (normalized dir) (* speed delta)))])

