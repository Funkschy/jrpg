(ns com.github.funkschy.jrpg.graphics
  (:require [com.github.funkschy.jrpg.components]
            [com.github.funkschy.jrpg.engine.ecs :as s :refer [defsystem def-batchsystem]]
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

(defn state-change? [state-machine & args]
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
   (fn [state {:keys [dir]}]
     ({(->Vec2 0 0)   :idle
       (->Vec2 -1 0)  :left
       (->Vec2 1 0)   :right
       (->Vec2 0 1)   :down
       (->Vec2 -1 1)  :down
       (->Vec2 1 1)   :down
       (->Vec2 0 -1)  :up
       (->Vec2 -1 -1) :up
       (->Vec2 1 -1)  :up} dir))})

(def-batchsystem draw-sprites
  [Transform Sprite]
  [ecs {:keys [renderer]} delta entities]
  (doseq [e (sort-by #(:layer (s/component-of ecs % Sprite)) entities)]
    (let [{:keys [sprite w-scale h-scale]} (s/component-of ecs e Sprite)
          {{:keys [x y]} :position}        (s/component-of ecs e Transform)]
      (r/draw-sprite renderer sprite x y w-scale h-scale 0))))

; (defsystem draw-sprites
;   [Transform Sprite]
;   [[{{:keys [x y]} :position} {:keys [sprite w-scale h-scale]}] {:keys [renderer]} delta]
;   (r/draw-sprite renderer sprite x y w-scale h-scale 0))

(defsystem update-animations
  [Animation Sprite]
  [[{:keys [animation] :as anim-comp} sprite] _ delta]
  (let [updated-animation (a/update animation delta)]
    [(assoc anim-comp :animation updated-animation)
     (assoc sprite :sprite (a/as-sprite updated-animation))]))

(defsystem update-velocity-state-machine
  [AnimationStateMachine Velocity Sprite Animation]
  [[{:keys [state-machine] :as sm} vel sprite old-animation] _ delta]
  (if (state-change? state-machine vel)
    (let [new-state-machine (update-state-machine state-machine vel)
          {:keys [animation flip-h?]} (current-state-data new-state-machine)
          flip-fn (comp (if flip-h? - +) #(Math/abs ^float %))]
      [(assoc sm :state-machine new-state-machine)
       vel
       (update sprite :w-scale flip-fn)
       (assoc old-animation :animation animation)])
    [sm vel sprite old-animation]))

