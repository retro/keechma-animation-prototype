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

;; (.config Promise #js {:cancellation true})
;; (def calc (spring/make-calculator {:tension 40 :friction 7 :overshoot-clamping? false}))
;; (def calc-2 (spring/make-calculator {:tension 100 :friction 7}))


;; (def anim-path [:kv :anim-state])

;; (defn raf-promise []
;;   (p/promise (fn [resolve reject]
;;                (let [anim-delay (AnimationDelay. resolve)]
;;                  (.start anim-delay)))))

;; (def id-key :keechma.toolbox.animation/id)

;; (defn reverse-position [anim-state]
;;   (when anim-state
;;     (-> anim-state
;;         (update :position #(.abs js/Math (- % 1))))))

;; (defn calculate-frames [{:keys [calculator values]} forward? current-state]
;;   (let [values (if forward? values (reduce-kv (fn [m k v] (assoc m k (reverse v))) {} values))
;;         state (get current-state :keechma.toolbox/anim-state)
;;         start-state (if (= forward? (:forward? state)) state (reverse-position state))]
;;     (vec (spring/calculate-frames calculator values start-state))))

;; (defn add-start-frames [animation frame-count]
;;   (let [frames (:frames animation)
;;         repeating-frame (assoc-in (first frames) [:keechma.toolbox/anim-state :placeholder?] true)
;;         frames-with-delay (into (vec (repeat frame-count repeating-frame)) frames)]
;;     (assoc animation :frames frames-with-delay)))

;; (defn add-end-frames [animation frame-count]
;;   (let [frames (:frames animation)
;;         repeating-frame (assoc-in (last frames) [:keechma.toolbox/anim-state :placeholder?] true)
;;         frames-with-delay (into frames (repeat frame-count repeating-frame))]
;;     (assoc animation :frames frames-with-delay)))

;; (defn calculate-forward-delay [prev-animation delay] 
;;   (if (= 0 delay)
;;     delay
;;     (let [max-idx (dec (count (:frames prev-animation)))]
;;       (loop [frame-idx 0]
;;         (let [frame-position (get-in prev-animation [:frames frame-idx :keechma.toolbox/anim-state :position])]
;;           (if (or (= max-idx frame-idx)
;;                   (>= frame-position delay))
;;             frame-idx
;;             (recur (inc frame-idx))))))))

;; (defn add-forward-delay [animations]
;;   (loop [animations animations
;;          with-delay []
;;          idx 0]
;;     (if (empty? animations)
;;       with-delay
;;       (if (= idx 0)
;;         (recur (rest animations)
;;                (conj with-delay (first animations))
;;                (inc idx))
;;         (let [current-animation (first animations)
;;               delay (calculate-forward-delay (last with-delay) (or (:delay current-animation) 0))]
;;           (recur (rest animations)
;;                  (conj with-delay (add-start-frames current-animation delay))
;;                  (inc idx)))))))

;; (defn add-back-delay [animations]
;;   (loop [animations animations
;;          with-delay []
;;          idx 0]
;;     (if (empty? animations)
;;       with-delay
;;       (if (= idx 0)
;;         (recur (rest animations)
;;                (conj with-delay (first animations))
;;                (inc idx))
;;         (let [current-animation (first animations)
;;               prev-animation (last with-delay)
;;               prev-delay (:delay prev-animation)]
;;           (if (zero? prev-delay)
;;             (recur (rest animations)
;;                    (conj with-delay current-animation)
;;                    (inc idx))
;;             (let [current-frames (:frames current-animation)
;;                   first-position (get-in current-frames [0 :keechma.toolbox/anim-state :position])
;;                   delay (* (- 1 first-position)
;;                            (- (count (:frames prev-animation))
;;                               (- (count current-frames)
;;                                  (calculate-forward-delay current-animation prev-delay))))]
;;               (recur (rest animations)
;;                      (conj with-delay (add-start-frames current-animation delay))
;;                      (inc idx)))))))))

;; (defn add-delay [animations forward?]
;;   (if forward?
;;     (add-forward-delay animations)
;;     (add-back-delay animations)))

;; (defn normalize-frames [animations forward?]
;;   (let [max-frames-count (apply max (map #(count (:frames %)) animations))]
;;     (map (fn [animation]
;;            (let [frames (:frames animation)
;;                  frames-count (count frames)]
;;              (if (not= max-frames-count frames-count)
;;                (add-end-frames animation (- max-frames-count frames-count)) 
;;                animation))) animations)))

;; (defn compute-frames [app-db id forward? animations]
;;   (let [current-state (vec (get-in app-db [:kv id-key id :values]))
;;         prev-forward? (get-in current-state [0 :keechma.toolbox/anim-state :forward?])
;;         same-direction? (or (nil? prev-forward?) (= forward? prev-forward?))
;;         current-state (vec (if same-direction? current-state (reverse current-state)))
;;         animations-with-frames
;;         (vec (map-indexed (fn [idx animation]
;;                             (assoc animation :frames (calculate-frames animation forward? (get current-state idx))))
;;                           (if forward? animations (reverse animations))))]
;;     (-> animations-with-frames
;;         (add-delay forward?)
;;         (normalize-frames forward?)
;;         )))

;; (defn get-values-for-frame [animations forward? frame]
;;   (mapv (fn [animation]
;;           (assoc-in (get-in animation [:frames frame])
;;                     [:keechma.toolbox/anim-state :forward?] forward?))
;;         animations))


;; (def next-version!
;;   ((fn []
;;       (let [version-counter (atom 0)]
;;         (fn []
;;           (swap! version-counter inc)
;;           @version-counter)))))

;; (defn log-frames [animations]
;;   (println
;;    (clojure.string/join
;;     "\n" (map (fn [f]
;;                 (clojure.string/join
;;                  "" (map (fn [ff] (if (get-in ff [:keechma.toolbox/anim-state :placeholder?]) "." "*")) f)))
;;               (map :frames animations)))))

;; (defn play-animation! [app-db id forward? animations]
;;   (let [normalized (compute-frames app-db id forward? animations)
;;         final-frame-count (count (:frames (first normalized)))
;;         version (next-version!)
;;         commit-frame (fn [i]
;;                        (fn [value app-db]
;;                          (when (>= version (get-in app-db [:kv id-key id :version]))
;;                            (pp/commit! (assoc-in app-db [:kv id-key id]
;;                                                  {:version version
;;                                                   :values (get-values-for-frame normalized forward? i)})))))
;;         check-version! (fn [value app-db]
;;                          (when (not= version (get-in app-db [:kv id-key id :version]))
;;                            (throw (ex-info "New animation version" {:new-animation-version? true}))))]
;;     (pp/make-pipeline
;;      {:begin (reduce (fn [acc i]
;;                        (if (= 0 i)
;;                          (conj acc (commit-frame i))
;;                          (concat acc [check-version! raf-promise check-version! (commit-frame i)])))
;;                      [] (range 0 final-frame-count))
;;       :rescue [(fn [value app-db error]
;;                  (when-not (= {:new-animation-version? true} (.-data (:payload error)))
;;                    (throw error)))]})))

;; (defn render-first-frame [app-db id forward? animations]
;;   (let [normalized (compute-frames app-db id forward? animations)]
;;     (assoc-in app-db [:kv id-key id :values] (get-values-for-frame normalized forward? 0))))

;; (def animations
;;   [{:values {:margin-left [0 200]
;;              :border-radius [0 20]
;;              :background-color ["#ebf230" "#ef7204"]
;;              :rotation [0 360]
;;              :width [40 40]}
;;     :calculator calc}
;;    {:values {:width [40 200]
;;              :rotation [360 360]
;;              :margin-left [200 200]
;;              }
;;     :delay 1
;;     :calculator calc}
;;    {:values {:width [200 40]
;;              :rotation [360 360]
;;              :margin-left [200 360]
;;              :background-color ["#ef7204" "#fff"]}
;;     :delay 1
;;     :calculator calc}
;;    {:values {:width [40 40]
;;              :margin-left [360 700]
;;              :rotation [360 720]
;;              :border-radius [20 0]
;;              :background-color ["#fff" "#3ac7ff"]}
;;     :delay 1
;;     :calculator calc}])

;; (def anim-controller
;;   (pp-controller/constructor
;;    (fn [] 
;;      true)
;;    {:start (pipeline! [value app-db]
;;              (pp/commit! (render-first-frame app-db :animation true animations))) 
;;     :animation (pipeline! [value app-db]
;;                  (play-animation! app-db :animation (= :forward value) animations))}))


(defn collect-frames [timeline]
  (loop [frames []
         idx 0]
    (let [next-frame (timeline idx)
          updated-frames (conj frames next-frame)]
      (if (:done? next-frame)
        updated-frames
        (recur updated-frames (inc idx))))))

(defn raf-chan []
  (let [c (chan)
        animation-delay (AnimationDelay. #(close! c))]
    (.start animation-delay)
    c))

(defn task-action! [action]
  (fn run-action!
    ([] (run-action! nil))
    ([payload]
     {:action action
      :payload payload})))

(def stop! (task-action! ::stop))
(def cancel! (task-action! ::cancel))
(def continue! (task-action! ::continue))

(def allowed-actions #{::stop ::cancel ::continue})

(defn register-task
  ([app-db id] (register-task app-db id ::running))
  ([app-db id status]
   (assoc-in app-db [:kv ::tasks id] {:status ::running
                                      :version (gensym id)})))

(defn stop-task [app-db id]
  (if (= ::running (get-in app-db [:kv ::tasks id :status]))
    (assoc-in app-db [:kv ::tasks id :status] ::stopped)
    app-db))

(defn cancel-task [app-db id]
  (if (= ::running (get-in app-db [:kv ::tasks id :status]))
    (assoc-in app-db [:kv ::tasks id :status] ::cancelled)
    app-db))

(defn update-app-db-atom! [payload app-db-atom]
  (if (nil? payload)
    app-db-atom
    (reset! app-db-atom payload)))

(defn ex-task-cancelled []
  (ex-info "Task cancelled" {::task-action :cancel}))


(defn task-loop [{:keys [args producer-fn ctrl app-db-atom value resolve reject id]}]
  (reset! app-db-atom (register-task @app-db-atom id))
  (let [started-at(.getTime (js/Date.))
        current-version (get-in @app-db-atom [:kv ::tasks id :version])
        task-reject (fn [payload]
                      (reset! app-db-atom (cancel-task @app-db-atom id))
                      (reject payload))
        task-resolve (fn []
                       (reset! app-db-atom (stop-task @app-db-atom id))
                       (resolve))]
    (go-loop [frame-count 0]
      (let [current-state (get-in @app-db-atom [:kv ::tasks id])]
        (cond
          (not= (:version current-state) current-version) (reject (ex-task-cancelled))
          (= ::cancelled (:status current-state)) (task-reject (ex-task-cancelled))
          (= ::stopped (:status current-state)) (task-resolve)
          :else

          (let [{:keys [action payload]} (producer-fn {:frame-count frame-count :started-at started-at} @app-db-atom)]
            (when (not (contains? allowed-actions action))
              (reject (ex-info "Task producer must wrap it's return value with an action" {})))

            (update-app-db-atom! payload app-db-atom)

            (case action
              ::cancel (task-reject (ex-task-cancelled))
              ::stop (task-resolve)
              ::continue (do
                           (<! (raf-chan))
                           (recur (inc frame-count)))
              nil)))))))



(defn blocking-task-runner [id producer-fn ctrl app-db-atom value]
  (p/promise (fn [resolve reject]
               (task-loop {:producer-fn producer-fn
                           :ctrl ctrl
                           :app-db-atom app-db-atom
                           :value value
                           :resolve resolve
                           :reject reject
                           :id id}))))

(defn blocking-task! [id producer-fn]
  (with-meta (partial blocking-task-runner id producer-fn) {:pipeline? true}))

(defn non-blocking-task-runner [id producer-fn ctrl app-db-atom value]
  (task-loop {:producer-fn producer-fn
              :ctrl ctrl
              :app-db-atom app-db-atom
              :value value
              :resolve identity
              :reject identity
              :id id})
  nil)

(defn non-blocking-task! [id producer-fn]
  (with-meta (partial non-blocking-task-runner id producer-fn) {:pipeline? true}))

(def Spring (.-Spring gravitas))

(defn animate-frame-count []
  (let [s (Spring. 1 200 20)]
    (.snap s 0)
    (.setEnd s 1)
    (fn [{:keys [frame-count]} app-db]
      (let [value (.x s)
            next-app-db (assoc-in app-db [:kv :animation]
                                  {:width (a/map-value-in-range value 50 500)
                                   :radius (a/map-value-in-range value 0 25)
                                   :background-color (a/interpolate-color value "#ff3300" "#666")})]
        (if (not (.done s))
          (continue! next-app-db)
          (stop! next-app-db))))))

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
                               (string? start) (a/interpolate-color value start end)
                               :else (a/map-value-in-range value start end))]
                 (assoc m k current)))
             {} styles))

(defn animate-press []
  (let [s (Spring. 0.9 800 40)
        se (start-end-styles start-styles press-down-styles)]
    (.snap s 0)
    (.setEnd s 1)
    (fn [_ app-db]
      (let [value (.x s)
            next-app-db (-> app-db
                            (assoc-in [:kv :animation :value] (get-current-styles value se))
                            (assoc-in [:kv :animation :position] value))]
        (if (not (.done s))
          (continue! next-app-db)
          (stop! next-app-db))))))

(defn animate-release [app-db]
  (let [s (Spring. 0.9 800 40)
        current (get-in app-db [:kv :animation])
        current-styles (or (:value current) start-styles)
        se (start-end-styles current-styles start-styles)
        position (or (- 1 (:position current)) 0)]
    (.snap s position)
    (.setEnd s 1)
    (fn [_ app-db]
      (let [value (.x s)
            next-app-db (-> app-db
                            (assoc-in [:kv :animation :value] (get-current-styles value se))
                            (assoc-in [:kv :animation :position] value))]
        (if (or (.done s)
                (> value 1.02))
          (stop! next-app-db)
          (continue! next-app-db))))))

(defn animate-button-to-loader [app-db]
  (let [s (Spring. 1 400 20)
        se (start-end-styles start-styles button->loader-styles)]
    (.snap s 0)
    (.setEnd s 1)
    (fn [_ app-db]
      (let [value (.x s)
            next-app-db (-> app-db
                            (assoc-in [:kv :animation :value] (get-current-styles value se))
                            (assoc-in [:kv :animation :position] value))]
        (if (or (.done s)
                (> value 1))
          (stop! next-app-db)
          (continue! next-app-db)
          )))))

(defn animate-loader-to-spinner [app-db]
  (let [s (Spring. 1 800 20)
        se (start-end-styles loader-styles loader-starting-styles)]
    (.snap s 0)
    (.setEnd s 1)
    (fn [{:keys [frame-count]} app-db]
      (let [value (.x s)
            next-app-db (-> app-db
                            (assoc-in [:kv :animation :value] (get-current-styles value se))
                            (assoc-in [:kv :animation :position] value)
                            (assoc-in [:kv :animation :value :rotation] frame-count))]
        (if (.done s)
          (stop! next-app-db)
          (continue! next-app-db)
          )))))

(defn animate-spinner-to-button [app-db]
  (let [s (Spring. 1 400 40)
        current (get-in app-db [:kv :animation])
        current-styles (or (:value current) loader-starting-styles)
        se (start-end-styles current-styles loader-button-styles)
        position (or (- 1 (:position current)) 0)]
    (.snap s position)
    (.setEnd s 1)
    (fn [_ app-db]
      (let [value (.x s)
            next-app-db (-> app-db
                            (assoc-in [:kv :animation :value] (get-current-styles value se))
                            (assoc-in [:kv :animation :position] value))]
        (if (not (.done s))
          (continue! next-app-db)
          (stop! next-app-db))))))

(defn animate-success [app-db]
  (let [s (Spring. 1 400 20)
        se (start-end-styles success-start-styles success-end-styles)]
    (.snap s 0)
    (.setEnd s 1)
    (fn [{:keys [frame-count]} app-db]
      (let [value (.x s)
            next-app-db (-> app-db
                            (assoc-in [:kv :animation :value] (get-current-styles value se))
                            (assoc-in [:kv :animation :position] value)
                            (assoc-in [:kv :animation :value :rotation] frame-count))]
        (if (not (.done s))
          (continue! next-app-db)
          (stop! next-app-db))))))

(defn noop [& args])

(defn delay-pipeline [msec]
  (p/promise (fn [resolve reject]
               (js/setTimeout resolve msec))))

(def anim-controller
  (pp-controller/constructor
   (fn [] true)
   {:start (pipeline! [value app-db]
             (pp/commit! (stop-task app-db :animate-button))
             (pp/commit! (assoc-in app-db [:kv :animation]
                                   {:id :button-start
                                    :position 0
                                    :value start-styles})))

    :animate-press (pipeline! [value app-db]
                     (pp/commit! (stop-task app-db :animate-button))
                     (blocking-task! :animate-button (animate-press))
                     (rescue! [error]
                       (noop)))
    :animate-submit (pipeline! [value app-db]
                      (pp/commit! (stop-task app-db :animate-button))
                      (blocking-task! :animate-button (animate-release app-db))
                      (blocking-task! :animate-button (animate-button-to-loader app-db))
                      (pp/commit! (assoc-in app-db [:kv :animation] 
                                            {:id :spinner-start
                                             :position 0
                                             :value loader-styles}))
                      (blocking-task! :animate-button (animate-loader-to-spinner app-db))
                      (non-blocking-task! :animate-button
                                          (fn [{:keys [frame-count]} app-db]
                                            (continue! (update-in app-db [:kv :animation :value :rotation] inc))))
                      (delay-pipeline (* 3 (rand-int 500)))
                      (pp/execute! :finish-loading true)
                      (rescue! [error]
                        (println error)))
    :finish-loading (pipeline! [value app-db]
                      (pp/commit! (stop-task app-db :animate-button))
                      (blocking-task! :animate-button (animate-spinner-to-button app-db))
                      (pp/commit! (assoc-in app-db [:kv :animation]
                                            {:id :button-start
                                             :position 0
                                             :value success-start-styles}))
                      (blocking-task! :animate-button (animate-success app-db))
                      (rescue! [error]
                        (println error)))}))

(def controllers
  {:anim anim-controller})
