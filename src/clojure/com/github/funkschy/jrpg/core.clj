(ns com.github.funkschy.jrpg.core
  (:gen-class)
  (:require
   [com.github.funkschy.jrpg.components :as c]
   [com.github.funkschy.jrpg.dialog :refer [monolog-state-machine]]
   [com.github.funkschy.jrpg.engine.aabb :refer [->AABB]]
   [com.github.funkschy.jrpg.engine.animation :as a]
   [com.github.funkschy.jrpg.engine.ecs :as s]
   [com.github.funkschy.jrpg.engine.math.vector :refer [->Vec2]]
   [com.github.funkschy.jrpg.engine.render :as r]
   [com.github.funkschy.jrpg.graphics :as g]
   [com.github.funkschy.jrpg.inputs :as i]
   [com.github.funkschy.jrpg.physics :as p]
   [com.github.funkschy.jrpg.resources :as res]
   [com.github.funkschy.jrpg.text :as t])
  (:import
   (com.github.funkschy.jrpg.engine Window Action)
   (org.lwjgl.opengl GL11)))

(defrecord GameState [ecs renderer input-fn inputs debug? debug-enabled-timestamp])

(defn- debug-state [gamestate timestamp pressed?]
  (let [dbg-ts (:debug-enabled-timestamp gamestate)
        can-debug? (> (- timestamp dbg-ts) 100)
        change-debug? (and can-debug? pressed?)
        should-debug? ((if change-debug? not identity) (:debug? gamestate))]
    [should-debug? (if change-debug? timestamp dbg-ts)]))

(defn- update-state [gamestate timestamp]
  (r/clear-screen (:renderer gamestate))
  (let [inputs ((:input-fn gamestate))
        [should-debug? new-dbg-ts] (debug-state gamestate timestamp (inputs Action/DEBUG))
        new-game-state (-> gamestate
                           (assoc :inputs inputs)
                           (update :ecs s/run-systems gamestate timestamp)
                           (assoc :debug? should-debug?)
                           (assoc :debug-enabled-timestamp new-dbg-ts))]
    (. ^Window (:window (:renderer gamestate)) update)
    new-game-state))

(def logical-dims [160 144])
(def inputs {(int \W) Action/UP
             (int \A) Action/LEFT
             (int \S) Action/DOWN
             (int \D) Action/RIGHT
             (int \G) Action/DEBUG
             (int \space) Action/INTERACT})

(defn -main []
  (let [^Window window (Window. 800 600 "JRPG" inputs false)
        {vs "vertex.glsl" fs "fragment.glsl"} (res/load "vertex.glsl" "fragment.glsl")
        renderer (r/create-renderer window (String. ^bytes vs) (String. ^bytes fs) logical-dims)

        {:strs [floor-light wall-light dialog-bg]}
        (res/load-textures renderer true "floor-light" "wall-light" "dialog-bg")

        cat-idle   ((res/load-animations renderer "cat") "idle")
        girl-anims (map (res/load-animations renderer "girl")
                        ["idle" "walk-down" "walk-up" "walk-horizontal" "walk-horizontal"])
        grandma-idle ((res/load-animations renderer "grandma") "idle")

        walk-sm (apply g/walk-state-machine girl-anims)

        hitbox (a/at (res/load-spritesheet renderer "hitbox" 16) 0 0)
        interaction-hitbox (a/at (res/load-spritesheet renderer "interaction-hitbox" 16) 0 0)

        font (t/load-font renderer "gb-font" 7)

        cat-monolog (monolog-state-machine "Moin meister" "Wie gehts?")
        grandma-monolog (monolog-state-machine "You should go outside for once" "Sup bruh")

        player (s/create-entity)
        sly-cat (s/create-entity)
        grandma (s/create-entity)
        floor (s/create-entity)
        wall (s/create-entity)

        now (System/currentTimeMillis)

        ecs (-> (s/create-ecs now)
                (s/add-entity player)
                (s/add-entity sly-cat)
                (s/add-entity floor)
                (s/add-entity wall)
                (s/add-entity grandma)

                (s/add-system i/handle-movements)
                (s/add-system p/update-position)
                (s/add-system p/resolve-collisions)
                (s/add-system i/check-possible-interactions)
                (s/add-system i/handle-interactions)
                (s/add-system g/update-velocity-state-machine)
                (s/add-system g/update-animations)
                (s/add-system g/draw-sprites)
                (s/add-system (g/draw-interaction-hitboxes-system interaction-hitbox))
                (s/add-system (g/draw-hitboxes-system hitbox))
                (s/add-system (g/draw-textbox-system dialog-bg font))

                (s/add-components wall
                                  (c/->Hitbox (->AABB (->Vec2 -80 -15) (->Vec2 160 20)))
                                  (c/->Transform (->Vec2 0 -60))
                                  (c/->Sprite wall-light 20 3 1))

                (s/add-components floor
                                  (c/->Transform (->Vec2 0 24))
                                  (c/->Sprite floor-light 20 25 0))

                (s/add-components sly-cat
                                  (c/->Transform (->Vec2 -50.0 30.0))
                                  (c/->Animation cat-idle false)
                                  (c/->Hitbox (->AABB (->Vec2 -8 -8) (->Vec2 16 16)))
                                  (c/->InteractionContent cat-monolog)
                                  (c/->InteractionHitbox (->AABB (->Vec2 -16 -16) (->Vec2 32 32)))
                                  (c/->Sprite nil 1 1 2))

                (s/add-components grandma
                                  (c/->Transform (->Vec2 50.0 30.0))
                                  (c/->Animation grandma-idle false)
                                  (c/->Hitbox (->AABB (->Vec2 -8 -8) (->Vec2 16 16)))
                                  (c/->InteractionContent grandma-monolog)
                                  (c/->InteractionHitbox (->AABB (->Vec2 -16 -16) (->Vec2 32 32)))
                                  (c/->Sprite nil 1 1 2))

                (s/add-components player
                                  (c/->Velocity (->Vec2 0 0) 100.0)
                                  (c/->Input 0 false)
                                  (c/->InteractionHitbox (->AABB (->Vec2 -16 -16) (->Vec2 32 32)))
                                  (c/->CurrentInteraction nil)
                                  (c/->Transform (->Vec2 0 0))
                                  (c/->Hitbox (->AABB (->Vec2 -8 -8) (->Vec2 16 16)))
                                  (c/->AnimationStateMachine walk-sm)
                                  (c/->Animation (first girl-anims) false)
                                  (c/->Sprite nil 1 1 2)))
        init-state (GameState. ecs renderer #(. window getInputActions) #{} false now)]

    (. window (setClearColor 0.5 0.5 0.5 1))
    (println "OpenGL version:" (GL11/glGetString GL11/GL_VERSION))

    (loop [state init-state]
      (let [now (System/currentTimeMillis)
            new-state (update-state state now)]

        (when-not (. window shouldClose)
          (recur new-state))))

    (. window destroy)))
