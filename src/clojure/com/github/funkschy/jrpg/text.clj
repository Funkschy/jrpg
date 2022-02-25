(ns com.github.funkschy.jrpg.text
  (:require
   [clojure.string :as str]
   [com.github.funkschy.jrpg.engine.animation :as a]
   [com.github.funkschy.jrpg.resources :as res]))

(defprotocol Font
  (to-sprite [font c] "Convert c to a Sprite")
  (char-dist [font] "Distance between 2 chars in the font")
  (char-dims [font] "Return a pair of [char width, char height]"))

(defrecord BitmapFont [sprite-sheet char-distance]
  Font
  (to-sprite [_ c]
    (let [upper (Character/toUpperCase ^char c)
          w     (:sprite-count-w sprite-sheet)
          idx   (inc (- (int upper) (int \,)))]
      (a/at sprite-sheet (mod idx w) (quot idx w))))
  (char-dims [_]
    [(:sprite-w sprite-sheet) (:sprite-h sprite-sheet)])
  (char-dist [_]
    char-distance))

(defn load-font [renderer font-name letter-size char-distance]
  (BitmapFont. (res/load-spritesheet renderer font-name letter-size) char-distance))

(defn word-to-sprites [font text start-x y]
  (let [char-w (first (char-dims font))
        char-d (char-dist font)]
    (->> text
         (reduce (fn [[x sprites] c]
              [(+ x char-w char-d) (conj sprites [(to-sprite font c) x y])])
            [start-x []])
         peek)))

(defn split-lines [font text x-offset y-offset width height]
  (let [[char-w char-h] (char-dims font)
        char-d          (char-dist font)
        space-h 2
        words (str/split text #"\s")]
    (loop [lines [[]], line-width 0, [w & r-words] words]
      (let [word-width (+ (* (count w) char-w) (* char-d (dec (count w))))
            line-idx   (dec (count lines))
            lines-h    (* line-idx (+ space-h char-h))
            x (+ x-offset line-width)
            y (+ y-offset lines-h)]
        ;; (prn (<= (+ line-width word-width) width) w line-width word-width)
        (assert (<= word-width width) "line will never fit")
        (cond
          ;; no more words
          (nil? w) lines

          ;; fits in same line
          (<= (+ line-width word-width) width)
          (recur (update lines line-idx (partial apply conj) (word-to-sprites font w x y))
                 (long (+ word-width line-width char-w)) ; + 1 char-w for space
                 r-words)

          ;; put in next line
          (<= (+ lines-h space-h char-h) height)
          (recur (conj lines (word-to-sprites font w x-offset (+ y space-h char-h)))
                 (long (+ word-width char-w)) ; + 1 char-w for space
                 r-words)

          :else (throw (IllegalArgumentException. "word does not fit")))))))
