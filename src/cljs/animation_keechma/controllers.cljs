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
           [animation-keechma.animation.core :refer [render-animation-end blocking-animate-state! non-blocking-animate-state! get-animation-state animation-id cancel-animation! stop-animation!]]))


(defn delay-pipeline [msec]
  (p/promise (fn [resolve reject]
               (js/setTimeout resolve msec))))

(def version [:create-user :form])

(def anim-controller
  (pp-controller/constructor
   (fn [params] true)
   {:start (pipeline! [value app-db]
             (println "STARTING")
             (cancel-animation! app-db :button version)
             (pp/commit! (render-animation-end app-db :button/init version))
             (rescue! [error]
               (println error)))
    :animate-press (pipeline! [value app-db]
                    (cancel-animation! app-db :button version)
                     (if (= :init (get-animation-state app-db :button version))
                       (blocking-animate-state! app-db :button/pressed version)
                       (blocking-animate-state! app-db :button/fail-pressed version)))
    :animate-load (pipeline! [value app-db]
                    (cancel-animation! app-db :button version)

                    (if (= :pressed (get-animation-state app-db :button version))
                      (blocking-animate-state! app-db :button/init version)
                      (blocking-animate-state! app-db :button/fail-init version))
                    (blocking-animate-state! app-db :button/button-loader version)
                    (pp/commit! (render-animation-end app-db :button/loader version))
                    (non-blocking-animate-state! app-db :button/loader version)
                    (delay-pipeline 1500)
                    (when (get-in app-db [:kv :should-fail?])
                      (throw (ex-info "BLA BLA" {})))
                    (stop-animation! app-db :button version)
                    (pp/commit! (render-animation-end app-db :button/button-loader version))
                    (blocking-animate-state! app-db :button/success-notice version)
                    (blocking-animate-state! app-db :button/init version)
                    (rescue! [error]
                      (pp/commit! (render-animation-end app-db :button/button-loader version))
                      (blocking-animate-state! app-db :button/fail-notice version)
                      (blocking-animate-state! app-db :button/fail-init version)))
    :toggle-should-fail (pipeline! [value app-db]
                          (pp/commit! (update-in app-db [:kv :should-fail?] not)))
    :stop (pipeline! [value app-db]
            (stop-animation! app-db :button version))}))

(def controllers
  {:anim anim-controller})
