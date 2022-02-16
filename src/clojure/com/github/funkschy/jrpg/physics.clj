(ns com.github.funkschy.jrpg.physics
  (:require [com.github.funkschy.jrpg.engine.ecs :as s :refer [defsystem]]
            [com.github.funkschy.jrpg.components]

            [com.github.funkschy.jrpg.engine.math.vector :refer [add scale normalized]])
  (:import [com.github.funkschy.jrpg.components Velocity Transform]))

(defsystem update-position
  [Velocity Transform]
  [[{:keys [dir speed] :as vel} transform] _ delta]
  [vel (update transform :position add (scale (normalized dir) (* speed delta)))])

