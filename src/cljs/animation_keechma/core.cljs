(ns animation-keechma.core
  (:require
   [reagent.core :as reagent]
   [keechma.app-state :as app-state]
   [animation-keechma.ui-system :refer [ui-system]]
   [animation-keechma.subscriptions :refer [subscriptions]]
   [animation-keechma.controllers :refer [controllers]]
   [animation-keechma.spring :as spring :refer [make-calculator]]
   ))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Vars

(defonce debug?
  ^boolean js/goog.DEBUG)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Initialize App

(def app-definition
  {:components    ui-system
   :controllers   controllers 
   :subscriptions subscriptions 
   :html-element  (.getElementById js/document "app")})

(defonce running-app (clojure.core/atom nil))

(defn start-app! []
  (reset! running-app (app-state/start! app-definition))
  )

(defn dev-setup []
  (when debug?
    (enable-console-print!)))

(defn reload []
  (let [current @running-app]
    (.clear js/console) 
    (if current
      (app-state/stop! current start-app!)
      (start-app!))))

(defn ^:export main []
  (dev-setup)
  (start-app!))
