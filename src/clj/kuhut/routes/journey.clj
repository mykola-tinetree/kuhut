(ns kuhut.routes.journey
  (:require [datomic.api :as datomic]
            [kuhut.common.connection :refer [kuhut-connection kuhut-db]]
            [taoensso.nippy :refer [thaw freeze]]
            [kuhut.routes.content :refer [get-knowledge update-template-knowledge ensure-template-recommendation-certainty
                                          template->exercise]]
            [kuhut.routes.executor :refer [execute]]
            [kuhut.common.util :refer [now roulette-wheel]]
            [clojure.pprint :refer [pprint]])
  )

(defn- record-step
  [user-ref agent exercise template time data]
  (datomic/transact-async
    (kuhut-connection)
    [{:db/id         "step"
      :step/user     user-ref
      :step/agent    agent
      :step/template template
      :step/time     time
      :step/data     data}
     [:db/add exercise :exercise/current-step "step"]])
  )

(defn- apply-user-move-to-element
  [element move]
  (mapv
    (fn [e]
      (cond
        (vector? e) (apply-user-move-to-element e move)
        (map? e) (if-let [k (:key e)] (assoc e :move (get move k)) e)
        :else e))
    element)
  )

(defn- apply-user-move
  [{:keys [template content]} move]
  {:template template
   :time     (now)
   :content  (apply-user-move-to-element content move)}
  )

(defn- code-answer-transform [code] (clojure.string/join "\n" (if (vector? code) code [code])))

(defn- process-exercise-element
  [[k & [f & r :as c]]]
  (if (and (map? f) (contains? f :move))
    (let [{:keys [move answer weight] :or {weight 1}} f]
      (case k
        :text (let [correct? (= move answer)]
                [[k (-> f
                        (dissoc :move :key :answer :weight)
                        (assoc :assessment (if correct? :correct :incorrect))
                        (#(if correct? % (assoc % :solution answer)))) move]
                 [[(if (or correct? (nil? answer)) 1 0) weight]]
                 (some? answer)])
        :options [(into [k (-> f
                               (dissoc :move :key :answer :weight)
                               (assoc :solution answer :selection move))] r)
                  [[(if (or (nil? answer) (= move answer)) 1 0) weight]]
                  (some? answer)]
        :selector [(into [k (-> f
                                (dissoc :move :key :answer :weight)
                                (assoc :solution answer :selection move))] r)
                   [[(if (or (nil? answer) (= move answer)) 1 0) weight]]
                   (some? answer)]
        :canvas nil                                         ;; TODO: finish this off
        :code (let [{:keys [code output]} answer
                    correct? (if output
                               (let [{:keys [exit out]} (execute (or (:language f) :null) move (:executor f))]
                                 (and (zero? exit)
                                      (= (clojure.string/trim out)
                                         (code-answer-transform output))))
                               (= move (code-answer-transform code)))]
                [[k (-> f
                        (dissoc :move :key :answer :weight)
                        (assoc :assessment (if correct? :correct :incorrect))
                        (#(if correct? % (assoc % :solution answer)))
                        (#(if output (assoc % :output (when output (if correct? :hidden :visible))) %))) move]
                 [[(if (or correct? (nil? answer)) 1 0) weight]]
                 (some? answer)])
        ))
    (reduce
      (fn [[result assessments changed?] element]
        (if (vector? element)
          (let [[e a c?] (process-exercise-element element)]
            [(conj result e) (into assessments a) (or changed? c?)])
          [(conj result element) assessments changed?]))
      [(into [k] (when (map? f) [f])) (if-let [w (:weight f)] [[1 w]] []) false]
      (if (map? f) r c)))
  )

(defn- make-next-move
  [user-id {:keys [template] :as exercise} user-move]
  (let [t (now)
        {:keys [content]} (apply-user-move exercise user-move)
        [move-content assessments changed?] (process-exercise-element content)]
    (when (seq assessments)
      (let [assessment-weight (reduce + 0 (map second assessments))
            assessment-sum (reduce + 0.0 (map (fn [[a b]] (* a b)) assessments))]
        (when (pos? assessment-weight)
          (let [assessment (/ assessment-sum assessment-weight)]
            (update-template-knowledge user-id template assessment 1.0)
            (ensure-template-recommendation-certainty user-id template assessment)))))
    (when changed?
      {:template template
       :time     t
       :content  move-content}))
  )

(defn- build-next-exercise
  [db user-ref concept-ref current-template]
  (let [attempt (fn []
                  (loop [{ref :db/id :as reference} (datomic/entity db concept-ref)]
                    (when-let [selections (->> (if (:concept/id reference)
                                                 (concat
                                                   (->> reference :concept/recommendations
                                                        (map (fn [{reference :link/reference weight :link/weight}]
                                                               {:ref reference :type :recommendation :weight weight})))
                                                   (->> reference :concept/components
                                                        (map (fn [{reference :link/reference weight :link/weight}]
                                                               {:ref reference :type :component :weight weight}))))
                                                 (into
                                                   [{:ref reference :type :template}]
                                                   (->> reference :template/recommendations
                                                        (map (fn [{reference :link/reference weight :link/weight}]
                                                               {:ref reference :type :recommendation :weight weight})))))
                                               (map #(merge % (select-keys (->> % :ref :db/id (get-knowledge db user-ref)) [:level :certainty])))
                                               (map (fn [{:keys [ref type weight level certainty]}]
                                                      ;; TODO: add a time dimension to the certainty calculation
                                                      [ref
                                                       (let [A (/ (- 1.0 (or level 0)) 2)
                                                             B (/ (- 1.0 (or certainty 0)) 2)]
                                                         (if (= (:db/id ref) current-template)
                                                           0
                                                           (case type
                                                             :template (+ (* A A) B)
                                                             :recommendation (* (+ (* A A) (* B B)) weight)
                                                             :component (* (+ (* A A A) (* B B)) weight))))]))
                                               seq)]
                      (let [{selection-ref :db/id :as selection} (roulette-wheel selections)]
                        (if (= selection-ref ref)
                          {:template ref
                           :time     (now)
                           :content  (-> reference :template/content read-string template->exercise)}
                          (recur selection))))))]
    (loop [x (attempt)] (if x x (recur (attempt)))))
  )

(defn- exercise-content->worksheet
  [element]
  (vec
    (keep
      (fn [e]
        (cond
          (vector? e) (exercise-content->worksheet e)
          (map? e) (let [f (dissoc e :answer :weight :comment)] (when (seq f) f))
          :else e))
      element))
  )

(defn next-move
  [{user-id :id} concept-id move reset?]
  (let [db (kuhut-db)
        concept-ref (->> [:concept/id concept-id] (datomic/entity db) :db/id)
        ;; TODO: need some checksum number to make sure the same exercise isn't being played from two different browsers
        {exercise-ref                            :db/id
         {{exercise-template :db/id} :step/template
          exercise-time              :step/time
          exercise-content           :step/data} :exercise/current-step
         } (or (datomic/q '[:find (pull ?exercise-ref [:db/id {:exercise/current-step [:step/template :step/time :step/data]}]) .
                            :in $ ?user-ref ?concept-ref
                            :where
                            [?exercise-ref :exercise/user ?user-ref]
                            [?exercise-ref :exercise/concept ?concept-ref]
                            ] db user-id concept-ref)
               {:db/id ((:tempids @(datomic/transact
                                     (kuhut-connection)
                                     [{:db/id            "exercise"
                                       :exercise/user    user-id
                                       :exercise/concept concept-ref}]))
                         "exercise")})
        exercise (when exercise-template
                   {:template exercise-template
                    :time     exercise-time
                    :content  (thaw exercise-content)})]
    (when move
      (record-step user-id :step.agent/user exercise-ref exercise-template (now) (freeze move)))
    (let [{:keys [template time content]} (or (if move
                                                (make-next-move user-id exercise move)
                                                (when (and exercise (not reset?)) (assoc exercise :time (now))))
                                              (build-next-exercise db user-id concept-ref exercise-template))
          [flag-ref flag-time flag-data] (datomic/q '[:find [?flag-ref ?time ?data]
                                                      :in $ ?user-ref ?template-ref
                                                      :where
                                                      [?flag-ref :flag/step ?step-ref]
                                                      [?step-ref :step/user ?user-ref]
                                                      [?step-ref :step/template ?template-ref]
                                                      [?flag-ref :flag/time ?time]
                                                      [?step-ref :step/data ?data]
                                                      ] db user-id template)]
      (record-step user-id :step.agent/kuhut exercise-ref template time (freeze content))
      (merge
        {:template (:template/id (datomic/entity db template))
         :content  (exercise-content->worksheet content)}
        (when flag-ref
          {:flag {:id      flag-ref
                  :time    (.getTime flag-time)
                  :content (-> flag-data thaw exercise-content->worksheet)}}))))
  )

(defn flag-move
  [{user-id :id :keys [first-name]} concept-id]
  (when first-name
    (when-let [[exercise-ref step-ref] (datomic/q '[:find [?exercise-ref ?step-ref]
                                                    :in $ ?user-ref ?concept-id
                                                    :where
                                                    [?concept-ref :concept/id ?concept-id]
                                                    [?exercise-ref :exercise/user ?user-ref]
                                                    [?exercise-ref :exercise/concept ?concept-ref]
                                                    [?exercise-ref :exercise/current-step ?step-ref]
                                                    ] (kuhut-db) user-id concept-id)]
      @(datomic/transact
         (kuhut-connection)
         [{:flag/step step-ref
           :flag/time (now)}
          [:db/retract exercise-ref :exercise/current-step step-ref]])
      true))
  )

(defn remove-flag
  [{user-id :id} flag-ref]
  (when (->> flag-ref (datomic/entity (kuhut-db)) :flag/step :step/user :db/id (= user-id))
    @(datomic/transact
       (kuhut-connection)
       [[:db.fn/retractEntity flag-ref]])
    true)
  )
