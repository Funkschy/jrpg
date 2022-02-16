(ns com.github.funkschy.jrpg.inputs
  (:require [com.github.funkschy.jrpg.components]
            [com.github.funkschy.jrpg.engine.ecs :as s :refer [defsystem]]
            [com.github.funkschy.jrpg.engine.math.vector :refer [->Vec2 add]])
  (:import [com.github.funkschy.jrpg.components Velocity Input]
           [com.github.funkschy.jrpg.engine Action]))

(def updates {Action/UP    (->Vec2 0 -1)
              Action/LEFT  (->Vec2 -1 0)
              Action/RIGHT (->Vec2 1 0)
              Action/DOWN  (->Vec2 0 1)})

(defsystem handle-inputs
  [Input Velocity]
  [[input velocity] {:keys [inputs]} delta]
  (->> (map updates inputs)
       (remove nil?)
       (reduce add (->Vec2 0 0))
       (assoc velocity :dir)
       (vector input)))
