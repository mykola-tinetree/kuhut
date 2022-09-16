(ns kuhut.handler
  (:require [reitit.ring :as reitit-ring]
            [kuhut.middleware :refer [middleware]]
            [kuhut.middleware.user :refer [wrap-user-identification]]
            [ring.middleware.edn :refer [wrap-edn-params]]
            [noir.response :refer [edn]]
            [config.core :refer [env]]
            [kuhut.common.session :refer [visitor create-visitor]]
            [kuhut.common.util :refer [uuid]]
            [kuhut.common.connection :refer [kuhut-db]]
            [hiccup.page :refer [include-js include-css html5]]
            [kuhut.shared.definitions :refer [FAVICON_RESOURCE_ID HUMAN_RIGHTS_RESOURCE_ID]]
            [kuhut.routes.content :refer [resource template-preview]]
            [kuhut.routes.journey :refer [next-move flag-move remove-flag]]
            [kuhut.routes.search :refer [search-concepts]]
            [kuhut.routes.user :refer [toggle-bookmark interests suggestions focus lock-focus unlock-focus remind-focus-pin]]
            [kuhut.routes.executor :refer [execute]]
            [kuhut.routes.studio :refer [studio-search-contents studio-search-users studio-concept studio-template studio-resource studio-add-content
                                         studio-remove-content studio-duplicate-content studio-change-content-type studio-set-concept-name
                                         studio-set-concept-description studio-set-content-edn
                                         studio-remove-concept-tag studio-rename-concept-tag studio-add-concept-tag
                                         studio-set-resource-attachment studio-remove-ref studio-add-component studio-add-recommendation
                                         studio-add-relation studio-load-template studio-set-concept-searchability studio-delete-user
                                         studio-set-resource-description studio-remove-resource-content studio-set-concept-cost
                                         studio-user-concept studio-user-template studio-user-knowledge studio-templates-stale
                                         studio-clear-template-flags studio-clear-template-flag studio-users-join-times]]
            [kuhut.routes.session :refer [enter join remind-password exit reset-password update-name]])
  )

(System/setProperty "java.awt.headless" "true")

(defn index-handler
  [_request]
  (let [r 456781]                                           ;; TODO: change this before or after each release - it updates the .css and .js files efficiently
    {:status  200
     :headers {"content-type" "text/html"}
     :body    (html5
                [:head
                 [:meta {:charset :utf-8}]
                 [:meta {:name :viewport :content "width=device-width,initial-scale=1"}]
                 [:title "kuhut"]
                 [:link {:rel :icon :type "image/png" :href (str "/resource/" FAVICON_RESOURCE_ID)}]
                 (include-css (str "/css/kuhut" (when-not (env :dev) ".min") ".css?z=" r)
                              "https://fonts.googleapis.com/css?family=Roboto:100,100i,300,300i,400,400i,500,500i,700,700i,900,900i&amp;subset=cyrillic,cyrillic-ext,latin-ext"
                              "https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.46.0/codemirror.min.css")
                 (include-js "https://ajax.googleapis.com/ajax/libs/jquery/3.4.1/jquery.min.js"
                             "https://ajax.googleapis.com/ajax/libs/jqueryui/1.12.1/jquery-ui.min.js"
                             "https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.46.0/codemirror.min.js"
                             "https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.46.0/addon/edit/closebrackets.min.js"
                             "https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.46.0/mode/clojure/clojure.min.js"
                             "https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.46.0/mode/python/python.min.js"
                             "https://cdnjs.cloudflare.com/ajax/libs/apexcharts/3.6.12/apexcharts.min.js"
                             "https://cdnjs.cloudflare.com/ajax/libs/mathjax/2.7.5/MathJax.js?config=TeX-MML-AM_CHTML")
                 [:script {:type "text/javascript"} "MathJax.Hub.Config({showMathMenu: false});"]]
                [:body [:div#app] (include-js (str "/js/app.js?z=" r))])})
  )

(defmacro lock-authorise
  [user & calls]
  `(when-not (:focus-locked? ~user)
     ~@calls)
  )

(defmacro studio-authorise
  [user & calls]
  `(when (-> ~user :roles (contains? :member.role/teacher))
     ~@calls)
  )

(def app
  (reitit-ring/ring-handler
    (reitit-ring/router
      (into [["/human-rights.pdf" {:get {:handler (fn [_] (resource (uuid HUMAN_RIGHTS_RESOURCE_ID)))}}]
             ["/resource/:id" {:get {:handler (fn [{{:keys [id]}    :path-params
                                                    {:keys [width]} :params}]
                                                (resource (uuid id) (when width (read-string width))))}}]
             ["/preview/:id" {:get {:handler (fn [{user         :session/user
                                                   {:keys [id]} :path-params}]
                                               (edn (template-preview user (uuid id))))}}]
             ["/execute" {:post {:handler (fn [{user                            :session/user
                                                {:keys [language code context]} :edn-params}]
                                            (edn (when user ;; TODO: this needs some work
                                                   (execute language code context))))}}]
             ["/session"
              ["/user" {:get {:handler (fn [{user :session/user}]
                                         (edn (select-keys user [:id :roles :first-name :last-name :focus-locked?])))}}]
              ["/login" {:post {:handler (fn [{user                                :session/user
                                               :keys                               [headers]
                                               {:keys [e-mail password remember?]} :edn-params}]
                                           (lock-authorise user
                                                           (if-let [member (enter user e-mail password remember? headers)]
                                                             (merge (edn (select-keys member [:id :roles :first-name :last-name :focus-locked?]))
                                                                    {:session/user member})
                                                             (edn nil))))}}]
              ["/join" {:post {:handler (fn [{user                                                         :session/user
                                              :keys                                                        [headers]
                                              {:keys [first-name last-name e-mail keep-progress? captcha]} :edn-params}]
                                          (lock-authorise user
                                                          (let [user (join user first-name last-name e-mail keep-progress? captcha
                                                                           (-> user :session :key) headers)]
                                                            (merge
                                                              (edn user)
                                                              (when user
                                                                {:session/user user})))))}}]
              ["/remind" {:post {:handler (fn [{user             :session/user
                                                {:keys [e-mail]} :edn-params}]
                                            (lock-authorise user (edn (remind-password user e-mail))))}}]
              ["/logout" {:post {:handler (fn [{user  :session/user
                                                :keys [headers cookies]}]
                                            (lock-authorise user
                                                            (let [visitor-cookie (get-in cookies ["kuhut-v-token" :value])]
                                                              (exit user)
                                                              (merge
                                                                (edn true)
                                                                {:session/user (-> visitor-cookie uuid visitor (or (create-visitor headers)))}))))}}]
              ["/resetpass" {:post {:handler (fn [{user                                :session/user
                                                   {:keys [old-password new-password]} :edn-params}]
                                               (lock-authorise user (edn (reset-password user old-password new-password))))}}]
              ["/updatename" {:post {:handler (fn [{user                           :session/user
                                                    {:keys [first-name last-name]} :edn-params}]
                                                (lock-authorise user (edn (update-name user first-name last-name))))}}]]
             ["/member"
              ["/focus" {:get  {:handler (fn [{user :session/user}]
                                           (edn (focus user)))}
                         :post {:handler (fn [{user                 :session/user
                                               {:keys [concept-id]} :edn-params}]
                                           (lock-authorise user (edn (focus user concept-id))))}}]
              ["/lock" {:post {:handler (fn [{user             :session/user
                                              {:keys [e-mail]} :edn-params}]
                                          (lock-authorise user (edn (lock-focus user e-mail))))}}]
              ["/unlock" {:post {:handler (fn [{user          :session/user
                                                {:keys [pin]} :edn-params}]
                                            (edn (unlock-focus user pin)))}}]
              ["/remind-lock" {:post {:handler (fn [{user :session/user}]
                                                 (edn (remind-focus-pin user)))}}]
              ["/interests" {:get {:handler (fn [{user :session/user}]
                                              (lock-authorise user (edn (interests user))))}}]
              ["/suggestions" {:get {:handler (fn [{user :session/user}]
                                                (lock-authorise user (edn (suggestions user))))}}]
              ["/toggle-bookmark" {:post {:handler (fn [{user                 :session/user
                                                         {:keys [concept-id]} :edn-params}]
                                                     (edn (toggle-bookmark user concept-id)))}}]]
             ["/move"
              ["/next" {:post {:handler (fn [{user                             :session/user
                                              {:keys [concept-id move reset?]} :edn-params}]
                                          (edn (next-move user concept-id move reset?)))}}]
              ["/flag" {:post {:handler (fn [{user                 :session/user
                                              {:keys [concept-id]} :edn-params}]
                                          (edn (flag-move user concept-id)))}}]
              ["/remove-flag" {:post {:handler (fn [{user           :session/user
                                                     {:keys [flag]} :edn-params}]
                                                 (edn (remove-flag user flag)))}}]]
             ["/search"
              ["/concepts" {:post {:handler (fn [{user                     :session/user
                                                  {:keys [ts search-text]} :params}]
                                              (lock-authorise user (edn (search-concepts user search-text ts))))}}]]
             ["/studio"
              ["/contents" {:post {:handler (fn [{user                                :session/user
                                                  {:keys [ts search-text only-types]} :params}]
                                              (studio-authorise user (edn (studio-search-contents user search-text only-types ts))))}}]
              ["/users" {:post {:handler (fn [{user                                       :session/user
                                               {:keys [ts search-text include-visitors?]} :params}]
                                           (studio-authorise user (edn (studio-search-users user search-text include-visitors? ts))))}}]
              ["/users/join-times" {:post {:handler (fn [{user                        :session/user
                                                          {:keys [include-visitors?]} :params}]
                                                      (studio-authorise user (edn (studio-users-join-times user include-visitors?))))}}]
              ["/templates"
               ["/stale" {:get {:handler (fn [{user :session/user}]
                                           (studio-authorise user (edn (studio-templates-stale user))))}}]]
              ["/concept/:id" {:get {:handler (fn [{user         :session/user
                                                    {:keys [id]} :path-params}]
                                                (studio-authorise user (edn (studio-concept user (kuhut-db) false (uuid id)))))}}]
              ["/template/:id" {:get {:handler (fn [{user         :session/user
                                                     {:keys [id]} :path-params}]
                                                 (studio-authorise user (edn (studio-template user (kuhut-db) false (uuid id)))))}}]
              ["/resource/:id" {:get {:handler (fn [{user         :session/user
                                                     {:keys [id]} :path-params}]
                                                 (studio-authorise user (edn (studio-resource user (kuhut-db) false (uuid id)))))}}]
              ["/load-template/:id" {:get {:handler (fn [{user         :session/user
                                                          {:keys [id]} :path-params}]
                                                      (studio-authorise user (edn (studio-load-template user (uuid id)))))}}]
              ["/knowledge"
               ["/user/:id" {:get {:handler (fn [{user         :session/user
                                                  {:keys [id]} :path-params}]
                                              (studio-authorise user (edn (studio-user-knowledge user (read-string id)))))}}]
               ["/concept/:id/:user-id" {:get {:handler (fn [{user                 :session/user
                                                              {:keys [user-id id]} :path-params}]
                                                          (studio-authorise user (edn (studio-user-concept user (read-string user-id) (uuid id)))))}}]
               ["/template/:id/:user-id" {:get {:handler (fn [{user                 :session/user
                                                               {:keys [user-id id]} :path-params}]
                                                           (studio-authorise user (edn (studio-user-template user (read-string user-id) (uuid id)))))}}]]
              ["/edit"
               ["/add-content" {:post {:handler    (fn [{user              :session/user
                                                         {:keys [id type]} :params}]
                                                     (studio-authorise user (edn (studio-add-content user id type))))
                                       :parameters {:query {:ts int?}}}}]
               ["/duplicate-content" {:post {:handler    (fn [{user                   :session/user
                                                               {:keys [id from type]} :params}]
                                                           (studio-authorise user (edn (studio-duplicate-content user id from type))))
                                             :parameters {:query {:ts int?}}}}]
               ["/remove-content" {:post {:handler (fn [{user              :session/user
                                                         {:keys [id type]} :params}]
                                                     (studio-authorise user (edn (studio-remove-content user id type))))}}]
               ["/delete-user" {:post {:handler (fn [{user         :session/user
                                                      {:keys [id]} :params}]
                                                  (studio-authorise user (edn (studio-delete-user user id))))}}]
               ["/remove-resource-content" {:post {:handler (fn [{user         :session/user
                                                                  {:keys [id]} :params}]
                                                              (studio-authorise user (edn (studio-remove-resource-content user id))))}}]
               ["/change-content-type" {:post {:handler (fn [{user                 :session/user
                                                              {:keys [id from to]} :params}]
                                                          (studio-authorise user (edn (studio-change-content-type user id from to))))}}]
               ["/set-concept-name" {:post {:handler (fn [{user              :session/user
                                                           {:keys [id name]} :params}]
                                                       (studio-authorise user (edn (studio-set-concept-name user id name))))}}]
               ["/set-concept-description" {:post {:handler (fn [{user                     :session/user
                                                                  {:keys [id description]} :params}]
                                                              (studio-authorise user (edn (studio-set-concept-description user id description))))}}]
               ["/set-concept-cost" {:post {:handler (fn [{user              :session/user
                                                           {:keys [id cost]} :params}]
                                                       (studio-authorise user (edn (studio-set-concept-cost user id (when (seq cost) (read-string cost))))))}}]
               ["/set-resource-description" {:post {:handler (fn [{user                     :session/user
                                                                   {:keys [id description]} :params}]
                                                               (studio-authorise user (edn (studio-set-resource-description user id description))))}}]
               ["/set-concept-searchability" {:post {:handler (fn [{user                        :session/user
                                                                    {:keys [id is-searchable?]} :params}]
                                                                (studio-authorise user (edn (studio-set-concept-searchability user id is-searchable?))))}}]
               ["/add-concept-tag" {:post {:handler (fn [{user             :session/user
                                                          {:keys [id tag]} :params}]
                                                      (studio-authorise user (edn (studio-add-concept-tag user id tag))))}}]
               ["/remove-concept-tag" {:post {:handler (fn [{user             :session/user
                                                             {:keys [id tag]} :params}]
                                                         (studio-authorise user (edn (studio-remove-concept-tag user id tag))))}}]
               ["/rename-concept-tag" {:post {:handler (fn [{user                 :session/user
                                                             {:keys [id from to]} :params}]
                                                         (studio-authorise user (edn (studio-rename-concept-tag user id from to))))}}]
               ["/clear-template-flag" {:post {:handler (fn [{user                 :session/user
                                                              {:keys [id user-id]} :params}]
                                                          (studio-authorise user (edn (studio-clear-template-flag user id user-id))))}}]
               ["/clear-template-flags" {:post {:handler (fn [{user         :session/user
                                                               {:keys [id]} :params}]
                                                           (studio-authorise user (edn (studio-clear-template-flags user id))))}}]
               ["/set-content-edn" {:post {:handler (fn [{user                   :session/user
                                                          {:keys [id type text]} :params}]
                                                      (studio-authorise user (edn (studio-set-content-edn user id type text))))}}]
               ["/set-resource-attachment" {:post {:handler (fn [{user                    :session/user
                                                                  {:keys [id attachment]} :params}]
                                                              (studio-authorise user (edn (studio-set-resource-attachment user (uuid id) attachment))))}}]
               ["/remove-ref" {:post {:handler (fn [{user                 :session/user
                                                     {:keys [ref parent]} :params}]
                                                 (studio-authorise user (edn (studio-remove-ref user ref parent))))}}]
               ["/add-component" {:post {:handler (fn [{user                          :session/user
                                                        {:keys [from to type weight]} :params}]
                                                    (studio-authorise user (edn (studio-add-component user from to type (read-string weight)))))}}]
               ["/add-recommendation" {:post {:handler (fn [{user                                       :session/user
                                                             {:keys [from from-type to to-type weight]} :params}]
                                                         (studio-authorise user (edn (studio-add-recommendation user from from-type to to-type (read-string weight)))))}}]
               ["/add-relation" {:post {:handler (fn [{user                                                   :session/user
                                                       {:keys [from from-type to to-type relation-type new?]} :params}]
                                                   (studio-authorise user (edn (studio-add-relation user from from-type to to-type relation-type new?))))}}]]]]
            (map vector ["/" "/profile" "/principles" "/studio" "/cookie-policy"] (repeat {:get {:handler index-handler}}))))
    (reitit-ring/routes
      (reitit-ring/create-resource-handler {:path "/" :root "/public"})
      (reitit-ring/create-default-handler))
    {:middleware (into middleware
                       [wrap-user-identification
                        wrap-edn-params])})
  )
