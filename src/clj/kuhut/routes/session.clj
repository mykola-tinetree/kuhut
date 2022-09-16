(ns kuhut.routes.session
  (:require [datomic.api :as datomic]
            [hiccup.page :refer [html5]]
            [java-time :refer [days]]
            [kuhut.common.connection :refer [kuhut-connection kuhut-db]]
            [taoensso.nippy :refer [freeze]]
            [clj-http.client :as client]
            [kuhut.common.util :refer [uuid now now+ hash-password unhash-password random-password send-e-mail]]
            [kuhut.common.session :refer [member visitor create-visitor]])
  )

(defn enter
  [{:keys [first-name]} e-mail password remember? request-headers]
  (when (and (not first-name) (seq e-mail) (seq password))
    (let [e-mail (clojure.string/lower-case e-mail)]
      (when-let [user-ref (datomic/q '[:find ?user-ref .
                                       :in $ ?e-mail ?password
                                       :where
                                       [?user-ref :member/e-mail ?e-mail]
                                       [?user-ref :member/password ?hashed-password]
                                       [(kuhut.common.util/unhash-password ?hashed-password) ?unhashed-password]
                                       [(= ?unhashed-password ?password)]
                                       ] (kuhut-db) e-mail password)]
        (let [session-key (uuid)
              expires (now+ (days (if remember? 30 7)))]
          @(datomic/transact
             (kuhut-connection)
             [{:session/key     session-key
               :session/user    user-ref
               :session/headers (freeze request-headers)
               :session/type    (if remember? :session.type/extended :session.type/member)
               :session/expiry  expires}])
          (member session-key)))))
  )

(defn remind-password
  [{:keys [first-name]} e-mail]
  (when-not first-name
    (let [e-mail (clojure.string/lower-case e-mail)]
      (when-let [[user-ref first-name] (datomic/q '[:find [?user-ref ?first-name]
                                                    :in $ ?e-mail
                                                    :where
                                                    [?user-ref :member/e-mail ?e-mail]
                                                    [?user-ref :member/first-name ?first-name]
                                                    ] (kuhut-db) e-mail)]
        (let [new-password (random-password 12)]
          @(datomic/transact (kuhut-connection) [[:db/add user-ref :member/password (hash-password new-password)]])
          (send-e-mail
            e-mail
            "kuhut.com: password reminder"
            (html5
              [:body
               "Dear " first-name "," [:br] [:br]
               "You have requested a reminder of your password." [:br] [:br]
               "Please use this temporary new password to log in: " [:strong new-password] [:br]
               "Once logged in, you will be able to reset the password using the Settings page of your profile." [:br] [:br]
               "Yours Sincerely," [:br]
               "The kuhut Team"]))
          :success))))
  )

(defn exit
  [{user-id :id}]
  (when user-id
    (when-let [session-keys (datomic/q '[:find [?session-key ...]
                                         :in $ ?user-ref
                                         :where
                                         [?session-key :session/user ?user-ref]
                                         ] (kuhut-db) user-id)]
      (let [t (now)]
        @(datomic/transact (kuhut-connection) (mapv #(identity {:db/id % :session/expiry t}) session-keys)))))
  )

(defn reset-password
  [{user-id :id :keys [first-name]} old-password new-password]
  (when first-name
    (let [current-password (unhash-password (datomic/q '[:find ?password .
                                                         :in $ ?user-ref
                                                         :where
                                                         [?user-ref :member/password ?password]
                                                         ] (kuhut-db) user-id))]
      (boolean
        (when (= old-password current-password)
          @(datomic/transact (kuhut-connection) [[:db/add user-id :member/password (hash-password new-password)]])))))
  )

(defn update-name
  [{user-id :id :keys [first-name]} new-first-name new-last-name]
  (when first-name
    (let [[current-first-name current-last-name] (datomic/q '[:find [?first-name ?last-name]
                                                              :in $ ?user-ref
                                                              :where
                                                              [?user-ref :member/first-name ?first-name]
                                                              [?user-ref :member/last-name ?last-name]
                                                              ] (kuhut-db) user-id)
          actions (concat
                    (when (not= current-first-name new-first-name)
                      [[:db/add user-id :member/first-name new-first-name]])
                    (when (not= current-last-name new-last-name)
                      [[:db/add user-id :member/last-name new-last-name]]))]
      (boolean
        (when (seq actions)
          @(datomic/transact (kuhut-connection) actions)))))
  )

(defn- verify-captcha
  [captcha]
  (-> (client/post "[REDACTED]"
                   {:form-params {:secret   "[REDACTED]"
                                  :response captcha}
                    :accept      :json
                    :as          :json})
      :body :success)
  )

(defn join
  [{user-id :id user-first-name :first-name} first-name last-name e-mail keep-progress? captcha session-key headers]
  (when-not user-first-name
    (when (verify-captcha captcha)
      (let [db (kuhut-db)
            e-mail (clojure.string/lower-case e-mail)]
        (when-not (datomic/entity db [:member/e-mail e-mail])
          (let [new-password (random-password 12)]
            @(datomic/transact
               (kuhut-connection)
               (if keep-progress?
                 [[:db/add user-id :member/e-mail e-mail]
                  [:db/add user-id :member/password (hash-password new-password)]
                  [:db/add user-id :member/first-name first-name]
                  [:db/add user-id :member/last-name last-name]]
                 [{:user/time         (now)
                   :user/focus        (datomic/q '[:find ?focus-ref . :in $ ?user-ref :where [?user-ref :user/focus ?focus-ref]] db user-id)
                   :member/e-mail     e-mail
                   :member/password   (hash-password new-password)
                   :member/first-name first-name
                   :member/last-name  last-name
                   :member/visitor    user-id}]))
            (send-e-mail
              e-mail
              "kuhut.com: e-mail address confirmation request"
              (html5
                [:body
                 "Dear " first-name "," [:br] [:br]
                 "Thank you for registering as a member of kuhut." [:br]
                 "Your temporary password is: " [:strong new-password] [:br] [:br]
                 "Use this password and e-mail to login at " [:a {:href "https://kuhut.com"} "https://kuhut.com"] [:br]
                 "Once logged in, you will be able to change the password using the Settings page of your profile." [:br] [:br]
                 "Yours Sincerely," [:br]
                 "The kuhut Team"]))
            (if keep-progress?
              (create-visitor headers)
              (visitor session-key)))))))
  )
