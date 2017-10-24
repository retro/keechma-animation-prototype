(ns animation-keechma.ui.main
  (:require [keechma.ui-component :as ui]
            [keechma.toolbox.ui :refer [sub> <cmd]]
            [garden.color :refer [hex?]]
            [garden.units :as units]
            ["gravitas/src/index" :as gravitas]
            [animation-keechma.animation.animator :as animator]))

(def Spring (.-Spring gravitas))

(extend-type Spring
  animator/IAnimator
  (position [this]
    (.x this))
  (done? [this]
    (.done this)))

(defrecord FrameAnimator []
  animator/IAnimator
  (position [this]
    0)
  (done? [this]
    false))

(def spring-default-config
  {:weight 1
   :spring 400
   :damping 20
   :snap 0
   :end 1})

(defn make-spring
  ([] (make-spring {}))
  ([user-config]
   (let [config (merge spring-default-config user-config)]
     (doto (Spring. (:weight config) (:spring config) (:damping config))
       (.snap (:snap config))
       (.setEnd (:end config))))))

(defn dispatcher [meta & args] [(:id meta) (:state meta)])


(defmulti animator dispatcher)
(defmulti step dispatcher)
(defmulti done? dispatcher)
(defmulti values dispatcher)

(defmethod animator :default [_ _]
  (animator/->DefaultAnimator))

(defmethod done? :default [meta animator]
  (animator/done? animator))

(defmethod step :default [meta data]
  data)

(defmethod values :default [meta]
  {})

(defmethod values [:button :init] [_]
  {:border-radius    "25px"
   :height           "50px"
   :width            "200px"
   :border-width     "2px"
   :border-style     "solid"
   :border-color     "#03cd94"
   :color            "#07ce95"
   :background-color "#fff"
   :font-size        "16px"
   :cursor           "pointer"
   :outline          "none"})

(defmethod animator [:button :init] [meta data]
  (let [prev (:prev meta)]
    (if (= :pressed (:state prev))
      (make-spring {:position (- 1 (:position prev)) :weight 0.9 :spring 800 :damping 40})
      (make-spring))))

(defmethod done? [:button :init] [meta animator]
  (or (animator/done? animator)
      (> (:position meta) 1.02)))

(defmethod values [:button :pressed] [_]
  {:border-radius    "25px"
   :height           "44px"
   :width            "180px"
   :border-width     "2px"
   :border-style     "solid"
   :border-color     "#03cd94"
   :color            "#fff"
   :background-color "#03cd94"
   :font-size        "14px"
   :cursor           "pointer"
   :outline          "none"})

(defmethod animator [:button :pressed] [meta data]
  (make-spring {:weight 0.9 :spring 800 :damping 40}))

(defmethod done? [:button :pressed] [meta animator]
  (or (animator/done? animator)
      (> (:position meta) 1.02)))


(defmethod values [:button :button-loader] [_]
  {:border-radius    "25px"
   :height           "50px"
   :width            "50px"
   :border-width     "4px"
   :border-style     "solid"
   :border-color     "#ccc"
   :color            "#fff"
   :background-color "#fff"
   :font-size        "14px"
   :cursor           "pointer"
   :outline          "none"})

(defmethod animator [:button :button-loader] [_]
  (make-spring {:spring 800 :damping 20}))

(defmethod done? [:button :button-loader] [meta animator]
  (or (animator/done? animator)
      (> (:position meta) 1)))


(defmethod animator [:button :loader] [_]
  (->FrameAnimator))

(defmethod step [:button :loader] [meta data]
  {:rotation (str "rotate(" (* 5 (:times-invoked meta)) "deg)")})

(defmethod values [:button :success-notice] [_]
  {:border-radius    "25px"
   :height           "50px"
   :width            "200px"
   :border-width     "2px"
   :border-style     "solid"
   :border-color     "#03cd94"
   :color            "#fff"
   :background-color "#03cd94"
   :font-size        "14px"
   :cursor           "pointer"
   :outline          "none"})

(defmethod animator [:button :success-notice] [_]
  (make-spring {:damping 40}))

(defmethod values [:button :fail-notice] [_]
  {:border-radius    "25px"
   :height           "50px"
   :width            "260px"
   :border-width     "4px"
   :border-style     "solid"
   :border-color     "#ff3300"
   :color            "#fff"
   :background-color "#ff3300"
   :font-size        "14px"
   :cursor           "pointer"
   :outline          "none"})

(defmethod animator [:button :fail-notice] [_]
  (make-spring {:damping 40}))

(defmethod values [:button :fail-init] [_]
  {:border-radius    "25px"
   :height           "50px"
   :width            "260px"
   :border-width     "2px"
   :border-style     "solid"
   :border-color     "#ff3300"
   :color            "#ff3300"
   :background-color "#fff"
   :font-size        "16px"
   :cursor           "pointer"
   :outline          "none"})

(defmethod animator [:button :fail-init] [meta data]
  (let [prev (:prev meta)]
    (if (= :pressed (:state prev))
      (make-spring {:position (- 1 (:position prev)) :weight 0.9 :spring 800 :damping 40})
      (make-spring))))

(defmethod done? [:button :fail-init] [meta animator]
  (or (animator/done? animator)
      (> (:position meta) 1.02)))


(defmethod values [:button :fail-pressed] [_]
  {:border-radius    "25px"
   :height           "44px"
   :width            "240px"
   :border-width     "2px"
   :border-style     "solid"
   :border-color     "#ff3300"
   :color            "#fff"
   :background-color "#ff3300"
   :font-size        "14px"
   :cursor           "pointer"
   :outline          "none"})

(defmethod animator [:button :fail-pressed] [meta data]
  (make-spring {:weight 0.9 :spring 800 :damping 40}))

(defmethod done? [:button :fail-pressed] [meta animator]
  (or (animator/done? animator)
      (> (:position meta) 1.02)))


(defn render-button [ctx anim]
  (let [state (get-in anim [:meta :state])]
    [:button {:style (:data anim)
              :on-mouse-down #(<cmd ctx :animate-press nil)
              :on-mouse-up #(<cmd ctx :animate-load nil)}
     (when (contains? #{:init :pressed} state)
       [:div {:style {:opacity 1}} "Submit"])
     (when (contains? #{:fail-init :fail-pressed} state)
       [:div {:style {:opacity 1}} "Submit Failed. Try Again"])
     (when (= :success-notice state)
       [:div {:style {:opacity 1}} "✔"])
     (when (= :fail-notice state)
       [:div {:style {:opacity 1}} "✘"])]))

(defn render-loader [ctx anim]
  [:div {:style {:border-radius "25px"
                 :height "50px"
                 :width "50px"
                 :display "flex"
                 :background "#ccc"
                 :overflow "hidden"
                 :justify-content "center"
                 :align-items "center"
                 :position "relative"}}
   [:div {:style {:background "#03cd94"
                  :width "50px"
                  :height "25px"
                  :margin-left "-25px"
                  :position "absolute"
                  :transform (get-in anim [:data :rotation])
                  :transform-origin "100% 50%"}}]
   [:div {:style {:border-radius "25px"
                  :background-color "white"
                  :width "42px"
                  :height "42px"
                  :position "relative"}}]])



(defn render [ctx]
  (let [anim (sub> ctx :animation)
        anim-state (get-in anim [:meta :state])] 
    [:div {:style {:box-sizing "border-box"}} "Foo - "
     [:div
      [:button {:on-click #(<cmd ctx :stop-animation true)} "Stop"]
      [:button {:on-click #(<cmd ctx :cancel true)} "Cancel"]
      [:button {:on-click #(<cmd ctx :start true)} "Reset"]
      [:button {:on-click #(<cmd ctx :finish-loading true)} "Finish Loading"]
      [:label
       [:input {:type :checkbox :checked (sub> ctx :should-fail?) :on-change #(<cmd ctx :toggle-should-fail)}]
       " Req should fail?"]]
     [:hr]
     
     [:div {:style {:display "flex"
                    :justify-content "center"
                    :align-items "center"
                    :height "100px"
                    :border "1px solid black"
                    :margin-top "101px"}}
      (when (contains? #{:init :pressed :button-loader :success-notice :fail-notice :fail-init :fail-pressed} anim-state)
        [render-button ctx anim])
      (when (contains? #{:loader} anim-state)
        [render-loader ctx anim])]]))

(def component (ui/constructor {:renderer render
                                :subscription-deps [:anim-state :animation :should-fail?]
                                :topic :anim}))
