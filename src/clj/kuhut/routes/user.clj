(ns kuhut.routes.user
  (:require [kuhut.common.connection :refer [kuhut-connection kuhut-db]]
            [kuhut.shared.definitions :refer [DEFAULT_TOPIC]]
            [datomic.api :as datomic]
            [hiccup.page :refer [html5]]
            [kuhut.routes.content :refer [load-concept]]
            [kuhut.common.util :refer [send-e-mail random-password hash-password unhash-password now uuid]])
  )

(defn toggle-bookmark
  [{user-id :id :keys [first-name]} concept-id]
  (let [concept-ref (->> [:concept/id concept-id] (datomic/entity (kuhut-db)) :db/id)]
    (when (and first-name concept-ref)
      @(datomic/transact (kuhut-connection) [[:kuhut/toggle-bookmark user-id concept-ref]])
      true))
  )

(defn focus
  ([{user-id :id}]
   (let [db (kuhut-db)]
     (load-concept
       db user-id
       (or (datomic/q '[:find ?concept-id .
                        :in $ ?user-ref
                        :where
                        [?user-ref :user/focus ?focus-ref]
                        [?focus-ref :concept/id ?concept-id]
                        ] db user-id)
           (uuid DEFAULT_TOPIC)))))
  ([{user-id :id} concept-id]
   (let [db (kuhut-db)]
     (when-let [concept-ref (->> [:concept/id concept-id] (datomic/entity db) :db/id)]
       @(datomic/transact (kuhut-connection) [[:db/add user-id :user/focus concept-ref]])
       true)))
  )

(defn interests
  [{user-id :id :keys [first-name]}]
  (when first-name
    (let [db (kuhut-db)]
      (map
        (partial load-concept db user-id)
        (datomic/q '[:find [?concept-id ...]
                     :in $ ?user-ref
                     :where
                     [?user-ref :member/interests ?interest-ref]
                     [?interest-ref :concept/id ?concept-id]
                     ] db user-id))))
  )

(defn suggestions
  [{user-id :id :keys [first-name]}]
  (when first-name
    (let [db (kuhut-db)]
      (if-let [automatic-suggestions (->> (datomic/q '[:find ?id ?level
                                                       :in $ ?user-ref
                                                       :where
                                                       [?ref :concept/popularity]
                                                       (not [?user-ref :member/interests ?ref])
                                                       [?ref :concept/id ?id]
                                                       [?knowledge-ref :knowledge/user ?user-ref]
                                                       [?knowledge-ref :knowledge/reference ?ref]
                                                       [?knowledge-ref :knowledge/level ?level]
                                                       ] db user-id)
                                          (filter #(-> % second (< 1)))
                                          (sort-by second >)
                                          (map (comp (partial load-concept db user-id) first))
                                          seq)]
        automatic-suggestions
        [(load-concept db user-id (uuid DEFAULT_TOPIC))])))
  )

(defn lock-focus
  [{user-id :id :keys [first-name]} e-mail]
  (when first-name
    (let [pin (random-password)
          [user-e-mail user-focus] (datomic/q '[:find [?user-e-mail ?focus-name]
                                                :in $ ?user-ref
                                                :where
                                                [?user-ref :member/e-mail ?user-e-mail]
                                                [?user-ref :user/focus ?focus-ref]
                                                [?focus-ref :concept/name ?focus-name]
                                                ] (kuhut-db) user-id)]
      @(datomic/transact (kuhut-connection) [{:db/id       "lock"
                                              :lock/e-mail e-mail
                                              :lock/pin    (hash-password pin)}
                                             [:db/add user-id :member/lock "lock"]])
      (send-e-mail
        e-mail
        "kuhut.com: lock pin"
        (html5
          [:body
           "Dear member of the kuhut community," [:br] [:br]
           "You have requested to lock " [:strong user-e-mail] " on topic " [:strong user-focus] [:br] [:br]
           "Please use the following PIN to unlock the account focus: " [:strong pin] [:br] [:br]
           "Yours Sincerely," [:br]
           "The kuhut Team"]))
      true))
  )

(defn unlock-focus
  [{user-id :id :keys [first-name]} pin]
  (boolean (when first-name
             (when-let [lock-ref (datomic/q '[:find ?lock-ref .
                                              :in $ ?user-ref ?pin
                                              :where
                                              [?user-ref :member/lock ?lock-ref]
                                              [?lock-ref :lock/pin ?hashed-pin]
                                              [(kuhut.common.util/unhash-password ?hashed-pin) ?unhashed-pin]
                                              [(= ?unhashed-pin ?pin)]
                                              ] (kuhut-db) user-id pin)]
               @(datomic/transact (kuhut-connection) [[:db.fn/retractEntity lock-ref]])
               true)))
  )

(defn remind-focus-pin
  [{user-id :id :keys [first-name]}]
  (when first-name
    (when-let [[user-e-mail e-mail pin] (datomic/q '[:find [?user-e-mail ?lock-e-mail ?lock-pin]
                                                     :in $ ?user-id
                                                     :where
                                                     [?user-ref :member/e-mail ?user-e-mail]
                                                     [?user-ref :member/lock ?lock-ref]
                                                     [?lock-ref :lock/e-mail ?lock-e-mail]
                                                     [?lock-ref :lock/pin ?lock-pin]
                                                     ] (kuhut-db) user-id)]
      (send-e-mail
        e-mail
        "kuhut.com: pin reminder"
        (html5
          [:body
           "Dear member of the kuhut community," [:br] [:br]
           "You have requested a reminder of the lock PIN for the following account: " [:strong user-e-mail] [:br] [:br]
           "Please use this pin to unlock the account focus: " [:strong (unhash-password pin)] [:br] [:br]
           "Yours Sincerely," [:br]
           "The kuhut Team"]))
      e-mail))
  )
