(ns com.github.funkschy.jrpg.engine.animation
  (:require [com.github.funkschy.jrpg.engine.render :refer (->Sprite image-sprite)])
  (:refer-clojure :exclude [update]))

(defrecord SpriteSheet [entire-sprite sprite-w sprite-h sprite-count-w sprite-count-h])

(defn sprite-sheet [texture-info sprite-w sprite-h]
  (SpriteSheet. (image-sprite texture-info)
                sprite-w
                sprite-h
                (/ (:width texture-info) sprite-w)
                (/ (:height texture-info) sprite-h)))

(defn at [{:keys [entire-sprite sprite-w sprite-h sprite-count-w sprite-count-h]} x y]
  (assert (and (< -1 x sprite-count-w) (< -1 y sprite-count-h)) "out of bounds sprite access")
  (->Sprite (:texture-info entire-sprite) (* x sprite-w) (* y sprite-h) sprite-w sprite-h))

(defprotocol Animation
  (update [a delta] "Update the animation with the time in ms since the last frame")
  (as-sprite [a] "Get the current state of the animation as a sprite"))

(defn- next-sprite [{:keys [sprites current-idx] :as a}]
  (let [next-idx (mod (inc current-idx) (count sprites))]
    (-> a
        (assoc :delta-sum 0)
        (assoc :sprite (sprites next-idx))
        (assoc :current-idx next-idx))))

(defrecord SpriteAnimation [sprites current-idx sprite-change-time delta-sum]
  Animation
  (update [a delta]
    (let [sum (+ (:delta-sum a) delta)]
      (if (> sum sprite-change-time)
        (next-sprite a)
        (assoc a :delta-sum sum))))

  (as-sprite [{:keys [sprites current-idx]}]
    (sprites current-idx)))

(defn sprite-animation
  ([sprite-sheet num-sprites]
   ; 0.1 = default frame duration in aseprite
   (sprite-animation sprite-sheet 0 num-sprites 0.1))

  ([sprite-sheet start num-sprites]
   (sprite-animation sprite-sheet start num-sprites 0.1))

  ([sprite-sheet start num-sprites sprite-change-time]
   (let [{:keys [sprite-count-w sprite-count-h]} sprite-sheet
         to-xy (fn [i] [(mod i sprite-count-w) (quot i sprite-count-w)])
         sprites (into [] (map (comp (partial apply at sprite-sheet) to-xy)
                               (range start (+ start num-sprites))))]
     (SpriteAnimation. sprites 0 sprite-change-time 0))))

(defn sprite-animation-secs
  ([sprite-sheet num-sprites animation-len-secs]
   (sprite-animation-secs sprite-sheet 0 num-sprites animation-len-secs))

  ([sprite-sheet start num-sprites animation-len-secs]
   (sprite-animation sprite-sheet start num-sprites (/ animation-len-secs num-sprites))))
