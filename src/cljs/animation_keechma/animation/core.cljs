(ns animation-keechma.animation.core
  (:require [animation-keechma.animation.helpers :as helpers]
            [animation-keechma.animation.animator :as animator]
            [animation-keechma.tasks :refer [stop-task blocking-raf! non-blocking-raf!]]))

(defn dispatcher [meta & args] [(:id meta) (:state meta)])

(defmulti animator dispatcher)
(defmulti step dispatcher)
(defmulti done? dispatcher)
(defmulti values dispatcher)

(defmethod animator :default [_ _]
  (animator/->DefaultAnimator))

(defmethod done? :default [meta animator]
  (animator/done? animator))

(defmethod step :default [meta data]
  data)

(defmethod values :default [meta]
  {})

(defn make-initial-meta
  ([identifier] (make-initial-meta identifier nil))
  ([[id state] prev]
   {:id id :state state :position 0 :times-invoked 0 :prev nil}))

(defn render-animation-end [app-db identifier]
  (let [[id _] identifier
        init-meta (make-initial-meta identifier)]
    (assoc-in app-db [:kv :animations id]
              {:data (values init-meta)
               :meta init-meta})))

(defn animate-state! [task-runner! app-db identifier]
  (let [[id state] identifier
        prev (get-in app-db [:kv :animations id])
        prev-values (:data prev)
        prev-meta (:meta prev)
        init-meta (make-initial-meta identifier prev-meta)
        animator (animator init-meta prev-values)
        values (values init-meta)
        start-end (helpers/start-end-values (helpers/prepare-values prev-values) (helpers/prepare-values values))]
    (task-runner!
     id
     (fn [{:keys [times-invoked]} app-db]
       (let [current (get-in app-db [:kv :animations id])
             current-meta (if (zero? times-invoked) init-meta (:meta current))
             next-position (animator/position animator)
             next-meta (assoc current-meta :times-invoked times-invoked :position next-position)
             done? (done? next-meta animator)
             next-data (step next-meta (helpers/get-current-styles (if done? 1 next-position) start-end))
             next-app-db (assoc-in app-db [:kv :animations id] {:data next-data :meta next-meta})]
         (if done?
           (stop-task next-app-db id)
           next-app-db))))))

(def blocking-animate-state! (partial animate-state! blocking-raf!))
(def non-blocking-animate-state! (partial animate-state! non-blocking-raf!))
