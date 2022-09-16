(ns checkdb
  (:require [datomic.api :as datomic]
            [kuhut.common.connection :refer [kuhut-db]]
            [kuhut.common.util :refer [unhash-password]])
  )

(defn -main
  [& _]
  (let [d (->> (datomic/q '[:find ?u ?l
                            :in $ ?t
                            :where
                            [?c :concept/name ?t]
                            [?k :knowledge/reference ?c]
                            [?k :knowledge/user ?u]
                            [?k :knowledge/level ?l]
                            ] (kuhut-db) "English in English")
               (sort-by second >))]
    (clojure.pprint/pprint
      d)
    (datomic/shutdown true))
  )
