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
  (let [anim-state (merge-animation-values (sub> ctx :anim-state))
        size (:size anim-state)]
    [:div
     [:button {:on-mouse-down #(<cmd ctx :animation :forward)
               :on-mouse-up #(<cmd ctx :animation :back)} "Animate"]
     [:button {:on-click #(<cmd ctx :restart true)} "Restart"]
     [:hr]
     (when anim-state
       [:div {:style {
                      :width (str (- (:width anim-state) 4) "px")
                      :height "36px"
                      :margin-left (str (:margin-left anim-state) "px")
                      :background-color (:background-color anim-state)
                      :border-radius (str (:border-radius anim-state) "px")
                      :transform (str "rotate(" (:rotation anim-state) "deg)")
                      :border "2px solid black"}}
        ])]))

(def component (ui/constructor {:renderer render
                                :subscription-deps [:anim-state]
                                :topic :anim}))
