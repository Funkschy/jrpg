(ns com.github.funkschy.jrpg.components)

(defrecord Animation [animation flip-h?])
(defrecord AnimationStateMachine [state-machine])
(defrecord Sprite [sprite w-scale h-scale])
(defrecord Transform [position])
(defrecord Velocity [dir speed])
(defrecord Input [])

