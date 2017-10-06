(ns animation-keechma.ui.main
  (:require [keechma.ui-component :as ui]
            [keechma.toolbox.ui :refer [sub> <cmd]]))

(defn render [ctx]
  (let [anim-state (sub> ctx :anim-state)
        size (* 100 (get-in anim-state [:value :size]))]
    [:div
     [:button {:on-click #(<cmd ctx :animation :start)} "Start animation"]
     [:button {:on-click #(<cmd ctx :animation :restart)} "Restart animation"]
     [:button {:on-click #(<cmd ctx :animation :stop)} "Stop animation"]
     [:hr]
     (when anim-state
       [:div {:style {:width (str size "px")
                      :height (str size "px")
                      :background (get-in anim-state [:value :color])
                      :border-radius (str (get-in anim-state [:value :radius]) "%")}}])]))

(def component (ui/constructor {:renderer render
                                :subscription-deps [:anim-state]
                                :topic :anim}))
