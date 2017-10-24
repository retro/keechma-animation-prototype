(ns animation-keechma.animation.core
  (:require [garden.color :as color]
            [garden.units :as units]))

(def frame-duration 16.667)

(defn rgb->hex [[r g b]]
  (color/rgb->hex {:red r :green g :blue b}))

(defn hex->rgb [hex]
  (let [{:keys [red green blue]} (color/hex->rgb hex)]
    [red green blue]))

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


(defn extract-css-unit [value]
  (if-let [unit (units/read-unit value)]
    {:value (:magnitude unit) :unit (name (:unit unit)) :animatable :unit}
    {:value value :animatable false}))

(defn prepare-style [style]
  (reduce-kv
   (fn [m k v]
     (assoc m k
            (cond
              (and (string? v) (color/hex? v)) {:value v :animatable :color}
              (string? v) (extract-css-unit v)
              (number? v) {:value v :animatable :number}
              :else {:value v :animatable false}))) {} style))

(defn select-keys-by-namespace
  ([data] (select-keys-by-namespace data nil))
  ([data ns]
   (reduce-kv (fn [m k v]
                (let [key-ns (namespace k)]
                  (if (= key-ns ns)
                    (assoc m (keyword (name k)) v)
                    m))) {} data)))

(defn start-end-styles [start end]
  (reduce-kv (fn [m k v]
               (assoc m k {:start v :end (get end k)})) {} start))

(defn get-current-styles [value styles]
  (reduce-kv (fn [m k {:keys [start end]}]
               (let [current (cond
                               (= start end) end
                               (string? start) (interpolate-color value start (or end start))
                               :else (map-value-in-range value start (or end start)))]
                 (assoc m k current)))
             {} styles))

