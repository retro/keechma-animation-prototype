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
                                             cancel-task!]]))


(def Spring (.-Spring gravitas))

(def start-styles
  {:border-color "#03cd94"
   :background-color "#fff"
   :color "#07ce95"
   :font-size 16
   :width 200
   :height 50
   :margin-left 0
   :margin-top 0
   :text-opacity 1
   :checkmark-dim 0
   :checkmark-pos 23
   :checkmark-opacity 0
   :border-width 2})

(def press-down-styles
  (merge start-styles
         {:width 180
          :height 40
          :font-size 13
          :margin-left 10
          :margin-top 5
          :color "#FFF"
          :background-color "#03cd94"}))

(def button->loader-styles
  (merge start-styles
         {:width 50
          :text-opacity 0
          :margin-left 75
          :border-width 4}))

(def success-start-styles
  (merge start-styles
         {:background-color "#03cd94"
          :text-opacity 0
          :checkmark-opacity 1
          :checkmark-dim 20
          :checkmark-pos 12}))

(def success-end-styles
  (merge success-start-styles
         {:background-color "#FFF"
          :text-opacity 1
          :checkmark-opacity 0}))

(def loader-styles
  {:background-color "#03cd94"
   :margin-left 75
   :spinner-opacity 0
   :rotation 0
   :inner-circle-opacity 1
   :inner-circle-dim 42
   :inner-circle-pos 4
   :checkmark-dim 0
   :checkmark-pos 25
   :width 50})

(def loader-starting-styles
  (merge loader-styles
         {:spinner-opacity 1
          :background-color "#ccc"
          :inner-circle-dim 42
          :inner-circle-pos 4}))

(def loader-button-styles
  (merge loader-starting-styles
         {:background-color "#03cd94"
          :inner-circle-opacity 0
          :spinner-opacity 0
          :checkmark-dim 20
          :width 200
          :checkmark-pos 15
          :margin-left 0}))

(defn start-end-styles [start end]
  (reduce-kv (fn [m k v]
               (assoc m k {:start v :end (get end k)})) {} start))

(defn get-current-styles [value styles]
  (reduce-kv (fn [m k {:keys [start end]}]
               (let [current (cond
                               (= start end) end
                               (string? start) (a/interpolate-color value start (or end start))
                               :else (a/map-value-in-range value start (or end start)))]
                 (assoc m k current)))
             {} styles))

(defn animate-press []
  (let [s (Spring. 0.9 800 40)
        se (start-end-styles start-styles press-down-styles)]
    (.snap s 0)
    (.setEnd s 1)
    (fn [{:keys [id]} app-db]
      (let [value (.x s)
            next-app-db (-> app-db
                            (assoc-in [:kv :animation :value] (get-current-styles value se))
                            (assoc-in [:kv :animation :position] value))]
        (if (not (.done s))
          next-app-db
          (stop-task! next-app-db id))))))

(defn animate-release [app-db]
  (let [s (Spring. 0.9 800 40)
        current (get-in app-db [:kv :animation])
        current-styles (or (:value current) start-styles)
        se (start-end-styles current-styles start-styles)
        position (or (- 1 (:position current)) 0)]
    (.snap s position)
    (.setEnd s 1)
    (fn [{:keys [id]} app-db]
      (let [value (.x s)
            next-app-db (-> app-db
                            (assoc-in [:kv :animation :value] (get-current-styles value se))
                            (assoc-in [:kv :animation :position] value))]
        (if (or (.done s)
                (> value 1.02))
          (stop-task! next-app-db id)
          next-app-db)))))

(defn animate-button-to-loader [app-db]
  (let [s (Spring. 1 400 20)
        se (start-end-styles start-styles button->loader-styles)]
    (.snap s 0)
    (.setEnd s 1)
    (fn [{:keys [id]} app-db]
      (let [value (.x s)
            next-app-db (-> app-db
                            (assoc-in [:kv :animation :value] (get-current-styles value se))
                            (assoc-in [:kv :animation :position] value))]
        (if (or (.done s)
                (> value 1))
          (stop-task! next-app-db id)
          next-app-db
          )))))

(defn animate-loader-to-spinner [app-db]
  (let [s (Spring. 1 800 20)
        se (start-end-styles loader-styles loader-starting-styles)]
    (.snap s 0)
    (.setEnd s 1)
    (fn [{:keys [times-invoked id]} app-db]
      (let [value (.x s)
            next-app-db (-> app-db
                            (assoc-in [:kv :animation :value] (get-current-styles value se))
                            (assoc-in [:kv :animation :position] value)
                            (assoc-in [:kv :animation :value :rotation] times-invoked))]
        (if (.done s)
          (stop-task! next-app-db id)
          next-app-db
          )))))

(defn animate-spinner-to-button [app-db]
  (let [s (Spring. 1 400 40)
        current (get-in app-db [:kv :animation])
        current-styles (or (:value current) loader-starting-styles)
        se (start-end-styles current-styles loader-button-styles)
        position (or (- 1 (:position current)) 0)]
    (.snap s position)
    (.setEnd s 1)
    (fn [{:keys [id]} app-db]
      (let [value (.x s)
            next-app-db (-> app-db
                            (assoc-in [:kv :animation :value] (get-current-styles value se))
                            (assoc-in [:kv :animation :position] value))]
        (if (not (.done s))
          next-app-db
          (stop-task! next-app-db id))))))

(defn animate-success [app-db]
  (let [s (Spring. 1 400 20)
        se (start-end-styles success-start-styles success-end-styles)]
    (.snap s 0)
    (.setEnd s 1)
    (fn [{:keys [times-invoked id]} app-db]
      (let [value (.x s)
            next-app-db (-> app-db
                            (assoc-in [:kv :animation :value] (get-current-styles value se))
                            (assoc-in [:kv :animation :position] value)
                            (assoc-in [:kv :animation :value :rotation] times-invoked))]
        (if (not (.done s))
          next-app-db
          (stop-task! next-app-db id))))))

(defn noop [& args])

(defn delay-pipeline [msec]
  (p/promise (fn [resolve reject]
               (js/setTimeout resolve msec))))

(def anim-controller
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

(def controllers
  {:anim anim-controller})
