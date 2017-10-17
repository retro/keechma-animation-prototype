(ns animation-keechma.ui.main
  (:require [keechma.ui-component :as ui]
            [keechma.toolbox.ui :refer [sub> <cmd]]))

(defn merge-animation-values [state]
  (:values (reduce 
            (fn [acc frame]
              (let [placeholder? (or (get-in frame [:keechma.toolbox/anim-state :placeholder?]) false)
                    values (:values frame)]
                (reduce-kv (fn [m k v]
                             (let [value-exists? (contains? (:values m) k)
                                   value-placeholder? (get-in m [:placeholders k])]
                               (if (or (not value-exists?)
                                       (and value-exists? value-placeholder? placeholder?)
                                       (and value-exists? value-placeholder? (not placeholder?))
                                       (and value-exists? (not value-placeholder?) (not placeholder?)))
                                 (-> m
                                     (assoc-in [:values k] v)
                                     (assoc-in [:placeholders k] placeholder?))
                                 m)
                               )) acc values)))
            {:values {} :placeholders {}} (:values state))))

(defn render [ctx]
  (let [{:keys [id value]} (sub> ctx :animation)]
    [:div {:style {:box-sizing "border-box"}} "Foo - "
     [:div
      [:button {:on-click #(<cmd ctx :stop-animation true)} "Stop"]
      [:button {:on-click #(<cmd ctx :cancel-animation true)} "Cancel"]
      [:button {:on-click #(<cmd ctx :start true)} "Reset"]
      [:button {:on-click #(<cmd ctx :finish-loading true)} "Finish Loading"]]
     [:hr]
     (case id
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
                               ;;:on-touch-start #(<cmd ctx :animate-press)
                               ;;:on-touch-end #(<cmd ctx :animate-submit)
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
       nil)]))

(def component (ui/constructor {:renderer render
                                :subscription-deps [:anim-state :animation]
                                :topic :anim}))
