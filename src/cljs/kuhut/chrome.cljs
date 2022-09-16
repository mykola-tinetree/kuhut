(ns kuhut.chrome
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as reagent]
            [kuhut.shared.definitions :refer [px pct alpha+ greyscale translate border white-colour black-colour leading-colour-0 leading-colour-1 leading-colour-2 leading-colour-3
                                              primary-font-family PRINCIPLES_LIFELONG_RESOURCE_ID
                                              PRINCIPLES_SAFE_RESOURCE_ID PRINCIPLES_HELPOTHERS_RESOURCE_ID
                                              PRINCIPLES_AI_RESOURCE_ID PRINCIPLES_EQUALITY_RESOURCE_ID]]
            [kuhut.util :refer [style build-input-field build-button build-checkbox valid-e-mail? timestamp contained build-level build-bookmark contain-event load-focus build-kuhut-icon jq jq-remove-class jq-add-class jq-focus jq-hide jq-width jq-append jq-serialise-array jq-css jq-on jq-off]]
            [kuhut.router :refer [path-for]]
            [kuhut.search :refer [build-concept-search]]
            [reagent.session :as session]
            [reagent.cookies :as cookies]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]
            [accountant.core :as accountant])
  )

(set! *warn-on-infer* true)

(defn- logout
  []
  (go
    (session/remove! :overlay)
    (<! (http/post "/session/logout"))
    (session/remove! :user)
    (session/remove! :focus)
    (accountant/navigate! (path-for :home)))
  )

(defn- build-narrow-main-toolbar
  [set-showing]
  (let [arrow [:div.control-button-arrow
               [:svg {:width (px 20) :height (px 40)}
                [:path {:d "M5,5 L15,20 L5,35" :stroke (alpha+ black-colour -0.8) :fill :none :stroke-width (px 1)}]]]
        {:keys [first-name focus-locked?]} (session/get :user)]
    (if first-name
      [:div.toolbar
       [:div.control-button {:on-click #(do
                                          (session/remove! :overlay)
                                          (accountant/navigate! (path-for :principles)))}
        [:div.control-button-title "Our 5 principles"]
        arrow]
       [:div.control-area.welcome
        (str "Welcome, " first-name "!")]
       (when-not focus-locked?
         [:div.control-button (merge
                                {:on-click #(do
                                              (session/remove! :overlay)
                                              (accountant/navigate! (path-for :profile)))}
                                (style :border-top (border 1 :solid (alpha+ black-colour -0.95))))
          [:div.control-button-title "View profile"]
          arrow])
       (when (-> :user session/get :roles (contains? :member.role/teacher))
         [:div.control-button {:on-click #(do
                                            (session/remove! :overlay)
                                            (accountant/navigate! (path-for :studio)))}
          [:div.control-button-title "Studio"]
          arrow])
       [:div.control-button (merge
                              {:on-click #(set-showing :lock)}
                              (when focus-locked?
                                (style :border-top (border 1 :solid (alpha+ black-colour -0.95)))))
        [:div.control-button-title (style :line-height (px 16)) (str (if focus-locked? "Unlock" "Lock") " focus topic")]
        [:div.control-button-subtitle (-> :focus session/get :name)]
        arrow]
       [:div.control-button {:on-click #(set-showing :settings)}
        [:div.control-button-title "Settings"]
        arrow]
       [:div.control-button {:on-click logout}
        [:div.control-button-title "Log out"]
        arrow]]
      [:div.toolbar
       [:div.control-button (merge
                              {:on-click #(do
                                            (session/remove! :overlay)
                                            (accountant/navigate! (path-for :principles)))}
                              (style :border-top (border 1 :solid (alpha+ black-colour -0.95))))
        [:div.control-button-title "Our 5 principles"]
        arrow]
       [:div.control-area
        "Embark on a " [:div.strong "life long"]
        " journey of discovery in a " [:div.strong "safe"]
        ", " [:div.strong "supportive"]
        " and " [:div.strong "effective"]
        " learning environment, and " [:div.strong "help"]
        " make the world a better place."]
       [:div.control-button (merge
                              {:on-click #(set-showing :login)}
                              (style :border-top (border 1 :solid (alpha+ black-colour -0.95))))
        [:div.control-button-title "Log in"]
        arrow]
       [:div.control-button {:on-click #(set-showing :signup)}
        [:div.control-button-title "Sign up"]
        arrow]]))
  )

(defn- build-lock-toolbar
  [set-showing home]
  (let [{:keys [focus-locked?] :as user} (session/get :user)
        lock-value (reagent/atom nil)
        lock-status (reagent/atom nil)
        submit-fn #(let [text @lock-value]
                     (if focus-locked?
                       (if (seq text)
                         (go
                           (let [result (:body (<! (http/post "/member/unlock" {:edn-params {:pin text}})))]
                             (if result
                               (do
                                 (session/remove! :overlay)
                                 (session/put! :user (assoc user :focus-locked? false))
                                 (when (-> :route session/get :title (not= :home))
                                   (accountant/navigate! (path-for :home))))
                               (reset! lock-status :invalid-pin))))
                         (reset! lock-status :invalid-pin))
                       (if (valid-e-mail? text)
                         (go
                           (<! (http/post "/member/lock" {:edn-params {:e-mail text}}))
                           (session/remove! :overlay)
                           (session/put! :user (assoc user :focus-locked? true))
                           (when (-> :route session/get :title (not= :home))
                             (accountant/navigate! (path-for :home))))
                         (reset! lock-status :invalid-e-mail))))]
    (fn [set-showing home]
      (let [{:keys [focus-locked?]} (session/get :user)]
        [:div.toolbar.lock
         [:div.toolbar-title
          [:div.button.toolbar-button.back {:on-click #(set-showing home)} "back"]
          "LOCK FOCUS"]
         [:div.toolbar-content
          [:div.toolbar-help
           "If you provide an e-mail address, we will send you the unlock PIN for the current focus:"
           [:div.strong (-> :focus session/get :name)]]
          [:div.toolbar-area
           [:div.toolbar-area-content
            [build-input-field :e-mail (if focus-locked? "Unlock PIN" "E-mail for unlock PIN") "pin-e-mail" @lock-value true #(reset! lock-value %) submit-fn]
            (when-let [s @lock-status]
              [:div {:class (str "status-message " (case s (:invalid-pin :invalid-e-mail) "error" "ok"))}
               (case s
                 :invalid-pin "Pin invalid. Please try again"
                 :invalid-e-mail "Please enter a valid e-mail"
                 (str "Pin reminder sent to " s))])
            [build-button (if focus-locked? "Unlock" "Lock") submit-fn]
            (when focus-locked?
              [:div.e-mail-reminder
               [:div.linked-reference {:on-click #(contained % (go
                                                                 (let [e-mail (:body (<! (http/post "/member/remind-lock")))]
                                                                   (reset! lock-status e-mail))))}
                "E-mail PIN reminder"]])]]]])))
  )

(defn- build-narrow-settings-name-change
  []
  (let [{:keys [first-name last-name] :as user} (session/get :user)
        first-name-value (reagent/atom first-name)
        last-name-value (reagent/atom last-name)
        reset-status (reagent/atom nil)
        update-name-fn #(let [first-name @first-name-value
                              last-name @last-name-value]
                          (doseq [x ["change-name-first-name" "change-name-last-name"]]
                            (-> (jq "#" x) (jq-remove-class "error")))
                          (when-not (seq first-name)
                            (-> (jq "#change-name-first-name") (jq-add-class "error")))
                          (when-not (seq last-name)
                            (-> (jq "#change-name-last-name") (jq-add-class "error")))
                          (if (seq first-name)
                            (if (seq last-name)
                              (go (let [response (<! (http/post "/session/updatename"
                                                                {:edn-params {:first-name first-name
                                                                              :last-name  last-name}}))
                                        response (:body response)]
                                    (reset! reset-status (if response :reset :failed))
                                    (when response
                                      (session/put! :user (assoc user :first-name first-name :last-name last-name)))))
                              (reset! reset-status :empty-last-name))
                            (reset! reset-status :empty-first-name)))]
    (fn []
      [:div.toolbar-area
       [:div.toolbar-area-title "Change name:"]
       [:div.toolbar-area-content
        [build-input-field :text "First name" "change-name-first-name" @first-name-value false #(reset! first-name-value %) #(-> (jq "#change-name-last-name") jq-focus)]
        [build-input-field :text "Last name" "change-name-last-name" @last-name-value false #(reset! last-name-value %) update-name-fn]
        [:div
         (case @reset-status
           :failed [:div {:class "status-message warn"} "No changes were made to your account"]
           :reset [:div {:class "status-message ok"} "Name updated successfully!"]
           :empty-first-name [:div {:class "status-message error"} "First name cannot be empty"]
           :empty-last-name [:div {:class "status-message error"} "Last name cannot be empty"]
           nil)]
        [build-button "Update name" update-name-fn]]]))
  )

(defn- build-narrow-settings-password-change
  []
  (let [old-password-value (reagent/atom nil)
        new-password-value (reagent/atom nil)
        new-password2-value (reagent/atom nil)
        reset-status (reagent/atom nil)
        reset-password-fn #(let [old-pass @old-password-value
                                 new-pass @new-password-value
                                 new-pass2 @new-password2-value]
                             (doseq [x ["change-password-old-password" "change-password-new-password" "change-password-new-password2"]]
                               (-> (jq "#" x) (jq-remove-class "error")))
                             (when-not (seq old-pass)
                               (-> (jq "#change-password-old-password") (jq-add-class "error")))
                             (when-not (seq new-pass)
                               (-> (jq "#change-password-new-password") (jq-add-class "error")))
                             (when-not (seq new-pass2)
                               (-> (jq "#change-password-new-password2") (jq-add-class "error")))
                             (if (= new-pass new-pass2)
                               (if (every? seq [old-pass new-pass new-pass2])
                                 (go (let [response (<! (http/post "/session/resetpass"
                                                                   {:edn-params {:old-password old-pass
                                                                                 :new-password new-pass}}))
                                           response (:body response)]
                                       (reset! reset-status (if response :reset :failed))
                                       (doseq [x [old-password-value new-password-value new-password2-value]]
                                         (reset! x nil))))
                                 (do
                                   (reset! reset-status :all-fields-required)
                                   (-> (jq (if (seq old-pass)
                                             (if (seq new-pass)
                                               "#change-password-new-password2"
                                               "#change-password-new-password")
                                             "#change-password-old-password"))
                                       jq-focus)))
                               (reset! reset-status :mismatch)))]
    (fn []
      [:div.toolbar-area
       [:div.toolbar-area-title "Change password:"]
       [:div.toolbar-area-content
        [:input (merge {:type :e-mail} (style :opacity 0 :position :absolute))]
        [build-input-field :password "Old password" "change-password-old-password" @old-password-value false #(reset! old-password-value %) #(-> (jq "#change-password-new-password") jq-focus)]
        [build-input-field :password "New password" "change-password-new-password" @new-password-value false #(reset! new-password-value %) #(-> (jq "#change-password-new-password2") jq-focus)]
        [build-input-field :password "Repeat new password" "change-password-new-password2" @new-password2-value false #(reset! new-password2-value %) reset-password-fn]
        [:div
         (case @reset-status
           :failed [:div {:class "status-message error"} "Your old password is incorrect - reset failed"]
           :reset [:div {:class "status-message ok"} "Password reset successful!"]
           :all-fields-required [:div {:class "status-message error"} "All fields are required"]
           :mismatch [:div {:class "status-message warn"} "New passwords do not match"]
           nil)]
        [build-button "Update password" reset-password-fn]]]))
  )

(defn- build-settings-toolbar
  [set-showing home]
  [:div.toolbar.settings
   [:div.toolbar-title
    [:div.button.toolbar-button.back {:on-click #(set-showing home)} "back"]
    "SETTINGS"]
   [:div.toolbar-content
    [build-narrow-settings-name-change]
    [build-narrow-settings-password-change]]]
  )

(defn- build-login-toolbar
  [set-showing with-controls?]
  (let [e-mail-value (reagent/atom nil)
        password-value (reagent/atom nil)
        remember-me? (reagent/atom false)
        response-status (reagent/atom nil)
        remind-password? (reagent/atom false)
        submit-fn #(let [remind? @remind-password?
                         e-mail @e-mail-value
                         password @password-value]
                     (when remind?
                       (reset! password-value nil))
                     (if (valid-e-mail? e-mail)
                       (if (or remind? (seq password))
                         (go (let [response (<! (http/post (str "/session" (if remind? "/remind" "/login") "?ts=" (timestamp))
                                                           {:edn-params (merge {:e-mail e-mail}
                                                                               (when-not remind?
                                                                                 {:password  password
                                                                                  :remember? @remember-me?}))}))]
                               (if remind?
                                 (if (= (:body response) :success)
                                   (do
                                     (reset! response-status :remind-success)
                                     (reset! remind-password? false))
                                   (reset! response-status :remind-fail))
                                 (if-let [user (:body response)]
                                   (do
                                     (session/remove! :overlay)
                                     (session/put! :user user)
                                     (session/remove! :focus)
                                     (accountant/navigate! (path-for :profile)))
                                   (reset! response-status :login-fail)))))
                         (do
                           (reset! response-status :empty-password)
                           (-> (jq "#login-password") (jq-add-class "error") jq-focus)))
                       (do
                         (reset! response-status :invalid-e-mail)
                         (-> (jq "#login-e-mail") (jq-add-class "error") jq-focus))))]
    (fn [set-showing with-controls?]
      [:div.toolbar
       [:div.toolbar-title
        (when with-controls?
          [:div.button.toolbar-button.back {:on-click #(set-showing :main)} "back"])
        (if @remind-password? "REMIND PASSWORD" "LOG IN")
        (when with-controls?
          [:div.button.toolbar-button.forth {:on-click #(set-showing :signup)} "sign up"])]
       [:div.toolbar-content
        [build-input-field :e-mail "E-mail" "login-e-mail" nil false #(reset! e-mail-value %) submit-fn]
        (when-not @remind-password?
          [build-input-field :password "Password" "login-password" nil false #(reset! password-value %) submit-fn])
        (when-not @remind-password?
          [:div.keep-logged-in [build-checkbox :multiple 14 @remember-me? "Keep me logged in" #(swap! remember-me? not)]])
        (when-let [resp @response-status]
          [:div
           [:div {:class (str "status-message " (case resp (:remind-fail :login-fail :empty-password :invalid-e-mail) "error" "ok"))}
            (case resp
              :remind-fail "E-mail not recognised"
              :login-fail "E-mail or password incorrect"
              :empty-password "Please enter a password"
              :invalid-e-mail "Please enter a valid e-mail"
              :remind-success "Password reminder sent successfully")]])
        [build-button (if @remind-password? "Send" "Log in") submit-fn]
        [:div.forgot-password
         [:div.linked-reference {:on-click #(do
                                              (reset! response-status nil)
                                              (swap! remind-password? not)
                                              (-> (jq "#login-e-mail") (jq-remove-class "error") jq-focus))}
          (if @remind-password? "Back to log in" "Forgot password")]]]]))
  )

(defn- build-signup-toolbar
  [set-showing with-controls?]
  (let [response-status (reagent/atom nil)
        keep-progress? (reagent/atom true)
        submit-fn #(let [{:keys [signup-first-name signup-last-name signup-e-mail g-recaptcha-response]
                          } (->> (-> (jq "#signup") jq-serialise-array (js->clj :keywordize-keys true))
                                 (map (fn [{:keys [name value]}] [(keyword name) value]))
                                 (into {}))]
                     (doseq [x ["signup-first-name" "signup-last-name" "signup-e-mail"]]
                       (-> (jq "#" x) (jq-remove-class "error")))
                     (when-not (seq signup-first-name)
                       (-> (jq "#signup-first-name") (jq-add-class "error") jq-focus))
                     (when-not (seq signup-last-name)
                       (-> (jq "#signup-last-name") (jq-add-class "error") jq-focus))
                     (if-not (valid-e-mail? signup-e-mail)
                       (do
                         (-> (jq "#signup-e-mail") (jq-add-class "error") jq-focus)
                         (reset! response-status :invalid-e-mail))
                       (if (seq g-recaptcha-response)
                         (if (every? seq [signup-first-name signup-last-name])
                           (go
                             (.reset js/grecaptcha)
                             (let [response (<! (http/post (str "/session/join?ts=" (timestamp))
                                                           {:edn-params {:first-name     signup-first-name
                                                                         :last-name      signup-last-name
                                                                         :e-mail         signup-e-mail
                                                                         :keep-progress? @keep-progress?
                                                                         :captcha        g-recaptcha-response}}))]
                               (if-let [user (:body response)]
                                 (do
                                   (reset! response-status :join-success)
                                   (session/put! :user user)
                                   (session/remove! :focus))
                                 (reset! response-status :e-mail-exists))))
                           (reset! response-status :all-fields-required))
                         (reset! response-status :captcha-required))))]
    (reagent/create-class
      {:reagent-render
       (fn [set-showing with-controls?]
         [:div.toolbar
          [:div.toolbar-title
           (when with-controls?
             [:div.button.toolbar-button.back {:on-click #(set-showing :main)} "back"])
           "SIGN UP"
           (when with-controls?
             [:div.button.toolbar-button.forth {:on-click #(set-showing :login)} "log in"])]
          [:div.toolbar-content
           [:form#signup {:action "" :method "POST"}
            [build-input-field :text "First name" "signup-first-name" nil false #() #(-> (jq "#signup-last-name") jq-focus)]
            [build-input-field :text "Last name" "signup-last-name" nil false #() #(-> (jq "#signup-e-mail") jq-focus)]
            [build-input-field :e-mail "E-mail" "signup-e-mail" nil false #() submit-fn]
            [:div.progress
             [:div.keep-progress [build-checkbox :single 14 @keep-progress? "Keep current progress" #(reset! keep-progress? true)]]
             [:div.fresh-account [build-checkbox :single 14 (not @keep-progress?) "Fresh account" #(reset! keep-progress? false)]]]
            [:div.g-recaptcha {:data-sitekey "6Le0k5sUAAAAAHo5NuAGooFsGw7AcKDpGpR5RPTi"}]
            (when-let [resp @response-status]
              [:div {:class (str "status-message " (case resp (:e-mail-exists :invalid-e-mail :all-fields-required :captcha-required) "error" "ok"))}
               (case resp
                 :e-mail-exists "This e-mail already exists at kuhut. Try resetting the password from the login form on the left"
                 :all-fields-required "All fields are required"
                 :captcha-required "Please solve the reCAPTCHA"
                 :invalid-e-mail "Please enter a valid e-mail"
                 :join-success (str "Success! We have sent you a welcome message to the provided e-mail (if it is not in the Inbox, please also check the Spam folder)"))])
            [build-button "Sign up" submit-fn]]]])
       :component-did-mount
       (fn [_]
         (-> (jq "#signup") (jq-append (jq "<script src='https://www.google.com/recaptcha/api.js' async defer></script>"))))}))
  )

(defn- build-wide-user-toolbar
  [set-showing]
  [:div.toolbar
   [:div.control-button {:on-click #(do
                                      (session/remove! :overlay)
                                      (accountant/navigate! (path-for :principles)))}
    [:div.control-button-title "Our 5 principles"]]
   [:div.control-button {:on-click #(set-showing :lock)}
    [:div.control-button-title (style :line-height (px 16)) (str (if (-> :user session/get :focus-locked?) "Unlock" "Lock") " focus topic")]
    [:div.control-button-subtitle (-> :focus session/get :name)]]
   (when (-> :user session/get :roles (contains? :member.role/teacher))
     [:div.control-button {:on-click #(do
                                        (session/remove! :overlay)
                                        (accountant/navigate! (path-for :studio)))}
      [:div.control-button-title "Studio"]])
   [:div.control-button {:on-click #(set-showing :settings)}
    [:div.control-button-title "Settings"]]
   [:div.control-button {:on-click logout}
    [:div.control-button-title "Log out"]]]
  )

(defn- build-wide-visitor-toolbar
  [set-showing]
  [:div.visitor-toolbar
   [:div.visitor-message
    "Embark on a " [:div.strong "life long"]
    " journey of discovery in a " [:div.strong "safe"]
    ", " [:div.strong "supportive"]
    " and " [:div.strong "effective"]
    " learning environment, and " [:div.strong "help"]
    " make the world a better place." [:br] [:br]
    (when (-> :route session/get :title (not= :principles))
      [:div.linked-reference {:on-click #(do
                                           (session/remove! :overlay)
                                           (accountant/navigate! (path-for :principles)))} "Read about our 5 principles"])]
   [:div.toolbars
    [build-login-toolbar set-showing false]
    [build-signup-toolbar false]]]
  )

(defn- build-narrow-toolbar
  []
  (let [view-showing? (reagent/atom :main)
        set-showing #(reset! view-showing? %)]
    (fn []
      (case @view-showing?
        :main [build-narrow-main-toolbar set-showing]
        :lock [build-lock-toolbar set-showing :main]
        :settings [build-settings-toolbar set-showing :main]
        :login [build-login-toolbar set-showing true]
        :signup [build-signup-toolbar set-showing true])))
  )

(defn- build-wide-toolbar
  []
  (let [view-showing? (reagent/atom (if (-> :user session/get :first-name) :user :visitor))
        set-showing #(reset! view-showing? %)]
    (fn []
      (case @view-showing?
        :user [build-wide-user-toolbar set-showing]
        :visitor [build-wide-visitor-toolbar set-showing]
        :settings [build-settings-toolbar set-showing :user]
        :lock [build-lock-toolbar set-showing :user])))
  )

(defn- build-home-subheader
  []
  (let [xid (reagent/atom (random-uuid))]
    (fn []
      (let [{:keys [id level name bookmarked?]} (session/get :focus)
            search-menu? (-> :overlay session/get (= :search))]
        [:div.page-subheader
         (when (-> :help-step session/get (= 1))
           [:div.help (style :position :absolute :top (px 50) :left (pct 50) :transform (translate (pct -50) (px 0)))
            [:svg (merge
                    {:width (px 20) :height (px 20)}
                    (style :position :absolute :top (px -20) :left (pct 50) :transform (translate (pct -50) (px 0))))
             [:path {:d "M0,20 L20,20 L10,0 z" :fill leading-colour-3}]]
            [:div.help-close-all.linked-reference {:on-click #(contained % (do
                                                                             (session/put! :help-step 4)
                                                                             (cookies/set! :help-step 4 {:path "/" :max-age (* 60 60 24 365)})))}
             "close help"]
            [:div.help-title
             "Help 1 of 3"]
            [:div.help-content
             "Find something interesting to learn."]
            [:div.help-next.button {:on-click #(contained % (do
                                                              (session/put! :help-step 2)
                                                              (cookies/set! :help-step 2 {:path "/" :max-age (* 60 60 24 365)})))}
             "next"]])
         (when-not search-menu?
           (let [{:keys [focus-locked?]} (session/get :user)]
             [:div.focus-panel (merge
                                 (when-not focus-locked?    ;; TODO: show a message?
                                   {:on-click #(contained % (do
                                                              (session/remove! :preview)
                                                              (session/update! :overlay (fn [o] (when (not= o :search) :search)))))})
                                 (when focus-locked?
                                   {:class "locked"}))
              [:div.search-icon
               (if focus-locked?
                 (let [colour (alpha+ leading-colour-0 -0.8)]
                   [:svg {:width (px 30) :height (px 20) :view-box "0 0 35 20"}
                    [:circle {:cx (px 15) :cy (px 10) :r (px 14) :stroke-width (px 2) :stroke colour :fill :none}]
                    [:circle {:cx (px 15) :cy (px 10) :r (px 9) :stroke-width (px 2) :stroke colour :fill :none}]
                    [:circle {:cx (px 15) :cy (px 10) :r (px 2) :stroke-width (px 3) :stroke colour :fill :none}]])
                 [:svg {:width (px 30) :height (px 30) :view-box "0 0 100 100"}
                  [:line {:x1 (px 20) :y1 (px 80) :x2 (px 35) :y2 (px 60) :stroke (alpha+ leading-colour-3 -0.3) :stroke-width (px 2)}]
                  [:circle {:cx (px 50) :cy (px 40) :r (px 20) :stroke (alpha+ leading-colour-3 -0.3) :stroke-width (px 2) :fill :none}]])]
              [:div.focus-title name]
              [build-level level true false contain-event]
              [build-bookmark id bookmarked? #(do
                                                (reset! xid (random-uuid))
                                                (session/update-in! [:focus :bookmarked?] not))]]))
         (when search-menu?
           [build-concept-search (if (-> :view session/get :narrow?) "learn about.." "what would you like to learn about?")
            #(when (= % id) (session/update-in! [:focus :bookmarked?] not))
            #(session/remove! :overlay)
            @xid])])))
  )

(defn- build-subheader
  [page]
  (case page
    :home [build-home-subheader]
    :profile [:div.page-subheader [:div.welcome-message (str "Welcome, " (-> :user session/get :first-name) "!")]]
    :principles [:div.page-subheader [:div.welcome-message "Our 5 Principles"]]
    :cookie-policy [:div.page-subheader [:div.welcome-message "Cookie Policy"]]
    :studio [:div.page-subheader [:div.welcome-message "Studio"]]
    nil)
  )

(defn page-chrome
  []
  (if-let [help-step-cookie (cookies/get "help-step")]
    (session/put! :help-step (js/parseInt help-step-cookie))
    (do
      (session/put! :help-step 1)
      (cookies/set! :help-step 1 {:path "/" :max-age (* 60 60 24 365)})))
  (let [hide-all #(do (-> (jq ".popup") jq-hide)
                      (-> (jq ".hint") (jq-css "visibility" "hidden"))
                      (session/remove! :preview)
                      (session/remove! :overlay))
        update-width #(session/put! :view (let [w (-> (jq js/window) jq-width)]
                                            {:width   w
                                             :narrow? (< w 700)}))
        cookie-message-visible? (reagent/atom (not (cookies/get :cookie-status)))]
    (-> (jq js/window)
        (jq-off "orientationchange") (jq-on "orientationchange" update-width)
        (jq-off "resize") (jq-on "resize" update-width))
    (update-width)
    (when-not (-> :view session/get :narrow?)
      (-> (jq js/window) (jq-off "click") (jq-on "click" hide-all)))
    (fn []
      (when-not (session/get :focus)
        (load-focus))
      (let [narrow? (-> :view session/get :narrow?)
            menu? (-> :overlay session/get (= :menu))
            search? (-> :overlay session/get (= :search))
            {:keys [page title]} (session/get :route)]
        [:div.page-chrome {:class    (if narrow? "narrow" "wide")
                           :on-click #(contained % (hide-all))}
         [:div.page-header-background
          [:div.page-header (merge
                              {:on-click contain-event}
                              (when menu? {:class "selected"}))
           [:div.page-header-content
            [:div.page-header-icon {:on-click #(do
                                                 (hide-all)
                                                 (session/remove! :focus)
                                                 (accountant/navigate! (path-for :home)))}
             [build-kuhut-icon]
             (when-not (or narrow? search?)
               [:div.page-header-title
                "kuhut"
                [:div.page-header-subtitle
                 "the culture of learning.."]])]
            (when-not narrow?
              [build-subheader title])
            (when (or narrow? (not search?))
              [:div.page-header-control (when menu? {:class "selected"})
               (when-let [first-name (-> :user session/get :first-name)]
                 (when-not narrow?
                   [:div.page-header-name {:on-click #(do
                                                        (accountant/navigate! (path-for :profile))
                                                        (session/remove! :overlay))}
                    first-name]))
               [:svg.menu {:view-box "0 0 100 100" :on-click #(contained % (do
                                                                             (session/remove! :preview)
                                                                             (session/update! :overlay (fn [o] (when (not= o :menu) :menu)))))}
                [:path {:d (if menu? "M25,25 L75,75 M25,75 L75,25" "M25,40 L75,40 M25,60 L75,60")}]]
               (when menu?
                 (if narrow?
                   [build-narrow-toolbar]
                   [build-wide-toolbar]))
               (when (-> :help-step session/get (= 3))
                 [:div.help (apply style (concat
                                           [:position :absolute :top (px 50)]
                                           (if narrow?
                                             [:left (px 5)]
                                             [:right (px 5)])))
                  [:svg (merge
                          {:width (px 20) :height (px 20)}
                          (apply style (concat
                                         [:position :absolute :top (px -20)]
                                         (if narrow?
                                           [:left (px 5)]
                                           [:right (px 5)]))))
                   [:path {:d "M0,20 L20,20 L10,0 z" :fill leading-colour-3}]]
                  [:div.help-close-all.linked-reference
                   {:on-click #(contained % (do
                                              (session/put! :help-step 4)
                                              (cookies/set! :help-step 4 {:path "/" :max-age (* 60 60 24 365)})))}
                   "close help"]
                  [:div.help-title
                   "Help 3 of 3"]
                  [:div.help-content
                   "Find out more about our mission, and " [:div.strong [:em "join the learning revolution!"]]]
                  [:div.help-prev.button {:on-click #(contained % (do
                                                                    (session/put! :help-step 2)
                                                                    (cookies/set! :help-step 2 {:path "/" :max-age (* 60 60 24 365)})))}
                   "previous"]])])]
           (when (and narrow? (not menu?))
             [build-subheader title])]]
         [:div.page-content
          (when page [page])]
         [:div.page-footer.no-user-select {:on-click #(session/remove! :preview)}
          "Copyright © 2016 - 2019 TineTree"
          [:div.linked-reference.page {:on-click #(accountant/navigate! (path-for :principles))} "Our 5 Principles"]
          [:div.linked-reference.page {:on-click #(accountant/navigate! (path-for :cookie-policy))} "Cookie Policy"]
          (when @cookie-message-visible?
            [:div.footer-cookie-message
             "We use cookies to make your browsing more enjoyable. "
             (when (-> :route session/get :title (not= :cookie-policy))
               [:div.linked-reference {:on-click #(accountant/navigate! (path-for :cookie-policy))} "View our Cookie Policy"])
             [:div.button {:on-click #(do
                                        (cookies/set! :cookie-status "acknowledged" {:path "/" :max-age (* 60 60 24 365)})
                                        (reset! cookie-message-visible? false))}
              "Acknowledge"]])]])))
  )

;; TODO: add keyboard controls for next?
