(ns animation-keechma.animation.core
  (:require ["bezier-easing" :as bezier-easing]
            ["rebound" :as rebound]
            [goog.color :as color]))

(def frame-duration 16.667)

(defn rgb->hex [[r g b]]
  (color/rgbToHex r g b))

(defn hex->rgb [hex]
  (js->clj (color/hexToRgb hex)))

(defn map-value-in-range
  ([value to-low to-high] (map-value-in-range value to-low to-high 0 1))
  ([value to-low to-high from-low] (map-value-in-range value to-low to-high from-low 1))
  ([value to-low to-high from-low from-high]
   (let [from-range-size (- from-high from-low)
         to-range-size (- to-high to-low)
         value-scale (/ (- value from-low) from-range-size)]
     (+ to-low (* value-scale to-range-size)))))

(defn interpolate-color
  ([value start-color end-color]
   (interpolate-color value start-color end-color 0))
  ([value start-color end-color from-low]
   (interpolate-color value start-color end-color from-low 1))
  ([value start-color end-color from-low from-high]
   (interpolate-color value start-color end-color from-low from-high false))
  ([value start-color end-color from-low from-high rgb?]
   (let [[start-r start-g start-b] (hex->rgb start-color)
         [end-r end-g end-b] (hex->rgb end-color)
         r (map-value-in-range value start-r end-r)
         g (map-value-in-range value start-g end-g)
         b (map-value-in-range value start-b end-b)]
     (if rgb?
       (str "rgb(" r "," g "," b ")")
       (rgb->hex (map #(.round js/Math (min 255 (max % 0))) [r g b]))))))


