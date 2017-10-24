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
                                             blocking-animation!
                                             non-blocking-task!
                                             non-blocking-animation!
                                             stop-task!
                                             cancel-task!]]
            [animation-keechma.ui.main :as ui-main]
            [animation-keechma.animation.animator :as animator]))


(defn delay-pipeline [msec]
  (p/promise (fn [resolve reject]
               (js/setTimeout resolve msec))))

(defn make-initial-meta
  ([identifier] (make-initial-meta identifier nil))
  ([[id state] prev]
   {:id id :state state :position 0 :times-invoked 0 :prev nil}))

(defn render-animation-end [app-db identifier]
  (let [[id _] identifier
        init-meta (make-initial-meta identifier)]
    (assoc-in app-db [:kv :animations id]
              {:data (ui-main/values init-meta)
               :meta init-meta})))

(defn animate-state! [task-runner! app-db identifier]
  (let [[id state] identifier
        prev (get-in app-db [:kv :animations id])
        prev-values (:data prev)
        prev-meta (:meta prev)
        init-meta (make-initial-meta identifier prev-meta)
        animator (ui-main/animator init-meta prev-values)
        values (ui-main/values init-meta)
        start-end (a/start-end-values (a/prepare-values prev-values) (a/prepare-values values))]
    (task-runner!
     id
     (fn [{:keys [times-invoked]} app-db]
       (let [current (get-in app-db [:kv :animations id])
             current-meta (if (zero? times-invoked) init-meta (:meta current))
             next-position (animator/position animator)
             next-meta (assoc current-meta :times-invoked times-invoked :position next-position)
             done? (ui-main/done? next-meta animator)
             next-data (ui-main/step next-meta (a/get-current-styles (if done? 1 next-position) start-end))
             next-app-db (assoc-in app-db [:kv :animations id] {:data next-data :meta next-meta})]
         (if done?
           (stop-task! next-app-db id)
           next-app-db))))))

(def blocking-animate-state! (partial animate-state! blocking-animation!))
(def non-blocking-animate-state! (partial animate-state! non-blocking-animation!))

(defn ignore-task-cancellation [error]
  (let [payload (.. error -payload -data)]
    (when-not (contains? payload :animation-keechma.tasks/task)
      error)))

(defn get-animation-state [app-db id]
  (get-in app-db [:kv :animations id :meta :state]))

(def anim-controller
  (pp-controller/constructor
   (fn [params] true)
   {:start (pipeline! [value app-db]
             (println "STARTING")
             (stop-task! app-db :button)
             (pp/commit! (render-animation-end app-db [:button :init]))
             (rescue! [error]
               (ignore-task-cancellation error)))
    :animate-press (pipeline! [value app-db]
                     (if (= :init (get-animation-state app-db :button))
                       (blocking-animate-state! app-db [:button :pressed])
                       (blocking-animate-state! app-db [:button :fail-pressed]))
                     (rescue! [error]
                       (ignore-task-cancellation error)))
    :animate-load (pipeline! [value app-db]
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
                      (ignore-task-cancellation error)
                      (pp/commit! (render-animation-end app-db [:button :button-loader]))
                      (blocking-animate-state! app-db [:button :fail-notice])
                      (blocking-animate-state! app-db [:button :fail-init])
                                          
                      (println "ERROR" error)))
    :toggle-should-fail (pipeline! [value app-db]
                          (pp/commit! (update-in app-db [:kv :should-fail?] not)))
    :stop (pipeline! [value app-db]
            (pp/commit! (stop-task! app-db :button)))}))

(def controllers
  {:anim anim-controller})
