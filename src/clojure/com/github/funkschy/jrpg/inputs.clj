(ns com.github.funkschy.jrpg.inputs
  (:require
   [com.github.funkschy.jrpg.components]
   [com.github.funkschy.jrpg.engine.aabb :refer [collisions]]
   [com.github.funkschy.jrpg.engine.ecs :as s :refer [def-batchsystem defsystem]]
   [com.github.funkschy.jrpg.engine.math.vector :refer [->Vec2 add]]
   [com.github.funkschy.jrpg.states :as sm])
  (:import
   [com.github.funkschy.jrpg.components Velocity Input InteractionHitbox Transform InteractionContent]
   [com.github.funkschy.jrpg.engine Action]))

(def ^:private updates
  {Action/UP    (->Vec2 0 -1)
   Action/LEFT  (->Vec2 -1 0)
   Action/RIGHT (->Vec2 1 0)
   Action/DOWN  (->Vec2 0 1)})

(defsystem handle-movements
  [[input velocity] {:keys [inputs]} _]
  [Input Velocity]
  (if-not (:interacting? input)
    (->> (map updates inputs)
         (remove nil?)
         (reduce add (->Vec2 0 0))
         (assoc velocity :dir)
         (vector input))
    [input (assoc velocity :dir (->Vec2 0 0))])) ; moving while interacting is rude

(defsystem handle-interactions
  [[{:keys [delta-sum] :as input} {:keys [last-collision-content] :as hit}] {:keys [inputs]} delta]
  [Input InteractionHitbox]
  (if (and (> delta-sum 1) (inputs Action/INTERACT) last-collision-content)
    (let [updated (update-in hit [:last-collision-content :content] sm/update-state-machine)
          value   (sm/current-state-data (get-in updated [:last-collision-content :content]))]
      (prn value)
      [(-> input (assoc :delta-sum 0) (assoc :interacting? (boolean value))) updated])
    [(update input :delta-sum + delta) hit]))

(defn- check-interaction-collisions [a ecs b]
  (if (not= a b)
    (let [a-pos  (:position (s/component-of ecs a Transform))
          {a-aabb :aabb curr-content :last-collision-content} (s/component-of ecs a InteractionHitbox)
          curr-content (:content curr-content)
          has-ongoing-interaction? (and curr-content (sm/current-state-data curr-content))

          b-pos  (:position (s/component-of ecs b Transform))
          b-aabb (:aabb (s/component-of ecs b InteractionHitbox))
          a-abs-pos (add a-pos (:rel-pos a-aabb))
          b-abs-pos (add b-pos (:rel-pos b-aabb))
          collision (apply min (collisions a-abs-pos a-aabb b-abs-pos b-aabb))
          coll-content  (if (zero? collision) nil (s/component-of ecs b InteractionContent))]
      (if-not has-ongoing-interaction? ; don't overwrite an already started interaction
        (s/assoc-component ecs a InteractionHitbox :last-collision-content coll-content)
        ecs))
    ecs))

(def-batchsystem check-possible-interactions
  [ecs _ _ entities]
  [Transform InteractionHitbox Input Velocity]
  (let [hitbox-ents (s/entities-with-components ecs #{InteractionHitbox InteractionContent Transform})]
    (reduce (fn [ecs a] (reduce (partial check-interaction-collisions a) ecs hitbox-ents))
            ecs
            entities)))
