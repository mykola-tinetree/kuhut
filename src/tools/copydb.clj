(ns copydb
  (:require [datomic.api :as datomic]
            [kuhut.common.connection :refer [KUHUT_URI]]
            [kuhut.common.util :refer [now]])
  )

(defn -main [& _]
  (print (str "WARNING: this is a nuclear copy option for the following database: " KUHUT_URI "\nProceed? (y/n) "))
  (flush)
  (let [source "[REDACTED]"]
    (when (= (read-line) "y")
      (println (str "copy database at " KUHUT_URI " from " source))
      (datomic/delete-database KUHUT_URI)
      (datomic/create-database KUHUT_URI)
      (let [read-db (datomic/db (datomic/connect source))
            write-connection (datomic/connect KUHUT_URI)]

        ;; schema
        (println ">> SCHEMA...")
        @(datomic/transact write-connection (-> "./extras/db/schema.edn" slurp read-string))

        ;; database functions
        (println ">> DATABASE FUNCTIONS...")
        @(datomic/transact
           write-connection
           [;; database functions
            {:db/ident :kuhut/toggle-bookmark
             :db/fn    (datomic/function '{:lang   :clojure
                                           :params [db user-ref concept-ref]
                                           :code   (when-let [concept (d/entity db concept-ref)]
                                                     (if-let [concept-popularity (:concept/popularity concept)]
                                                       (let [user (d/entity db user-ref)]
                                                         (if (:member/first-name user)
                                                           (if (some #(= (:db/id %) concept-ref) (:member/interests user))
                                                             [[:db/retract user-ref :member/interests concept-ref]
                                                              [:db/add concept-ref :concept/popularity (dec concept-popularity)]]
                                                             [[:db/add user-ref :member/interests concept-ref]
                                                              [:db/add concept-ref :concept/popularity (inc concept-popularity)]])
                                                           (throw (Exception. (str "visitors cannot bookmark concepts")))))
                                                       (throw (Exception. (str "cannot change the popularity of " (:concept/id concept))))))})}])

        ;; resource
        (println ">> RESOURCE...")
        (doseq [[id type description content] (datomic/q '[:find ?id ?type ?description ?content
                                                           :where
                                                           [?ref :resource/id ?id]
                                                           [?ref :resource/type ?type]
                                                           [?ref :resource/description ?description]
                                                           [?ref :resource/content ?content]
                                                           ] read-db)]
          @(datomic/transact write-connection [{:resource/id          id
                                                :resource/type        type
                                                :resource/description description
                                                :resource/content     content}]))

        ;; template
        (println ">> TEMPLATE...")
        (doseq [[id time content] (datomic/q '[:find ?id ?time ?content
                                               :where
                                               [?ref :template/id ?id]
                                               [?ref :template/time ?time]
                                               [?ref :template/content ?content]
                                               ] read-db)]
          @(datomic/transact write-connection [{:template/id      id
                                                :template/time    time
                                                :template/content content}]))

        ;; concept
        (println ">> CONCEPT...")
        (doseq [ref (datomic/q '[:find [?ref ...]
                                 :where
                                 [?ref :concept/id]
                                 ] read-db)]
          (let [concept (datomic/entity read-db ref)]
            @(datomic/transact write-connection [(merge
                                                   {:concept/id   (:concept/id concept)
                                                    :concept/time (:concept/time concept)
                                                    :concept/name (:concept/name concept)}
                                                   (when-let [description (:concept/description concept)]
                                                     {:concept/description description})
                                                   (when-let [cost (:concept/cost concept)]
                                                     {:concept/cost cost})
                                                   (when-let [tags (-> concept :concept/tags seq)]
                                                     {:concept/tags (vec tags)})
                                                   (when-let [weight (:concept/weight concept)]
                                                     {:concept/weight weight})
                                                   (when-let [popularity (:concept/popularity concept)]
                                                     {:concept/popularity popularity}))])))

        ;; link
        (println ">> LINK...")
        @(datomic/transact
           write-connection
           (mapcat
             (fn [[from time weight to]]
               (let [id (str "link-" (rand))]
                 [{:db/id          id
                   :link/time      time
                   :link/weight    weight
                   :link/reference (let [r (datomic/entity read-db to)]
                                     (if (:concept/id r)
                                       [:concept/id (:concept/id r)]
                                       [:template/id (:template/id r)]))}
                  [:db/add [:concept/id from] :concept/components id]]))
             (datomic/q '[:find ?from ?time ?weight ?to
                          :where
                          [?c :concept/id ?from]
                          [?c :concept/components ?ccs]
                          [?ccs :link/time ?time]
                          [?ccs :link/weight ?weight]
                          [?ccs :link/reference ?to]
                          ] read-db)))
        @(datomic/transact
           write-connection
           (mapcat
             (fn [[from time weight to]]
               (let [id (str "link-" (rand))]
                 [{:db/id          id
                   :link/time      time
                   :link/weight    weight
                   :link/reference (let [r (datomic/entity read-db to)]
                                     (if (:concept/id r)
                                       [:concept/id (:concept/id r)]
                                       [:template/id (:template/id r)]))}
                  [:db/add [:concept/id from] :concept/recommendations id]]))
             (datomic/q '[:find ?from ?time ?weight ?to
                          :where
                          [?c :concept/id ?from]
                          [?c :concept/recommendations ?ccs]
                          [?ccs :link/time ?time]
                          [?ccs :link/weight ?weight]
                          [?ccs :link/reference ?to]
                          ] read-db)))
        @(datomic/transact
           write-connection
           (mapcat
             (fn [[from time weight to]]
               (let [id (str "link-" (rand))]
                 [{:db/id          id
                   :link/time      time
                   :link/weight    weight
                   :link/reference (let [r (datomic/entity read-db to)]
                                     (if (:concept/id r)
                                       [:concept/id (:concept/id r)]
                                       [:template/id (:template/id r)]))}
                  [:db/add [:template/id from] :template/recommendations id]]))
             (datomic/q '[:find ?from ?time ?weight ?to
                          :where
                          [?c :template/id ?from]
                          [?c :template/recommendations ?ccs]
                          [?ccs :link/time ?time]
                          [?ccs :link/weight ?weight]
                          [?ccs :link/reference ?to]
                          ] read-db)))

        ;; member + their visitor
        (println ">> USER...")
        (let [members (datomic/q '[:find ?ref ?e-mail :where [?ref :member/e-mail ?e-mail]] read-db)]
          @(datomic/transact
             write-connection
             (map
               (fn [ref]
                 (let [member (datomic/entity read-db ref)]
                   (merge
                     {:user/time         (or (:user/time member) (now))
                      :user/focus        [:concept/id (-> member :user/focus :concept/id)]
                      :member/e-mail     (:member/e-mail member)
                      :member/password   (:member/password member)
                      :member/first-name (:member/first-name member)
                      :member/last-name  (:member/last-name member)}
                     (when-let [roles (-> member :member/roles seq)]
                       {:member/roles (vec roles)}))))
               (map first members)))
          @(datomic/transact
             write-connection
             (map
               (fn [[e-mail id]]
                 [:db/add [:member/e-mail e-mail] :member/interests [:concept/id id]])
               (datomic/q '[:find ?e-mail ?id
                            :where
                            [?ref :member/e-mail ?e-mail]
                            [?ref :member/interests ?r]
                            [?r :concept/id ?id]
                            ] read-db)))
          (let [visitors (datomic/q '[:find ?from ?e-mail ?id
                                      :where
                                      [?ref :relation/from ?from]
                                      [?ref :relation/to ?to]
                                      [?to :member/e-mail ?e-mail]
                                      [?from :user/focus ?focus]
                                      [?focus :concept/id ?id]
                                      ] read-db)]
            @(datomic/transact
               write-connection
               (mapcat
                 (fn [[ref e-mail concept-id]]
                   (let [id (str "visitor-" (rand))]
                     [{:db/id      id
                       :user/time  (datomic/q '[:find (min ?time) . :in $ ?user :where [?step :step/user ?user] [?step :step/time ?time]] read-db ref)
                       :user/focus [:concept/id concept-id]}
                      [:db/add [:member/e-mail e-mail] :member/visitor id]]))
                 visitors))
            (let [relevant-users (into {} (concat
                                            (map
                                              (fn [[ref e-mail]]
                                                [ref (->> [:member/e-mail e-mail] (datomic/entity (datomic/db write-connection)) :db/id)])
                                              members)
                                            (map
                                              (fn [[ref e-mail]]
                                                [ref (->> [:member/e-mail e-mail] (datomic/entity (datomic/db write-connection)) :member/visitor :db/id)])
                                              visitors)))]

              ;; member + their visitor step
              (println ">> STEP...")
              @(datomic/transact
                 write-connection
                 (map
                   (fn [[ref agent id time data]]
                     {:step/user     (relevant-users ref)
                      :step/agent    agent
                      :step/template [:template/id id]
                      :step/time     time
                      :step/data     data})
                   (datomic/q '[:find ?users ?agent ?id ?time ?data
                                :in $ [?users ...]
                                :where
                                [?step :step/user ?users]
                                [?step :step/agent ?agent-ref]
                                [?agent-ref :db/ident ?agent]
                                [?step :step/template ?template-ref]
                                [?template-ref :template/id ?id]
                                [?step :step/time ?time]
                                [?step :step/data ?data]
                                ] read-db (map first relevant-users))))

              ;; member + their visitor knowledge
              (println ">> KNOWLEDGE...")
              @(datomic/transact
                 write-connection
                 (map
                   (fn [[ref id time level certainty]]
                     {:knowledge/user      (relevant-users ref)
                      :knowledge/reference [:concept/id id]
                      :knowledge/time      time
                      :knowledge/level     level
                      :knowledge/certainty certainty})
                   (datomic/q '[:find ?users ?id ?time ?level ?certainty
                                :in $ [?users ...]
                                :where
                                [?knowledge :knowledge/user ?users]
                                [?knowledge :knowledge/reference ?reference]
                                [?reference :concept/id ?id]
                                [?knowledge :knowledge/time ?time]
                                [?knowledge :knowledge/level ?level]
                                [?knowledge :knowledge/certainty ?certainty]
                                ] read-db (map first relevant-users))))
              @(datomic/transact
                 write-connection
                 (map
                   (fn [[ref id time level certainty]]
                     {:knowledge/user      (relevant-users ref)
                      :knowledge/reference [:template/id id]
                      :knowledge/time      time
                      :knowledge/level     level
                      :knowledge/certainty certainty})
                   (datomic/q '[:find ?users ?id ?time ?level ?certainty
                                :in $ [?users ...]
                                :where
                                [?knowledge :knowledge/user ?users]
                                [?knowledge :knowledge/reference ?reference]
                                [?reference :template/id ?id]
                                [?knowledge :knowledge/time ?time]
                                [?knowledge :knowledge/level ?level]
                                [?knowledge :knowledge/certainty ?certainty]
                                ] read-db (map first relevant-users))))

              )))

        )
      (datomic/shutdown true)
      (println (str "database rebuilt at " KUHUT_URI))))
  )
