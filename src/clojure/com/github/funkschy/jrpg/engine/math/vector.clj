(ns com.github.funkschy.jrpg.engine.math.vector)

(defprotocol Vector
  (add [v1 v2])
  (mult [v1 v2])
  (scale [v scalar])
  (len [v])
  (normalized [v]))

(defrecord Vec2 [^float x ^float y])

(extend-type Vec2
  Vector
  (add [{x1 :x y1 :y} {x2 :x y2 :y}]
    (Vec2. (+ x1 x2) (+ y1 y2)))

  (mult [{x1 :x y1 :y} {x2 :x y2 :y}]
    (Vec2. (* x1 x2) (* y1 y2)))

  (scale [{:keys [x y]} scalar]
    (Vec2. (* scalar x) (* scalar y)))

  (len [{:keys [x y]}]
    (Math/sqrt ^float (+ (* x x) (* y y))))

  (normalized [{:keys [x y] :as v}]
    (let [l (len v)]
      (if (zero? l)
        v
        (Vec2. (/ x l) (/ y l))))))
