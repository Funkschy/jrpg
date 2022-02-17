(ns com.github.funkschy.jrpg.core
  (:gen-class)
  (:require [com.github.funkschy.jrpg.inputs :as i]
            [com.github.funkschy.jrpg.physics :as p]
            [com.github.funkschy.jrpg.graphics :as g]
            [com.github.funkschy.jrpg.components :as c]

            [com.github.funkschy.jrpg.engine.ecs :as s]
            [com.github.funkschy.jrpg.engine.render :as r]
            [com.github.funkschy.jrpg.engine.animation :as a]
            [com.github.funkschy.jrpg.engine.math.vector :refer [->Vec2]])
  (:import (com.github.funkschy.jrpg.engine Window)
           (org.lwjgl.opengl GL11)))

(def fs "
precision mediump float;
varying vec2 v_texcoord;
uniform sampler2D u_texture;

void main() {
    gl_FragColor = texture2D(u_texture, v_texcoord);
}
")

(def vs "
attribute vec4 a_position;
attribute vec2 a_texcoord;

uniform mat4 u_matrix;
uniform mat4 u_texture_matrix;

varying vec2 v_texcoord;

void main() {
  gl_Position = u_matrix * a_position;
  v_texcoord = (u_texture_matrix * vec4(a_texcoord, 0.0, 1.0)).xy;
}
")

(defrecord GameState [ecs renderer input-fn inputs])

(defn- update-state [gamestate timestamp]
  (r/clear-screen (:renderer gamestate))
  (let [new-game-state (-> gamestate
                           (update :inputs (fn [_] ((:input-fn gamestate))))
                           (update :ecs s/run-systems gamestate timestamp))]
    (. ^Window (:window (:renderer gamestate)) update)
    new-game-state))


(def logical-dims [160 144])

(defn -main [& args]
  (let [^Window window (Window. 800 600 "Test" false)
        renderer (r/create-renderer window vs fs logical-dims)

        girl-t (r/create-texture renderer "girl.png" false)
        cat-t (r/create-texture renderer "cat.png" false)
        floor-t (r/create-texture renderer "floor-wood-tile.png" true)
        wall-t (r/create-texture renderer "light-wall-tile.png" true)

        cat-idle (a/sprite-animation (a/sprite-sheet cat-t 16 16) 8)

        idle (a/sprite-animation-secs (a/sprite-sheet girl-t 16 16) 4 1.5)
        walk-down (a/sprite-animation (a/sprite-sheet girl-t 16 16) 4 6)
        walk-up (a/sprite-animation (a/sprite-sheet girl-t 16 16) 10 6)
        walk-left (a/sprite-animation (a/sprite-sheet girl-t 16 16) 16 6)
        walk-right (a/sprite-animation (a/sprite-sheet girl-t 16 16) 16 6)
        walk-sm (g/walk-state-machine idle walk-down walk-up walk-left walk-right)

        player (s/create-entity)
        sly-cat (s/create-entity)
        floor (s/create-entity)
        wall (s/create-entity)

        ecs (-> (s/create-ecs (System/currentTimeMillis))
                (s/add-entity player)
                (s/add-entity sly-cat)
                (s/add-entity floor)
                (s/add-entity wall)

                (s/add-system i/handle-inputs)
                (s/add-system p/update-position)
                (s/add-system g/update-velocity-state-machine)
                (s/add-system g/update-animations)
                (s/add-batchsystem g/draw-sprites)

                (s/add-components wall
                                  (c/->Transform (->Vec2 0 -60))
                                  (c/->Sprite (r/image-sprite wall-t) 20 3 1))

                (s/add-components floor
                                  (c/->Transform (->Vec2 0 24))
                                  (c/->Sprite (r/image-sprite floor-t) 20 25 0))

                (s/add-components sly-cat
                                  (c/->Transform (->Vec2 -50.0 30.0))
                                  (c/->Animation cat-idle false)
                                  (c/->Sprite nil 1 1 1))

                (s/add-components player
                                  (c/->Velocity (->Vec2 0 0) 100.0)
                                  (c/->Input)
                                  (c/->Transform (->Vec2 0 0))
                                  (c/->AnimationStateMachine walk-sm)
                                  (c/->Animation idle false)
                                  (c/->Sprite nil 1 1 1)))
        init-state (GameState. ecs renderer #(. window getInputActions) #{})]

    (. window (setClearColor 0.5 0.5 0.5 1))
    (println "OpenGL version:" (GL11/glGetString GL11/GL_VERSION))

    (loop [state init-state]
      (let [now (System/currentTimeMillis)
            new-state (update-state state now)]

        (when-not (. window shouldClose)
          (recur new-state))))

    (. window destroy)))
