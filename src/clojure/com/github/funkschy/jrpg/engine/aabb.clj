(ns com.github.funkschy.jrpg.engine.aabb
  (:require
   [com.github.funkschy.jrpg.engine.math.vector :refer [add]])
  (:import
   [com.github.funkschy.jrpg.engine.math.vector Vec2]))


(defrecord AABB [^Vec2 rel-pos ^Vec2 size])

(defn collisions [^Vec2 a-abs-pos ^AABB a-aabb ^Vec2 b-abs-pos ^AABB b-aabb]
  (let [size-a (:size a-aabb)
        size-b (:size b-aabb)
        a-1 a-abs-pos
        a-2 (add a-1 size-a)
        b-1 b-abs-pos
        b-2 (add b-1 size-b)]
    [(max 0 (- (min (:x a-2) (:x b-2)) (max (:x a-1) (:x b-1))))
     (max 0 (- (min (:y a-2) (:y b-2)) (max (:y a-1) (:y b-1))))]))
