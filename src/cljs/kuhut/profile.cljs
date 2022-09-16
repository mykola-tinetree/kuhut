(ns kuhut.profile
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as reagent]
            [kuhut.concept :refer [build-concept-preview build-interest-preview]]
            [kuhut.search :refer [build-concept-search]]
            [kuhut.util :refer [timestamp build-bookmark build-level scroll-to]]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]
            [reagent.session :as session]
            [accountant.core :as accountant])
  )

(set! *warn-on-infer* true)

(defn profile-page
  []
  (let [xid (reagent/atom (random-uuid))
        interests (reagent/atom nil)
        interest-search-visible? (reagent/atom false)
        suggestions (reagent/atom nil)
        select-concept (fn [id]
                         (go
                           (<! (http/post "/member/focus" {:edn-params {:concept-id id}}))
                           (accountant/navigate! "/")))
        load-interests #(go (reset! interests (:body (<! (http/get (str "/member/interests?ts=" (timestamp)))))))
        load-suggestions #(go (reset! suggestions (:body (<! (http/get (str "/member/suggestions?ts=" (timestamp)))))))
        bookmark-toggled #(do
                            (reset! xid (random-uuid))
                            (load-interests)
                            (load-suggestions))]
    (bookmark-toggled)
    (fn []
      [:div.profile-page
       [:div.profile-area
        [:div.profile-area-title "YOUR INTERESTS"]
        [:div.profile-area-content
         (into [:div.profile-interests]
               (map
                 (fn [{:keys [id] :as interest}]
                   [build-interest-preview interest bookmark-toggled #(select-concept id)])
                 @interests))
         [:div#search-interests.linked-reference.profile-interest-search-more
          {:on-click #(when (swap! interest-search-visible? not)
                        (scroll-to "search-interests" -84))}
          (if-not @interest-search-visible?
            "bookmark some new interests"
            (when (-> :view session/get :narrow?) "hide search"))]
         (when @interest-search-visible?
           [build-concept-search "find new interests" bookmark-toggled
            #(reset! interest-search-visible? false) @xid])]]
       [:div.profile-area
        [:div.profile-area-title "LEARNING SUGGESTIONS"]
        (into [:div.profile-area-content]
              (map
                (fn [{:keys [id] :as suggestion}]
                  [build-concept-preview suggestion bookmark-toggled #(select-concept id)])
                @suggestions))]]))
  )
