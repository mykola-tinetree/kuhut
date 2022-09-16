(ns kuhut.util
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as reagent]
            [kuhut.shared.definitions :refer [px alpha+ black-colour incorrect-colour keyname]]
            [cljs-http.client :as http]
            [reagent.session :as session]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]])
  )

(set! *warn-on-infer* true)

(defn timestamp [] (.getTime (js/Date.)))

(defn stop-propagation [e] (.stopPropagation ^js/Event e))
(defn contain-event [e] (.preventDefault ^js/Event e) (stop-propagation e))
(defn contained [e code] (contain-event e) code)

(defn style [& info] {:style (into {} (map (fn [[k v]] [k (if (keyword? v) (keyname v) v)]) (partition 2 info)))})

(def ^:const e-mail-pattern #"[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?")

(defn valid-e-mail?
  [e-mail]
  (and (string? e-mail) (re-matches e-mail-pattern e-mail))
  )

(defn get-level-colour
  [level]
  (let [level (-> level (min 1) (max 0))]
    (str "hsl(" (->> level (- 1) (* 100) Math/round int) ",75%," (-> level (* 20) (+ 40) Math/round int) "%)"))
  )

(defn polar-to-cartesian
  [cx cy r a]
  (let [q (-> a (- 90) (* Math/PI) (/ 180.0))]
    [(- cx (-> q Math/cos (* r)))
     (-> q Math/sin (* r) (+ cy))])
  )

(defn get-level-data
  [level r c]
  (let [sa 180
        ea (- sa (* (min level 0.9999) 360))
        [sx sy] (polar-to-cartesian c c r sa)
        [ex ey] (polar-to-cartesian c c r ea)
        laf (if (<= (- sa ea) 180) 0 1)]
    [r sx sy ex ey laf])
  )

(defn jq
  ([f s & n] (js/$ (apply str (into n [s f]))))
  ([f] (js/$ f)))
(defn jq-css
  ([o k v] (.css ^js/$ o k v))
  ([o k] (.css ^js/$ o k)))
(defn jq-prop [o p v] (.prop ^js/$ o p v))
(defn jq-is [o p] (.is ^js/$ o p))
(defn jq-remove-class [o c] (.removeClass ^js/$ o c))
(defn jq-add-class [o c] (.addClass ^js/$ o c))
(defn jq-animate
  ([o p d f] (.animate ^js/$ o p d f))
  ([o p d] (.animate ^js/$ o p d)))
(defn jq-focus [o] (.focus ^js/$ o))
(defn jq-blur [o] (.blur ^js/$ o))
(defn jq-click [o] (.click ^js/$ o))
(defn jq-hide [o] (.hide ^js/$ o))
(defn jq-show [o] (.show ^js/$ o))
(defn jq-width [o] (.width ^js/$ o))
(defn jq-height [o] (.height ^js/$ o))
(defn jq-scroll-left [o] (.scrollLeft ^js/$ o))
(defn jq-scroll-top [o] (.scrollTop ^js/$ o))
(defn jq-append [o e] (.append ^js/$ o e))
(defn jq-remove [o] (.remove ^js/$ o))
(defn jq-offset [o] (.offset ^js/$ o))
(defn jq-select [o] (.select ^js/$ o))
(defn jq-find [o e] (.find ^js/$ o e))
(defn jq-get [o i] (.get ^js/$ o i))
(defn jq-first [o] (.first ^js/$ o))
(defn jq-parent [o] (.parent ^js/$ o))
(defn jq-on [o e f] (.on ^js/$ o e f))
(defn jq-off [o e] (.off ^js/$ o e))
(defn jq-text
  ([o] (.text ^js/$ o))
  ([o t] (.text ^js/$ o t)))
(defn jq-serialise-array [o] (.serializeArray ^js/$ o))
(defn jq-val
  ([o v] (.val ^js/$ o v))
  ([o] (.val ^js/$ o)))

(defn build-flag
  [on-click]
  (let [hint-id (str "hint-" (random-uuid))]
    [:div.flag.no-user-select
     [:div.hint {:id hint-id}
      [:div.linked-reference {:on-click #(contained % (do
                                                        (session/put! :overlay :menu)
                                                        (-> (jq ".hint") (jq-css "visibility" "hidden"))))}
       "Become a member now"] " to flag this worksheet as " [:u "out of date"] "."]
     [:svg {:view-box "0 0 100 100"
            :on-click #(contained % (if (-> :user session/get :first-name)
                                      (on-click)
                                      (when (-> (jq "#" hint-id) (jq-css "visibility") (not= "visible"))
                                        (-> (jq ".hint") (jq-css "visibility" "hidden"))
                                        (-> (jq "#" hint-id)
                                            (jq-css "visibility" "visible")
                                            (jq-css "opacity" "1")
                                            (jq-animate #js {:visibility :visible} 10000
                                                        (fn [] (-> (jq "#" hint-id)
                                                                   (jq-animate #js {:opacity 0} 2000
                                                                               (fn [] (-> (jq "#" hint-id)
                                                                                          (jq-css "visibility" "hidden")))))))))))}
      [:circle.main {:cx (px 50) :cy (px 50) :r (px 45)}]
      [:rect {:x (px 32) :y (px 25) :width (px 7) :height (px 50) :rx (px 5) :ry (px 5)}]
      [:path {:d "M42,25 L72,25 L67,37.5 L72,50 L42,50 z"}]]])
  )

(defn build-bookmark
  [concept-id bookmarked? on-toggle]
  (let [hint-id (str "hint-" (random-uuid))]
    [:div.concept-bookmark.no-user-select
     (merge
       {:on-click #(contained % (if (-> :user session/get :first-name)
                                  (go
                                    (<! (http/post "/member/toggle-bookmark" {:edn-params {:concept-id concept-id}}))
                                    (on-toggle))
                                  (when (-> (jq "#" hint-id) (jq-css "visibility") (not= "visible"))
                                    (-> (jq ".hint") (jq-css "visibility" "hidden"))
                                    (-> (jq "#" hint-id)
                                        (jq-css "visibility" "visible")
                                        (jq-css "opacity" "1")
                                        (jq-animate #js {:visibility :visible} 10000
                                                    (fn [] (-> (jq "#" hint-id)
                                                               (jq-animate #js {:opacity 0} 2000
                                                                           (fn [] (-> (jq "#" hint-id)
                                                                                      (jq-css "visibility" "hidden")))))))))))}
       (when bookmarked?
         {:class "selected"}))
     [:div.hint.no-user-select {:id hint-id}
      [:div.linked-reference {:on-click #(contained % (do
                                                        (session/put! :overlay :menu)
                                                        (-> (jq ".hint") (jq-css "visibility" "hidden"))))}
       "Become a member now"] " and keep track of all your favourite topics!"]
     [:svg {:width (px 20) :height (px 35) :view-box "0 0 25 55"}
      [:path {:d "M5,0 L25,0 L25,49 L15,41 L5,49 L5,0"}]
      [:polygon {:points "15,22 17,27 22,27 18,31 20,37 15,33 10,37 12,31 8,27 13,27"}]]])
  )

(def ^:private levels [[0 "LET'S DO IT!"] [0.05 "GREAT START"] [0.2 "GOOD PROGRESS"] [0.5 "COMPETENT"] [0.75 "PROFICIENT"] [0.9 "EXPERT"]])

(defn- level-title [level] (->> levels (filter (fn [[k]] (<= k level))) last second))

(defn build-level
  [level focus? titled? on-click]
  (let [colour (get-level-colour level)
        [r sx sy ex ey laf] (get-level-data level 18 20)
        size (if titled? 100 (if focus? 36 60))
        level (-> level (min 1) (max 0) (* 100) Math/round int)
        done? (= level 100)]
    [:div.level (merge
                  (when-let [s (seq (concat
                                      (when focus? ["focus"])
                                      (when done? ["done"])
                                      (when titled? ["titled"])))]
                    {:class (clojure.string/join " " s)})
                  {:on-click on-click})
     [:svg {:width (px size) :height (px size) :view-box (str "0 0 40 40")}
      [:circle (merge
                 {:cx (px 20) :cy (px 20) :r (px 18)}
                 (when done? {:stroke colour})
                 (when titled? {:stroke-width (px 5)}))]
      (when-not done?
        [:path {:d (str "M" sx "," sy " A" r "," r ",0," laf ",1," ex "," ey) :stroke colour}])
      (when titled?
        [:text.title.no-user-select
         {:x (px 21) :y (px (if (zero? level) 22 20)) :fill (if (zero? level) (alpha+ black-colour -0.6) colour)}
         (if (zero? level) "INTERESTED" (level-title (/ level 100.0)))])
      (when (or (not titled?) (pos? level))
        [:text.value.no-user-select
         (merge
           {:x (px 21) :y (px (if titled? 29 25))}
           (when-not done? {:fill colour}))
         (str level "%")])]])
  )

(defn build-input-field
  [type title id value auto-focus? on-change on-enter]
  (let [input-value (reagent/atom (or value ""))
        focused? (reagent/atom false)]
    (fn [type title id value auto-focus? on-change on-enter]
      [:div.text-input (merge
                         {:on-click #(-> (jq "#" id) jq-focus)}
                         (when @focused? {:class "focused"}))
       (let [smaller? (or @focused? (seq @input-value))]
         [:div.text-input-title.no-user-select (when smaller? {:class "smaller"}) (str title (when smaller? ":"))])
       [:input.text-input-field {:type        type :id id :name id
                                 :value       @input-value
                                 :auto-focus  auto-focus?
                                 :on-change   #(let [t (.-value ^js/HTMLInputElement (.-target ^js/Event %))
                                                     t (if (-> t count (> 1000)) (subs t 0 1000) t)]
                                                 (reset! input-value t)
                                                 (on-change t))
                                 :on-key-down #(do
                                                 (-> (jq "#" id) (jq-remove-class "error"))
                                                 (when (= (.-which ^js/Event %) 13)
                                                   (on-enter)))
                                 :on-focus    #(reset! focused? true)
                                 :on-blur     #(reset! focused? false)}]]))
  )

(defn build-button
  [title on-click]
  [:div.button {:on-click #(contained % (on-click))}
   title]
  )

(defn build-checkbox
  [type size checked? title on-click]
  [:div.checkbox.no-user-select (merge
                                  {:on-click #(contained % (on-click))}
                                  (style :line-height (px (* size 0.9)) :font-size (px size)))
   [:svg (merge
           {:width (px size) :height (px size) :view-box "0 0 100 100"}
           (style :margin-right (px (* size 0.15))))
    (case type
      :multiple [:rect {:x (px 5) :y (px 5) :width (px 90) :height (px 90) :rx (px 15) :ry (px 15)}]
      [:circle {:cx (px 50) :cy (px 50) :r (px 45)}])
    (when checked?
      (case type
        :multiple [:path {:d "M30,50 L48,70 L75,25" :stroke-linecap :round}]
        [:circle {:cx (px 50) :cy (px 50) :r (px 30) :stroke :none :fill (alpha+ black-colour -0.3)}]))]
   [:div.checkbox-title (style :padding-top (px (* size 0.1)) :margin-left (px (* size 1.15))) title]]
  )

(defn build-delete-button
  [size on-click]
  [:div.delete (when on-click {:on-click #(contained % (on-click))})
   [:svg {:width (px size) :height (px size) :view-box "0 0 16 16"}
    [:path {:d "M2,2 L14,14 M14,2 L2,14" :stroke incorrect-colour :stroke-width 2 :stroke-linecap :round}]]]
  )

(defn build-tooltip
  [content]
  [:div.tooltip
   [:div.tooltip-content content]
   [:div.tooltip-tip]]
  )

(defn load-focus
  []
  (go (session/put! :focus (:body (<! (http/get (str "/member/focus?ts=" (timestamp)))))))
  )

(defn document [] ^js/Document js/document)

(defn copy-to-clipboard
  [text]
  (let [t (jq "<input>")]
    (-> (jq "body") (jq-append t))
    (-> t (jq-val text) jq-select)
    (.execCommand (document) "copy")
    (jq-remove t))
  )

(defn build-kuhut-icon
  []
  [:svg.kuhut-icon {:width (px 38) :height (px 38) :view-box "0 0 100 100"}
   [:path.link {:d "M50,30 L75,70 L25,70 z"}]
   [:circle.motivation {:cx (px 50) :cy (px 30) :r 12}]
   [:circle.assessment {:cx (px 75) :cy (px 70) :r 12}]
   [:circle.education {:cx (px 25) :cy (px 70) :r 12}]]
  )

(defn scroll-to
  ([id offset] (-> (jq "html, body") (jq-animate #js {:scrollTop (-> (jq "#" id) jq-offset
                                                                     (js->clj :keywordize-keys true)
                                                                     :top (+ offset))} 300)))
  ([id] (scroll-to id 0))
  )