(ns com.github.funkschy.jrpg.graphics
  (:require
   [com.github.funkschy.jrpg.components]
   [com.github.funkschy.jrpg.engine.animation :as a]
   [com.github.funkschy.jrpg.engine.ecs :as s :refer [def-batchsystem defsystem]]
   [com.github.funkschy.jrpg.engine.math.vector :refer [->Vec2]]
   [com.github.funkschy.jrpg.engine.render :as r]
   [com.github.funkschy.jrpg.states :as sm])
  (:import
   [com.github.funkschy.jrpg.components Transform Sprite Animation Velocity AnimationStateMachine Hitbox InteractionHitbox CurrentInteraction Input]))

(defn walk-state-machine [idle down-anim up-anim left-anim right-anim]
  {:current :idle
   :states  {:idle  {:animation idle :flip-h? false}
             :down  {:animation down-anim :flip-h? false}
             :up    {:animation up-anim :flip-h? false}
             :left  {:animation left-anim :flip-h? true}
             :right {:animation right-anim :flip-h? false}}
   :transition-fn
   (fn [_ {:keys [dir]}]
     ({(->Vec2 0 0)   :idle
       (->Vec2 -1 0)  :left
       (->Vec2 1 0)   :right
       (->Vec2 0 1)   :down
       (->Vec2 -1 1)  :down
       (->Vec2 1 1)   :down
       (->Vec2 0 -1)  :up
       (->Vec2 -1 -1) :up
       (->Vec2 1 -1)  :up} dir))})

                                        ;(defsystem draw-sprites
                                        ;  [Transform Sprite]
                                        ;  [[{{:keys [x y]} :position} {:keys [sprite w-scale h-scale]}] {:keys [renderer]} delta]
                                        ;  (r/draw-sprite renderer sprite x y w-scale h-scale 0))

(def-batchsystem draw-sprites
  [ecs {:keys [renderer]} _ entities]
  [Transform Sprite]
  (doseq [e (sort-by #(:layer (s/component-of ecs % Sprite)) entities)]
    (let [{:keys [sprite w-scale h-scale]} (s/component-of ecs e Sprite)
          {{:keys [x y]} :position} (s/component-of ecs e Transform)]
      (r/draw-sprite renderer sprite x y (* w-scale (:src-w sprite)) (* h-scale (:src-h sprite))))))

(defn- draw-hitbox [renderer hitbox-sprite {{:keys [x y]} :position} {{:keys [rel-pos size]} :aabb}]
  (r/draw-sprite renderer
                 hitbox-sprite
                 (+ x (:x rel-pos) (/ (:x size) 2))
                 (+ y (:y rel-pos) (/ (:y size) 2))
                 (:x size)
                 (:y size)))

(defn draw-hitboxes-system [hitbox-sprite]
  (s/->SystemData
   (fn [[transform hitbox] {:keys [renderer debug?]} _]
     (when debug?
       (draw-hitbox renderer hitbox-sprite transform hitbox)))
   [Transform Hitbox]))

(defn draw-interaction-hitboxes-system [hitbox-sprite]
  (s/->SystemData
   (fn [[transform interaction-hitbox] {:keys [renderer debug?]} _]
     (when debug?
       (draw-hitbox renderer hitbox-sprite transform interaction-hitbox)))
   [Transform InteractionHitbox]))

(defn draw-textbox-system [textbox-sprite font]
  (s/->SystemData
   (fn [[{:keys [interacting?]} {:keys [content]}] {:keys [renderer]} _]
     (when interacting?
       (println content)
       (r/draw-sprite renderer textbox-sprite 0 45)))
   [Input CurrentInteraction]))

(defsystem update-animations
  [[{:keys [animation] :as anim-comp} sprite] _ delta]
  [Animation Sprite]
  (let [updated-animation (a/update animation delta)]
    [(assoc anim-comp :animation updated-animation)
     (assoc sprite :sprite (a/as-sprite updated-animation))]))

(defsystem update-velocity-state-machine
  [[{:keys [state-machine] :as sm} vel sprite old-animation] _ _]
  [AnimationStateMachine Velocity Sprite Animation]
  (if (sm/state-change? state-machine vel)
    (let [new-state-machine (sm/update-state-machine state-machine vel)
          {:keys [animation flip-h?]} (sm/current-state-data new-state-machine)
          flip-fn (comp (if flip-h? - +) #(Math/abs ^float %))]
      [(assoc sm :state-machine new-state-machine)
       vel
       (update sprite :w-scale flip-fn)
       (assoc old-animation :animation animation)])
    [sm vel sprite old-animation]))
