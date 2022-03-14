(ns com.github.funkschy.jrpg.engine.input
  (:require
   [com.github.funkschy.jrpg.engine.gamepad :refer [gamepad gamepad-name pressed-buttons]])
  (:import
   [com.github.funkschy.jrpg.engine InputHandler]))

(defn game-input-handler [key-map button-map]
  (let [state (atom {:actions #{} :gamepads {}})]
    (reify InputHandler
      (^void keyDown [_ ^int key]
       (when-let [action (key-map key)]
         (swap! state update :actions conj action)))

      (^void keyUp [_ ^int key]
       (when-let [action (key-map key)]
         (swap! state update :actions disj action)))

      (^void gamepadConnected [_ ^int gamepad-id]
       (let [gamepad (gamepad gamepad-id)]
         (println "Connected Gamepad " (gamepad-name gamepad))
         (swap! state assoc-in [:gamepads gamepad-id] gamepad)))

      (^void joystickDisconnected [_ ^int gamepad-id]
       (println "Disconnected Joystick " gamepad-id)
       (swap! state (fn [state]
                      (-> state
                          (update :gamepads dissoc gamepad-id)))))

      (getActions [_]
        (let [{:keys [actions gamepads]} @state]
          (reduce (fn [actions buttons]
                    (apply conj actions (map button-map buttons)))
                  actions
                  (map pressed-buttons (vals gamepads))))))))
