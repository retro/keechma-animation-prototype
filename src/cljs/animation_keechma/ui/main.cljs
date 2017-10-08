(ns animation-keechma.ui.main
  (:require [keechma.ui-component :as ui]
            [keechma.toolbox.ui :refer [sub> <cmd]]))

(defn render [ctx]
  (let [anim-state (sub> ctx :anim-state)
        size (get-in anim-state [:value :size])]
    [:div
     [:button {:on-click #(<cmd ctx :animation :forward)} "Start animation"]
     [:button {:on-click #(<cmd ctx :animation :back)} "Reverse animation"]
     [:hr]
     (when anim-state
       [:div {:style {:width (str size "px")
                      :height (str size "px")
                      :border (str (/ size 20) "px solid black")
                      :background (get-in anim-state [:value :color])
                      :border-radius (str (get-in anim-state [:value :radius]) "%") 
                      }}])]))

(def component (ui/constructor {:renderer render
                                :subscription-deps [:anim-state]
                                :topic :anim}))
