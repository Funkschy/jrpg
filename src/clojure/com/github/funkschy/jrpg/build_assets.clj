(ns com.github.funkschy.jrpg.build-assets
  (:require [clojure.java.shell :refer [sh]]))

(def animation-spritesheets
  ["cat"
   "girl"])

(def sliced-spritesheets
  ["background"])

(def simple-spritesheets
  ["gb-font"
   "hitbox"])

(defn- get-aseprite-path [filename]
  (str "pixelart/" filename ".aseprite"))

(defn- get-spritesheet-path [filename]
  (str "resources/" filename ".png"))

(defn- get-spritesheet-meta-path [filename]
  (str "resources/" filename ".json"))

(defn- animation-args [asset]
  ["-b" (get-aseprite-path asset)
   "--format" "json-array"
   "--sheet" (get-spritesheet-path asset)
   "--data" (get-spritesheet-meta-path asset)
   "--list-tags"])

(defn- sliced-args [asset]
  ["-b" (get-aseprite-path asset)
   "--save-as" "resources/{slice}.png"
   "--split-slices"])

(defn- simple-args [asset]
  ["-b" (get-aseprite-path asset)
   "--sheet" (get-spritesheet-path asset)])

(defn- run [aseprite-path assets arg-fn]
  (doseq [asset assets]
    (let [{:keys [exit out err]}
          (apply sh aseprite-path (arg-fn asset))]
      (when-not (zero? exit)
        (println err)))))

(defn build-all
  ([] (throw (IllegalArgumentException. "Please pass path to the aseprite executable")))
  ([aseprite-path]
   (run aseprite-path animation-spritesheets animation-args)
   (run aseprite-path sliced-spritesheets sliced-args)
   (run aseprite-path simple-spritesheets simple-args)
   (println "finished building assets")))
