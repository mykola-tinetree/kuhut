(ns kuhut.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.session :as session]
            [reitit.frontend :as reitit]
            [clerk.core :as clerk]
            [accountant.core :as accountant]
            [kuhut.router :refer [router path-for]]
            [kuhut.chrome :refer [page-chrome]]
            [kuhut.home :refer [home-page]]
            [kuhut.principles :refer [principles-page]]
            [kuhut.policies.cookie-policy :refer [cookie-policy-page]]
            [kuhut.profile :refer [profile-page]]
            [kuhut.studio :refer [studio-page]]
            [kuhut.shared.definitions :refer [keyname]]
            [kuhut.util :refer [timestamp document]]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]])
  )

(set! *warn-on-infer* true)

(defn mount-root [] (reagent/render [page-chrome] (.getElementById (document) "app")))

(defn init!
  []
  (clerk/initialize!)
  (accountant/configure-navigation!
    {:nav-handler
     (fn [path]
       (go (let [{{:keys [focus-locked? roles first-name] :as user} :body} (<! (http/get (str "/session/user?ts=" (timestamp))))]
             (session/put! :user user)
             (let [{{:keys [name]} :data
                    :keys          [path-params]} (reitit/match-by-path router path)
                   name (if first-name
                          (if focus-locked?
                            (if (= name :principles) :principles :home)
                            (if (and (= name :studio) (not (contains? roles :member.role/teacher)))
                              :home name))
                          (case name (:profile :studio) :home name))]
               (reagent/after-render clerk/after-render!)
               (set! (.-title (document)) (str "kuhut - " (keyname name)))
               (session/put! :route {:title name
                                     :page  (case name
                                              :home #'home-page
                                              :profile #'profile-page
                                              :principles #'principles-page
                                              :cookie-policy #'cookie-policy-page
                                              :studio #'studio-page)})
               (clerk/navigate-page! (path-for name path-params))))))
     :path-exists?
     (fn [path]
       (boolean (reitit/match-by-path router path)))})
  (accountant/dispatch-current!)
  (mount-root)
  )
