(ns animation-keechma.tasks
  (:require [cljs.core.async :refer [<! put! take! chan close!]]
            [medley.core :refer [dissoc-in]]
            [promesa.core :as p])
  (:import [goog.async AnimationDelay])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

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
                  (put! res-chan {:width (.-innerWidth js/window)
                                  :height (.-innerHeight js/window)}))]
    (.addEventListener js/window "resize" handler)
    #(.removeEventListener js/window "resize" handler)))

(defn window-mouse-move-runner [res-chan]
  (let [handler #(put! res-chan {:x (.-screenX %)  :y (.-screenY %)})]
    (.addEventListener js/window "mousemove" handler)
    #(.removeEventListener js/window "mousemove" handler)))

(defn register-task
  ([app-db id runner-chan] (register-task app-db id runner-chan ::running))
  ([app-db id runner-chan state]
   (let [version (gensym id)]
     (-> app-db
         (assoc-in [:kv ::tasks id :version] version)
         (assoc-in [:kv ::tasks id :states version] state)
         (assoc-in [:kv ::tasks id :chans version] runner-chan)))))

(defn update-task-state [state]
  (fn [app-db id]
    (let [current (get-in app-db [:kv ::tasks id])
          version (:version current)
          states (:states current)
          current-state (get states version)
          runner-chan (get-in app-db [:kv ::tasks id :chans version])]
      (when (and runner-chan
                 (or (= ::stopped state)
                     (= ::cancelled state)))
        (close! runner-chan))
      (assoc-in app-db [:kv ::tasks id :states version] state))))

(def stop-task! (update-task-state ::stopped))

(def cancel-task! (update-task-state ::cancelled))

(defn clear-task-version [app-db id version]
  (-> app-db
      (dissoc-in [:kv ::tasks id :states version])
      (dissoc-in [:kv ::tasks id :chans version])))

(defn update-app-db-atom! [payload app-db-atom]
  (if (nil? payload)
    app-db-atom
    (reset! app-db-atom payload)))

(defn ex-task-cancelled [id version]
  (ex-info "Task cancelled" {::task {:id id :version version :state ::cancelled}}))

(defn task-loop [{:keys [args producer reducer ctrl app-db-atom value resolve reject id]}]
  (let [runner-chan (chan)]
    (reset! app-db-atom (register-task @app-db-atom id runner-chan))
    (let [started-at (.getTime (js/Date.))
          runner-cancel (producer runner-chan)
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
      (go-loop [times-invoked 0]
        (let [runner-value (<! runner-chan)
              current (get-in @app-db-atom [:kv ::tasks id])
              current-version (:version current)
              state (get-in current [:states version])]
          (cond
            (and (= ::running state) (not= version current-version)) (task-reject (ex-task-cancelled id version))
            (= ::cancelled state) (task-reject (ex-task-cancelled id version))
            (= ::stopped state) (task-resolve)
            :else
            
            (when runner-value
              (let [new-app-db (reducer {:times-invoked times-invoked :started-at started-at :id id :value runner-value} @app-db-atom)]
                (reset! app-db-atom new-app-db)
                (recur (inc times-invoked))))))))))


(defn blocking-task-runner [producer id reducer ctrl app-db-atom value]
  (p/promise (fn [resolve reject]
               (task-loop {:reducer reducer
                           :producer producer
                           :ctrl ctrl
                           :app-db-atom app-db-atom
                           :value value
                           :resolve resolve
                           :reject reject
                           :id id}))))

(defn blocking-task! [producer id reducer]
  (with-meta (partial blocking-task-runner producer id reducer) {:pipeline? true}))

(defn non-blocking-task-runner [producer id reducer ctrl app-db-atom value]
  (task-loop {:reducer reducer
              :producer producer
              :ctrl ctrl
              :app-db-atom app-db-atom
              :value value
              :resolve identity
              :reject identity
              :id id})
  nil)

(defn non-blocking-task! [producer id reducer]
  (with-meta (partial non-blocking-task-runner producer id reducer) {:pipeline? true}))


(def blocking-animation! (partial blocking-task! raf-runner))
(def non-blocking-animation! (partial non-blocking-task! raf-runner))
