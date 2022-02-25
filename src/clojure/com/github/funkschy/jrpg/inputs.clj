(ns com.github.funkschy.jrpg.inputs
  (:require
   [com.github.funkschy.jrpg.components]
   [com.github.funkschy.jrpg.engine.aabb :refer [collisions]]
   [com.github.funkschy.jrpg.engine.ecs :as s :refer [def-batchsystem defsystem]]
   [com.github.funkschy.jrpg.engine.math.vector :refer [->Vec2 add]]
   [com.github.funkschy.jrpg.states :as sm])
  (:import
   [com.github.funkschy.jrpg.components Velocity Input InteractionHitbox Transform InteractionContent CurrentInteraction]
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

(def interaction-cooldown 0.5)

(defsystem handle-interactions
  [[{:keys [delta-sum] :as input} {:keys [content] :as interaction}] {:keys [inputs]} delta]
  [Input CurrentInteraction]
  (if (and (> delta-sum interaction-cooldown) (inputs Action/INTERACT) content)
    (let [updated  (update interaction :content sm/update-state-machine)
          value    (sm/current-state-data (:content updated))
          ongoing? (boolean value)]
      [(-> input (assoc :delta-sum 0) (assoc :interacting? ongoing?))
       updated])

    [(update input :delta-sum + delta) interaction]))

(defn- get-entity-collision [ecs a b]
  (let [a-pos  (:position (s/component-of ecs a Transform))
        {a-aabb :aabb} (s/component-of ecs a InteractionHitbox)
        b-pos  (:position (s/component-of ecs b Transform))
        b-aabb (:aabb (s/component-of ecs b InteractionHitbox))
        a-abs-pos (add a-pos (:rel-pos a-aabb))
        b-abs-pos (add b-pos (:rel-pos b-aabb))
        collision (apply min (collisions a-abs-pos a-aabb b-abs-pos b-aabb))]
    (when-not (zero? collision)
      b)))

(defn- first-collision [ecs a hitbox-ents]
  (->> hitbox-ents
       (filter (partial not= a))
       (map (partial get-entity-collision ecs a))
       (remove nil?)
       (first)))

(defn- update-current-interaction [hitbox-ents ecs a]
  (let[collision-ent (first-collision ecs a hitbox-ents)
       content       (:content (s/component-of ecs collision-ent InteractionContent))]
    (s/assoc-component ecs a CurrentInteraction :content content)))

(def-batchsystem check-possible-interactions
  [ecs _ _ entities]
  [Transform InteractionHitbox CurrentInteraction Input Velocity]
  (let [hitbox-ents (s/entities-with-components ecs #{InteractionHitbox InteractionContent Transform})
        in-interaction? (fn [e] (:interacting? (s/component-of ecs e Input)))]
    (reduce (partial update-current-interaction hitbox-ents)
            ecs
            (remove in-interaction? entities)))) ;  don't overwrite an already started interaction
