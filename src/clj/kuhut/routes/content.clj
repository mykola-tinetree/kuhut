(ns kuhut.routes.content
  (:require [datomic.api :as datomic]
            [kuhut.common.util :refer [now uuid]]
            [kuhut.common.connection :refer [kuhut-db kuhut-connection]])
  (:import (javax.imageio ImageIO)
           (java.awt Image)
           (java.awt.image BufferedImage)
           (java.io ByteArrayOutputStream ByteArrayInputStream))
  )

(defn resource
  ([id width]
   (when-let [[content-type content] (datomic/q '[:find [?content-type ?content]
                                                  :in $ ?id
                                                  :where
                                                  [?resource-ref :resource/id ?id]
                                                  [?resource-ref :resource/type ?content-type]
                                                  [?resource-ref :resource/content ?content]
                                                  ] (kuhut-db) id)]
     (let [content (if (and width (#{"image/png" "image/jpeg" "image/gif" "image/bmp"} content-type))
                     (try
                       (let [source-image (ImageIO/read (clojure.java.io/input-stream content))
                             thumbnail (.getScaledInstance source-image (min width (.getWidth source-image)) -1 Image/SCALE_SMOOTH)
                             bufferedThumbnail (BufferedImage. (.getWidth thumbnail nil) (.getHeight thumbnail nil) (.getType source-image))]
                         (.drawImage (.getGraphics bufferedThumbnail) thumbnail 0 0 nil)
                         (let [os (ByteArrayOutputStream.)]
                           (ImageIO/write bufferedThumbnail (last (clojure.string/split content-type #"/")) os)
                           (ByteArrayInputStream. (.toByteArray os))))
                       (catch Exception _ (clojure.java.io/input-stream content)))
                     (clojure.java.io/input-stream content))]
       {:headers {"content-type" content-type}
        :body    content})))
  ([id] (resource id nil))
  )

(defn get-knowledge
  [db user-ref reference]
  (when-let [[ref time level certainty
              ] (datomic/q '[:find [?ref ?time ?level ?certainty]
                             :in $ ?user-ref ?reference
                             :where
                             [?ref :knowledge/user ?user-ref]
                             [?ref :knowledge/reference ?reference]
                             [?ref :knowledge/time ?time]
                             [?ref :knowledge/level ?level]
                             [?ref :knowledge/certainty ?certainty]
                             ] db user-ref reference)]
    (let [reference (datomic/entity db reference)]
      (if (-> time (compare (or (:concept/time reference) (:template/time reference))) neg?)
        (if (:concept/id reference)
          (if-let [components (->> (datomic/q '[:find ?ref ?weight
                                                :in $ ?user-ref ?concept-ref
                                                :where
                                                [?concept-ref :concept/components ?link-ref]
                                                [?link-ref :link/reference ?ref]
                                                [?link-ref :link/weight ?weight]
                                                ] db user-ref (:db/id reference))
                                   (map (fn [[ref weight]] [weight (get-knowledge db user-ref ref)]))
                                   seq)]
            (let [level (/ (reduce + 0 (map (fn [[w c]] (* (or (:level c) 0) w)) components))
                           (:concept/weight reference))
                  certainty (/ (reduce + 0 (map (fn [[w c]] (* (or (:certainty c) 0) w)) components))
                               (:concept/weight reference))]
              @(datomic/transact
                 (kuhut-connection)
                 [[:db/add ref :knowledge/time (now)]
                  [:db/add ref :knowledge/level level]
                  [:db/add ref :knowledge/certainty certainty]])
              {:id        ref
               :level     level
               :certainty certainty})
            {:id ref})
          {:id ref})
        {:id        ref
         :level     level
         :certainty certainty})))
  )

(defn update-template-knowledge
  [user-ref template-ref new-level new-certainty]
  (let [db (kuhut-db)
        t (now)
        knowledge-adder (fn [[l0 c0] [l1 c1]] [(+ l0 l1) (+ c0 c1)])
        {:keys [level certainty] :or {level 0 certainty 0}} (get-knowledge db user-ref template-ref)]
    (when-let [updates (->> (loop [[[ref lc cc :as f] & more] [[template-ref (- new-level level) (- new-certainty certainty)]]
                                   to-update nil]
                              (if f
                                (if (and (zero? lc) (zero? cc))
                                  (recur more to-update)
                                  (recur
                                    (into more
                                          (->> (datomic/q '[:find ?concept-ref ?link-weight ?concept-weight
                                                            :in $ ?ref
                                                            :where
                                                            [?link-ref :link/reference ?ref]
                                                            [?concept-ref :concept/components ?link-ref]
                                                            [?link-ref :link/weight ?link-weight]
                                                            [?concept-ref :concept/weight ?concept-weight]
                                                            ] db ref)
                                               (map (fn [[r w W]] (let [C (/ w W)] [r (* lc C) (* cc C)])))))
                                    (merge-with knowledge-adder to-update {ref [lc cc]})))
                                to-update))
                            (mapcat (fn [[ref [lc cc]]]
                                      (let [{:keys [id level certainty] :or {level 0 certainty 0}
                                             } (or (get-knowledge db user-ref ref)
                                                   {:id ((:tempids @(datomic/transact
                                                                      (kuhut-connection)
                                                                      [{:db/id               "knowledge"
                                                                        :knowledge/user      user-ref
                                                                        :knowledge/reference ref
                                                                        :knowledge/time      t
                                                                        :knowledge/level     0.0
                                                                        :knowledge/certainty 0.0}]))
                                                          "knowledge")})]
                                        (concat
                                          [[:db/add id :knowledge/time t]]
                                          (when-not (zero? lc)
                                            [[:db/add id :knowledge/level (double (+ level lc))]])
                                          (when-not (zero? cc)
                                            [[:db/add id :knowledge/certainty (double (+ certainty cc))]])))))
                            seq)]
      @(datomic/transact
         (kuhut-connection)
         updates)))
  )

(defn ensure-template-recommendation-certainty
  [user-ref template-ref certainty]
  (let [db (kuhut-db)]
    (doseq [[template-ref level certainty
             ] (loop [[f & more] (->> template-ref (datomic/entity db) :template/recommendations (map :link/reference))
                      r []]
                 (if f
                   (if (:concept/id f)
                     (recur (into more (->> f :concept/components (map :link/reference))) r)
                     (let [ref (:db/id f)
                           {template-level :level template-certainty :certainty
                            :or            {template-level 0 template-certainty 0}} (get-knowledge db user-ref ref)]
                       (recur more (if (> template-certainty certainty) (conj r [ref template-level certainty]) r))))
                   r))]
      (update-template-knowledge user-ref template-ref level certainty)))
  )

(defn load-concept
  [db user-id concept-id]
  (when-let [{ref :db/id :as concept} (datomic/entity db [:concept/id concept-id])]
    (merge
      {:id          concept-id
       :name        (:concept/name concept)
       :bookmarked? (boolean (datomic/q '[:find ?concept-ref .
                                          :in $ ?user-ref ?concept-ref
                                          :where
                                          [?user-ref :member/interests ?concept-ref]
                                          ] db user-id ref))}
      (when-let [tags (-> concept :concept/tags seq)]
        {:tags (set tags)})
      (when-let [description (:concept/description concept)]
        {:description description})
      (when-let [cost (:concept/cost concept)]
        {:cost cost})
      (when-let [purchase-time (datomic/q '[:find ?purchase-time .
                                            :in $ ?user-ref ?concept-ref
                                            :where
                                            [?order-ref :order/user ?user-ref]
                                            [?order-ref :order/concept ?concept-ref]
                                            [?order-ref :order/time ?purchase-time]
                                            ] db user-id ref)]
        {:purchase-time (.getTime purchase-time)})
      (when-let [level (:level (get-knowledge db user-id ref))]
        {:level level})))
  )

(defn template->exercise
  [[k & [f & r :as c] :as template]]
  (case k
    :random (when-let [s (seq (if (map? f) r c))] (let [item (rand-nth s)] (if (vector? item) (template->exercise item) item)))
    (vec
      (mapcat
        (fn [e]
          (cond
            (vector? e) (case (first e)
                          :shuffle (let [[f & r :as c] (rest e)] (when-let [s (seq (if (map? f) r c))] (->> s (map template->exercise) shuffle)))
                          [(template->exercise e)])
            (map? e) [(if (contains? e :answer) (assoc e :key (->> (uuid) (str "k-") keyword)) e)]
            :else [e]))
        template)))
  )

(defn template-preview
  [user id]
  (when-let [content (->> [:template/id id] (datomic/entity (kuhut-db)) :template/content)]
    (try
      (-> content read-string template->exercise)
      (catch Exception _)))
  )
