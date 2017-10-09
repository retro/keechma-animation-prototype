(ns animation-keechma.controllers
  (:require [keechma.toolbox.pipeline.controller :as pp-controller]
            [keechma.toolbox.pipeline.core :as pp :refer-macros [pipeline!]]
            [cljs.core.async :refer [<! put! take! chan]]
            [promesa.core :as p]
            [promesa.impl :refer [Promise]]
            ["dynamics.js" :as dyn]
            [animation-keechma.spring :as spring :refer [make-calculator]])
  (:import [goog.async AnimationDelay])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(.config Promise #js {:cancellation true})
(def calc (spring/make-calculator {:tension 40 :friction 7 :overshoot-clamping? false}))
(def calc-2 (spring/make-calculator {:tension 100 :friction 7}))


(def anim-path [:kv :anim-state])

(defn raf-promise []
  (p/promise (fn [resolve reject]
               (let [anim-delay (AnimationDelay. resolve)]
                 (.start anim-delay)))))

(def id-key :keechma.toolbox.animation/id)

(defn reverse-position [anim-state]
  (when anim-state
    (-> anim-state
        (update :position #(.abs js/Math (- % 1))))))

(defn calculate-frames [{:keys [calculator values]} forward? current-state]
  (let [values (if forward? values (reduce-kv (fn [m k v] (assoc m k (reverse v))) {} values))
        state (get current-state :keechma.toolbox/anim-state)
        start-state (if (= forward? (:forward? state)) state (reverse-position state))]
    (vec (spring/calculate-frames calculator values start-state))))

(defn add-start-frames [animation frame-count]
  (let [frames (:frames animation)
        repeating-frame (assoc-in (first frames) [:keechma.toolbox/anim-state :placeholder?] true)
        frames-with-delay (into (vec (repeat frame-count repeating-frame)) frames)]
    (assoc animation :frames frames-with-delay)))

(defn add-end-frames [animation frame-count]
  (let [frames (:frames animation)
        repeating-frame (assoc-in (last frames) [:keechma.toolbox/anim-state :placeholder?] true)
        frames-with-delay (into frames (repeat frame-count repeating-frame))]
    (assoc animation :frames frames-with-delay)))

(defn calculate-forward-delay [prev-animation delay] 
  (if (= 0 delay)
    delay
    (let [max-idx (dec (count (:frames prev-animation)))]
      (loop [frame-idx 0]
        (let [frame-position (get-in prev-animation [:frames frame-idx :keechma.toolbox/anim-state :position])]
          (if (or (= max-idx frame-idx)
                  (>= frame-position delay))
            frame-idx
            (recur (inc frame-idx))))))))

(defn add-forward-delay [animations]
  (loop [animations animations
         with-delay []
         idx 0]
    (if (empty? animations)
      with-delay
      (if (= idx 0)
        (recur (rest animations)
               (conj with-delay (first animations))
               (inc idx))
        (let [current-animation (first animations)
              delay (calculate-forward-delay (last with-delay) (or (:delay current-animation) 0))]
          (recur (rest animations)
                 (conj with-delay (add-start-frames current-animation delay))
                 (inc idx)))))))

(defn add-back-delay [animations]
  (loop [animations animations
         with-delay []
         idx 0]
    (if (empty? animations)
      with-delay
      (if (= idx 0)
        (recur (rest animations)
               (conj with-delay (first animations))
               (inc idx))
        (let [current-animation (first animations)
              prev-animation (last with-delay)
              prev-delay (:delay prev-animation)]
          (if (zero? prev-delay)
            (recur (rest animations)
                   (conj with-delay current-animation)
                   (inc idx))
            (let [current-frames (:frames current-animation)
                  first-position (get-in current-frames [0 :keechma.toolbox/anim-state :position])
                  delay (* (- 1 first-position)
                           (- (count (:frames prev-animation))
                              (- (count current-frames)
                                 (calculate-forward-delay current-animation prev-delay))))]
              (recur (rest animations)
                     (conj with-delay (add-start-frames current-animation delay))
                     (inc idx)))))))))

(defn add-delay [animations forward?]
  (if forward?
    (add-forward-delay animations)
    (add-back-delay animations)))

(defn normalize-frames [animations forward?]
  (let [max-frames-count (apply max (map #(count (:frames %)) animations))]
    (map (fn [animation]
           (let [frames (:frames animation)
                 frames-count (count frames)]
             (if (not= max-frames-count frames-count)
               (add-end-frames animation (- max-frames-count frames-count)) 
               animation))) animations)))

(defn compute-frames [app-db id forward? animations]
  (let [current-state (vec (get-in app-db [:kv id-key id :values]))
        prev-forward? (get-in current-state [0 :keechma.toolbox/anim-state :forward?])
        same-direction? (or (nil? prev-forward?) (= forward? prev-forward?))
        current-state (vec (if same-direction? current-state (reverse current-state)))
        animations-with-frames
        (vec (map-indexed (fn [idx animation]
                            (assoc animation :frames (calculate-frames animation forward? (get current-state idx))))
                          (if forward? animations (reverse animations))))]
    (-> animations-with-frames
        (add-delay forward?)
        (normalize-frames forward?)
        )))

(defn get-values-for-frame [animations forward? frame]
  (mapv (fn [animation]
          (assoc-in (get-in animation [:frames frame])
                    [:keechma.toolbox/anim-state :forward?] forward?))
        animations))

(def version-counter (atom 0))

(defn next-version! []
  (swap! version-counter #(+ 0.00001 %))
  @version-counter)

(defn log-frames [animations]
  (println
   (clojure.string/join
    "\n" (map (fn [f]
                (clojure.string/join
                 "" (map (fn [ff] (if (get-in ff [:keechma.toolbox/anim-state :placeholder?]) "." "*")) f)))
              (map :frames animations)))))

(defn play-animation! [app-db id forward? animations]
  (let [normalized (compute-frames app-db id forward? animations)
        final-frame-count (count (:frames (first normalized)))
        version (next-version!)
        commit-frame (fn [i]
                       (fn [value app-db]
                         (when (>= version (get-in app-db [:kv id-key id :version]))
                           (pp/commit! (assoc-in app-db [:kv id-key id]
                                                 {:version version
                                                  :values (get-values-for-frame normalized forward? i)})))))
        check-version! (fn [value app-db]
                         (when (not= version (get-in app-db [:kv id-key id :version]))
                           (throw (ex-info "New animation version" {:new-animation-version? true}))))]
    (pp/make-pipeline
     {:begin (reduce (fn [acc i]
                       (if (= 0 i)
                         (conj acc (commit-frame i))
                         (concat acc [check-version! raf-promise check-version! (commit-frame i)])))
                     [] (range 0 final-frame-count))
      :rescue [(fn [value app-db error]
                 (when-not (= {:new-animation-version? true} (.-data (:payload error)))
                   (throw error)))]})))

(defn render-first-frame [app-db id forward? animations]
  (let [normalized (compute-frames app-db id forward? animations)]
    (assoc-in app-db [:kv id-key id :values] (get-values-for-frame normalized forward? 0))))

(def animations
  [{:values {:margin-left [0 200]
             :border-radius [0 20]
             :background-color ["#ebf230" "#ef7204"]
             :rotation [0 360]
             :width [40 40]}
    :calculator calc}
   {:values {:width [40 200]
             :rotation [360 360]
             :margin-left [200 200]}
    :delay 0.5
    :calculator calc}
   {:values {:width [200 40]
             :rotation [360 360]
             :margin-left [200 360]
             :background-color ["#ef7204" "#666"]}
    :delay 1.02
    :calculator calc}])

(def anim-controller
  (pp-controller/constructor
   (fn [] 
     true)
   {:start (pipeline! [value app-db]
             (pp/commit! (render-first-frame app-db :animation true animations)))
    :restart (pipeline! [value app-db]
               (println "TEST")
               (pp/commit! (assoc-in app-db [:kv id-key] nil))
               (pp/commit! (render-first-frame app-db :animation true animations)))
    :animation (pipeline! [value app-db]
                 (play-animation! app-db :animation (= :forward value) animations))}))

(def controllers
  {:anim anim-controller})
