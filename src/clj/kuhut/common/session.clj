(ns kuhut.common.session
  (:require [kuhut.common.connection :refer [kuhut-connection kuhut-db]]
            [kuhut.shared.definitions :refer [DEFAULT_TOPIC]]
            [kuhut.common.util :refer [uuid now now+]]
            [taoensso.nippy :refer [freeze]]
            [datomic.api :as datomic]
            [java-time :refer [days]])
  )

(defn member
  [session-key]
  (when session-key
    (let [db (kuhut-db)]
      (when-let [[member session] (datomic/q '[:find [(pull ?member-ref [:db/id {:member/roles [:db/ident]} :member/first-name :member/last-name :member/lock]) (pull ?session-ref [:session/expiry {:session/type [:db/ident]}])]
                                               :in $ ?session-key ?time
                                               :where
                                               [?session-ref :session/key ?session-key]
                                               [?session-ref :session/expiry ?expiry]
                                               [(>= ?expiry ?time)]
                                               [?session-ref :session/user ?member-ref]
                                               [?member-ref :member/e-mail]
                                               ] db session-key (now))]
        (merge
          {:id            (:db/id member)
           :first-name    (:member/first-name member)
           :last-name     (:member/last-name member)
           :focus-locked? (-> member :member/lock boolean)
           :session       {:type   (-> session :session/type :db/ident)
                           :key    session-key
                           :expiry (:session/expiry session)}}
          (when-let [roles (-> member :member/roles seq)]
            {:roles (->> roles (map :db/ident) set)})))))
  )

(defn visitor
  [session-key]
  (when session-key
    (let [db (kuhut-db)]
      (when-let [[visitor-ref session-type session-expiry
                  ] (datomic/q '[:find [?visitor-ref ?session-type ?expiry]
                                 :in $ ?session-key ?time
                                 :where
                                 [?session-ref :session/key ?session-key]
                                 [?session-ref :session/expiry ?expiry]
                                 [(>= ?expiry ?time)]
                                 [?session-ref :session/user ?visitor-ref]
                                 [?session-ref :session/type ?session-type-ref]
                                 [?session-type-ref :db/ident ?session-type]
                                 ] db session-key (now))]
        {:id      visitor-ref
         :session {:type   session-type
                   :key    session-key
                   :expiry session-expiry}})))
  )

(defn create-visitor
  [request-headers]
  (let [session-key (uuid)]
    @(datomic/transact
       (kuhut-connection)
       [{:db/id      "visitor"
         :user/time  (now)
         :user/focus (->> [:concept/id (uuid DEFAULT_TOPIC)] (datomic/entity (kuhut-db)) :db/id)}
        {:session/key     session-key
         :session/user    "visitor"
         :session/headers (freeze request-headers)
         :session/type    :session.type/visitor
         :session/expiry  (now+ (days 7))}])
    (visitor session-key))
  )
