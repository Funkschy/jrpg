(ns com.github.funkschy.jrpg.dialog)

(defn monolog-state-machine [& lines]
  {:current 0
   :states   (vec (cons nil lines))
   :transition-fn
   (fn [state]
     (mod (inc state) (inc (count lines))))})
