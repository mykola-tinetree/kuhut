(ns kuhut.common.util
  (:require [java-time :as time]
            [kuhut.common.connection :refer [kuhut-db kuhut-connection]]
            [kuhut.shared.definitions :refer [TF_IDF_SEARCH_STATS_RESOURCE_ID]]
            [postal.core :refer [send-message]]
            [taoensso.nippy :refer [thaw freeze]]
            [datomic.api :as datomic])
  (:import (java.util UUID))
  )

(defn uuid
  ([] (UUID/randomUUID))
  ([s] (try (UUID/fromString s) (catch Exception _ nil)))
  )

(defn now [] (time/to-java-date (time/instant)))
(defn now+ [& offsets] (time/to-java-date (apply time/plus (time/instant) offsets)))

(defn hash-password [password] (freeze password))
(defn unhash-password [password] (thaw password))
(defn random-password
  ([n] (let [c (map char (concat (range 48 58) (range 65 91) (range 97 123) (repeat 5 45)))] (apply str (take n (repeatedly #(rand-nth c))))))
  ([] (random-password 8)))

(defn send-e-mail
  [to subject content]
  (send-message
    {:user "[REDACTED]"
     :pass "[REDACTED]"
     :host "[REDACTED]"
     :port 587}
    {:from    "[REDACTED]"
     :to      to
     :subject subject
     :body    [{:type    "text/html"
                :content content}]})
  )

(defn roulette-wheel
  [data]
  (let [total (reduce + 0 (map second data))]
    (if (-> total Math/abs (< 1e-6))
      (-> data rand-nth first)
      (let [r (rand)]
        (loop [sum 0.0
               [[id value] & others] data]
          (let [sum (-> value (/ total) (+ sum))]
            (if (>= sum r)
              id
              (recur sum others)))))))
  )

(defn cookie-expiry->str
  [expiry]
  (time/format "E, d MMM yyyy HH:mm:ss 'GMT'" (time/zoned-date-time (time/offset-date-time expiry "UTC") "GMT"))
  )

(defn- terms-per-document
  [documents]
  (map (fn [[k v]] [k (flatten [(map #(for [i (range (- (count %) 3))] (subs % i (+ i 4))) v)
                                (map #(for [i (range (- (count %) 2))] (subs % i (+ i 3))) v)
                                (map #(for [i (range (dec (count %)))] (subs % i (+ i 2))) v)
                                (map #(for [i (range (count %))] (subs % i (inc i))) v)])]) documents)
  )

(defn tf-idf
  [documents]
  (let [n (double (count documents))
        terms-per-document (terms-per-document documents)
        normalised-term-frequencies (into {} (map (fn [[k t]] [k (into {} (map (fn [[k v]] [k (/ v (double (count t)))]) (frequencies t)))]) terms-per-document))
        inverse-document-frequency-per-term (into {} (map
                                                       (fn [t] [t (+ 1 (Math/log (/ n (count (remove false? (map #(contains? % t) (vals normalised-term-frequencies)))))))])
                                                       (set (flatten (map second terms-per-document)))))]
    {:tf  normalised-term-frequencies
     :idf inverse-document-frequency-per-term})
  )

(defn query-similarity
  [q stats]
  (let [s-idf (:idf stats)
        q-f (into {} (map (fn [t] [t (get s-idf t 0.0)]) (-> {:a [q]} terms-per-document first second)))
        q-len (Math/sqrt (reduce + 0 (map #(Math/pow % 2) (vals q-f))))]
    (map
      (fn [[k s-tf]]
        (let [s-f (into {} (map (fn [[k v]] [k (* v (get s-idf k))]) s-tf))
              dot-product (reduce + 0 (map (fn [[k v]] (* v (get s-f k 0.0))) q-f))
              s-len (Math/sqrt (reduce + 0 (map #(Math/pow % 2) (vals s-f))))]
          [k (/ dot-product (* q-len s-len))]))
      (:tf stats)))
  )

(defn update-tf-idf-search-stats
  []
  (let [stats (->> (datomic/q '[:find [(pull ?r [:concept/id :concept/name :concept/description :concept/tags]) ...]
                                :where
                                [?r :concept/popularity]
                                ] (kuhut-db))
                   (map (fn [c] [(:concept/id c) (into [(-> c :concept/name (or "") clojure.string/lower-case)
                                                        (-> c :concept/description (or "") clojure.string/lower-case)]
                                                       (:concept/tags c))]))
                   (into {})
                   tf-idf)]
    @(datomic/transact
       (kuhut-connection)
       [[:db/add [:resource/id (uuid TF_IDF_SEARCH_STATS_RESOURCE_ID)] :resource/content (freeze stats)]])
    true)
  )
