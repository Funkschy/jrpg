(ns com.github.funkschy.jrpg.engine.math.m4
  (:refer-clojure :exclude [identity])
  (:import (java.nio FloatBuffer)
           (org.lwjgl.system MemoryUtil)))

(def identity
  (float-array [1 0 0 0
                0 1 0 0
                0 0 1 0
                0 0 0 1]))

(defn orthographic [left right bottom top near far]
  (float-array [(/ 2 (- right left)) 0 0 0
                0 (/ 2 (- top bottom)) 0 0
                0 0 (/ 2 (- near far)) 0

                (/ (+ left right) (- left right))
                (/ (+ bottom top) (- bottom top))
                (/ (+ near far) (- near far))
                1]))

(defn projection [w h d]
  (float-array [(/ 2 w) 0 0 0
                0 (/ -2 h) 0 0
                0 0 (/ 2 d) 0
                -1 1 0 1]))

(defn translation [x y z]
  (float-array [1 0 0 0
                0 1 0 0
                0 0 1 0
                x y z 1]))

(defn x-rotation [radians]
  (let [c (Math/cos radians)
        s (Math/sin radians)]
    (float-array [1 0 0 0
                  0 c s 0
                  0 (- s) c 0
                  0 0 0 1])))

(defn y-rotation [radians]
  (let [c (Math/cos radians)
        s (Math/sin radians)]
    (float-array [c 0 (- s) 0
                  0 1 0 0
                  s 0 c 0
                  0 0 0 1])))

(defn z-rotation [radians]
  (let [c (Math/cos radians)
        s (Math/sin radians)]
    (float-array [c s 0 0
                  (- s) c 0 0
                  0 0 1 0
                  0 0 0 1])))

(defn scale [x y z]
  (float-array [x 0 0 0
                0 y 0 0
                0 0 z 0
                0 0 0 1]))

(defn multiply
  ([[a00 a01 a02 a03 a10 a11 a12 a13 a20 a21 a22 a23 a30 a31 a32 a33]
    [b00 b01 b02 b03 b10 b11 b12 b13 b20 b21 b22 b23 b30 b31 b32 b33]]
   (float-array [(+ (* b00 a00) (* b01 a10) (* b02 a20) (* b03 a30))
                 (+ (* b00 a01) (* b01 a11) (* b02 a21) (* b03 a31))
                 (+ (* b00 a02) (* b01 a12) (* b02 a22) (* b03 a32))
                 (+ (* b00 a03) (* b01 a13) (* b02 a23) (* b03 a33))
                 (+ (* b10 a00) (* b11 a10) (* b12 a20) (* b13 a30))
                 (+ (* b10 a01) (* b11 a11) (* b12 a21) (* b13 a31))
                 (+ (* b10 a02) (* b11 a12) (* b12 a22) (* b13 a32))
                 (+ (* b10 a03) (* b11 a13) (* b12 a22) (* b13 a33))
                 (+ (* b20 a00) (* b21 a10) (* b22 a20) (* b23 a30))
                 (+ (* b20 a01) (* b21 a11) (* b22 a21) (* b23 a31))
                 (+ (* b20 a02) (* b21 a12) (* b22 a22) (* b23 a32))
                 (+ (* b20 a03) (* b21 a13) (* b22 a22) (* b23 a33))
                 (+ (* b30 a00) (* b31 a10) (* b32 a20) (* b33 a30))
                 (+ (* b30 a01) (* b31 a11) (* b32 a21) (* b33 a31))
                 (+ (* b30 a02) (* b31 a12) (* b32 a22) (* b33 a32))
                 (+ (* b30 a03) (* b31 a13) (* b32 a22) (* b33 a33))]))
  ([a b c & r] (reduce multiply (multiply (multiply a b) c) r)))

(defn ^FloatBuffer to-buf [^floats matrix]
  (let [^FloatBuffer buffer (MemoryUtil/memAllocFloat (count matrix))]
    (.flip (.put buffer matrix))))
