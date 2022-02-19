(ns com.github.funkschy.jrpg.resources
  (:require [com.github.funkschy.jrpg.engine.render :as r]
            [com.github.funkschy.jrpg.engine.animation :as a])
  (:refer-clojure :exclude [load]))

(defn- load-single [filename]
  (let [cl (. (Thread/currentThread) getContextClassLoader)]
    (with-open [stream (. cl (getResourceAsStream filename))]
      (. stream readAllBytes))))

(defn load [& filenames]
  (into {} (map #(vector % (load-single %)) filenames)))

(defn load-spritesheet [renderer filename sprite-size]
  (let [img-file (str filename ".png")
        resource ((load img-file) img-file)
        texture  (r/create-texture renderer resource false)]
    (a/sprite-sheet texture sprite-size sprite-size)))

(defn load-animations [renderer filename]
  (let [img-file  (str filename ".png")
        json-file (str filename ".json")
        resources (load img-file json-file)
        texture   (r/create-texture renderer (resources img-file) false)
        json      (resources json-file)]
    (a/sprite-animation-json texture json)))

(defn load-textures [renderer repeat? & filenames]
  (let [img-files  (map #(str % ".png") filenames)
        resources  (apply load img-files)
        mk-texture #(r/create-texture renderer (resources (str % ".png")) repeat?)
        textures   (map #(vector % (r/image-sprite (mk-texture %))) filenames)]
    (into {} textures)))

