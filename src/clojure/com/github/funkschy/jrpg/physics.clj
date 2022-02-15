(ns com.github.funkschy.jrpg.physics
  (:require [com.github.funkschy.jrpg.engine.ecs :as s :refer [defsystem]]
            [com.github.funkschy.jrpg.components]

            [com.github.funkschy.jrpg.engine.math.vector :refer [add scale normalized]])
  (:import [com.github.funkschy.jrpg.components Velocity Transform]))

(defsystem update-position
           #{Velocity Transform}
           [ecs _ delta entities]
           (reduce
             (fn [ecs e]
               (->> (s/component-of ecs e Velocity)
                    ((fn [{:keys [dir speed]}] (scale (normalized dir) (* speed delta))))
                    (s/update-components ecs e Transform :position add)))
             ecs
             entities))

