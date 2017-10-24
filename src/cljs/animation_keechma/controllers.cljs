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
            [animation-keechma.ui.main :as ui-main :refer [animation]]
            [animation-keechma.animation.animator :as animator]))


#_(def anim-controller
  (pp-controller/constructor
   (fn [] true)
   {:start (pipeline! [value app-db]
             (pp/commit! (cancel-task! app-db :animate-button))
             (pp/commit! (cancel-task! app-db :window-resize))

             (pp/commit! (assoc-in app-db [:kv :animation]
                                   {:id :button-start
                                    :position 0
                                    :value start-styles}))
             (pp/execute! :measure-screen nil)
             #_(non-blocking-task! window-mouse-move-runner :mouse-move
                                 (fn [{:keys [value]} app-db]
                                   (println value)
                                   app-db))
             (rescue! [error]
               (println error)))
    :measure-screen (pipeline! [value app-db]
                      #_(blocking-task! window-resize-runner :window-resize
                                      (fn [{:keys [value]} app-db]
                                        (println value)
                                        app-db))
                      ;;(println "I'M BLOCKED BY THE WINDOW MEASURER")
                      (rescue! [error]
                        (println error)))
    :stop (pipeline! [value app-db]
            (pp/commit! (cancel-task! app-db :animate-button))
            (pp/commit! (cancel-task! app-db :window-resize))
            (pp/commit! (cancel-task! app-db :mouse-move))
            (rescue! [error]
              (println error)))
    :animate-press (pipeline! [value app-db]
                     (pp/commit! (cancel-task! app-db :animate-button))
                     (blocking-animation! :animate-button (animate-press))
                     (rescue! [error]
                       (println error)))
    :animate-submit (pipeline! [value app-db]
                      (pp/commit! (stop-task! app-db :animate-button))
                      (blocking-animation! :animate-button (animate-release app-db))
                      (blocking-animation! :animate-button (animate-button-to-loader app-db))
                      (pp/commit! (assoc-in app-db [:kv :animation] 
                                            {:id :spinner-start
                                             :position 0
                                             :value loader-styles}))
                      (blocking-animation! :animate-button (animate-loader-to-spinner app-db))
                      (non-blocking-animation! :animate-button
                                          (fn [{:keys [times-invoked]} app-db]
                                            (update-in app-db [:kv :animation :value :rotation] inc)))
                      (delay-pipeline (* 3 (rand-int 500)))
                      (pp/execute! :finish-loading true)
                      (rescue! [error]
                        (println error)))
    :finish-loading (pipeline! [value app-db]
                      (pp/commit! (stop-task! app-db :animate-button))
                      (blocking-animation! :animate-button (animate-spinner-to-button app-db))
                      (pp/commit! (assoc-in app-db [:kv :animation]
                                            {:id :button-start
                                             :position 0
                                             :value success-start-styles}))
                      (blocking-animation! :animate-button (animate-success app-db))
                      (rescue! [error]
                        (println error)))
    :cancel (pipeline! [value app-db]
              (println "CANCEL")
              (pp/commit! (stop-task! app-db :mouse-move))
              (pp/commit! (stop-task! app-db :window-resize)))}))


;; (defn animate-press []
;;   (let [s (Spring. 0.9 800 40)
;;         se (start-end-styles start-styles press-down-styles)]
;;     (.snap s 0)
;;     (.setEnd s 1)
;;     (fn [{:keys [id]} app-db]
;;       (let [value (.x s)
;;             next-app-db (-> app-db
;;                             (assoc-in [:kv :animation :value] (get-current-styles value se))
;;                             (assoc-in [:kv :animation :position] value))]
;;         (if (not (.done s))
;;           next-app-db
;;           (stop-task! next-app-db id))))))

;; (defn animate-release [app-db]
;;   (let [s (Spring. 0.9 800 40)
;;         current (get-in app-db [:kv :animation])
;;         current-styles (or (:value current) start-styles)
;;         se (start-end-styles current-styles start-styles)
;;         position (or (- 1 (:position current)) 0)]
;;     (.snap s position)
;;     (.setEnd s 1)
;;     (fn [{:keys [id]} app-db]
;;       (let [value (.x s)
;;             next-app-db (-> app-db
;;                             (assoc-in [:kv :animation :value] (get-current-styles value se))
;;                             (assoc-in [:kv :animation :position] value))]
;;         (if (or (.done s)
;;                 (> value 1.02))
;;           (stop-task! next-app-db id)
;;           next-app-db)))))

;; (defn animate-button-to-loader [app-db]
;;   (let [s (Spring. 1 400 20)
;;         se (start-end-styles start-styles button->loader-styles)]
;;     (.snap s 0)
;;     (.setEnd s 1)
;;     (fn [{:keys [id]} app-db]
;;       (let [value (.x s)
;;             next-app-db (-> app-db
;;                             (assoc-in [:kv :animation :value] (get-current-styles value se))
;;                             (assoc-in [:kv :animation :position] value))]
;;         (if (or (.done s)
;;                 (> value 1))
;;           (stop-task! next-app-db id)
;;           next-app-db
;;           )))))

;; (defn animate-loader-to-spinner [app-db]
;;   (let [s (Spring. 1 800 20)
;;         se (start-end-styles loader-styles loader-starting-styles)]
;;     (.snap s 0)
;;     (.setEnd s 1)
;;     (fn [{:keys [times-invoked id]} app-db]
;;       (let [value (.x s)
;;             next-app-db (-> app-db
;;                             (assoc-in [:kv :animation :value] (get-current-styles value se))
;;                             (assoc-in [:kv :animation :position] value)
;;                             (assoc-in [:kv :animation :value :rotation] times-invoked))]
;;         (if (.done s)
;;           (stop-task! next-app-db id)
;;           next-app-db
;;           )))))

;; (defn animate-spinner-to-button [app-db]
;;   (let [s (Spring. 1 400 40)
;;         current (get-in app-db [:kv :animation])
;;         current-styles (or (:value current) loader-starting-styles)
;;         se (start-end-styles current-styles loader-button-styles)
;;         position (or (- 1 (:position current)) 0)]
;;     (.snap s position)
;;     (.setEnd s 1)
;;     (fn [{:keys [id]} app-db]
;;       (let [value (.x s)
;;             next-app-db (-> app-db
;;                             (assoc-in [:kv :animation :value] (get-current-styles value se))
;;                             (assoc-in [:kv :animation :position] value))]
;;         (if (not (.done s))
;;           next-app-db
;;           (stop-task! next-app-db id))))))

;; (defn animate-success [app-db]
;;   (let [s (Spring. 1 400 20)
;;         se (start-end-styles success-start-styles success-end-styles)]
;;     (.snap s 0)
;;     (.setEnd s 1)
;;     (fn [{:keys [times-invoked id]} app-db]
;;       (let [value (.x s)
;;             next-app-db (-> app-db
;;                             (assoc-in [:kv :animation :value] (get-current-styles value se))
;;                             (assoc-in [:kv :animation :position] value)
;;                             (assoc-in [:kv :animation :value :rotation] times-invoked))]
;;         (if (not (.done s))
;;           next-app-db
;;           (stop-task! next-app-db id))))))


(def Spring (.-Spring gravitas))





(defn noop [& args])

(defn delay-pipeline [msec]
  (p/promise (fn [resolve reject]
               (js/setTimeout resolve msec))))

(defn make-initial-meta
  ([identifier] (make-initial-meta identifier nil))
  ([[id state] prev]
   {:id id :state state :position 0 :times-invoked 0 :prev nil}))

(defn render-animation-state [app-db identifier]
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
             (pp/commit! (render-animation-state app-db [:button :init]))
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
                    (pp/commit! (render-animation-state app-db [:button :loader]))
                    (non-blocking-animate-state! app-db [:button :loader])
                    (delay-pipeline 1500)
                    (when (get-in app-db [:kv :should-fail?])
                      (throw (ex-info "BLA BLA" {})))
                    (stop-task! app-db :button)
                    (pp/commit! (render-animation-state app-db [:button :button-loader]))
                    (blocking-animate-state! app-db [:button :success-notice])
                    (blocking-animate-state! app-db [:button :init])
                    (rescue! [error]
                      (ignore-task-cancellation error)
                      (pp/commit! (render-animation-state app-db [:button :button-loader]))
                      (blocking-animate-state! app-db [:button :fail-notice])
                      (blocking-animate-state! app-db [:button :fail-init])
                                          
                      (println "ERROR" error)))
    :toggle-should-fail (pipeline! [value app-db]
                          (pp/commit! (update-in app-db [:kv :should-fail?] not)))
    :stop (pipeline! [value app-db]
            (pp/commit! (stop-task! app-db :button)))}))

(def controllers
  {:anim anim-controller})
