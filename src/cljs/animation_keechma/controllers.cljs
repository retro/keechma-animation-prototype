(ns animation-keechma.controllers
  (:require [keechma.toolbox.pipeline.controller :as pp-controller]
            [keechma.toolbox.pipeline.core :as pp :refer-macros [pipeline!]]
            [cljs.core.async :refer [<! put! take! chan close!]]
            [promesa.core :as p]
            [promesa.impl :refer [Promise]]
            [animation-keechma.spring :as spring :refer [make-calculator]]
            [animation-keechma.animation.core :as a]
            ["gravitas/src/index" :as gravitas]
            [animation-keechma.tasks :refer [blocking-task!
                                             blocking-raf!
                                             non-blocking-task!
                                             non-blocking-raf!
                                             stop-task!
                                             cancel-task!]]
           [animation-keechma.animation.core :refer [render-animation-end blocking-animate-state! non-blocking-animate-state!]]
           [animation-keechma.animation.helpers :refer [get-animation-state]]))


(defn delay-pipeline [msec]
  (p/promise (fn [resolve reject]
               (js/setTimeout resolve msec))))


(def anim-controller
  (pp-controller/constructor
   (fn [params] true)
   {:start (pipeline! [value app-db]
             (println "STARTING")
             (cancel-task! app-db :button)
             (pp/commit! (render-animation-end app-db [:button :init]))
             (rescue! [error]
               
               (println "++++++" error)))
    :animate-press (pipeline! [value app-db]
                    (cancel-task! app-db :button)
                     (if (= :init (get-animation-state app-db :button))
                       (blocking-animate-state! app-db [:button :pressed])
                       (blocking-animate-state! app-db [:button :fail-pressed]))
                     (rescue! [error]
                       (println "*****" error)))
    :animate-load (pipeline! [value app-db]
                    (cancel-task! app-db :button)

                    (if (= :pressed (get-animation-state app-db :button))
                       (blocking-animate-state! app-db [:button :init])
                       (blocking-animate-state! app-db [:button :fail-init]))
                    (blocking-animate-state! app-db [:button :button-loader])
                    (pp/commit! (render-animation-end app-db [:button :loader]))
                    (non-blocking-animate-state! app-db [:button :loader])
                    (delay-pipeline 1500)
                    (when (get-in app-db [:kv :should-fail?])
                      (throw (ex-info "BLA BLA" {})))
                    (stop-task! app-db :button)
                    (pp/commit! (render-animation-end app-db [:button :button-loader]))
                    (blocking-animate-state! app-db [:button :success-notice])
                    (blocking-animate-state! app-db [:button :init])
                    (rescue! [error]
                      (pp/commit! (render-animation-end app-db [:button :button-loader]))
                      (blocking-animate-state! app-db [:button :fail-notice])
                      (blocking-animate-state! app-db [:button :fail-init])
                                          
                      (println "ERROR" error)))
    :toggle-should-fail (pipeline! [value app-db]
                          (pp/commit! (update-in app-db [:kv :should-fail?] not)))
    :stop (pipeline! [value app-db]
            (stop-task! app-db :button))}))

(def controllers
  {:anim anim-controller})
