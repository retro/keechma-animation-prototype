(ns animation-keechma.spring)

(def af-time-millis 16.667)
(def max-delta-time-sec 0.064)
(def solver-timestep-sec 0.001)

(defn displacement-distance-for-state
  ([state] (displacement-distance-for-state state :current))
  ([state which]
   (.abs js/Math (- (:end state) (get-in state [which :position])))))

(defn state-at-rest? [state]
  (and (< (.abs js/Math (get-in state [:current :velocity]))
          (get-in state [:config :rest-speed-threshold]))
       (or (<= (displacement-distance-for-state state :current)
               (get-in state [:config :displacement-from-rest-threshold]))
           (= (get-in state [:config :tension]) 0))))

(defn state-overshooting? [state]
  (let [{:keys [start end]} state
        tension (get-in state [:config :tension])
        current (get-in state [:current :position])]
    (and (pos? tension)
         (or (and (< start end) (> current end))
             (and (> start end) (< current end))))))

(defn update-velocity [state velocity]
  (assoc-in state [:current :velocity] velocity))

(defn update-at-rest [state] 
  (if (or (state-at-rest? state)
          (and (:overshoot-clamping? state) (state-overshooting? state)))
    (let [new-state (if (pos? (get-in state [:config :tension]))
                      (-> state
                          (assoc :start (:end state))
                          (assoc-in [:current :position] (:end state)))
                      (-> state
                          (assoc :end (get-in state [:current :position])
                                 :start (:end state))))]
      (-> new-state
          (update-velocity 0)
          (assoc :rest? true)))
    state))

(defn interpolate [state time-accumulator]
  (if (pos? time-accumulator)
    (let [alpha (/ time-accumulator solver-timestep-sec)] 
      (-> state
          (assoc-in [:current :position]
                    (+ (* (get-in state [:current :position]) alpha)
                       (* (get-in state [:prev :position]) (- 1 alpha))))
          (assoc-in [:current :velocity]
                    (+ (* (get-in state [:current :velocity]) alpha)
                       (* (get-in state [:prev :velocity]) (- 1 alpha))))))
    state))

(defn update-frames [state]
  (assoc state :frames
         (conj (:frames state) (get-in state [:current :position]))))

(defn calculate-advanced-state-step [calculation]
  (let [{:keys [tension friction position velocity temp-position temp-velocity prev-position prev-velocity end]} calculation
        prev-position position
        prev-velocity velocity

        a-velocity velocity
        a-acceleration (- (* tension (- end temp-position)) (* friction velocity))

        temp-position (+ position (* a-velocity solver-timestep-sec 0.5))
        temp-velocity (+ velocity (* a-acceleration solver-timestep-sec 0.5))
        b-velocity temp-velocity
        b-acceleration (- (* tension (- end temp-position))
                          (* friction temp-velocity))

        temp-position (+ position (* b-velocity solver-timestep-sec 0.5))
        temp-velocity (+ velocity (* b-acceleration solver-timestep-sec 0.5))
        c-velocity temp-velocity
        c-acceleration (- (* tension (- end temp-position))
                          (* friction temp-velocity))

        temp-position (+ position (* c-velocity solver-timestep-sec))
        temp-velocity (+ velocity (* c-acceleration solver-timestep-sec))
        d-velocity temp-velocity
        d-acceleration (- (* tension (- end temp-position))
                          (* friction temp-velocity))
        
        dxdt (* (/ 1.0 6.0)
                (+ a-velocity (* 2.0 (+ b-velocity c-velocity)) d-velocity))
        dvdt (* (/ 1.0 6.0)
                (+ a-acceleration (* 2.0 (+ b-acceleration c-acceleration)) d-acceleration))

        position (+ position (* dxdt solver-timestep-sec))
        velocity (+ velocity (* dvdt solver-timestep-sec))]
    (assoc calculation
           :temp-position temp-position
           :temp-velocity temp-velocity
           :prev-velocity prev-velocity
           :prev-position prev-position
           :position position
           :velocity velocity)))

(defn calculate-advanced-state [time-accumulator state]
  (let [calculation {:end (:end state)
                     :tension (get-in state [:config :tension])
                     :friction (get-in state [:config :friction])
                     :position (get-in state [:current :position])
                     :velocity (get-in state [:current :velocity])
                     :temp-position (get-in state [:temp :position])
                     :temp-velocity (get-in state [:temp :velocity])
                     :prev-position (get-in state [:prev :position])
                     :prev-velocity (get-in state [:prev :velocity])}]
    (loop [current-time-accumulator time-accumulator
           current-calculation calculation]
      (let [new-calculation (calculate-advanced-state-step current-calculation)]
        (if (or (< (- current-time-accumulator solver-timestep-sec) solver-timestep-sec))
          [current-time-accumulator
           (assoc state
                  :prev {:position (:prev-position new-calculation)
                         :velocity (:prev-velocity new-calculation)}
                  :temp {:position (:temp-position new-calculation)
                         :velocity (:temp-velocity new-calculation)}
                  :current {:position (:position new-calculation)
                            :velocity (:velocity new-calculation)})]
          (recur (- current-time-accumulator solver-timestep-sec)
                 new-calculation))))))

(defn adjust-delta-time [real-delta-time]
  (if (> real-delta-time max-delta-time-sec)
    max-delta-time-sec
    real-delta-time))

(defn advance [state time-accumulator time real-delta-time]
  (if (and (:rest? state) (state-at-rest? state))
    state
    (let [adjusted-delta-time (adjust-delta-time real-delta-time)
          new-time-accumulator (+ adjusted-delta-time time-accumulator)
          [rest-time-accumulator new-state] (calculate-advanced-state new-time-accumulator state)
          final-time-accumulator (- rest-time-accumulator solver-timestep-sec)]
      [final-time-accumulator
       (-> new-state
           (interpolate final-time-accumulator)
           (update-at-rest)
           (update-frames))])))

(defn make-calculator
  ([tension friction] (make-calculator tension friction false))
  ([tension friction overshoot-clamping?]
   (let [config {:rest-speed-threshold 0.001
                 :displacement-from-rest-threshold 0.001
                 :tension (+ (* (- tension 30.0) 3.62) 194.0)
                 :friction (+ (* (- friction 8.0) 3.0) 25.0)
                 :overshoot-clamping? overshoot-clamping?}]
     (fn [start end]
       (let [init {:start start
                   :end end
                   :config config
                   :current {:velocity 0 :position 0}
                   :prev {:velocity 0 :position 0}
                   :temp {:velocity 0 :position 0}
                   :rest? false
                   :frames []}]
         (loop [current-time-millis af-time-millis
                last-time-millis -1
                time-accumulator 0
                state init]
           (let [elapsed-time-millis (if (= -1 last-time-millis)
                                       (- current-time-millis (dec current-time-millis))
                                       (- current-time-millis last-time-millis))]
             (if (:rest? state)
               state
               (let [[new-time-accumulator new-state]
                     (advance state
                              time-accumulator (/ current-time-millis 1000.0) (/ elapsed-time-millis 1000.0))]
                 (recur (+ current-time-millis af-time-millis)
                        current-time-millis
                        new-time-accumulator
                        new-state))))))))))
