(ns animation-keechma.ui.main
  (:require [keechma.ui-component :as ui]
            [keechma.toolbox.ui :refer [sub> <cmd]]))

(defn render [ctx]
  (let [anim-state {:value (apply merge (map :values (sub> ctx :anim-state)))}
        size (get-in anim-state [:value :size])]
    [:div
     [:button {:on-mouse-down #(<cmd ctx :animation :forward)
               :on-mouse-up #(<cmd ctx :animation :back)} "Animate"]
     [:hr]
     (when anim-state
       [:div {:style {:width (str size "px")
                      :height (str size "px")
                     ;; :margin-top (str size "px")
                      :margin-left (str size "px")
                      ;;:border (str (/ size 20) "px solid black")
                      :background (get-in anim-state [:value :color])
                      :border-radius (str (get-in anim-state [:value :radius]) "%")
                      :position "relative"
                      :transform (str "rotate(" (get-in anim-state [:value :rotation]) "deg)")
                      }}
        [:div {:style {:height (str (/ size 10) "px")
                       :width (str (* size 0.9) "px")
                       :background "white"
                       :position "absolute"
                       :top "50%"
                       :left "50%"
                       :margin-top (str (- (/ size 20)) "px")
                       :margin-left (str (- (* size 0.45)) "px")
                       :transform (str "rotate(" 90 "deg)")
                       }}]
        [:div {:style {:height (str (/ size 10) "px")
                       :width (str (* size 0.9) "px")
                       :background "white"
                       :position "absolute"
                       :top "50%"
                       :left "50%"
                       :opacity (get-in anim-state [:value :alpha])
                       :margin-top (str (- (/ size 20)) "px")
                       :margin-left (str (- (* size 0.45)) "px")
                       :transform (str "rotate(" (- (get-in anim-state [:value :rotation])) "deg)")
                       }}]])]))

(def component (ui/constructor {:renderer render
                                :subscription-deps [:anim-state]
                                :topic :anim}))
