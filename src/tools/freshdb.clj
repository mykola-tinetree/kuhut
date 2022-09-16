(ns freshdb
  (:require [datomic.api :as datomic]
            [taoensso.nippy :refer [freeze thaw]]
            [kuhut.common.connection :refer [KUHUT_URI]]
            [kuhut.shared.definitions :refer [GOALS_OF_KUHUT_RESOURCE_ID LOADING_RESOURCE_ID HUMAN_RIGHTS_RESOURCE_ID
                                              TF_IDF_SEARCH_STATS_RESOURCE_ID FAVICON_RESOURCE_ID DEFAULT_TOPIC]]
            [kuhut.common.util :refer [now hash-password uuid update-tf-idf-search-stats]]
            [clojure.java.io :as io])
  (:import (java.io ByteArrayOutputStream))
  )

(defn -main [& _]
  (print (str "WARNING: this is a nuclear option for the following database: " KUHUT_URI "\nProceed? (y/n) "))
  (flush)
  (when (= (read-line) "y")
    (println (str "rebuilding database at " KUHUT_URI))
    (datomic/delete-database KUHUT_URI)
    (datomic/create-database KUHUT_URI)
    (let [connection (datomic/connect KUHUT_URI)]
      @(datomic/transact connection (-> "./extras/db/schema.edn" slurp read-string))
      @(datomic/transact
         connection
         (let [t (now)]
           [

            ;; database functions
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
                                                       (throw (Exception. (str "cannot change the popularity of " (:concept/id concept))))))})}

            ;; concept
            {:db/id               "python"
             :concept/id          (uuid DEFAULT_TOPIC)
             :concept/name        "Python programming"
             :concept/description "Learn the basics of one of the most popular programming languages - Python."
             :concept/tags        ["for everyone" "suitable for children" "for beginners" "programming"]
             :concept/components  ["c0"]
             :concept/time        t
             :concept/weight      1.0
             :concept/popularity  1}

            ;; template
            {:db/id            "q0"
             :template/id      (uuid)
             :template/time    t
             :template/content (str [:worksheet
                                     "Can you code?"
                                     [:options {:type :single-choice :answer #{1}}
                                      [:shuffle
                                       [:option {:value 0} "No"]
                                       [:option {:value 1} "Yes"]]]])}

            ;; link
            {:db/id          "c0"
             :link/reference "q0"
             :link/time      t
             :link/weight    1.0}

            ;; resource
            {:resource/id          (uuid GOALS_OF_KUHUT_RESOURCE_ID)
             :resource/type        "image/png"
             :resource/description "the three goals of kuhut"
             :resource/content     (with-open [in (io/input-stream "./extras/images/triad.png")
                                               out (ByteArrayOutputStream.)]
                                     (io/copy in out)
                                     (.toByteArray out))}
            {:resource/id          (uuid LOADING_RESOURCE_ID)
             :resource/type        "image/gif"
             :resource/description "loading"
             :resource/content     (with-open [in (io/input-stream "./extras/images/loading.gif")
                                               out (ByteArrayOutputStream.)]
                                     (io/copy in out)
                                     (.toByteArray out))}
            {:resource/id          (uuid FAVICON_RESOURCE_ID)
             :resource/type        "image/png"
             :resource/description "favicon"
             :resource/content     (with-open [in (io/input-stream "./extras/images/favicon.png")
                                               out (ByteArrayOutputStream.)]
                                     (io/copy in out)
                                     (.toByteArray out))}
            {:resource/id          (uuid HUMAN_RIGHTS_RESOURCE_ID)
             :resource/type        "application/pdf"
             :resource/description "The Universal Declaration of Human Rights"
             :resource/content     (with-open [in (io/input-stream "./extras/documents/human-rights.pdf")
                                               out (ByteArrayOutputStream.)]
                                     (io/copy in out)
                                     (.toByteArray out))}
            {:resource/id          (uuid TF_IDF_SEARCH_STATS_RESOURCE_ID)
             :resource/type        "application/octet-stream"
             :resource/description "The TF-IDF concept search statistics"}

            ]))
      (update-tf-idf-search-stats))
    (datomic/shutdown true)
    (println (str "database rebuilt at " KUHUT_URI)))
  )
