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
      [:button {:on-click #(<cmd ctx :cancel-animation true)} "Cancel"]]
     [:hr]
     (case id
       :button-start [:button {:style {:border-radius "25px"
                                       :height (str (:height value) "px")
                                       :border-style "solid"
                                       :border-width "2px"
                                       :outline "none"
                                       :cursor "pointer"
                                       :overflow "hidden"
                                       :color (:color value)
                                       :font-size (str (:font-size value) "px")
                                       :border-color (:border-color value)
                                       :background "white"
                                       :width (str (:width value) "px")
                                       :margin-left (str (:margin-left value) "px")
                                       :margin-top (str (:margin-top value) "px")
                                       :padding 0}
                               :on-mouse-down #(<cmd ctx :animate-press)
                               :on-mouse-up #(<cmd ctx :animate-submit)}
                      [:span {:style {:opacity (:text-opacity value)}} "Submit"]]
       nil)]))

(def component (ui/constructor {:renderer render
                                :subscription-deps [:anim-state :animation]
                                :topic :anim}))
