(ns com.github.funkschy.jrpg.text
  (:require [com.github.funkschy.jrpg.resources :as res]
            [com.github.funkschy.jrpg.engine.animation :as a]))

(defn load-font [renderer font-name letter-size]
  (let [font (res/load-spritesheet renderer font-name letter-size)
        w    (:sprite-count-w font)]
    (fn [c]
      (let [upper (Character/toUpperCase c)
            idx   (inc (- (int upper) (int \,)))]
        (a/at font (mod idx w) (quot idx w))))))

(defn to-sprites [font text]
  (map font text))
