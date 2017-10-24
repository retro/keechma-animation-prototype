(ns animation-keechma.ui.main
  (:require [keechma.ui-component :as ui]
            [keechma.toolbox.ui :refer [sub> <cmd]]
            [garden.color :refer [hex?]]
            [garden.units :as units]
            ["gravitas/src/index" :as gravitas]))

(def Spring (.-Spring gravitas))


(defprotocol IAnimator
  (position [this meta data])
  (done? [this]))

(extend-type Spring
  IAnimator
  (position [this meta data]
    (.x this))
  (done? [this]
    (.done this)))

(defrecord DefaultAnimator []
  IAnimator
  (position [this meta data]
    1)
  (done? [this]
    true))

(defmulti animator (fn [meta prev-data]
                 [(:id meta) (:state meta)]))

(defmethod animator :default [_ _]
  (->DefaultAnimator))

(defmulti done? (fn [meta current-data]
                  [(:id meta) (:state meta)]))

(defmethod done? :default [meta data animator]
  (done? animator))

(defmulti step (fn [meta data] [(:id meta) (:state meta)]))

(defmethod step :default [meta data]
  data)

#_(case id
    :button-start [:button {:style {:border-radius "25px"
                                    :height (str (:height value) "px")
                                    :border-style "solid"
                                    :border-width (str (:border-width value) "px")
                                    :outline "none"
                                    :cursor "pointer"
                                    :overflow "hidden"
                                    :color (:color value)
                                    :font-size (str (:font-size value) "px")
                                    :border-color (:border-color value)
                                    :background (:background-color value)
                                    :width (str (:width value) "px")
                                    :margin-left (str (:margin-left value) "px")
                                    :margin-top (str (:margin-top value) "px")
                                    :padding 0
                                    :position "absolute"
                                    :-webkit-tap-highlight-color "rgba(0,0,0,0)"}
                            :on-mouse-down #(<cmd ctx :animate-press)
                            :on-click #(<cmd ctx :animate-submit)
                            }
                   [:span {:style {:opacity (:text-opacity value)}} "Submit"]
                   [:div {:style {:position "absolute"
                                  :border-radius "100%"
                                  :top (str (:checkmark-pos value) "px")
                                  :left "50%"
                                  :width (str (:checkmark-dim value) "px")
                                  :height (str (:checkmark-dim value) "px")
                                  :line-height "20px"
                                  :text-align "center"
                                  :overflow "hidden"
                                  :color "white"
                                  :opacity (:checkmark-opacity value)
                                  :margin-left (str "-" (/ (:checkmark-dim value) 2) "px")
                                  }} "✔"]]
    :spinner-start [:div {:style {:width (str (:width value) "px")
                                  :height "50px"
                                  :border-radius "25px"
                                  :background-color (:background-color value)
                                  :margin-left (:margin-left value)
                                  :overflow "hidden"
                                  :position "relative"}}
                    [:div {:style {:background "#03cd94"
                                   :position "absolute"
                                   :width "50px"
                                   :height "50px"
                                   :left "-25px"
                                   :top "-25px"
                                   :opacity (:spinner-opacity value)
                                   :transform (str "rotate(" (* 6 (:rotation value)) "deg)")
                                   :transform-origin "100% 100%"
                                   }}]
                    [:div {:style {:position "absolute"
                                   :width (str (- (:width value) (- 50 (:inner-circle-dim value))) "px")
                                   :height (str (:inner-circle-dim value) "px")
                                   :left (str (:inner-circle-pos value) "px")
                                   :top (str (:inner-circle-pos value) "px")
                                   :background "white"
                                   :border-radius (str (/ (:inner-circle-dim value) 2) "px")
                                   :opacity (:inner-circle-opacity value)
                                   }}]
                    [:div {:style {:position "absolute"
                                   :border-radius "100%"
                                   :top (str (:checkmark-pos value) "px")
                                   :left "50%"
                                   :width (str (:checkmark-dim value) "px")
                                   :height (str (:checkmark-dim value) "px")
                                   :line-height "20px"
                                   :text-align "center"
                                   :overflow "hidden"
                                   :color "white"
                                   :margin-left (str "-" (/ (:checkmark-dim value) 2) "px")
                                   }} "✔"]]
    nil)



#_(def animation
  {:states {'init           {:press 'pressed}
            'pressed        {:release 'button-loader}
            'button-loader  {:next 'loader}
            'loader         {:success 'success-notice
                             :fail    'fail-notice}
            'success-notice {:next 'init}
            'fail-notice    {:next 'fail-init}
            'fail-init      {:press 'fail-pressed}
            'fail-pressed   {:release 'button-loader}}
   })


















(def animation
  {:id     :button
   :states {:init           {:style {:border-radius    "25px"
                                     :height           "50px"
                                     :width            "200px"
                                     :border-width     "2px"
                                     :border-style     "solid"
                                     :border-color     "#03cd94"
                                     :color            "#07ce95"
                                     :background-color "#fff"
                                     :font-size        "16px"
                                     :cursor           "pointer"
                                     :outline          "none"}}
            :pressed        {:style {:border-radius    "25px"
                                     :height           "44px"
                                     :width            "180px"
                                     :border-width     "2px"
                                     :border-style     "solid"
                                     :border-color     "#03cd94"
                                     :color            "#fff"
                                     :background-color "#03cd94"
                                     :font-size        "14px"
                                     :cursor           "pointer"
                                     :outline          "none"}}
            :button-loader  {:style {:border-radius    "25px"
                                     :height           "50px"
                                     :width            "50px"
                                     :border-width     "4px"
                                     :border-style     "solid"
                                     :border-color     "#ccc"
                                     :color            "#fff"
                                     :background-color "#fff"
                                     :font-size        "14px"
                                     :cursor           "pointer"
                                     :outline          "none"}}
            :loader         {:style {:border-radius   "25px"
                                     :height          "50px"
                                     :width           "50px"
                                     :display         "flex"
                                     :background      "#ccc"
                                     :overflow        "hidden"
                                     :justify-content "center"
                                     :align-items     "center"
                                     :position        "relative"}}
            :success-notice {:style {:border-radius    "25px"
                                     :height           "50px"
                                     :width            "50px"
                                     :border-width     "4px"
                                     :border-style     "solid"
                                     :border-color     "#03cd94"
                                     :color            "#fff"
                                     :background-color "#03cd94"
                                     :font-size        "14px"
                                     :cursor           "pointer"
                                     :outline          "none"}}
            :fail-notice    {:style {:border-radius    "25px"
                                     :height           "50px"
                                     :width            "50px"
                                     :border-width     "4px"
                                     :border-style     "solid"
                                     :border-color     "#ff3300"
                                     :color            "#fff"
                                     :background-color "#ff3300"
                                     :font-size        "14px"
                                     :cursor           "pointer"
                                     :outline          "none"}}
            :fail-init      {:style {:border-radius    "25px"
                                     :height           "50px"
                                     :width            "200px"
                                     :border-width     "2px"
                                     :border-style     "solid"
                                     :border-color     "#ff3300"
                                     :color            "#ff3300"
                                     :background-color "#fff"
                                     :font-size        "16px"
                                     :cursor           "pointer"
                                     :outline          "none"}}
            :fail-pressed   {:style {:border-radius    "25px"
                                     :height           "44px"
                                     :width            "180px"
                                     :border-width     "2px"
                                     :border-style     "solid"
                                     :border-color     "#ff3300"
                                     :color            "#fff"
                                     :background-color "#ff3300"
                                     :font-size        "14px"
                                     :cursor           "pointer"
                                     :outline          "none"}}}})





(defn render-init [ctx anim]
  [:button {:style (:data anim)
            :on-mouse-down #(<cmd ctx :animate-press nil)}
   [:span {:style {:opacity 1}} "Submit"]])

(defn render-pressed []
  [:button {:style {:border-radius "25px"
                    :height "44px"
                    :width "180px"
                    :border-width "2px"
                    :border-style "solid"
                    :border-color "#03cd94"
                    :color "#fff"
                    :background-color "#03cd94"
                    :font-size "14px"
                    :cursor "pointer"
                    :outline "none"}}
   [:span {:style {:opacity 1}} "Submit"]])

(defn render-button-loader []
  [:button {:style {:border-radius "25px"
                    :height "50px"
                    :width "50px"
                    :border-width "4px"
                    :border-style "solid"
                    :border-color "#ccc"
                    :color "#fff"
                    :background-color "#fff"
                    :font-size "14px"
                    :cursor "pointer"
                    :outline "none"}}
   [:span {:style {:opacity 0}} "Submit"]])

(defn render-loader []
  [:div {:style {:border-radius "25px"
                 :height "50px"
                 :width "50px"
                 :display "flex"
                 :background "#ccc"
                 :overflow "hidden"
                 :justify-content "center"
                 :align-items "center"
                 :position "relative"}}
   [:div {:style {:background "#ff3300"
                  :width "50px"
                  :height "25px"
                  :margin-left "-25px"
                  :position "absolute"
                  :transform "rotate(23deg)"
                  :transform-origin "100% 50%"}}]
   [:div {:style {:border-radius "25px"
                  :background-color "white"
                  :width "42px"
                  :height "42px"
                  :position "relative"}}]])

(defn render-button-success []
  [:button {:style {:border-radius "25px"
                    :height "50px"
                    :width "50px"
                    :border-width "4px"
                    :border-style "solid"
                    :border-color "#03cd94"
                    :color "#fff"
                    :background-color "#03cd94"
                    :font-size "14px"
                    :cursor "pointer"
                    :outline "none"}}
   [:span {:style {:opacity 1}} "✔"]])



(defn render-button-fail []
  [:button {:style {:border-radius "25px"
                    :height "50px"
                    :width "50px"
                    :border-width "4px"
                    :border-style "solid"
                    :border-color "#ff3300"
                    :color "#fff"
                    :background-color "#ff3300"
                    :font-size "14px"
                    :cursor "pointer"
                    :outline "none"}}
   [:span {:style {:opacity 1}} "✘"]])

(defn render-fail-init []
  [:button {:style {:border-radius "25px"
                    :height "50px"
                    :width "200px"
                    :border-width "2px"
                    :border-style "solid"
                    :border-color "#ff3300"
                    :color "#ff3300"
                    :background-color "#fff"
                    :font-size "16px"
                    :cursor "pointer"
                    :outline "none"}}
   [:span {:style {:opacity 1}} "Submit Failed"]])

(defn render-fail-pressed []
  [:button {:style {:border-radius "25px"
                    :height "44px"
                    :width "180px"
                    :border-width "2px"
                    :border-style "solid"
                    :border-color "#ff3300"
                    :color "#fff"
                    :background-color "#ff3300"
                    :font-size "14px"
                    :cursor "pointer"
                    :outline "none"}}
   [:span {:style {:opacity 1}} "Submit"]])

(defn render [ctx]
  (let [anim (sub> ctx :animation)] 
    [:div {:style {:box-sizing "border-box"}} "Foo - "
     [:div
      [:button {:on-click #(<cmd ctx :stop-animation true)} "Stop"]
      [:button {:on-click #(<cmd ctx :cancel true)} "Cancel"]
      [:button {:on-click #(<cmd ctx :start true)} "Reset"]
      [:button {:on-click #(<cmd ctx :finish-loading true)} "Finish Loading"]]
     [:hr]
     (into [:div]
           (map (fn [renderer]
                  [:div {:style {:display "flex"
                                 :justify-content "center"
                                 :align-items "center"
                                 :height "100px"
                                 :border "1px solid black"
                                 :margin-bottom "-1px"}}
                   [renderer]]) [render-pressed render-button-loader render-loader render-button-success render-button-fail render-fail-init render-fail-pressed]))
     [:div {:style {:display "flex"
                    :justify-content "center"
                    :align-items "center"
                    :height "100px"
                    :border "1px solid black"
                    :margin-top "101px"}}
      (println anim)
      (when (= :init (get-in anim [:meta :state]))
        [render-init ctx anim])]]))

(def component (ui/constructor {:renderer render
                                :subscription-deps [:anim-state :animation]
                                :topic :anim}))
