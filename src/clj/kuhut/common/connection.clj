(ns kuhut.common.connection
  (:require [datomic.api :as datomic])
  )

(def ^:const KUHUT_URI (str "[REDACTED]"))

(def kuhut-connection (memoize (fn [] (datomic/connect KUHUT_URI))))
(defn kuhut-db [] (datomic/db (kuhut-connection)))
