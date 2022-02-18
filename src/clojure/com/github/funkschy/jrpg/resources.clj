(ns com.github.funkschy.jrpg.resources
  (:refer-clojure :exclude [load]))

(defn- load-single [filename]
  (let [cl (. (Thread/currentThread) getContextClassLoader)]
    (with-open [stream (. cl (getResourceAsStream filename))]
      (. stream readAllBytes))))

(defn load [& filenames]
  (into {} (map #(vector % (load-single %)) filenames)))
