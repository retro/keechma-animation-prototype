(ns animation-keechma.controllers
  (:require [keechma.toolbox.pipeline.controller :as pp-controller]
            [keechma.toolbox.pipeline.core :as pp :refer-macros [pipeline!]]
            [cljs.core.async :refer [<! put! take! chan close!]]
            [promesa.core :as p]
            [promesa.impl :refer [Promise]]
            [animation-keechma.spring :as spring :refer [make-calculator]]
            [animation-keechma.animation.core :as a]
            ["gravitas/src/index" :as gravitas])
  (:import [goog.async AnimationDelay])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))


(defn collect-frames [timeline]
  (loop [frames []
         idx 0]
    (let [next-frame (timeline idx)
          updated-frames (conj frames next-frame)]
      (if (:done? next-frame)
        updated-frames
        (recur updated-frames (inc idx))))))

(defn raf-runner [res-chan]
  (let [is-running? (atom true)
        wait-delay (fn wait-delay []
                     (.start (AnimationDelay.
                              (fn [val]
                                (put! res-chan val)
                                (when @is-running? (wait-delay))))))]
    (wait-delay)
    #(reset! is-running? false)))

(defn window-resize-runner [res-chan]
  (let [handler (fn [e]
                  (println "HANDLER")
                  (put! res-chan {:width (.-innerWidth js/window)
                                  :height (.-innerHeight js/window)}))]
    (.addEventListener js/window "resize" handler)
    #(.removeEventListener js/window "resize" handler)))

(defn register-task
  ([app-db id] (register-task app-db id ::running))
  ([app-db id state]
   (let [version (gensym id)
         current (get-in app-db [:kv ::tasks id])
         states (or (:states current) {})
         new {:version version
              :states (assoc states version state)}]
     (assoc-in app-db [:kv ::tasks id] new))))

(defn update-task-state [state]
  (fn [app-db id]
    (let [current (get-in app-db [:kv ::tasks id])
          version (:version current)
          states (:states current)
          current-state (get states version)]
      (if (= ::running current-state)
        (assoc-in app-db [:kv ::tasks id :states]
                  (assoc states version state))
        app-db))))

(def stop-task (update-task-state ::stopped))

(def cancel-task (update-task-state ::cancelled))

(defn clear-task-version [app-db id version]
  (let [new-states (dissoc (get-in app-db [:kv ::tasks id :states]) version)]
    (assoc-in app-db [:kv ::tasks id :states] new-states)))

(defn update-app-db-atom! [payload app-db-atom]
  (if (nil? payload)
    app-db-atom
    (reset! app-db-atom payload)))

(defn ex-task-cancelled [version]
  (ex-info "Task cancelled" {::task-action :cancel ::task-version version}))

(defn task-loop [{:keys [args runner-fn producer-fn ctrl app-db-atom value resolve reject id]}]
  (reset! app-db-atom (register-task @app-db-atom id))
  (let [started-at (.getTime (js/Date.))
        runner-chan (chan)
        runner-cancel (runner-fn runner-chan)
        version (get-in @app-db-atom [:kv ::tasks id :version])
        task-reject (fn [payload]
                      (close! runner-chan)
                      (runner-cancel)
                      (reset! app-db-atom (clear-task-version @app-db-atom id version))
                      (reject payload))
        task-resolve (fn []
                       (close! runner-chan)
                       (runner-cancel)
                       (reset! app-db-atom (clear-task-version @app-db-atom id version))
                       (resolve))]
    (go-loop [frame-count 0]
      (let [runner-value (<! runner-chan)
            current (get-in @app-db-atom [:kv ::tasks id])
            current-version (:version current)
            state (get-in current [:states version])]
        (cond
          (and (= ::running state) (not= version current-version)) (task-reject (ex-task-cancelled version))
          (= ::cancelled state) (task-reject (ex-task-cancelled version))
          (= ::stopped state) (task-resolve)
          :else
          
          (when runner-value
            (let [new-app-db (producer-fn {:frame-count frame-count :started-at started-at :id id :value runner-value} @app-db-atom)]
              (reset! app-db-atom new-app-db)
              (recur (inc frame-count)))))))))



(defn blocking-task-runner [runner-fn id producer-fn ctrl app-db-atom value]
  (p/promise (fn [resolve reject]
               (task-loop {:producer-fn producer-fn
                           :runner-fn runner-fn
                           :ctrl ctrl
                           :app-db-atom app-db-atom
                           :value value
                           :resolve resolve
                           :reject reject
                           :id id}))))

(defn blocking-task! [runner-fn id producer-fn]
  (with-meta (partial blocking-task-runner runner-fn id producer-fn) {:pipeline? true}))

(defn non-blocking-task-runner [runner-fn id producer-fn ctrl app-db-atom value]
  (task-loop {:producer-fn producer-fn
              :runner-fn runner-fn
              :ctrl ctrl
              :app-db-atom app-db-atom
              :value value
              :resolve identity
              :reject identity
              :id id})
  nil)

(defn non-blocking-task! [runner-fn id producer-fn]
  (with-meta (partial non-blocking-task-runner runner-fn id producer-fn) {:pipeline? true}))



(def blocking-animation! (partial blocking-task! raf-runner))
(def non-blocking-animation! (partial non-blocking-task! raf-runner))


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
          (stop-task next-app-db id))))))

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
          (stop-task next-app-db id)
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
          (stop-task next-app-db id)
          next-app-db
          )))))

(defn animate-loader-to-spinner [app-db]
  (let [s (Spring. 1 800 20)
        se (start-end-styles loader-styles loader-starting-styles)]
    (.snap s 0)
    (.setEnd s 1)
    (fn [{:keys [frame-count id]} app-db]
      (let [value (.x s)
            next-app-db (-> app-db
                            (assoc-in [:kv :animation :value] (get-current-styles value se))
                            (assoc-in [:kv :animation :position] value)
                            (assoc-in [:kv :animation :value :rotation] frame-count))]
        (if (.done s)
          (stop-task next-app-db id)
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
          (stop-task next-app-db id))))))

(defn animate-success [app-db]
  (let [s (Spring. 1 400 20)
        se (start-end-styles success-start-styles success-end-styles)]
    (.snap s 0)
    (.setEnd s 1)
    (fn [{:keys [frame-count id]} app-db]
      (let [value (.x s)
            next-app-db (-> app-db
                            (assoc-in [:kv :animation :value] (get-current-styles value se))
                            (assoc-in [:kv :animation :position] value)
                            (assoc-in [:kv :animation :value :rotation] frame-count))]
        (if (not (.done s))
          next-app-db
          (stop-task next-app-db id))))))

(defn noop [& args])

(defn delay-pipeline [msec]
  (p/promise (fn [resolve reject]
               (js/setTimeout resolve msec))))

(def anim-controller
  (pp-controller/constructor
   (fn [] true)
   {:start (pipeline! [value app-db]
             (pp/commit! (cancel-task app-db :animate-button))
             (pp/commit! (cancel-task app-db :window-resize))
             (non-blocking-task! window-resize-runner :window-resize
                                 (fn [{:keys [value]} app-db]
                                   (println value)
                                   app-db))
             (pp/commit! (assoc-in app-db [:kv :animation]
                                   {:id :button-start
                                    :position 0
                                    :value start-styles})))
    :stop (pipeline! [value app-db]
            (pp/commit! (cancel-task app-db :animate-button)))
    :animate-press (pipeline! [value app-db]
                     (pp/commit! (cancel-task app-db :animate-button))
                     (blocking-animation! :animate-button (animate-press))
                     (rescue! [error]
                       (println error)))
    :animate-submit (pipeline! [value app-db]
                      (pp/commit! (stop-task app-db :animate-button))
                      (blocking-animation! :animate-button (animate-release app-db))
                      (blocking-animation! :animate-button (animate-button-to-loader app-db))
                      (pp/commit! (assoc-in app-db [:kv :animation] 
                                            {:id :spinner-start
                                             :position 0
                                             :value loader-styles}))
                      (blocking-animation! :animate-button (animate-loader-to-spinner app-db))
                      (non-blocking-animation! :animate-button
                                          (fn [{:keys [frame-count]} app-db]
                                            (update-in app-db [:kv :animation :value :rotation] inc)))
                      (delay-pipeline (* 3 (rand-int 500)))
                      (pp/execute! :finish-loading true)
                      (rescue! [error]
                        (println error)))
    :finish-loading (pipeline! [value app-db]
                      (pp/commit! (stop-task app-db :animate-button))
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
              (pp/commit! (stop-task app-db :window-resize)))}))

(def controllers
  {:anim anim-controller})
