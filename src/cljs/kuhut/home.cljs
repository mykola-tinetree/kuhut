(ns kuhut.home
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs-http.client :as http]
            [reagent.core :as reagent]
            [kuhut.util :refer [timestamp get-level-data build-bookmark contain-event build-level style build-delete-button contained load-focus jq jq-css]]
            [kuhut.search :refer [build-concept-search]]
            [kuhut.worksheet :refer [build-worksheet]]
            [kuhut.shared.definitions :refer [px alpha+ brightness+ leading-colour-0 leading-colour-1 leading-colour-2 leading-colour-3 black-colour white-colour]]
            [kuhut.concept :refer [build-concept-preview]]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]
            [reagent.session :as session])
  )

(set! *warn-on-infer* true)

(defn home-page
  []
  [:div.home-page
   (when-not (-> :user session/get :first-name)
     (when-let [level (-> :focus session/get :level)]
       (when (>= level 0.2)
         [:div.signup-banner
          [:div.linked-reference {:on-click #(contained % (do
                                                            (session/put! :overlay :menu)
                                                            (-> (jq ".hint") (jq-css "visibility" "hidden"))))}
           "Create an account"] " to save your progress"])))
   (let [{:keys [width narrow?]} (session/get :view)]
     [build-worksheet
      (if narrow? width (min (* 0.9 width) 1010))
      (-> :focus session/get :id)
      load-focus])]
  )
