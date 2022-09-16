(ns kuhut.routes.studio
  (:require [datomic.api :as datomic]
            [java-time :refer [to-millis-from-epoch]]
            [taoensso.nippy :refer [freeze thaw]]
            [kuhut.routes.content :refer [get-knowledge]]
            [kuhut.common.util :refer [now update-tf-idf-search-stats unhash-password]]
            [kuhut.common.connection :refer [kuhut-db kuhut-connection]]
            [clojure.string :refer [index-of lower-case]])
  (:import (java.io FileInputStream))
  )

(declare studio-template)
(declare studio-resource)

(defn studio-concept
  [user db basic? id]
  (let [concept (datomic/pull db [:db/id :concept/name :concept/description :concept/tags :concept/weight :concept/popularity :concept/cost] [:concept/id id])]
    (merge
      {:id             id
       :type           :concept
       :is-searchable? (-> concept :concept/popularity some?)}
      (when-let [name (:concept/name concept)]
        {:name name})
      (when-let [description (:concept/description concept)]
        {:description description})
      (when-let [tags (-> concept :concept/tags seq)]
        {:tags (set tags)})
      (when-let [weight (:concept/weight concept)]
        {:weight weight})
      (when-let [cost (:concept/cost concept)]
        {:cost cost})
      (when-not basic?
        (merge
          (when-let [components (->> (datomic/pull db [{:concept/components [:db/id {:link/reference [:concept/id :template/id]}
                                                                             :link/time :link/weight]}] [:concept/id id]) :concept/components
                                     (map (fn [{ref :db/id time :link/time weight :link/weight reference :link/reference}]
                                            {:ref       ref
                                             :component (if-let [concept-id (:concept/id reference)]
                                                          (studio-concept user db true concept-id)
                                                          (studio-template user db true (:template/id reference)))
                                             :time      (to-millis-from-epoch time)
                                             :weight    weight}))
                                     (sort-by :time)
                                     seq)]
            {:components components})
          (when-let [component-of (->> (datomic/q '[:find ?link-ref ?target-id ?link-time ?link-weight
                                                    :in $ ?id
                                                    :where
                                                    [?concept-ref :concept/id ?id]
                                                    [?link-ref :link/reference ?concept-ref]
                                                    [?target-ref :concept/components ?link-ref]
                                                    [?target-ref :concept/id ?target-id]
                                                    [?link-ref :link/time ?link-time]
                                                    [?link-ref :link/weight ?link-weight]
                                                    ] db id)
                                       (map (fn [[ref id time weight]] {:ref     ref
                                                                        :concept (studio-concept user db true id)
                                                                        :time    (to-millis-from-epoch time)
                                                                        :weight  weight}))
                                       (sort-by :time)
                                       seq)]
            {:component-of component-of})
          (when-let [recommendations (->> (datomic/pull db [{:concept/recommendations [:db/id {:link/reference [:concept/id :template/id]}
                                                                                       :link/time :link/weight]}] [:concept/id id]) :concept/recommendations
                                          (map (fn [{ref :db/id time :link/time weight :link/weight reference :link/reference}]
                                                 {:ref            ref
                                                  :recommendation (if-let [concept-id (:concept/id reference)]
                                                                    (studio-concept user db true concept-id)
                                                                    (studio-template user db true (:template/id reference)))
                                                  :time           (to-millis-from-epoch time)
                                                  :weight         weight}))
                                          (sort-by :time)
                                          seq)]
            {:recommendations recommendations})
          (when-let [recommended-for (->> (datomic/q '[:find [(pull ?link-ref [:db/id :link/time :link/weight
                                                                               {:concept/_recommendations [:concept/id]}
                                                                               {:template/_recommendations [:template/id]}]) ...]
                                                       :in $ ?id
                                                       :where
                                                       [?concept-ref :concept/id ?id]
                                                       [?link-ref :link/reference ?concept-ref]
                                                       (or [?target-ref :concept/recommendations ?link-ref]
                                                           [?target-ref :template/recommendations ?link-ref])
                                                       ] db id)
                                          (map (fn [{ref :db/id time :link/time weight :link/weight :as link}]
                                                 {:ref     ref
                                                  :content (if-let [concept-id (-> link :concept/_recommendations :concept/id)]
                                                             (studio-concept user db true concept-id)
                                                             (studio-template user db true (-> link :template/_recommendations :template/id)))
                                                  :time    (to-millis-from-epoch time)
                                                  :weight  weight}))
                                          (sort-by :time)
                                          seq)]
            {:recommended-for recommended-for})
          (when-let [relations (->> (datomic/q '[:find [(pull ?relation-ref [:db/id :relation/time :relation/type {:relation/to [:concept/id :template/id :resource/id]}]) ...]
                                                 :in $ ?id
                                                 :where
                                                 [?concept-ref :concept/id ?id]
                                                 [?relation-ref :relation/from ?concept-ref]
                                                 ] db id)
                                    (map (fn [{ref :db/id time :relation/time type :relation/type :as relation}]
                                           {:ref      ref
                                            :relation (if-let [concept-id (-> relation :relation/to :concept/id)]
                                                        (studio-concept user db true concept-id)
                                                        (if-let [template-id (-> relation :relation/to :template/id)]
                                                          (studio-template user db true template-id)
                                                          (studio-resource user db true (-> relation :relation/to :resource/id))))
                                            :time     (to-millis-from-epoch time)
                                            :type     type}))
                                    (sort-by :time)
                                    seq)]
            {:relations relations})
          (when-let [related-to (->> (datomic/q '[:find [(pull ?relation-ref [:db/id :relation/time :relation/type {:relation/from [:concept/id :template/id :resource/id]}]) ...]
                                                  :in $ ?id
                                                  :where
                                                  [?concept-ref :concept/id ?id]
                                                  [?relation-ref :relation/to ?concept-ref]
                                                  ] db id)
                                     (map (fn [{ref :db/id time :relation/time type :relation/type :as relation}]
                                            {:ref     ref
                                             :content (if-let [concept-id (-> relation :relation/from :concept/id)]
                                                        (studio-concept user db true concept-id)
                                                        (if-let [template-id (-> relation :relation/from :template/id)]
                                                          (studio-template user db true template-id)
                                                          (studio-resource user db true (-> relation :relation/from :resource/id))))
                                             :time    (to-millis-from-epoch time)
                                             :type    type}))
                                     (sort-by :time)
                                     seq)]
            {:related-to related-to})))))
  )

(defn studio-template
  [user db basic? id]
  (let [{ref     :db/id
         content :template/content} (datomic/entity db [:template/id id])]
    (merge
      {:id      id
       :type    :template
       :content content}
      (if basic?
        (when-let [staleness (datomic/q '[:find (count ?flag-ref) .
                                          :in $ ?ref
                                          :where
                                          [?flag-ref :flag/step ?step-ref]
                                          [?step-ref :step/template ?ref]
                                          ] db ref)]
          {:staleness staleness})
        (when-let [flags (->> (datomic/q '[:find ?user-ref ?e-mail ?first-name ?last-name
                                           :in $ ?ref
                                           :where
                                           [?flag-ref :flag/step ?step-ref]
                                           [?step-ref :step/template ?ref]
                                           [?step-ref :step/user ?user-ref]
                                           [?user-ref :member/e-mail ?e-mail]
                                           [?user-ref :member/first-name ?first-name]
                                           [?user-ref :member/last-name ?last-name]
                                           ] db ref)
                              (map (fn [[user-ref e-mail first-name last-name]]
                                     {:id         user-ref
                                      :e-mail     e-mail
                                      :first-name first-name
                                      :last-name  last-name}))
                              (sort-by :e-mail)
                              seq)]
          {:flags flags}))
      (when-not basic?
        (merge
          (when-let [component-of (->> (datomic/q '[:find ?link-ref ?target-id ?link-time ?link-weight
                                                    :in $ ?template-ref
                                                    :where
                                                    [?link-ref :link/reference ?template-ref]
                                                    [?target-ref :concept/components ?link-ref]
                                                    [?target-ref :concept/id ?target-id]
                                                    [?link-ref :link/time ?link-time]
                                                    [?link-ref :link/weight ?link-weight]
                                                    ] db ref)
                                       (map (fn [[ref id time weight]] {:ref     ref
                                                                        :concept (studio-concept user db true id)
                                                                        :time    (to-millis-from-epoch time)
                                                                        :weight  weight}))
                                       (sort-by :time)
                                       seq)]
            {:component-of component-of})
          (when-let [recommendations (->> (datomic/pull db [{:template/recommendations [:db/id {:link/reference [:concept/id :template/id]}
                                                                                        :link/time :link/weight]}] ref) :template/recommendations
                                          (map (fn [{ref :db/id time :link/time weight :link/weight reference :link/reference}]
                                                 {:ref            ref
                                                  :recommendation (if-let [concept-id (:concept/id reference)]
                                                                    (studio-concept user db true concept-id)
                                                                    (studio-template user db true (:template/id reference)))
                                                  :time           (to-millis-from-epoch time)
                                                  :weight         weight}))
                                          (sort-by :time)
                                          seq)]
            {:recommendations recommendations})
          (when-let [recommended-for (->> (datomic/q '[:find [(pull ?link-ref [:db/id :link/time :link/weight
                                                                               {:concept/_recommendations [:concept/id]}
                                                                               {:template/_recommendations [:template/id]}]) ...]
                                                       :in $ ?template-ref
                                                       :where
                                                       [?link-ref :link/reference ?template-ref]
                                                       (or [?target-ref :concept/recommendations ?link-ref]
                                                           [?target-ref :template/recommendations ?link-ref])
                                                       ] db ref)
                                          (map (fn [{ref :db/id time :link/time weight :link/weight :as link}]
                                                 {:ref     ref
                                                  :content (if-let [concept-id (-> link :concept/_recommendations :concept/id)]
                                                             (studio-concept user db true concept-id)
                                                             (studio-template user db true (-> link :template/_recommendations :template/id)))
                                                  :time    (to-millis-from-epoch time)
                                                  :weight  weight}))
                                          (sort-by :time)
                                          seq)]
            {:recommended-for recommended-for})
          (when-let [relations (->> (datomic/q '[:find [(pull ?relation-ref [:db/id :relation/time :relation/type {:relation/to [:concept/id :template/id :resource/id]}]) ...]
                                                 :in $ ?template-ref
                                                 :where
                                                 [?relation-ref :relation/from ?template-ref]
                                                 ] db ref)
                                    (map (fn [{ref :db/id time :relation/time type :relation/type :as relation}]
                                           {:ref      ref
                                            :relation (if-let [concept-id (-> relation :relation/to :concept/id)]
                                                        (studio-concept user db true concept-id)
                                                        (if-let [template-id (-> relation :relation/to :template/id)]
                                                          (studio-template user db true template-id)
                                                          (studio-resource user db true (-> relation :relation/to :resource/id))))
                                            :time     (to-millis-from-epoch time)
                                            :type     type}))
                                    (sort-by :time)
                                    seq)]
            {:relations relations})
          (when-let [related-to (->> (datomic/q '[:find [(pull ?relation-ref [:db/id :relation/time :relation/type {:relation/from [:concept/id :template/id :resource/id]}]) ...]
                                                  :in $ ?template-ref
                                                  :where
                                                  [?relation-ref :relation/to ?template-ref]
                                                  ] db ref)
                                     (map (fn [{ref :db/id time :relation/time type :relation/type :as relation}]
                                            {:ref     ref
                                             :content (if-let [concept-id (-> relation :relation/from :concept/id)]
                                                        (studio-concept user db true concept-id)
                                                        (if-let [template-id (-> relation :relation/from :template/id)]
                                                          (studio-template user db true template-id)
                                                          (studio-resource user db true (-> relation :relation/from :resource/id))))
                                             :time    (to-millis-from-epoch time)
                                             :type    type}))
                                     (sort-by :time)
                                     seq)]
            {:related-to related-to})))))
  )

(defn studio-resource
  [user db basic? id]
  (let [resource (datomic/pull db [:db/id :resource/type :resource/description] [:resource/id id])]
    (merge
      {:id   id
       :type :resource}
      (when-let [content-type (:resource/type resource)]
        {:content-type content-type})
      (when-let [description (:resource/description resource)]
        {:description description})
      (when-not basic?
        (merge
          (when-let [relations (->> (datomic/q '[:find [(pull ?relation-ref [:db/id :relation/time :relation/type {:relation/to [:concept/id :template/id :resource/id]}]) ...]
                                                 :in $ ?id
                                                 :where
                                                 [?resource-ref :resource/id ?id]
                                                 [?relation-ref :relation/from ?resource-ref]
                                                 ] db id)
                                    (map (fn [{ref :db/id time :relation/time type :relation/type :as relation}]
                                           {:ref      ref
                                            :relation (if-let [concept-id (-> relation :relation/to :concept/id)]
                                                        (studio-concept user db true concept-id)
                                                        (if-let [template-id (-> relation :relation/to :template/id)]
                                                          (studio-template user db true template-id)
                                                          (studio-resource user db true (-> relation :relation/to :resource/id))))
                                            :time     (to-millis-from-epoch time)
                                            :type     type}))
                                    (sort-by :time)
                                    seq)]
            {:relations relations})
          (when-let [related-to (->> (datomic/q '[:find [(pull ?relation-ref [:db/id :relation/time :relation/type {:relation/from [:concept/id :template/id :resource/id]}]) ...]
                                                  :in $ ?id
                                                  :where
                                                  [?resource-ref :resource/id ?id]
                                                  [?relation-ref :relation/to ?resource-ref]
                                                  ] db id)
                                     (map (fn [{ref :db/id time :relation/time type :relation/type :as relation}]
                                            {:ref     ref
                                             :content (if-let [concept-id (-> relation :relation/from :concept/id)]
                                                        (studio-concept user db true concept-id)
                                                        (if-let [template-id (-> relation :relation/from :template/id)]
                                                          (studio-template user db true template-id)
                                                          (studio-resource user db true (-> relation :relation/from :resource/id))))
                                             :time    (to-millis-from-epoch time)
                                             :type    type}))
                                     (sort-by :time)
                                     seq)]
            {:related-to related-to})))))
  )

(defn studio-search-contents
  [user search-text only-types ts]
  (let [db (kuhut-db)
        search-text (-> search-text (or "") clojure.string/lower-case)
        data (concat
               (when (contains? only-types :concept)
                 (map
                   #(conj % :concept)
                   (keep
                     (fn [concept]
                       (when-let [ls (seq (concat
                                            (let [id (-> concept :concept/id str)]
                                              (when (-> id lower-case (index-of search-text))
                                                [(count id)]))
                                            (when-let [name (:concept/name concept)]
                                              (when (-> name lower-case (index-of search-text))
                                                [(count name)]))
                                            (when-let [description (:concept/description concept)]
                                              (when (-> description lower-case (index-of search-text))
                                                [(count description)]))
                                            (keep
                                              (fn [tag]
                                                (when (-> tag lower-case (index-of search-text))
                                                  (count tag)))
                                              (:concept/tags concept))))]
                         [(:concept/id concept) (apply min ls)]))
                     (datomic/q '[:find [(pull ?ref [:concept/id :concept/name :concept/description :concept/tags]) ...] :where [?ref :concept/id]] db))))
               (when (contains? only-types :template)
                 (map
                   #(conj % :template)
                   (datomic/q '[:find ?template-id (min ?len)
                                :in $ ?search-text
                                :where
                                [?template-ref :template/id ?template-id]
                                [(get-else $ ?template-ref :template/content "") ?template-content]
                                [(str ?template-id) ?template-id-str]
                                [(clojure.string/lower-case ?template-content) ?template-content-lc]
                                (or-join [?template-id-str ?template-content-lc ?search-text ?len]
                                         (and [(clojure.string/index-of ?template-id-str ?search-text)]
                                              [(count ?template-id-str) ?len])
                                         (and [(clojure.string/index-of ?template-content-lc ?search-text)]
                                              [(count ?template-content-lc) ?len]))
                                ] db search-text)))
               (when (contains? only-types :resource)
                 (map
                   #(conj % :resource)
                   (datomic/q '[:find ?resource-id (min ?len)
                                :in $ ?search-text
                                :where
                                [?resource-ref :resource/id ?resource-id]
                                [(get-else $ ?resource-ref :resource/description "") ?resource-description]
                                [(str ?resource-id) ?resource-id-str]
                                [(clojure.string/lower-case ?resource-description) ?resource-description-lc]
                                (or-join [?resource-id-str ?resource-description-lc ?search-text ?len]
                                         (and [(clojure.string/index-of ?resource-id-str ?search-text)]
                                              [(count ?resource-id-str) ?len])
                                         (and [(clojure.string/index-of ?resource-description-lc ?search-text)]
                                              [(count ?resource-description-lc) ?len]))
                                ] db search-text))))]
    {:ts    ts
     :data  (->> data
                 (sort-by second)
                 (take 20)
                 (map (fn [[id _ type]] ((case type :concept studio-concept :template studio-template studio-resource) user db true id))))
     :count (count data)})
  )

(defn studio-search-users
  [user search-text include-visitors? ts]
  (let [db (kuhut-db)
        search-text (-> search-text (or "") clojure.string/lower-case)
        data (keep
               (fn [user]
                 (when-let [ls (seq (concat
                                      (let [e-mail (:member/e-mail user)]
                                        (when (-> e-mail lower-case (index-of search-text))
                                          [(count e-mail)]))
                                      (let [first-name (:member/first-name user)]
                                        (when (-> first-name lower-case (index-of search-text))
                                          [(count first-name)]))
                                      (let [last-name (:member/last-name user)]
                                        (when (-> last-name lower-case (index-of search-text))
                                          [(count last-name)]))))]
                   [(:db/id user) (apply min ls)]))
               (datomic/q '[:find [(pull ?ref [:db/id :member/e-mail :member/first-name :member/last-name]) ...] :where [?ref :member/e-mail]] db))]
    {:ts    ts
     :data  (->> data
                 (sort-by second)
                 (take 20)
                 (map (fn [[id]]
                        (let [member (datomic/pull db [:member/e-mail :member/first-name :member/last-name {:member/interests [:concept/id]}
                                                       {:user/focus [:concept/id]} {:member/roles [:db/ident]} :order/_user
                                                       {:member/lock [:lock/e-mail :lock/pin]}] id)]
                          (merge
                            {:id               id
                             :e-mail           (:member/e-mail member)
                             :first-name       (:member/first-name member)
                             :last-name        (:member/last-name member)
                             :roles            (->> member :member/roles (map :db/ident) set)
                             :focus            (-> member :user/focus :concept/id)
                             :interests        (->> member :member/interests (map :concept/id))
                             :number-of-orders (-> member :order/_user count)}
                            (when-let [lock (:member/lock member)]
                              {:lock {:e-mail (:lock/e-mail lock)
                                      :pin    (-> lock :lock/pin unhash-password)}}))))))
     :count (count data)})
  )

(defn studio-users-join-times
  [user include-visitors?]
  (->> (datomic/q
         (into '[:find ?t ?user-ref
                 :where
                 [?user-ref :user/time ?t]]
               (when-not include-visitors?
                 ['[?user-ref :member/e-mail ?e-mail]]))
         (kuhut-db))
       (sort-by first)
       (reduce
         (fn [[[f s] & c :as q] [t]]
           (if f
             (if (-> t .getTime (- (.getTime f)) (> 86400000))
               (cons [t (inc s)] q)
               (cons [f (inc s)] c))
             (list [t 1])))
         nil)
       reverse
       vec)
  )

(defn studio-user-knowledge
  [user id]
  (keep
    (fn [concept]
      (when (not-any? :concept/_components (:link/_reference concept))
        (:concept/id concept)))
    (datomic/q '[:find [(pull ?ref [:concept/id {:link/_reference [:concept/_components]}]) ...]
                 :in $ ?user-ref
                 :where
                 [?knowledge-ref :knowledge/user ?user-ref]
                 [?knowledge-ref :knowledge/reference ?ref]
                 [?ref :concept/id ?id]
                 ] (kuhut-db) id))
  )

(defn studio-user-concept
  [user user-id id]
  (let [db (kuhut-db)
        concept-ref (->> [:concept/id id] (datomic/entity db) :db/id)]
    {:data       (studio-concept user db true id)
     :components (map
                   (fn [component]
                     (let [cid (-> component :link/reference :concept/id)
                           tid (-> component :link/reference :template/id)]
                       [(or cid tid) (if cid :concept :template) (:link/weight component)]))
                   (datomic/q '[:find [(pull ?ref [{:link/reference [:concept/id :template/id]} :link/weight]) ...]
                                :in $ ?concept-ref
                                :where
                                [?concept-ref :concept/components ?ref]
                                ] db concept-ref))
     :level      (:level (get-knowledge db user-id concept-ref))})
  )

(defn studio-user-template
  [user user-id id]
  (let [db (kuhut-db)]
    {:data  (studio-template user db true id)
     :level (:level (get-knowledge db user-id (->> [:template/id id] (datomic/entity db) :db/id)))})
  )

(defn studio-add-content
  [user id type]
  @(datomic/transact
     (kuhut-connection)
     [(case type
        :concept {:concept/id   id
                  :concept/time (now)}
        :template {:template/id      id
                   :template/time    (now)
                   :template/content ""}
        {:resource/id id})])
  true
  )

(defn- studio-duplicate-concept
  [db id from]
  (let [t (now)
        concept (datomic/pull db [:db/id :concept/name :concept/description :concept/tags :concept/popularity
                                  :concept/cost :concept/weight
                                  {:concept/components [:link/reference :link/weight]}
                                  {:concept/recommendations [:link/reference :link/weight]}]
                              [:concept/id from])]
    @(datomic/transact
       (kuhut-connection)
       (vec
         (concat
           [(merge
              {:db/id        "concept"
               :concept/id   id
               :concept/time t}
              (let [name (:concept/name concept)]
                (when (seq name)
                  {:concept/name (str name " copy")}))
              (let [description (:concept/description concept)]
                (when (seq description)
                  {:concept/description description}))
              (let [tags (:concept/tags concept)]
                (when (seq tags)
                  {:concept/tags (vec tags)}))
              (when-let [weight (:concept/weight concept)]
                {:concept/weight weight})
              (when-let [popularity (:concept/popularity concept)]
                {:concept/popularity popularity})
              (when-let [cost (:concept/cost concept)]
                {:concept/cost cost})
              (when-let [components (-> concept :concept/components seq)]
                {:concept/components (mapv
                                       (fn [component]
                                         {:link/reference (-> component :link/reference :db/id)
                                          :link/time      t
                                          :link/weight    (:link/weight component)})
                                       components)})
              (when-let [recommendations (-> concept :concept/recommendations seq)]
                {:concept/recommendations (mapv
                                            (fn [recommendation]
                                              {:link/reference (-> recommendation :link/reference :db/id)
                                               :link/time      t
                                               :link/weight    (:link/weight recommendation)})
                                            recommendations)}))]
           (map
             (fn [[to type]]
               {:relation/from "concept"
                :relation/to   to
                :relation/time t
                :relation/type type})
             (datomic/q '[:find ?relation-to-ref ?relation-type
                          :in $ ?from
                          :where
                          [?concept-ref :concept/id ?from]
                          [?relation-ref :relation/from ?concept-ref]
                          [?relation-ref :relation/to ?relation-to-ref]
                          [?relation-ref :relation/type ?relation-type]
                          ] db from))))))
  )

(defn- studio-duplicate-template
  [db id from]
  (let [t (now)
        template (datomic/pull db [:db/id :template/content
                                   {:template/recommendations [:link/reference :link/weight]}]
                               [:template/id from])]
    @(datomic/transact
       (kuhut-connection)
       (vec
         (concat
           [(merge
              {:db/id         "template"
               :template/id   id
               :template/time t}
              (when-let [content (:template/content template)]
                {:template/content content})
              (when-let [recommendations (-> template :template/recommendations seq)]
                {:template/recommendations (mapv
                                             (fn [recommendation]
                                               {:link/reference (-> recommendation :link/reference :db/id)
                                                :link/time      t
                                                :link/weight    (:link/weight recommendation)})
                                             recommendations)}))]
           (map
             (fn [[to type]]
               {:relation/from "template"
                :relation/to   to
                :relation/time t
                :relation/type type})
             (datomic/q '[:find ?relation-to-ref ?relation-type
                          :in $ ?from
                          :where
                          [?template-ref :template/id ?from]
                          [?relation-ref :relation/from ?template-ref]
                          [?relation-ref :relation/to ?relation-to-ref]
                          [?relation-ref :relation/type ?relation-type]
                          ] db from))))))
  )

(defn- studio-duplicate-resource
  [db id from]
  (let [t (now)
        resource (datomic/pull db [:db/id :resource/description :resource/type :resource/content]
                               [:resource/id from])]
    @(datomic/transact
       (kuhut-connection)
       (vec
         (concat
           [(merge
              {:db/id       "resource"
               :resource/id id}
              (let [description (:resource/description resource)]
                (when (seq description)
                  {:resource/description (str description " copy")}))
              (when-let [type (:resource/type resource)]
                {:resource/type type})
              (when-let [content (:resource/content resource)]
                {:resource/content content}))]
           (map
             (fn [[to type]]
               {:relation/from "resource"
                :relation/to   to
                :relation/time t
                :relation/type type})
             (datomic/q '[:find ?relation-to-ref ?relation-type
                          :in $ ?from
                          :where
                          [?resource-ref :resource/id ?from]
                          [?relation-ref :relation/from ?resource-ref]
                          [?relation-ref :relation/to ?relation-to-ref]
                          [?relation-ref :relation/type ?relation-type]
                          ] db from))))))
  )

(defn studio-duplicate-content
  [user id from type]
  ((case type
     :concept studio-duplicate-concept
     :template studio-duplicate-template
     studio-duplicate-resource)
    (kuhut-db) id from)
  true
  )

(defn- update-times
  [id]
  (let [db (kuhut-db)
        t (now)
        concepts (loop [[f & more] [(->> [:concept/id id] (datomic/entity db) :db/id)]
                        result #{}]
                   (if f
                     (recur
                       (into more (datomic/q '[:find [?ref ...]
                                               :in $ ?concept-ref
                                               :where
                                               [?link-ref :link/reference ?concept-ref]
                                               [?ref :concept/components ?link-ref]
                                               ] db f))
                       (conj result f))
                     result))]
    @(datomic/transact (kuhut-connection) (map (fn [ref] [:db/add ref :concept/time t]) concepts)))
  )

(defn- update-concept-weight
  [id]
  (let [db (kuhut-db)
        weight (->> [:concept/id id] (datomic/entity db) :concept/weight)]
    @(datomic/transact
       (kuhut-connection)
       [(if-let [total-weight (datomic/q '[:find (sum ?weight) .
                                           :with ?link-ref
                                           :in $ ?from
                                           :where
                                           [?from-ref :concept/id ?from]
                                           [?from-ref :concept/components ?link-ref]
                                           [?link-ref :link/weight ?weight]
                                           ] db id)]
          [:db/add [:concept/id id] :concept/weight total-weight]
          [:db/retract [:concept/id id] :concept/weight weight])]))
  )

(defn studio-clear-template-flags
  [user id]
  (when-let [actions (->> (datomic/q '[:find [?ref ...]
                                       :in $ ?id
                                       :where
                                       [?template-ref :template/id ?id]
                                       [?ref :flag/step ?step-ref]
                                       [?step-ref :step/template ?template-ref]
                                       ] (kuhut-db) id)
                          (map (fn [ref] [:db.fn/retractEntity ref]))
                          seq)]
    @(datomic/transact (kuhut-connection) actions)
    true)
  )

(defn studio-clear-template-flag
  [user id user-id]
  (when-let [ref (datomic/q '[:find ?ref .
                              :in $ ?id ?user-id
                              :where
                              [?template-ref :template/id ?id]
                              [?step-ref :step/template ?template-ref]
                              [?step-ref :step/user ?user-id]
                              [?ref :flag/step ?step-ref]
                              ] (kuhut-db) id user-id)]
    @(datomic/transact
       (kuhut-connection)
       [[:db.fn/retractEntity ref]])
    true)
  )

(defn studio-remove-content
  [user id type]
  (let [db (kuhut-db)
        content (datomic/entity db [(-> type name (str "/id") keyword) id])
        ref (:db/id content)
        parents (when (#{:concept :template} type)
                  (datomic/q '[:find [?parent ...]
                               :in $ ?ref
                               :where
                               [?link-ref :link/reference ?ref]
                               [?concept-ref :concept/components ?link-ref]
                               [?concept-ref :concept/id ?parent]
                               ] db ref))]
    (when (= type :template)
      (studio-clear-template-flags user id))
    @(datomic/transact
       (kuhut-connection)
       (mapv
         (fn [ref] [:db.fn/retractEntity ref])
         (concat
           [ref]
           (datomic/q '[:find [?link-ref ...]
                        :in $ ?content-ref
                        :where
                        [?link-ref :link/reference ?content-ref]
                        ] db ref)
           (datomic/q '[:find [?relation-ref ...]
                        :in $ ?content-ref
                        :where
                        (or [?relation-ref :relation/from ?content-ref]
                            [?relation-ref :relation/to ?content-ref])
                        ] db ref)
           (datomic/q '[:find [?exercise-ref ...]
                        :in $ ?content-ref
                        :where
                        [?exercise-ref :exercise/concept ?content-ref]
                        ] db ref)
           (datomic/q '[:find [?exercise-ref ...]
                        :in $ ?content-ref
                        :where
                        [?exercise-ref :step/template ?content-ref]
                        [?exercise-ref :exercise/current-step ?step-ref]
                        ] db ref)
           (datomic/q '[:find [?knowledge-ref ...]
                        :in $ ?content-ref
                        :where
                        [?knowledge-ref :knowledge/reference ?content-ref]
                        ] db ref))))
    (when (#{:concept :template} type)
      (doseq [parent parents]
        (update-times parent)
        (update-concept-weight parent))
      (when (:concept/popularity content)
        (update-tf-idf-search-stats)))
    true)
  )

(defn studio-remove-resource-content
  [user id]
  (when-let [[ref content-type content] (datomic/q '[:find [?ref ?content-type ?content]
                                                     :in $ ?id
                                                     :where
                                                     [?ref :resource/id ?id]
                                                     [?ref :resource/type ?content-type]
                                                     [?ref :resource/content ?content]
                                                     ] (kuhut-db) id)]
    @(datomic/transact
       (kuhut-connection)
       [[:db/retract ref :resource/type content-type]
        [:db/retract ref :resource/content content]])
    true)
  )

(defn studio-change-content-type
  [user id from to]
  (studio-remove-content user id from)
  (studio-add-content user id to)
  )

(defn studio-set-concept-name
  [user id name]
  (let [concept (datomic/entity (kuhut-db) [:concept/id id])]
    (when-let [actions (seq (if (seq name)
                              [[:db/add [:concept/id id] :concept/name name]]
                              (when-let [name (:concept/name concept)]
                                [[:db/retract [:concept/id id] :concept/name name]])))]
      @(datomic/transact
         (kuhut-connection)
         (into
           [[:db/add [:concept/id id] :concept/time (now)]]
           actions)))
    (when (:concept/popularity concept)
      (update-tf-idf-search-stats))
    true)
  )

(defn studio-set-concept-description
  [user id description]
  (let [concept (datomic/entity (kuhut-db) [:concept/id id])]
    (when-let [actions (seq (if (seq description)
                              [[:db/add [:concept/id id] :concept/description description]]
                              (when-let [description (:concept/description concept)]
                                [[:db/retract [:concept/id id] :concept/description description]])))]
      @(datomic/transact
         (kuhut-connection)
         (into
           [[:db/add [:concept/id id] :concept/time (now)]]
           actions)))
    (when (:concept/popularity concept)
      (update-tf-idf-search-stats))
    true)
  )

(defn studio-set-concept-cost
  [user id cost]
  (when-let [actions (seq (if cost
                            [[:db/add [:concept/id id] :concept/cost (double cost)]]
                            (when-let [cost (->> [:concept/id id] (datomic/entity (kuhut-db)) :concept/cost)]
                              [[:db/retract [:concept/id id] :concept/cost cost]])))]
    @(datomic/transact
       (kuhut-connection)
       (into
         [[:db/add [:concept/id id] :concept/time (now)]]
         actions))
    true)
  )

(defn studio-set-resource-description
  [user id description]
  (when-let [actions (seq (if (seq description)
                            [[:db/add [:resource/id id] :resource/description description]]
                            (when-let [description (->> [:resource/id id] (datomic/entity (kuhut-db)) :resource/description)]
                              [[:db/retract [:resource/id id] :resource/description description]])))]
    @(datomic/transact
       (kuhut-connection)
       actions)
    true)
  )

(defn studio-set-concept-searchability
  [user id is-searchable?]
  @(datomic/transact
     (kuhut-connection)
     (into
       [[:db/add [:concept/id id] :concept/time (now)]]
       (if is-searchable?
         [[:db/add [:concept/id id] :concept/popularity 0]]
         (let [db (kuhut-db)
               {ref :db/id :as concept} (datomic/pull db [:db/id :concept/popularity] [:concept/id id])]
           (concat
             [[:db/retract ref :concept/popularity (:concept/popularity concept)]]
             (map
               (fn [member-ref] [:db/retract member-ref :member/interests ref])
               (datomic/q '[:find [?member-ref ...]
                            :in $ ?interest-ref
                            :where
                            [?member-ref :member/interests ?interest-ref]
                            ] db ref)))))))
  (update-tf-idf-search-stats)
  true
  )

(defn studio-add-concept-tag
  [user id tag]
  @(datomic/transact
     (kuhut-connection)
     [[:db/add [:concept/id id] :concept/time (now)]
      [:db/add [:concept/id id] :concept/tags tag]])
  (when (->> [:concept/id id] (datomic/entity (kuhut-db)) :concept/popularity)
    (update-tf-idf-search-stats))
  true
  )

(defn studio-remove-concept-tag
  [user id tag]
  @(datomic/transact
     (kuhut-connection)
     [[:db/add [:concept/id id] :concept/time (now)]
      [:db/retract [:concept/id id] :concept/tags tag]])
  (when (->> [:concept/id id] (datomic/entity (kuhut-db)) :concept/popularity)
    (update-tf-idf-search-stats))
  true
  )

(defn studio-rename-concept-tag
  [user id from to]
  @(datomic/transact
     (kuhut-connection)
     [[:db/add [:concept/id id] :concept/time (now)]
      [:db/retract [:concept/id id] :concept/tags from]
      [:db/add [:concept/id id] :concept/tags to]])
  (when (->> [:concept/id id] (datomic/entity (kuhut-db)) :concept/popularity)
    (update-tf-idf-search-stats))
  true
  )

(defn studio-set-content-edn
  [{user-id :id} id type edn]
  (let [db (kuhut-db)]
    (when (= type :template)
      (let [flags (datomic/q '[:find [?flag-ref ...]
                               :in $ ?user-ref ?template-id
                               :where
                               [?template-ref :template/id ?template-id]
                               [?flag-ref :flag/step ?step-ref]
                               [?step-ref :step/user ?user-ref]
                               [?step-ref :step/template ?template-ref]
                               ] db user-id id)]
        (when (seq flags)
          @(datomic/transact
             (kuhut-connection)
             (map
               (fn [flag] [:db.fn/retractEntity flag])
               flags)))))
    @(datomic/transact
       (kuhut-connection)
       (case type
         :template [[:db/add [:template/id id] :template/time (now)]
                    [:db/add [:template/id id] :template/content edn]]
         [[:db/add [:resource/id id] :resource/type "text/plain"]
          [:db/add [:resource/id id] :resource/content (.getBytes edn)]]))
    (when (= type :template)
      (doseq [parent (datomic/q '[:find [?parent ...]
                                  :in $ ?id
                                  :where
                                  [?ref :template/id ?id]
                                  [?link-ref :link/reference ?ref]
                                  [?concept-ref :concept/components ?link-ref]
                                  [?concept-ref :concept/id ?parent]
                                  ] db id)]
        (update-times parent)))
    true)
  )

(defn studio-set-resource-attachment
  [user id {:keys [tempfile content-type filename] :as attachment}]
  (let [attachment-bytes (when attachment
                           (let [fullfile (clojure.java.io/file tempfile)
                                 attachment-bytes (byte-array (.length fullfile))
                                 is (FileInputStream. fullfile)]
                             (doto is (.read attachment-bytes) (.close))
                             attachment-bytes))]
    @(datomic/transact
       (kuhut-connection)
       (into
         [[:db/add [:resource/id id] :resource/type content-type]
          [:db/add [:resource/id id] :resource/content attachment-bytes]]
         (let [current-description (->> [:resource/id id] (datomic/entity (kuhut-db)) :resource/description)]
           (when-not (seq current-description)
             [[:db/add [:resource/id id] :resource/description filename]]))))
    true)
  )

(defn studio-remove-ref
  [user ref parent]
  @(datomic/transact (kuhut-connection) [[:db.fn/retractEntity ref]])
  (when parent
    (update-concept-weight parent)
    (update-times parent))
  true
  )

(defn studio-add-component
  [user from to type weight]
  (let [t (now)]
    @(datomic/transact
       (kuhut-connection)
       (if-let [link-ref (datomic/q '[:find ?link-ref .
                                      :in $ ?from ?to
                                      :where
                                      [?from-ref :concept/id ?from]
                                      (or [?to-ref :concept/id ?to]
                                          [?to-ref :template/id ?to])
                                      [?from-ref :concept/components ?link-ref]
                                      [?link-ref :link/reference ?to-ref]
                                      ] (kuhut-db) from to)]
         [[:db/add link-ref :link/time t]
          [:db/add link-ref :link/weight (double weight)]]
         [{:db/id          "component"
           :link/reference [(-> type name (str "/id") keyword) to]
           :link/time      t
           :link/weight    (double weight)}
          [:db/add [:concept/id from] :concept/components "component"]]))
    (update-concept-weight from)
    (update-times from)
    true)
  )

(defn studio-add-recommendation
  [user from from-type to to-type weight]
  (let [t (now)]
    @(datomic/transact
       (kuhut-connection)
       (if-let [link-ref (datomic/q '[:find ?link-ref .
                                      :in $ ?from ?to
                                      :where
                                      (or [?from-ref :concept/id ?from]
                                          [?from-ref :template/id ?from])
                                      (or [?to-ref :concept/id ?to]
                                          [?to-ref :template/id ?to])
                                      (or [?from-ref :concept/recommendations ?link-ref]
                                          [?from-ref :template/recommendations ?link-ref])
                                      [?link-ref :link/reference ?to-ref]
                                      ] (kuhut-db) from to)]
         [[:db/add link-ref :link/time t]
          [:db/add link-ref :link/weight (double weight)]]
         [{:db/id          "recommendation"
           :link/reference [(-> to-type name (str "/id") keyword) to]
           :link/time      t
           :link/weight    (double weight)}
          [:db/add [(-> from-type name (str "/id") keyword) from] (-> from-type name (str "/recommendations") keyword) "recommendation"]]))
    true)
  )

(defn studio-add-relation
  [user from from-type to to-type relation-type new?]
  (let [t (now)]
    (if new?
      @(datomic/transact (kuhut-connection) [{:relation/from [(-> from-type name (str "/id") keyword) from]
                                              :relation/to   [(-> to-type name (str "/id") keyword) to]
                                              :relation/time t
                                              :relation/type relation-type}])
      @(datomic/transact
         (kuhut-connection)
         (if-let [relation-ref (datomic/q '[:find ?relation-ref .
                                            :in $ ?from ?to
                                            :where
                                            (or [?from-ref :concept/id ?from]
                                                [?from-ref :template/id ?from]
                                                [?from-ref :resource/id ?from])
                                            (or [?to-ref :concept/id ?to]
                                                [?to-ref :template/id ?to]
                                                [?to-ref :resource/id ?to])
                                            [?relation-ref :relation/from ?from-ref]
                                            [?relation-ref :relation/to ?to-ref]
                                            ] (kuhut-db) from to)]
           [[:db/add relation-ref :relation/time t]
            [:db/add relation-ref :relation/type relation-type]]
           [{:relation/from [(-> from-type name (str "/id") keyword) from]
             :relation/to   [(-> to-type name (str "/id") keyword) to]
             :relation/time t
             :relation/type relation-type}])))
    true)
  )

(defn studio-load-template
  [user id]
  (->> [:template/id id] (datomic/entity (kuhut-db)) :template/content)
  )

(defn studio-templates-stale
  [user]
  (->> (datomic/q '[:find ?id (count ?id)
                    :with ?flag-ref
                    :where
                    [?flag-ref :flag/step ?step-ref]
                    [?step-ref :step/template ?ref]
                    [?ref :template/id ?id]
                    ] (kuhut-db))
       (sort-by second >)
       (take 10))
  )

(defn studio-delete-user
  [user id]
  (let [db (kuhut-db)]
    @(datomic/transact
       (kuhut-connection)
       (map
         (fn [ref] [:db.fn/retractEntity ref])
         (concat
           (when-let [lock (datomic/q '[:find ?ref . :in $ ?id :where [?id :member/lock ?ref]] db id)]
             [lock])                                        ;; TODO: this will become a component at the next full database refresh
           (datomic/q '[:find [?ref ...] :in $ ?id :where [?ref :flag/step ?step] [?step :step/user ?id]] db id)
           (datomic/q '[:find [?ref ...] :in $ ?id :where [?ref :step/user ?id]] db id)
           (datomic/q '[:find [?ref ...] :in $ ?id :where [?ref :exercise/user ?id]] db id)
           (datomic/q '[:find [?ref ...] :in $ ?id :where [?ref :knowledge/user ?id]] db id)
           (datomic/q '[:find [?ref ...] :in $ ?id :where [?ref :order/user ?id]] db id)
           (datomic/q '[:find [?ref ...] :in $ ?id :where [?ref :session/user ?id]] db id)
           [id])))
    true)
  )
