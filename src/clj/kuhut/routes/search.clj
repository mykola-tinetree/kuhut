(ns kuhut.routes.search
  (:require [datomic.api :as datomic]
            [kuhut.common.connection :refer [kuhut-db]]
            [kuhut.common.util :refer [uuid query-similarity]]
            [kuhut.shared.definitions :refer [TF_IDF_SEARCH_STATS_RESOURCE_ID]]
            [taoensso.nippy :refer [thaw]]
            [kuhut.routes.content :refer [load-concept]])
  )

(defn search-concepts
  [{user-id :id} search-text ts]
  (let [db (kuhut-db)
        search-text (-> search-text (or "") clojure.string/lower-case)]
    {:ts   ts
     :data (->> (if (seq search-text)
                  (->> [:resource/id (uuid TF_IDF_SEARCH_STATS_RESOURCE_ID)] (datomic/entity db) :resource/content thaw
                       (query-similarity search-text))
                  (datomic/q '[:find ?id ?popularity :where [?ref :concept/popularity ?popularity] [?ref :concept/id ?id]] db))
                (sort-by second >) (take 5) (map first) (mapv (partial load-concept db user-id)))})
  )
