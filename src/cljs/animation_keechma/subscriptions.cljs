(ns animation-keechma.subscriptions
  (:require [animation-keechma.animation.core :refer [get-animation]])
  (:require-macros [reagent.ratom :refer [reaction]]))

(def subscriptions
  {:animation (fn [app-db-atom]
                (reaction
                 (get-animation @app-db-atom :button [:create-user :form])))
   :should-fail? (fn [app-db-atom]
                   (reaction
                    (get-in @app-db-atom [:kv :should-fail?])))
   :anim-state (fn [app-db-atom]
                 (reaction
                  (get-in @app-db-atom [:kv :keechma.toolbox.animation/id :animation])))})
