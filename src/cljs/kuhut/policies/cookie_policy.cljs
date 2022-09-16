(ns kuhut.policies.cookie-policy
  (:require [reagent.session :as session]
            [kuhut.router :refer [path-for]]
            [kuhut.util :refer [contained]]
            [accountant.core :as accountant])
  )

(set! *warn-on-infer* true)

(defn cookie-policy-page
  [user]
  [:div.policy-page
   [:div.policy-group
    [:div.policy-title "Does kuhut.com use cookies?"]
    [:div.policy-content "Yes, kuhut.com uses the cookies to ensure that everyone who uses the website has the best possible experience. By continuing to use the website, you are agreeing to the use of cookies for the purposes described in this policy."]]
   [:div.policy-group
    [:div.policy-title "What is a cookie?"]
    [:div.policy-content "An HTTP cookie (or simply - cookie) is a small piece of data stored on the user's computer by the user's web browser, in order to keep a record of arbitrary pieces of information related to the user's activity on the website."]]
   [:div.policy-group
    [:div.policy-title "What cookies does kuhut.com use?"]
    [:div.policy-content "kuhut.com uses the following cookies:"
     [:ul
      [:li [:em "cookie-status"] ": this is a cookie that tells kuhut.com whether the user has accepted our cookie policy;"]
      [:li [:em "help-step"] ": this is a cookie that tells kuhut.com which help page to show to the user;"]
      [:li [:em "kuhut-v-token"] " and " [:em "kuhut-m-token"] ": these two cookies tell kuhut.com which session (whether logged in or as a visitor) the user is connected to. This helps us make sure that you can enjoy all the features of the website without having to re-login on every page. This also allows you to use the website's main features without having to sign up or log in at all."]
      [:li [:em "the reCAPTCHA cookies"] ": these are a set of cookies provided by Google in order to enable the reCAPTCHA functionality on the "
       (if (-> :user session/get :first-name)
         "sign up page"
         [:div.linked-reference {:on-click #(contained % (session/put! :overlay :menu))} "sign up page"])
       ". Learn more about "
       [:div.linked-reference.external {:on-click #(do (.open js/window "https://www.google.com/recaptcha") false)} "reCAPTCHA"]
       "."]]]]
   [:div.policy-group
    [:div.policy-title "How can the cookies be managed?"]
    [:div.policy-content "Most browsers allow you to control the cookies through their settings. Please note that kuhut.com will not work properly without the cookies described above."]]
   [:div.linked-reference.membership {:on-click #(do
                                                   (session/remove! :overlay)
                                                   (accountant/navigate! (path-for :home)))} "back to learning"]]
  )
