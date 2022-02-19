(ns com.github.funkschy.jrpg.engine.animation
  (:require [clojure.data.json :as json]
            [com.github.funkschy.jrpg.engine.render :refer (->Sprite image-sprite)])
  (:refer-clojure :exclude [update]))

(defprotocol Animation
  (update [a delta] "Update the animation with the time in ms since the last frame")
  (as-sprite [a] "Get the current state of the animation as a sprite"))

(defrecord SpriteSheet [entire-sprite sprite-w sprite-h sprite-count-w sprite-count-h])

(defn sprite-sheet [texture-info sprite-w sprite-h]
  (SpriteSheet. (image-sprite texture-info)
                sprite-w
                sprite-h
                (quot (:width texture-info) sprite-w)
                (quot (:height texture-info) sprite-h)))

(defn at [{:keys [entire-sprite sprite-w sprite-h sprite-count-w sprite-count-h]} x y]
  (assert (and (< -1 x sprite-count-w) (< -1 y sprite-count-h)) "out of bounds sprite access")
  (->Sprite (:texture-info entire-sprite) (* x sprite-w) (* y sprite-h) sprite-w sprite-h))

(defn- next-sprite [a sprites current-idx]
  (let [next-idx (mod (inc current-idx) (count sprites))]
    (-> a
        (assoc :delta-sum 0)
        (assoc :current-idx next-idx))))

(defrecord SpriteAnimation [sprites current-idx sprite-change-time delta-sum]
  Animation
  (update [a delta]
    (let [sum (+ delta-sum delta)]
      (if (> sum sprite-change-time)
        (next-sprite a sprites current-idx)
        (assoc a :delta-sum sum))))

  (as-sprite [_]
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


;A sprite animation imported from the spritesheet json generated by Aseprite
(defrecord AsepriteAnimation [sprite-infos current-idx delta-sum]
  Animation
  (update [a delta]
    (let [sum (+ delta-sum delta)]
      (if (> sum (:duration (sprite-infos current-idx)))
        (next-sprite a sprite-infos current-idx)
        (assoc a :delta-sum sum))))

  (as-sprite [_]
    (:sprite (sprite-infos current-idx))))

(def ^:private item-regex #".* (\d+)\..*")
(defn- frame-nr [[n _]]
  (Integer/parseInt (last (re-find item-regex n))))

(defn- get-frames [data]
  (if (map? (data "frames"))
    ; format json-hash
    (vec (map second (sort-by frame-nr (data "frames"))))
    ; format json-array
    (data "frames")))

(defn sprite-animation-json [texture-info json-filename]
  (let [data   (json/read-str (slurp json-filename))
        frames (get-frames data)
        sprite (fn [{{:strs [x y w h]} "frame" duration "duration"}]
                 {:sprite (->Sprite texture-info x y w h)
                  :duration (/ duration 1000)})
        anim   (fn [{:strs [name from to]}]
                 [name (AsepriteAnimation. (vec (map (comp sprite frames)
                                                     (range from (inc to)))) 0 0)])
        frame-tags (get-in data ["meta" "frameTags"])]
    (if (empty? frame-tags)
      (AsepriteAnimation. (vec (map sprite frames)) 0 0)
      (into {} (map anim frame-tags)))))
