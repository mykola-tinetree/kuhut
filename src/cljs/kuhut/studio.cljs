(ns kuhut.studio
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as reagent]
            [kuhut.util :refer [build-checkbox contained timestamp style contain-event copy-to-clipboard get-level-data get-level-colour jq jq-text jq-focus jq-append jq-click jq-blur jq-prop jq-val jq-find jq-get document]]
            [kuhut.shared.definitions :as defs :refer [px pct alpha+ flex border brightness+]]
            [kuhut.worksheet :refer [render-worksheet-content]]
            [reagent.format :refer [format]]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]
            [reagent.session :as session])
  )

(set! *warn-on-infer* true)

(defn build-add-button
  [size on-click]
  [:div.button.add {:on-click #(contained % (on-click))}
   [:svg {:width (px size) :height (px size) :view-box "0 0 16 16"}
    [:line {:x1 8 :y1 2 :x2 8 :y2 14 :stroke defs/correct-colour :stroke-width 2 :stroke-linecap :round}]
    [:line {:x1 2 :y1 8 :x2 14 :y2 8 :stroke defs/correct-colour :stroke-width 2 :stroke-linecap :round}]]]
  )

(defn build-duplicate-button
  [size on-click]
  [:div.button.duplicate {:on-click #(contained % (on-click))}
   [:svg {:width (px size) :height (px size) :view-box "0 0 16 16"}
    [:rect {:x 2 :y 2 :width 7 :height 10 :rx 1 :ry 1 :stroke defs/leading-colour-0 :fill :none :stroke-width 1 :stroke-linecap :round}]
    [:rect {:x 5 :y 3.5 :width 7 :height 10 :rx 1 :ry 1 :stroke defs/leading-colour-0 :fill :none :stroke-width 1 :stroke-linecap :round}]]]
  )

(defn build-delete-button
  [size on-click]
  [:div.button.delete {:on-click #(contained % (on-click))}
   [:svg {:width (px size) :height (px size) :view-box "0 0 16 16"}
    [:line {:x1 2 :y1 2 :x2 14 :y2 14 :stroke defs/incorrect-colour :stroke-width 2 :stroke-linecap :round}]
    [:line {:x1 14 :y1 2 :x2 2 :y2 14 :stroke defs/incorrect-colour :stroke-width 2 :stroke-linecap :round}]]]
  )

(defn highlight-text
  [s h]
  (if (seq h)
    (let [sc (-> s (or "") clojure.string/lower-case)
          hc (-> h (or "") clojure.string/lower-case)
          sl (count s)
          hl (count h)]
      (loop [last-index 0
             result []]
        (if (>= last-index sl)
          result
          (if-let [new-index (clojure.string/index-of sc hc last-index)]
            (let [end-index (+ new-index hl)]
              (recur end-index (into result (concat (when (> new-index last-index)
                                                      [(subs s last-index new-index)])
                                                    [[:div.highlight (subs s new-index end-index)]]))))
            (conj result (subs s last-index))))))
    [s])
  )

(def ^:const TYPES ["concept" "template" "resource"])

(defn- build-content-item
  [{:keys [id type] :as content} search on-delete]
  [:div.content-item
   (when on-delete
     [:div.content-delete-button
      [build-delete-button 16 #(go
                                 (<! (http/post "/studio/edit/remove-content" {:edn-params {:id id :type type}}))
                                 (on-delete id))]])
   [:div.content-information
    [:div.content-title
     (into [:div.content-id {:on-click #(contained % (copy-to-clipboard (-> ^js/Event % .-target jq jq-text)))}]
           (if (seq search)
             (highlight-text (str id) search)
             [(str id)]))]
    (when (= type :concept)
      (let [{:keys [name tags]} content]
        (into [:div.content-name]
              (concat
                (if (seq search)
                  (highlight-text name search)
                  [name])
                (map (fn [t] (into [:div.content-tag] (if (seq search) (highlight-text t search) [t]))) (sort tags))))))
    (let [description ((if (= type :template) :content :description) content)]
      (into [(case type :template :div.template-description :div.content-description)]
            (if (seq search)
              (highlight-text description search)
              [description])))]
   [:div.content-preview
    [:div.content-type (str (when (= type :resource) (str (:content-type content) " ")) (name type))]
    (case type
      :concept [:div.cost (if-let [cost (:cost content)] (format "£%.2f" cost) "FREE")]
      :resource (case (:content-type content)
                  ("image/png" "image/jpg" "image/jpeg" "image/gif" "image/tiff" "image/bmp" "image/svg" "image/svg+xml"
                    ) [:img {:src (str "/resource/" id "?ts=" (timestamp))}]
                  ("audio/mp3") [:audio {:controls :controls :type "audio/mpeg"
                                         :src      (str "/resource/" id "?ts=" (timestamp))}
                                 "Your browser doesn't support audio."]
                  nil)
      :template (when-let [staleness (:staleness content)]
                  [:div.flags (str staleness " flags")]))
    (when-let [weight (:weight content)]
      [:div.weight weight])]]
  )

(defn- build-user-dynamics-area
  []
  (let [id (str "chart-" (random-uuid))
        chart (atom nil)
        include-visitors? (reagent/atom false)
        update-chart-with-data (fn [values]
                                 (when-let [c @chart] (.destroy ^js/ApexCharts c))
                                 (.render ^js/ApexCharts (reset! chart (js/ApexCharts.
                                                                         (.getElementById (document) id)
                                                                         (clj->js {:chart   {:type       :line
                                                                                             :toolbar    {:show false}
                                                                                             :zoom       {:enabled false}
                                                                                             :animations {:enabled false}}
                                                                                   :tooltip {:x {:format "yyyy-MM-dd HH:mm"}}
                                                                                   :markers {:size        4
                                                                                             :colors      [defs/leading-colour-0]
                                                                                             :strokeWidth 2
                                                                                             :hover       {:size 6}}
                                                                                   :stroke  {:width 4}
                                                                                   :series  [{:name "number of users"
                                                                                              :data values}]
                                                                                   :colors  [(alpha+ defs/leading-colour-3 -0.5)]
                                                                                   :xaxis   {:type :datetime}})))))
        update-chart #(go
                        (let [values (:body (<! (http/post "/studio/users/join-times"
                                                           {:edn-params {:include-visitors? @include-visitors?}})))
                              values (conj values [(new js/Date) (-> values last second)])]
                          (update-chart-with-data values)))]
    (update-chart)
    (fn []
      (when @chart (update-chart))
      [:div.explorer-area
       [:div.explorer-area-title
        [:div.explorer-refresh.button.no-user-select {:on-click #(update-chart)}
         "refresh"]
        "User dynamics"]
       [:div.explorer-area-content
        [build-checkbox :multiple 16 @include-visitors? "include visitors?" #(swap! include-visitors? not)]
        [:div (merge {:id id} (style :height (px 500)))]]]))
  )

(defn- build-stale-template-item
  [id]
  (let [content (reagent/atom nil)
        current-id (atom nil)]
    (fn [id]
      (when (not= id @current-id)
        (reset! current-id id)
        (go (reset! content (:body (<! (http/get (str "/studio/template/" id)))))))
      (when-let [c @content]
        [build-content-item c nil nil])))
  )

(defn- build-stale-templates-area
  [on-content-select]
  (let [stale-templates (reagent/atom nil)
        recalculate #(go (reset! stale-templates (:body (<! (http/get "/studio/templates/stale")))))]
    (recalculate)
    (fn [on-content-select]
      [:div.explorer-area
       [:div.explorer-area-title
        [:div.explorer-refresh.button.no-user-select {:on-click #(recalculate)}
         "refresh"]
        "Stale templates"]
       [:div.explorer-area-content
        (into [:div.explorer-stale-templates]
              (map
                (fn [[id n]]
                  [:div.explorer-stale-template
                   [:div.explorer-stale-template-content {:on-click #(on-content-select id :template)}
                    [build-stale-template-item id]]
                   [:div.explorer-stale-template-count n]])
                @stale-templates))]]))
  )

(defn- explorer-tab
  [on-content-select]
  [:div.explorer-tab
   [build-user-dynamics-area]
   [build-stale-templates-area on-content-select]]
  )

(defn- build-content-search-control
  [text placeholder types auto-focus? on-select on-cancel on-delete]
  (let [type-filter (reagent/atom (set types))
        search-value (reagent/atom text)
        selection (reagent/atom nil)
        options (reagent/atom nil)
        recalculate #(go
                       (let [response (:body (<! (http/post "/studio/contents"
                                                            {:edn-params {:ts          (timestamp)
                                                                          :search-text @search-value
                                                                          :only-types  (->> @type-filter
                                                                                            (map (fn [x] (keyword x)))
                                                                                            set)}})))
                             curr-ts (:ts @options)]
                         (when (and curr-ts (< curr-ts (:ts response)))
                           (reset! options response)
                           (reset! selection (when (seq @options) 0)))))
        id (str (random-uuid))
        cancel (fn []
                 (reset! search-value "")
                 (reset! selection nil)
                 (reset! options nil)
                 (-> (jq "#" id) jq-blur)
                 (on-cancel))]
    (fn [text placeholder types auto-focus? on-select on-cancel on-delete]
      (let [selected @selection
            current-options (:data @options)
            search @search-value]
        [:div.content-search-control
         [:div.content-search-input
          (when (or selected (seq search))
            [build-delete-button 20 cancel])
          [:input {:id          id
                   :type        :text
                   :auto-focus  (or auto-focus? (-> current-options seq boolean))
                   :value       search
                   :placeholder placeholder
                   :on-change   #(do
                                   (reset! search-value (.-value ^js/HTMLInputElement (.-target ^js/Event %)))
                                   (recalculate))
                   :on-key-down #(let [k (.-which ^js/Event %)]
                                   (case k
                                     13                     ;; <enter>
                                     (when selected
                                       (let [item (nth current-options selected)]
                                         (on-select (:id item) (:type item)))
                                       (cancel))
                                     27                     ;; <esc>
                                     (cancel)
                                     38                     ;; <up>
                                     (contained % (reset! selection (when selected (mod (dec selected) (count current-options)))))
                                     40                     ;; <down>
                                     (contained % (reset! selection (when selected (mod (inc selected) (count current-options)))))
                                     nil
                                     ))
                   :on-focus    #(do
                                   (reset! options {:ts (timestamp)})
                                   (recalculate))}]]
         (let [tf @type-filter]
           (into [:div.content-search-types
                  (when-let [c (:count @options)]
                    [:div.total-count "Found: " c])]
                 (for [x types]
                   [build-checkbox :multiple 16 (tf x) x #(do
                                                            (swap! type-filter (fn [t] ((if (t x) disj conj) t x)))
                                                            (-> (jq "#" id) jq-focus))])))
         (when (seq current-options)
           (into [:div.content-search-results]
                 (map-indexed
                   (fn [index content]
                     [:div.content-search-result (merge
                                                   {:on-mouse-move #(when (not= selected index)
                                                                      (reset! selection index))
                                                    :on-click      #(do
                                                                      (on-select (:id content) (:type content))
                                                                      (cancel))}
                                                   (when (= selected index)
                                                     {:class "selected"}))
                      [build-content-item content search #(do (recalculate) (on-delete %))]])
                   current-options)))])))
  )

(defn- build-editable-text
  [text placeholder with-enter? on-change]
  (let [is-editing? (reagent/atom false)]
    (fn [text placeholder with-enter? on-change]
      [:div.text-editor (merge
                          {:tab-index   0
                           :on-key-down #(let [k (.-which ^js/Event %)]
                                           (case k
                                             13             ;; <enter>
                                             (when (or (not with-enter?) (.-metaKey ^js/KeyboardEvent %) (.-ctrlKey ^js/KeyboardEvent %))
                                               (contained % (do
                                                              (-> (.-target ^js/Event %) jq (jq-prop "contenteditable" false))
                                                              (reset! is-editing? false)
                                                              (on-change (-> (.-target ^js/Event %) jq jq-text))
                                                              (-> (.-target ^js/Event %) jq (jq-text text)))))
                                             27             ;; <esc>
                                             (contained % (do
                                                            (-> (.-target ^js/Event %) jq (jq-text text))
                                                            (-> (.-target ^js/Event %) jq jq-blur)))
                                             nil
                                             ))
                           :on-click    #(do
                                           (-> (.-target ^js/Event %) jq (jq-prop "contenteditable" true))
                                           (-> (.-target ^js/Event %) jq jq-focus)
                                           (reset! is-editing? true))
                           :on-blur     #(do
                                           (-> (.-target ^js/Event %) jq (jq-prop "contenteditable" false))
                                           (reset! is-editing? false))}
                          (when-not @is-editing?
                            {:class (str "text-value" (when-not (seq text) " empty"))}))
       (if (seq text) text (if @is-editing? "" placeholder))]))
  )

(defn- build-editable-image
  [id content-type on-change updated?]
  (let [element-id (str (random-uuid))]
    (fn [id content-type on-change updated?]
      [:div.image
       [:input (merge
                 {:type      :file
                  :id        element-id
                  :on-change #(when-let [attachment (aget (.-files ^js/HTMLInputElement (.-target ^js/Event %)) 0)]
                                (go
                                  (<! (http/post "/studio/edit/set-resource-attachment"
                                                 {:multipart-params {:id         id
                                                                     :attachment attachment}}))
                                  (on-change)))}
                 (style :display :none))]
       (if content-type
         [:img.editor {:src      (str "/resource/" id "?x=" updated?)
                       :on-click #(-> (jq "#" element-id) jq-click)}]
         [:div.editor {:on-click #(-> (jq "#" element-id) jq-click)} "upload image"])]))
  )

(defn- build-editable-audio
  [id content-type on-change updated?]
  (let [element-id (str (random-uuid))]
    (fn [id content-type on-change updated?]
      [:div.audio
       [:input (merge
                 {:type      :file
                  :id        element-id
                  :on-change #(when-let [attachment (aget (.-files ^js/HTMLInputElement (.-target ^js/Event %)) 0)]
                                (go
                                  (<! (http/post "/studio/edit/set-resource-attachment"
                                                 {:multipart-params {:id         id
                                                                     :attachment attachment}}))
                                  (on-change)))}
                 (style :display :none))]
       [:div.button {:on-click #(-> (jq "#" element-id) jq-click)} (if content-type "Update" "Upload")]
       [:audio {:controls :controls :type "audio/mpeg" :src (str "/resource/" id "?x=" updated?)}
        "Your browser doesn't support audio."]]))
  )

(defn- build-editable-document
  [id content-type on-change updated?]
  (let [element-id (str (random-uuid))]
    (fn [id content-type on-change updated?]
      [:div.document
       [:input (merge
                 {:type      :file
                  :id        element-id
                  :on-change #(when-let [attachment (aget (.-files ^js/HTMLInputElement (.-target ^js/Event %)) 0)]
                                (go
                                  (<! (http/post "/studio/edit/set-resource-attachment"
                                                 {:multipart-params {:id         id
                                                                     :attachment attachment}}))
                                  (on-change)))}
                 (style :display :none))]
       [:div.editor {:on-click #(-> (jq "#" element-id) jq-click)}
        (if content-type "update document" "upload document")]
       (when content-type
         [:div.viewer.linked-reference.external {:on-click #(do (.open js/window (str "/resource/" id "?x=" updated?)) false)}
          "view document"])]))
  )

(defn- build-editable-edn
  [id type]
  (let [cm (atom nil)
        current-id (atom id)
        preview (reagent/atom nil)
        initial-code (reagent/atom "")
        cm-code (reagent/atom "")
        container (reagent/atom nil)
        load-template-preview #(go (reset! preview (:body (<! (http/get (str "/preview/" @current-id "?ts=" (timestamp)))))))
        cm-set-value #(.setValue ^js/CodeMirror @cm %)
        cm-get-value #(.getValue ^js/CodeMirror @cm)
        load-code #(go
                     (reset! initial-code (:body (<! (http/get (str (case type :template "/studio/load-template/" "/resource/") @current-id "?ts=" (timestamp))))))
                     (cm-set-value @initial-code)
                     (reset! cm-code @initial-code)
                     (when (= type :template)
                       (load-template-preview)))
        save-action #(when (not= @initial-code @cm-code)
                       (go
                         (reset! initial-code @cm-code)
                         (<! (http/post "/studio/edit/set-content-edn" {:edn-params {:id @current-id :type type :text (cm-get-value)}}))
                         (when (= type :template)
                           (load-template-preview))))]
    (reagent/create-class
      {:reagent-render
       (fn [id type]
         (when (not= id @current-id)
           (reset! current-id id)
           (when (= type :template)
             (load-template-preview)))
         [:div.edn
          [:textarea {:value "" :read-only true}]
          [:div.button.revert (merge
                                {:on-click #(load-code)}
                                (when (= @initial-code @cm-code)
                                  {:class "inactive"}))
           "Revert"]
          [:div.button.save (merge
                              {:on-click #(save-action)}
                              (when (= @initial-code @cm-code)
                                {:class "inactive"}))
           "Save"]
          (when (= type :template)
            (when-let [p @preview]
              [:div.template-preview
               [:div.worksheet
                [render-worksheet-content p
                 (let [{:keys [width narrow?]} (session/get :view)]
                   (if narrow? width (min (* 0.9 width) 1010)))
                 nil container false true]]]))])
       :component-did-mount
       (fn [this]
         (reset! cm (.fromTextArea js/CodeMirror
                                   (-> this reagent/dom-node jq (jq-find "textarea") (jq-get 0))
                                   #js {:mode :clojure :lineNumbers true}))
         (.on ^js/CodeMirror @cm "change" (fn [e o] (reset! cm-code (.getValue ^js/CodeMirror e))))
         (.setOption ^js/CodeMirror @cm "extraKeys" (clj->js {"Cmd-S"  #(save-action)
                                                              "Ctrl-S" #(save-action)}))
         (load-code))
       :component-will-update
       (fn [_ [_ id]]
         (when (not= id @current-id)
           (reset! current-id id)
           (load-code)))}))
  )

(defn- build-components-list
  [id components on-change on-click on-delete]
  (into [:div.connection-list]
        (concat
          (map-indexed
            (fn [index {:keys [component weight ref]}]
              [:div.connection (merge
                                 {:on-click #(on-click (:id component) (:type component))}
                                 (when (odd? index) {:class "odd"}))
               [:div.connection-controls {:on-click contain-event}
                [build-delete-button 14
                 #(go
                    (<! (http/post "/studio/edit/remove-ref" {:edn-params {:ref    ref
                                                                           :parent id}}))
                    (on-change))]]
               [:div.connection-data {:on-click contain-event}
                [build-editable-text (str weight) "set weight" false
                 #(go
                    (<! (http/post "/studio/edit/add-component" {:edn-params {:from id :to (:id component) :type (:type component) :weight %}}))
                    (on-change))]]
               [build-content-item component "" nil]])
            components)
          [[build-content-search-control "" "add a component" ["concept" "template"] false
            #(go
               (<! (http/post "/studio/edit/add-component" {:edn-params {:from id :to % :type %2 :weight "1"}}))
               (on-change)) #() on-delete]]))
  )

(defn- build-component-of-list
  [id type components on-change on-click on-delete]
  (into [:div.connection-list]
        (concat
          (map-indexed
            (fn [index {:keys [concept weight ref]}]
              [:div.connection (merge
                                 {:on-click #(on-click (:id concept) :concept)}
                                 (when (odd? index) {:class "odd"}))
               [:div.connection-controls {:on-click contain-event}
                [build-delete-button 14
                 #(go
                    (<! (http/post "/studio/edit/remove-ref" {:edn-params {:ref    ref
                                                                           :parent (:id concept)}}))
                    (on-change))]]
               [build-content-item concept "" nil]
               [:div.connection-data {:on-click contain-event}
                [build-editable-text (str weight) "set weight" false
                 #(go
                    (<! (http/post "/studio/edit/add-component" {:edn-params {:from (:id concept) :to id :type type :weight %}}))
                    (on-change))]]])
            components)
          [[build-content-search-control "" "add to concept" ["concept"] false
            #(go
               (<! (http/post "/studio/edit/add-component" {:edn-params {:from % :to id :type type :weight "1"}}))
               (on-change)) #() on-delete]]))
  )

(defn- build-recommendations-list
  [id type recommendations on-change on-click on-delete]
  (into [:div.connection-list]
        (concat
          (map-indexed
            (fn [index {:keys [recommendation weight ref]}]
              [:div.connection (merge
                                 {:on-click #(on-click (:id recommendation) (:type recommendation))}
                                 (when (odd? index) {:class "odd"}))
               [:div.connection-controls {:on-click contain-event}
                [build-delete-button 14
                 #(go
                    (<! (http/post "/studio/edit/remove-ref" {:edn-params {:ref ref}}))
                    (on-change))]]
               [:div.connection-data {:on-click contain-event}
                [build-editable-text (str weight) "set weight" false
                 #(go
                    (<! (http/post "/studio/edit/add-recommendation" {:edn-params {:from      id
                                                                                   :from-type type
                                                                                   :to        (:id recommendation)
                                                                                   :to-type   (:type recommendation)
                                                                                   :weight    %}}))
                    (on-change))]]
               [build-content-item recommendation "" nil]])
            recommendations)
          [[build-content-search-control "" "add a recommendation" ["concept" "template"] false
            #(go
               (<! (http/post "/studio/edit/add-recommendation" {:edn-params {:from      id
                                                                              :from-type type
                                                                              :to        %
                                                                              :to-type   %2
                                                                              :weight    "1"}}))
               (on-change)) #() on-delete]]))
  )

(defn- build-recommended-for-list
  [id type recommendations on-change on-click on-delete]
  (into [:div.connection-list]
        (concat
          (map-indexed
            (fn [index {:keys [content weight ref]}]
              [:div.connection (merge
                                 {:on-click #(on-click (:id content) (:type content))}
                                 (when (odd? index) {:class "odd"}))
               [:div.connection-controls {:on-click contain-event}
                [build-delete-button 14
                 #(go
                    (<! (http/post "/studio/edit/remove-ref" {:edn-params {:ref ref}}))
                    (on-change))]]
               [build-content-item content "" nil]
               [:div.connection-data {:on-click contain-event}
                [build-editable-text (str weight) "set weight" false
                 #(go
                    (<! (http/post "/studio/edit/add-recommendation" {:edn-params {:from      (:id content)
                                                                                   :from-type (:type content)
                                                                                   :to        id
                                                                                   :to-type   type
                                                                                   :weight    %}}))
                    (on-change))]]])
            recommendations)
          [[build-content-search-control "" "recommend for concept or template" ["concept" "template"] false
            #(go
               (<! (http/post "/studio/edit/add-recommendation" {:edn-params {:from      %
                                                                              :from-type %2
                                                                              :to        id
                                                                              :to-type   type
                                                                              :weight    "1"}}))
               (on-change)) #() on-delete]]))
  )

(defn- build-relations-list
  [id type relations on-change on-click on-delete]
  (into [:div.connection-list.relations]
        (concat
          (map-indexed
            (fn [index {:keys         [relation ref]
                        relation-type :type}]
              [:div.connection (merge
                                 {:on-click #(on-click (:id relation) (:type relation))}
                                 (when (odd? index) {:class "odd"}))
               [:div.connection-controls {:on-click contain-event}
                [build-delete-button 14
                 #(go
                    (<! (http/post "/studio/edit/remove-ref" {:edn-params {:ref ref}}))
                    (on-change))]]
               [:div.connection-data {:on-click contain-event}
                [build-editable-text relation-type "add relation" false
                 #(go
                    (<! (http/post "/studio/edit/add-relation" {:edn-params {:from          id
                                                                             :from-type     type
                                                                             :to            (:id relation)
                                                                             :to-type       (:type relation)
                                                                             :relation-type %}}))
                    (on-change))]]
               [build-content-item relation "" nil]])
            relations)
          [[build-content-search-control "" "add a relation" TYPES false
            #(go
               (<! (http/post "/studio/edit/add-relation" {:edn-params {:from          id
                                                                        :from-type     type
                                                                        :to            %
                                                                        :to-type       %2
                                                                        :relation-type ""
                                                                        :new?          true}}))
               (on-change)) #() on-delete]]))
  )

(defn- build-related-to-list
  [id type relations on-change on-click on-delete]
  (into [:div.connection-list.relations]
        (concat
          (map-indexed
            (fn [index {:keys         [content ref]
                        relation-type :type}]
              [:div.connection (merge
                                 {:on-click #(on-click (:id content) (:type content))}
                                 (when (odd? index) {:class "odd"}))
               [:div.connection-controls {:on-click contain-event}
                [build-delete-button 14
                 #(go
                    (<! (http/post "/studio/edit/remove-ref" {:edn-params {:ref ref}}))
                    (on-change))]]
               [build-content-item content "" nil]
               [:div.connection-data {:on-click contain-event}
                [build-editable-text relation-type "add relation" false
                 #(go
                    (<! (http/post "/studio/edit/add-relation" {:edn-params {:from          (:id content)
                                                                             :from-type     (:type content)
                                                                             :to            id
                                                                             :to-type       type
                                                                             :relation-type %}}))
                    (on-change))]]])
            relations)
          [[build-content-search-control "" "make related to" TYPES false
            #(go
               (<! (http/post "/studio/edit/add-relation" {:edn-params {:from          %
                                                                        :from-type     %2
                                                                        :to            id
                                                                        :to-type       type
                                                                        :relation-type ""
                                                                        :new?          true}}))
               (on-change)) #() on-delete]]))
  )

(defn- build-content-title
  [type id show-content load-content history on-delete]
  [:div.title
   [build-delete-button 20 #(go
                              (<! (http/post "/studio/edit/remove-content" {:edn-params {:id id :type type}}))
                              (on-delete id))]
   [build-duplicate-button 20 #(let [new-id (random-uuid)]
                                 (go (<! (http/post "/studio/edit/duplicate-content" {:edn-params {:id new-id :from id :type type}}))
                                     (load-content new-id type)))]
   [:div.id {:on-click #(contained % (-> (.-target ^js/Event %) jq jq-text copy-to-clipboard))}
    (str id)]
   (into [:select {:value     (name type)
                   :on-change #(let [k (-> (.-target ^js/Event %) jq jq-val keyword)]
                                 (when (not= k type)
                                   (go
                                     (<! (http/post "/studio/edit/change-content-type" {:edn-params {:id id :from type :to k}}))
                                     (load-content id k))))}]
         (for [x TYPES] [:option {:value x} x]))]
  )

(defn- build-concept-content
  [concept show-content load-content history on-delete]
  (let [selected-connection (reagent/atom :components)]
    (fn [{:keys        [id description tags is-searchable? weight cost
                        components component-of recommendations recommended-for relations related-to]
          concept-name :name}
         show-content load-content history on-delete]
      [:div.selected-content
       [build-content-title :concept id show-content load-content history on-delete]
       [:div.name
        [build-editable-text concept-name "add name" false
         #(go
            (<! (http/post "/studio/edit/set-concept-name" {:edn-params {:id id :name %}}))
            (load-content id :concept))]
        (into [:div.tags]
              (concat
                (for [tag (sort tags)]
                  [:div.tag
                   [build-editable-text tag "add tag" false
                    #(go
                       (if (seq %)
                         (<! (http/post "/studio/edit/rename-concept-tag" {:edn-params {:id id :from tag :to %}}))
                         (<! (http/post "/studio/edit/remove-concept-tag" {:edn-params {:id id :tag tag}})))
                       (load-content id :concept))]
                   [build-delete-button 10
                    #(go
                       (<! (http/post "/studio/edit/remove-concept-tag" {:edn-params {:id id :tag tag}}))
                       (load-content id :concept))]])
                [[build-editable-text "" "add tag" false
                  #(when (seq %)
                     (go
                       (<! (http/post "/studio/edit/add-concept-tag" {:edn-params {:id id :tag %}}))
                       (load-content id :concept)))]]))]
       [:div.description
        [build-editable-text description "add description" true
         #(go
            (<! (http/post "/studio/edit/set-concept-description" {:edn-params {:id id :description %}}))
            (load-content id :concept))]]
       [:div.concept-cost
        "Cost: "
        [:div.free
         [build-checkbox :single 20 (nil? cost) "free"
          #(go
             (<! (http/post "/studio/edit/set-concept-cost" {:edn-params {:id id :cost nil}}))
             (load-content id :concept))]]
        [:div.paid
         [build-checkbox :single 20 (some? cost) "paid"
          #(go
             (<! (http/post "/studio/edit/set-concept-cost" {:edn-params {:id id :cost "0.0"}}))
             (load-content id :concept))]
         (when (some? cost)
           [:div.price
            "£"
            [build-editable-text (str cost) "add cost" false
             #(go
                (<! (http/post "/studio/edit/set-concept-cost" {:edn-params {:id id :cost %}}))
                (load-content id :concept))]])]]
       [build-checkbox :multiple 20 is-searchable? "searchable"
        #(go
           (<! (http/post "/studio/edit/set-concept-searchability" {:edn-params {:id id :is-searchable? (not is-searchable?)}}))
           (load-content id :concept))]
       [:div.concept-weight "Weight: " (or weight 0)]
       [:div.connections
        [:div.connection-titles
         [:div.connection-title (merge
                                  {:on-click #(reset! selected-connection :components)}
                                  (when (= @selected-connection :components)
                                    {:class "selected"}))
          (str "components (" (count components) ")")]
         [:div.connection-title (merge
                                  {:on-click #(reset! selected-connection :component-of)}
                                  (when (= @selected-connection :component-of)
                                    {:class "selected"}))
          (str "component of (" (count component-of) ")")]
         [:div.connection-title (merge
                                  {:on-click #(reset! selected-connection :recommendations)}
                                  (when (= @selected-connection :recommendations)
                                    {:class "selected"}))
          (str "recommendations (" (count recommendations) ")")]
         [:div.connection-title (merge
                                  {:on-click #(reset! selected-connection :recommended-for)}
                                  (when (= @selected-connection :recommended-for)
                                    {:class "selected"}))
          (str "recommended for (" (count recommended-for) ")")]
         [:div.connection-title (merge
                                  {:on-click #(reset! selected-connection :relations)}
                                  (when (= @selected-connection :relations)
                                    {:class "selected"}))
          (str "relations (" (count relations) ")")]
         [:div.connection-title (merge
                                  {:on-click #(reset! selected-connection :related-to)}
                                  (when (= @selected-connection :related-to)
                                    {:class "selected"}))
          (str "related to (" (count related-to) ")")]]
        (case @selected-connection
          :components [build-components-list id components #(load-content id :concept) load-content on-delete]
          :component-of [build-component-of-list id :concept component-of #(load-content id :concept) load-content on-delete]
          :recommendations [build-recommendations-list id :concept recommendations #(load-content id :concept) load-content on-delete]
          :recommended-for [build-recommended-for-list id :concept recommended-for #(load-content id :concept) load-content on-delete]
          :relations [build-relations-list id :concept relations #(load-content id :concept) load-content on-delete]
          [build-related-to-list id :concept related-to #(load-content id :concept) load-content on-delete])]]))
  )

(defn- build-template-content
  [template show-content load-content history on-delete on-user-e-mail-selection-change]
  (let [selected-connection (reagent/atom :component-of)]
    (fn [{:keys [id component-of recommendations recommended-for relations related-to flags]}
         show-content load-content history on-delete on-user-e-mail-selection-change]
      [:div.selected-content
       [build-content-title :template id show-content load-content history on-delete]
       (when flags
         [:div.flags
          [:div.flags-title "Flags: "]
          (into [:div.flags-list]
                (map
                  (fn [{:keys   [e-mail first-name last-name]
                        user-id :id}]
                    [:div.flag-user                         ;; TODO: add flag time
                     [:div.button {:on-click #(on-user-e-mail-selection-change e-mail)}
                      (str first-name " " last-name " (" e-mail ")")]
                     [build-delete-button 16 #(go
                                                (<! (http/post "/studio/edit/clear-template-flag" {:edn-params {:id id :user-id user-id}}))
                                                (load-content id :template))]])
                  flags))
          [:div.clear-button
           [:div.button {:on-click #(go
                                      (<! (http/post "/studio/edit/clear-template-flags" {:edn-params {:id id}}))
                                      (load-content id :template))}
            (str "Clear " (count flags) " flag" (when (-> flags count (> 1)) "s"))]]])
       [:div.content [build-editable-edn id :template]]
       [:div.connections
        [:div.connection-titles
         [:div.connection-title (merge
                                  {:on-click #(reset! selected-connection :component-of)}
                                  (when (= @selected-connection :component-of)
                                    {:class "selected"}))
          (str "component of (" (count component-of) ")")]
         [:div.connection-title (merge
                                  {:on-click #(reset! selected-connection :recommendations)}
                                  (when (= @selected-connection :recommendations)
                                    {:class "selected"}))
          (str "recommendations (" (count recommendations) ")")]
         [:div.connection-title (merge
                                  {:on-click #(reset! selected-connection :recommended-for)}
                                  (when (= @selected-connection :recommended-for)
                                    {:class "selected"}))
          (str "recommended for (" (count recommended-for) ")")]
         [:div.connection-title (merge
                                  {:on-click #(reset! selected-connection :relations)}
                                  (when (= @selected-connection :relations)
                                    {:class "selected"}))
          (str "relations (" (count relations) ")")]
         [:div.connection-title (merge
                                  {:on-click #(reset! selected-connection :related-to)}
                                  (when (= @selected-connection :related-to)
                                    {:class "selected"}))
          (str "related to (" (count related-to) ")")]]

        ;; TODO: add a 'referenced from' tab, and check for '#uuid "uuid"'?

        (case @selected-connection
          :component-of [build-component-of-list id :template component-of #(load-content id :template) load-content on-delete]
          :recommendations [build-recommendations-list id :template recommendations #(load-content id :template) load-content on-delete]
          :recommended-for [build-recommended-for-list id :template recommended-for #(load-content id :template) load-content on-delete]
          :relations [build-relations-list id :template relations #(load-content id :template) load-content on-delete]
          [build-related-to-list id :template related-to #(load-content id :template) load-content on-delete])]]))
  )

;; TODO: for things such as components etc - try not to show the component itself

(defn- get-resource-type
  [content-type]
  (case content-type
    ("image/png" "image/jpg" "image/jpeg" "image/gif" "image/tiff" "image/bmp" "image/svg" "image/svg+xml"
      ) :image
    ("audio/mp3") :audio
    "text/plain" :edn
    "application/pdf" :document
    nil)
  )

(defn- build-resource-content
  [resource show-content load-content history on-delete]
  (let [selected-connection (reagent/atom :relations)
        selected-resource-type (reagent/atom (or (-> resource :content-type get-resource-type) :image))
        updated (reagent/atom (random-uuid))]
    (fn [{:keys [id description content-type relations related-to]}
         show-content load-content history on-delete]
      (let [type (or (get-resource-type content-type) @selected-resource-type)]
        [:div.selected-content
         [build-content-title :resource id show-content load-content history on-delete]
         [:div.description
          [build-editable-text description "add description" true
           #(go
              (<! (http/post "/studio/edit/set-resource-description" {:edn-params {:id id :description %}}))
              (load-content id :resource))]]
         [:div.content
          (into [:select {:value     (name type)
                          :on-change #(let [k (-> (.-target ^js/Event %) jq jq-val keyword)]
                                        (when (not= k type)
                                          (go
                                            (<! (http/post "/studio/edit/remove-resource-content" {:edn-params {:id id}}))
                                            (load-content id :resource)
                                            (reset! selected-resource-type k))))}]
                (for [x ["image" "audio" "document" "edn"]] [:option {:value x} x]))
          (let [on-update #(do
                             (reset! updated (random-uuid))
                             (load-content id :resource))]
            (case type
              :image [build-editable-image id content-type on-update @updated]
              :audio [build-editable-audio id content-type on-update @updated]
              :document [build-editable-document id content-type on-update @updated]
              [build-editable-edn id :resource]))]
         [:div.connections
          [:div.connection-titles
           [:div.connection-title (merge
                                    {:on-click #(reset! selected-connection :relations)}
                                    (when (= @selected-connection :relations)
                                      {:class "selected"}))
            (str "relations (" (count relations) ")")]
           [:div.connection-title (merge
                                    {:on-click #(reset! selected-connection :related-to)}
                                    (when (= @selected-connection :related-to)
                                      {:class "selected"}))
            (str "related to (" (count related-to) ")")]]
          (case @selected-connection
            :relations [build-relations-list id :resource relations #(load-content id :resource) load-content on-delete]
            [build-related-to-list id :resource related-to #(load-content id :resource) load-content on-delete])]])))
  )

(defn- content-tab
  [id type on-user-e-mail-selection-change]
  (let [history (atom {:items []
                       :index nil})
        selected-content (reagent/atom nil)
        show-content (fn [{:keys [index items]}]
                       (go
                         (reset! selected-content (when index
                                                    (let [[id type] (nth items index)]
                                                      (:body (<! (http/get (str "/studio/" (name type) "/" id)))))))))
        load-content (fn [id type]
                       (show-content
                         (swap! history
                                (fn [{:keys [items index]}]
                                  (let [items (conj (subvec items 0 (if (seq items)
                                                                      (if (-> items (nth index) first (= id)) index (inc index))
                                                                      0))
                                                    [id type])]
                                    {:items items
                                     :index (-> items count dec)})))))
        on-delete (fn [id]
                    (show-content
                      (swap! history
                             (fn [{:keys [index items]}]
                               (let [items (vec (remove (fn [[item-id]] (= item-id id)) items))]
                                 {:items items
                                  :index (when (seq items) (min index (-> items count dec)))})))))]
    (when id
      (load-content id type))
    (fn [id type on-user-e-mail-selection-change]
      [:div.content-tab
       [build-content-search-control "" "search content" TYPES false load-content #() on-delete]
       [:div.content-controls
        [:div.prev (when-let [s (:index @history)]
                     (when (pos? s)
                       [:div.button {:on-click #(show-content (swap! history update :index dec))}
                        "prev"]))]
        [build-add-button 30 #(let [id (random-uuid)]
                                (go
                                  (<! (http/post "/studio/edit/add-content" {:edn-params {:id id :type :concept}}))
                                  (load-content id :concept)))]
        [:div.next (when-let [s (:index @history)]
                     (when (< s (-> @history :items count dec))
                       [:div.button {:on-click #(show-content (swap! history update :index inc))}
                        "next"]))]]
       (when-let [{:keys [type] :as content} @selected-content]
         (case type
           :concept [build-concept-content content show-content load-content history on-delete]
           :template [build-template-content content show-content load-content history on-delete on-user-e-mail-selection-change]
           [build-resource-content content show-content load-content history on-delete]))]))
  )

(defn- build-user-content
  [id user-id type weight on-click]
  (let [content (reagent/atom nil)
        open? (reagent/atom false)
        current-id (atom nil)
        current-user-id (atom nil)]
    (fn [id user-id type weight on-click]
      (when (or (not= id @current-id) (not= user-id @current-user-id))
        (reset! current-id id)
        (reset! current-user-id user-id)
        (go (reset! content (:body (<! (http/get (str "/studio/knowledge/" (name type) "/" id "/" user-id "?ts=" (timestamp))))))))
      (when-let [{:keys [data components level]} @content]
        [:div.user-search-item-field
         [:div.user-search-content
          [:div.user-search-content-toggle
           (when (seq components)
             [:div.user-search-content-toggle-button.button {:on-click #(swap! open? not)}
              [:svg {:width (px 20) :height (px 20)}
               [:path {:d (if @open? "M2,6 L18,6 L10,14 z" "M6,2 L14,10 L6,18 z") :stroke :none :fill (alpha+ defs/leading-colour-3 -0.5)}]]])]
          [:div.user-search-content-data {:on-click #(on-click id type)}
           [build-content-item data nil nil]]
          (when weight
            [:div.user-search-content-weight weight])
          [:div.user-search-content-level
           (when level
             (let [pr 25
                   [r sx sy ex ey laf] (get-level-data level pr 30)
                   colour (get-level-colour level)
                   level (-> level (* 100) Math/round int)
                   done? (= level 100)]
               [:svg {:width (px 60) :height (px 60)}
                [:circle {:cx     (px 30) :cy (px 30) :r (px pr) :stroke-width (px 2)
                          :stroke (if done? colour (brightness+ defs/black-colour 0.95))
                          :fill   (if done? colour :none)}]
                (when-not done?
                  [:path {:d    (str "M" sx "," sy " A" r "," r ",0," laf ",1," ex "," ey)
                          :fill :none :stroke colour :stroke-width (px 3) :stroke-linecap :round}])
                [:text.no-user-select {:x (px 31) :y (px 36) :font-size (px 16) :text-anchor :middle :fill (if done? :white colour) :font-weight 400}
                 (str level "%")]]))]]
         (when @open?
           (into [:div.user-search-items (when @open? {:class "open"})]
                 (map
                   (fn [[id type weight]]
                     [build-user-content id user-id type weight on-click])
                   components)))])))
  )

(defn- build-user-knowledge
  [id on-click]
  (let [knowledge (reagent/atom nil)
        current-id (atom nil)]
    (fn [id on-click]
      (when (not= id @current-id)
        (reset! current-id id)
        (go (reset! knowledge (:body (<! (http/get (str "/studio/knowledge/user/" id "?ts=" (timestamp))))))))
      (when-let [k @knowledge]
        (into [:div.user-search-knowledge] (map (fn [cid] [build-user-content cid id :concept nil on-click]) k)))))
  )

(defn- build-user-item
  [index user search-text on-click on-delete]
  (let [selected-tab (reagent/atom nil)]
    (fn [index {:keys [id e-mail first-name last-name roles focus lock interests number-of-orders]}
         search-text on-click on-delete]
      (let [tab @selected-tab]
        [:div.user-search-user {:class (clojure.string/join " " (concat
                                                                  (when (odd? index) ["odd"])
                                                                  (when tab ["open"])))}
         [:div.user-search-user-data
          [build-delete-button 10 #(go
                                     (<! (http/post "/studio/edit/delete-user" {:edn-params {:id id}}))
                                     (on-delete))]
          [:div.user-search-user-info
           (into [:div.user-search-user-info-e-mail {:on-click #(contained % (copy-to-clipboard e-mail))}]
                 (highlight-text e-mail search-text))
           [:div.user-search-user-info-name
            (into [:div.user-search-user-info-first-name] (highlight-text first-name search-text))
            (into [:div.user-search-user-info-last-name] (highlight-text last-name search-text))]
           [:div.user-search-user-info-roles (->> roles (map name) (clojure.string/join ","))]]
          [:div.user-search-user-tab.clickable.button
           (merge (style :flex (flex 0 0 80))
                  {:on-click #(reset! selected-tab (when (not= tab :focus) :focus))}
                  (when (= tab :focus)
                    {:class "selected"}))
           "focus"]
          [:div.user-search-user-tab
           (merge (style :flex (flex 0 0 120))
                  {:class (clojure.string/join " " (concat
                                                     (when (seq interests) ["clickable button"])
                                                     (when (= tab :interests) ["selected"])))}
                  (when (seq interests)
                    {:on-click #(reset! selected-tab (when (not= tab :interests) :interests))}))
           (str "Interests (" (count interests) ")")]
          [:div.user-search-user-tab
           (merge (style :flex (flex 0 0 100))
                  {:class (clojure.string/join " " (concat
                                                     (when-not (zero? number-of-orders) ["clickable button"])
                                                     (when (= tab :orders) ["selected"])))}
                  (when-not (zero? number-of-orders)
                    {:on-click #(reset! selected-tab (when (not= tab :orders) :orders))}))
           (str "Orders (" number-of-orders ")")]
          [:div.user-search-user-tab.clickable.button
           (merge (style :flex (flex 0 0 120))
                  {:on-click #(reset! selected-tab (when (not= tab :knowledge) :knowledge))}
                  (when (= tab :knowledge)
                    {:class "selected"}))
           "Knowledge"]
          [:div.user-search-user-tab
           (merge (style :flex (flex 0 0 80) :text-align :right :margin-right (px 10))
                  {:class (clojure.string/join " " (concat
                                                     (when lock ["clickable button"])
                                                     (when (= tab :lock) ["selected"])))}
                  (when lock
                    {:on-click #(reset! selected-tab (when (not= tab :lock) :lock))}))
           (if lock "locked" "unlocked")]]
         (when-let [tab @selected-tab]
           [:div.user-search-user-view
            (case tab
              :focus [build-user-content focus id :concept nil on-click]
              :interests (into [:div.user-search-interests] (map (fn [cid] [build-user-content cid id :concept nil on-click]) interests))
              :knowledge [build-user-knowledge id on-click]
              :lock [:div.user-search-lock
                     [:div.user-search-lock-e-mail (:e-mail lock)]
                     [:div.user-search-lock-pin (:pin lock)]]
              [:div tab])])])))
  )

(defn- users-tab
  [e-mail on-click on-user-e-mail-selection-change]
  (let [search-value (reagent/atom (or e-mail ""))
        include-visitors? (reagent/atom false)
        options (reagent/atom nil)
        recalculate #(go
                       (let [response (:body (<! (http/post "/studio/users"
                                                            {:edn-params {:ts                (timestamp)
                                                                          :search-text       @search-value
                                                                          :include-visitors? @include-visitors?}})))
                             curr-ts (:ts @options)]
                         (when (or (nil? curr-ts) (< curr-ts (:ts response)))
                           (reset! options response))))
        cancel #(do
                  (reset! search-value "")
                  (on-user-e-mail-selection-change @search-value)
                  (recalculate))]
    (fn [e-mail on-click on-user-e-mail-selection-change]
      (let [search @search-value]
        [:div.users-tab
         [:div.user-search-input
          (when (seq search)
            [build-delete-button 20 cancel])
          [:input {:type        :text
                   :auto-focus  true
                   :value       search
                   :placeholder "search users"
                   :on-change   #(do
                                   (reset! search-value (.-value ^js/HTMLInputElement (.-target ^js/Event %)))
                                   (on-user-e-mail-selection-change @search-value)
                                   (recalculate))
                   :on-key-down #(case (.-which ^js/Event %)
                                   27                       ;; <esc>
                                   (cancel)
                                   nil)
                   :on-focus    #(recalculate)}]]
         [:div.user-search-count (str "Found: " (:count @options))]
         [:div.user-search-visitors-filter
          [build-checkbox :multiple 16 @include-visitors? "include visitors?" #(do
                                                                                 (swap! include-visitors? not)
                                                                                 (recalculate))]]
         (into [:div.user-search-results]
               (map-indexed
                 (fn [index user]
                   [build-user-item index user search on-click recalculate])
                 (:data @options)))])))
  )

(defn studio-page
  []
  (let [selected-tab (reagent/atom :explorer)
        selected-content (reagent/atom nil)
        selected-user-e-mail (reagent/atom nil)
        on-content-select #(do
                             (reset! selected-tab :content)
                             (reset! selected-content [% %2]))
        on-user-e-mail-selection-change #(do
                                           (reset! selected-tab :users)
                                           (reset! selected-user-e-mail %))]
    (fn []
      (let [tab @selected-tab]
        [:div.studio-page
         [:div.top-panel
          [:div.studio-tab-list
           [:div.studio-tab-button (merge
                                     {:on-click #(reset! selected-tab :explorer)}
                                     (when (= tab :explorer) {:class "selected"})) "Explorer"]
           [:div.studio-tab-button (merge
                                     {:on-click #(reset! selected-tab :content)}
                                     (when (= tab :content) {:class "selected"})) "Content"]
           [:div.studio-tab-button (merge
                                     {:on-click #(reset! selected-tab :users)}
                                     (when (= tab :users) {:class "selected"})) "Users"]]]
         [:div.bottom-panel
          (case tab
            :explorer [explorer-tab on-content-select]
            :content (let [[id type] @selected-content]
                       [content-tab id type on-user-e-mail-selection-change])
            [users-tab @selected-user-e-mail on-content-select on-user-e-mail-selection-change])]])))
  )
