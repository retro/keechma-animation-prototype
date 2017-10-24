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
            [animation-keechma.ui.main :as ui-main :refer [animation]]))


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
  ([id state] (make-initial-meta id state nil))
  ([id state prev]
   {:id id :state state :position 0 :times-invoked 0 :prev nil}))

(defn render-animation-state [app-db animation state]
  (let [id (:id animation)]
    (assoc-in app-db [:kv :animations id]
              {:data (get-in animation [:states state :style])
               :meta (make-initial-meta id state)})))

(defn animate-state [app-db animation state]
  (let [id (:id animation)
        prev (get-in app-db [:kv :animations id])
        init-meta (make-initial-meta id state (:meta prev))
        animator (ui-main/animator init-meta (:data prev))]
    (fn [{:keys [times-invoked]} app-db]
      (let [current (get-in app-db [:kv :animations id])
            anim-meta (if (zero? times-invoked) init-meta)
            next-data (ui-main/step )]))))

(def anim-controller
  (pp-controller/constructor
   (fn [params] true)
   {:start (pipeline! [value app-db]
             (println "STARTING")
             (pp/commit! (render-animation-state app-db animation :init))
             (rescue! [error]
               (println "START ERROR" error)))
    :animate-press (pipeline! [value app-db]
                     (animate-state app-db animation :pressed)
                     ;;(blocking-animation! (animate-state app-db animation :pressed))
                     (rescue! [error]
                       (println "ANIMATE PRESS ERROR" error)))}))

(def controllers
  {:anim anim-controller})
