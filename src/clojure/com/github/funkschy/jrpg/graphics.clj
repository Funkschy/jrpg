(ns com.github.funkschy.jrpg.graphics
  (:require [com.github.funkschy.jrpg.components]
            [com.github.funkschy.jrpg.engine.ecs :as s :refer [defsystem]]
            [com.github.funkschy.jrpg.engine.render :as r]
            [com.github.funkschy.jrpg.engine.animation :as a]
            [com.github.funkschy.jrpg.engine.math.vector :refer [->Vec2]])
  (:import [com.github.funkschy.jrpg.components Transform Sprite Animation Velocity AnimationStateMachine]))

(defn- next-state [state-machine args]
  (apply (:transition-fn state-machine) (:current state-machine) args))

(defn update-state-machine [state-machine & args]
  (assoc state-machine :current (next-state state-machine args)))

(defn current-state-data [state-machine]
  ((:states state-machine) (:current state-machine)))

(defn state-change? [state-machine args]
  (not= (next-state state-machine args)
        (:current state-machine)))

(defn walk-state-machine [idle down-anim up-anim left-anim right-anim]
  {:current :idle
   :states  {:idle  {:animation idle :flip-h? false}
             :down  {:animation down-anim :flip-h? false}
             :up    {:animation up-anim :flip-h? false}
             :left  {:animation left-anim :flip-h? true}
             :right {:animation right-anim :flip-h? false}}
   :transition-fn
   (fn [state & {:keys [dir]}]
     ({(->Vec2 0 0)   :idle
       (->Vec2 -1 0)  :left
       (->Vec2 1 0)   :right
       (->Vec2 0 1)   :down
       (->Vec2 -1 1)  :down
       (->Vec2 1 1)   :down
       (->Vec2 0 -1)  :up
       (->Vec2 -1 -1) :up
       (->Vec2 1 -1)  :up} dir))})

(defsystem draw-sprites
           #{Transform Sprite}
           [ecs {:keys [renderer]} delta entities]
           (doseq [e entities]
             (let [{:keys [sprite w-scale h-scale]} (s/component-of ecs e Sprite)
                   {{:keys [x y]} :position} (s/component-of ecs e Transform)]
               (r/draw-sprite renderer sprite x y w-scale h-scale 0))))

(defsystem update-animations
           #{Animation Sprite}
           [ecs _ delta entities]
           (reduce
             (fn [ecs e]
               (let [sprite (fn [ecs] (-> (s/component-of ecs e Animation) :animation a/as-sprite))]
                 (as-> ecs updated-ecs
                       (s/update-components updated-ecs e Animation :animation a/update delta)
                       (s/assoc-components updated-ecs e Sprite :sprite (sprite updated-ecs)))))
             ecs
             entities))

(defn- change-animation-if-needed [ecs e & args]
  (letfn [(state-machine [ecs]
            (:state-machine (s/component-of ecs e AnimationStateMachine)))]
    (if (state-change? (state-machine ecs) args)
      (let [updated-ecs (apply s/update-components
                               ecs
                               e
                               AnimationStateMachine
                               :state-machine
                               update-state-machine
                               args)
            {:keys [animation flip-h?]} (current-state-data (state-machine updated-ecs))
            flip-fn (comp (if flip-h? - +) #(Math/abs ^float %))
            updated-ecs (s/assoc-components updated-ecs e Animation :animation animation)]
        (s/update-components updated-ecs e Sprite :w-scale flip-fn))
      ecs)))

(defsystem update-velocity-state-machine
           #{AnimationStateMachine Velocity}
           [ecs _ delta entities]
           (reduce #(change-animation-if-needed %1 %2 :dir (:dir (s/component-of %1 %2 Velocity)))
                   ecs
                   entities))

