(ns animation-keechma.subscriptions
  (:require-macros [reagent.ratom :refer [reaction]]))

(def subscriptions
  {:animation (fn [app-db-atom]
                (reaction
                 (get-in @app-db-atom [:kv :animations :button])))
   :anim-state (fn [app-db-atom]
                 (reaction
                  (get-in @app-db-atom [:kv :keechma.toolbox.animation/id :animation])))})
