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
(def calc (spring/make-calculator {:tension 200 :friction 7 :overshoot-clamping? false}))
(def calc-2 (spring/make-calculator {:tension 10 :friction 7}))

(def fwd
  {:color ["#228b22" "#ff0000"]
   :size [100 200]
   :radius [0 100]
   :rotation [0 90]
   :alpha [1 0]})

(def back
  {:color ["#ff0000" "#228b22"]
   :size [200 100]
   :radius [100 0]
   :rotation [90 0]
   :alpha [0 1]})

(def anim-path [:kv :anim-state])

(defn raf-promise []
  (p/promise (fn [resolve reject]
               (let [anim-delay (AnimationDelay. resolve)]
                 (.start anim-delay)))))

(def id-key :keechma.toolbox.animation/id)

(defn reverse-position [anim-state]
  (when anim-state
    (update anim-state :position #(.abs js/Math (- % 1)))))

(defn calculate-frames [{:keys [calculator values]} forward? current-state]
  (let [values (if forward? values (reduce-kv (fn [m k v] (assoc m k (reverse v))) {} values))
        state (get current-state :keechma.toolbox/anim-state)
        start-state (if (= forward? (:forward? state)) state (reverse-position state))]
    (vec (spring/calculate-frames calculator values start-state))))

(defn delay-to-frames-count [frames-count delay]
  (.round js/Math (* frames-count (/ delay 100))))

(defn add-start-frames [frames frame-count]
  (into (vec (repeat frame-count (first frames))) frames))

(defn add-end-frames [frames frame-count]
  (into frames (repeat frame-count (last frames))))

(defn add-delay [animations forward? max-frames-count]
  (map (fn [animation]
         (if-let [delay (:delay animation)]
           (let [first-position (get-in animation [:frames 0 :keechma.toolbox/anim-state :position])
                 delay (* (.abs js/Math (- 1 first-position)) delay)]
             (assoc animation :frames
                    ((if forward? add-start-frames add-end-frames)
                     (:frames animation) (delay-to-frames-count delay max-frames-count))))
           animation)) animations))

(defn normalize-frames [animations forward?]
  (let [max-frames-count (apply max (map #(count (:frames %)) animations))]
    (map (fn [animation]
           (let [frames (:frames animation)
                 frames-count (count frames)]
             (if (not= max-frames-count frames-count)
               (assoc animation :frames
                      ((if forward? add-end-frames add-start-frames) frames (- max-frames-count frames-count)))
               animation))) animations)))

(defn play-animation! [app-db id forward? & animations]
  (let [current-state (vec (get-in app-db [:kv id-key id]))
        animations-with-frames
        (vec (map-indexed (fn [idx animation]
                            (assoc animation :frames (calculate-frames animation forward? (get current-state idx))))
                          animations))

        max-frames-count (apply max (map #(count (:frames %)) animations-with-frames))
        normalized (-> animations-with-frames
                       (add-delay forward? max-frames-count)
                       (normalize-frames forward?))
        final-frame-count (count (:frames (first normalized)))
        get-frame-values (fn [frame]
                           (map (fn [animation]
                                  (assoc-in (get-in animation [:frames frame])
                                            [:keechma.toolbox/anim-state :forward?] forward?))
                                normalized))]
    (pp/make-pipeline
     {:begin (reduce (fn [acc i]
                       (if (= 0 i)
                         (conj acc
                               (fn [value app-db]
                                 (pp/commit! (assoc-in app-db [:kv id-key id] (get-frame-values i)))))
                         (concat acc
                                 [raf-promise
                                  (fn [value app-db]
                                    (pp/commit! (assoc-in app-db [:kv id-key id] (get-frame-values i))))])))
                     [] (range 0 final-frame-count))
      :rescue [(fn [value app-db error]
                 (println error))]})))





(def anim-controller
  (pp-controller/constructor
   (fn [] 
     true)
   {:start (pipeline! [value app-db]
             
             (rescue! [error]
               (println error)))
    :animation (pp/exclusive (pipeline! [value app-db]
                               (play-animation! app-db :animation (= :forward value)
                                                {:values {:radius [0 100]
                                                          :alpha [1 0]
                                                          :rotation [0 90]}
                                                 :calculator calc}
                                                {:values {:color ["#228b22" "#ff0000"]
                                                          :size [100 200]}
                                                 :calculator calc
                                                 :delay 50})))}))

(def controllers
  {:anim anim-controller})
