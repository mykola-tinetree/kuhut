(ns kuhut.search
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs-http.client :as http]
            [reagent.core :as reagent]
            [kuhut.util :refer [timestamp contained build-delete-button build-kuhut-icon jq jq-css]]
            [kuhut.concept :refer [build-concept-preview]]
            [kuhut.shared.definitions :refer [px leading-colour-3]]
            [kuhut.router :refer [path-for]]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]
            [accountant.core :as accountant]
            [reagent.session :as session])
  )

(set! *warn-on-infer* true)

(defn build-concept-search
  [placeholder on-bookmark-toggle on-close xid]
  (let [last-xid (atom nil)
        search-value (reagent/atom "")
        search-results (reagent/atom nil)
        search-fn #(go
                     (let [response (:body (<! (http/post (str "/search/concepts?ts=" (timestamp))
                                                          {:edn-params {:search-text @search-value}})))
                           curr-ts (:ts @search-results)]
                       (when (or (nil? curr-ts) (< curr-ts (:ts response)))
                         (reset! search-results response))))]
    (search-fn)
    (fn [placeholder on-bookmark-toggle on-close xid]
      (when (not= xid @last-xid)
        (reset! last-xid xid)
        (search-fn))
      (let [search @search-value
            narrow? (-> :view session/get :narrow?)]
        [:div.search-view
         [:div.search-input
          [:div.logo {:on-click #(contained % (on-close))}
           [build-kuhut-icon]]
          [:div.input
           (when (seq search)
             [:div.clear-button {:on-click #(contained % (reset! search-value ""))}
              [build-delete-button 14 nil]])
           [:input {:type        :text
                    :value       search
                    :auto-focus  true
                    :placeholder placeholder
                    :on-change   #(do
                                    (reset! search-value (.-value ^js/HTMLInputElement (.-target ^js/Event %)))
                                    (search-fn))
                    :on-key-down #(case (.-which ^js/Event %)
                                    27                      ;; <esc>
                                    (contained % (if (empty? @search-value)
                                                   (on-close)
                                                   (do
                                                     (reset! search-value "")
                                                     (search-fn))))
                                    nil
                                    )}]]
          (when-not narrow?
            [:div.hide-search.linked-reference {:on-click #(contained % (on-close))}
             "hide search"])]
         (into [:div.search-results]
               (map
                 (fn [{:keys [id] :as concept}]
                   [build-concept-preview concept
                    #(do
                       (search-fn)
                       (on-bookmark-toggle id))
                    #(go (<! (http/post "/member/focus" {:edn-params {:concept-id id}}))
                         (-> (jq ".hint") (jq-css "visibility" "hidden"))
                         (session/remove! :overlay)
                         (session/remove! :focus)
                         (accountant/navigate! (path-for :home)))])
                 (:data @search-results)))])))
  )
