(ns animation-keechma.ui.main
  (:require [keechma.ui-component :as ui]
            [keechma.toolbox.ui :refer [sub> <cmd]]))

(defn merge-animation-values [state]
  (:values (reduce (fn [acc frame]
                     (let [placeholder? (or (get-in frame [:keechma.toolbox/anim-state :placeholder?]) false)
                           values (:values frame)]
                       (reduce-kv (fn [m k v]
                                    (let [value-exists? (contains? (:values m) k)
                                          value-placeholder? (get-in m [:placeholders k])]
                                      (cond
                                        (not value-exists?)
                                        (-> m
                                            (assoc-in [:values k] v)
                                            (assoc-in [:placeholders k] placeholder?))

                                        (and value-exists? value-placeholder? placeholder?)
                                        (-> m
                                            (assoc-in [:values k] v)
                                            (assoc-in [:placeholders k] placeholder?))

                                        (and value-exists? value-placeholder? (not placeholder?))
                                        (-> m
                                            (assoc-in [:values k] v)
                                            (assoc-in [:placeholders k] placeholder?))

                                        (and value-exists? (not value-placeholder?) (not placeholder?))
                                        (-> m
                                            (assoc-in [:values k] v)
                                            (assoc-in [:placeholders k] placeholder?))
                                        
                                        :else m))) acc values)))
                   {:values {} :placeholders {}} state))
  ;;(apply merge (map :values state))
  )

(defn render [ctx]
  (let [anim-state (merge-animation-values (sub> ctx :anim-state))
        size (:size anim-state)]
    [:div
     [:button {:on-mouse-down #(<cmd ctx :animation :forward)
               :on-mouse-up #(<cmd ctx :animation :back)} "Animate"]
     [:hr]
     (when anim-state
       [:div {:style {:width (str (:width anim-state) "px")
                      :height "40px"
                      :margin-left (str (:margin-left anim-state) "px")
                      :background-color (:background-color anim-state)
                      :border-radius (str (:border-radius anim-state) "px")
                      :transform (str "rotate(" (:rotation anim-state) "deg)")
                      }}
       ])]))

(def component (ui/constructor {:renderer render
                                :subscription-deps [:anim-state]
                                :topic :anim}))
