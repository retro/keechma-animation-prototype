(ns animation-keechma.controllers
  (:require [keechma.toolbox.pipeline.controller :as pp-controller]
            [keechma.toolbox.pipeline.core :as pp :refer-macros [pipeline!]]
            [cljs.core.async :refer [<! put! take! chan]]
            [promesa.core :as p]
            [promesa.impl :refer [Promise]]
            ["dynamics.js" :as dyn]
            [animation-keechma.spring :as spring :refer [make-calculator]])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(.config Promise #js {:cancellation true})

;; (defn animate-step [make-promise]
;;   (pipeline! [value app-db]
;;     (when (not (get-in app-db [:kv :anim-state :done?]))
;;       (pipeline! [value app-db]
;;         (make-promise)
;;         (pp/commit! (assoc-in app-db [:kv :anim-state] value))
;;         (when-not (:done? value)
;;           (animate-step make-promise))))))

;; (def start {:radius 0
;;             :color "#f30"
;;             :size 1})

;; (def end {:radius 100
;;           :color "#000"
;;           :size 2})

;; (defn animate! [start end]
;;   (let [anim-chan (chan)
;;         make-promise (fn []
;;                        (p/promise (fn [resolve _]
;;                                     (take! anim-chan resolve))))]
;;     (put! anim-chan {:value start :done? false})
;;     (.animate dyn (clj->js start) (clj->js end)
;;               #js {:type (.-spring dyn)
;;                    :duration 5000
;;                    :change (fn [val progress]
;;                              (let [done? (= progress 1)]
;;                                (put! anim-chan {:value (js->clj val :keywordize-keys true)
;;                                                 :done? done?})))})
;;     (animate-step make-promise)))


;; (def anim-controller
;;   (pp-controller/constructor
;;    (fn [_] true)
;;    {:animation (pp/exclusive
;;                 (pipeline! [value app-db]
;;                   (when (= :restart value)
;;                     (pipeline! [value app-db]
;;                       (pp/commit! (assoc-in app-db [:kv :anim-state] nil))
;;                       :start))
;;                   (when (= :start value)
;;                     (pipeline! [value app-db]
;;                       (println "BEFORE ANIMATE" value)
;;                       (animate! (or (get-in app-db [:kv :anim-state :value]) start) end)
;;                       (println "AFTER ANIMATE" value)))))
;;     :reset (pipeline! [value app-db]
;;              (pp/commit! (assoc-in app-db [:kv :anim-state] {:done? false :value start})))}))

(def calc (spring/make-calculator {:tension 40 :friction 7}))
(def fwd
  {:color ["#ff3300" "#0000ff"]
   :size [100 400]
   :radius [0 100]})

(def back
  {:color ["#0000ff" "#f30"]
   :size [400 100]
   :radius [100 0]})

(def anim-path [:kv :anim-state])

(defn make-pipeline [path id frames]
  
  (pp/make-pipeline
   {:begin (reduce-kv 
            (fn [m k frame]
              (if (= k 0)
                (conj m (fn [value app-db]
                          (pp/commit! (assoc-in app-db path {:id id :value frame}))))
                (concat m
                        [(fn [value app-db]
                           (p/promise (fn [resolve reject]
                                        (.requestAnimationFrame js/window resolve))))
                         (fn [value app-db]
                           (pp/commit! (assoc-in app-db path {:id id :value frame})))]))) [] (vec frames))}))

(defn reverse-position [anim-state]
  (when anim-state
    (update anim-state :position #(.abs js/Math (- % 1)))))

(defn calculate-forward [app-db path]
  (let [current-state (get-in app-db path)
        id (:id current-state)
        anim-state (get-in current-state [:value :keechma.toolbox/anim-state])]
    (if (= :forward id)
      anim-state
      (reverse-position anim-state))))

(defn calculate-back [app-db path]
  (let [current-state (get-in app-db path)
        id (:id current-state)
        anim-state (get-in current-state [:value :keechma.toolbox/anim-state])]
    (if (= :back id)
      anim-state
      (reverse-position anim-state))))

(def anim-controller
  (pp-controller/constructor
   (fn [] 
     true)
   {:animation (pp/exclusive (pipeline! [value app-db]
                               (if (= value :forward)
                                 (make-pipeline [:kv :anim-state] :forward
                                                (spring/calculate-frames calc fwd (calculate-forward app-db [:kv :anim-state])))
                                 (make-pipeline [:kv :anim-state] :back
                                                (spring/calculate-frames calc back (calculate-back app-db [:kv :anim-state]))))))}))

(def controllers
  {:anim anim-controller})
