(ns kuhut.middleware.user
  (:require [kuhut.common.session :refer [member visitor create-visitor]]
            [kuhut.common.util :refer [uuid cookie-expiry->str]])
  )

(defn wrap-user-identification
  [handler]
  (fn [request]
    (let [visitor-token (uuid (get-in request [:cookies "kuhut-v-token" :value]))
          member-token (uuid (get-in request [:cookies "kuhut-m-token" :value]))
          user (or (member member-token)
                   (visitor visitor-token)
                   (create-visitor (:headers request)))
          response (handler (assoc request :session/user user))
          {:keys [first-name] :as user} (or (:session/user response) user)]
      (assoc response
        :cookies (merge
                   (when (not= (boolean member-token) (boolean first-name))
                     {"kuhut-m-token" (merge
                                        {:path  "/"
                                         :value (if first-name (-> user :session :key str) "")}
                                        (if first-name
                                          (when (-> user :session :type (= :session.type/extended))
                                            {:expires (-> user :session :expiry cookie-expiry->str)})
                                          {:max-age 0}))})
                   (when-not (or (:first-name user) (-> user :session :key (= visitor-token) (and visitor-token)))
                     {"kuhut-v-token" {:path    "/"
                                       :value   (-> user :session :key str)
                                       :expires (-> user :session :expiry cookie-expiry->str)}})))))
  )
