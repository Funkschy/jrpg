(ns com.github.funkschy.jrpg.engine.gamepad
  (:import [org.lwjgl.glfw GLFWGamepadState GLFW]))

(def buttons
  [:A
   :B
   :X
   :Y
   :left-bumper
   :right-bumper
   :back
   :start
   :guide
   :left-thumb
   :right-thumb
   :dpad-up
   :dpad-right
   :dpad-down
   :dpad-left])

(defrecord Gamepad [joystick-id ^GLFWGamepadState state])

(defn gamepad [joystick-id]
  (Gamepad. joystick-id (GLFWGamepadState/create)))

(defn gamepad-name [{:keys [joystick-id]}]
  (GLFW/glfwGetGamepadName joystick-id))

(defn pressed-buttons [{:keys [joystick-id ^GLFWGamepadState state]}]
  (GLFW/glfwGetGamepadState joystick-id state)
  (reduce
   (fn [pressed button-ordinal]
     (if (= (. state buttons ^int button-ordinal) GLFW/GLFW_PRESS)
       (conj pressed (buttons button-ordinal))
       pressed))
   #{}
   (range (count buttons))))
