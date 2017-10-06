(ns animation-keechma.controllers
  (:require [keechma.toolbox.pipeline.controller :as pp-controller]
            [keechma.toolbox.pipeline.core :as pp :refer-macros [pipeline!]]
            [cljs.core.async :refer [<! put! take! chan]]
            [promesa.core :as p]
            [promesa.impl :refer [Promise]]
            ["dynamics.js" :as dyn])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(.config Promise #js {:cancellation true})

(defn animate-step [make-promise]
  (pipeline! [value app-db]
    (when (not (get-in app-db [:kv :anim-state :done?]))
      (pipeline! [value app-db]
        (make-promise)
        (pp/commit! (assoc-in app-db [:kv :anim-state] value))
        (when-not (:done? value)
          (animate-step make-promise))))))

(def start {:radius 0
            :color "#f30"
            :size 1})

(def end {:radius 100
          :color "#000"
          :size 2})

(defn animate! [start end]
  (let [anim-chan (chan)
        make-promise (fn []
                       (p/promise (fn [resolve _]
                                    (take! anim-chan resolve))))]
    (put! anim-chan {:value start :done? false})
    (.animate dyn (clj->js start) (clj->js end)
              #js {:type (.-spring dyn)
                   :duration 5000
                   :change (fn [val progress]
                             (let [done? (= progress 1)]
                               (put! anim-chan {:value (js->clj val :keywordize-keys true)
                                                :done? done?})))})
    (animate-step make-promise)))


(def anim-controller
  (pp-controller/constructor
   (fn [_] true)
   {:animation (pp/exclusive
                (pipeline! [value app-db]
                  (when (= :restart value)
                    (pipeline! [value app-db]
                      (pp/commit! (assoc-in app-db [:kv :anim-state] nil))
                      :start))
                  (when (= :start value)
                    (pipeline! [value app-db]
                      (println "BEFORE ANIMATE" value)
                      (animate! (or (get-in app-db [:kv :anim-state :value]) start) end)
                      (println "AFTER ANIMATE" value)))))
    :reset (pipeline! [value app-db]
             (pp/commit! (assoc-in app-db [:kv :anim-state] {:done? false :value start})))}))




(def controllers
  {:anim anim-controller})
