(ns kuhut.concept
  (:require [kuhut.util :refer [build-bookmark build-level contain-event contained]])
  )

(set! *warn-on-infer* true)

(defn build-concept-preview
  [{:keys [id name bookmarked? tags description cost level]} on-bookmark-toggle on-click]
  [:div.concept-preview {:on-click #(contained % (on-click))}
   [:div.concept-preview-title
    [:div.concept-preview-name name
     [:div.concept-preview-tags (clojure.string/join ", " tags)]]
    [:div.concept-preview-cost
     (when-not cost
       [:div.concept-preview-cost-free "FREE"])]
    [build-bookmark id bookmarked? on-bookmark-toggle]]
   [:div.concept-preview-content
    [:div.concept-preview-content-description description]
    [:div.concept-preview-content-level
     (when level
       [build-level level false false contain-event])]]]
  )

(defn build-interest-preview
  [{:keys [id name bookmarked? level]} on-bookmark-toggle on-click]
  [:div.interest-preview {:on-click #(contained % (on-click))}
   [:div.interest-preview-level
    [build-level level false true #()]]
   [:div.interest-preview-name name]
   [build-bookmark id bookmarked? on-bookmark-toggle]]
  )
