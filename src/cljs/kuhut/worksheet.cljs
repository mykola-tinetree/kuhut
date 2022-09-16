(ns kuhut.worksheet
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as reagent]
            [kuhut.shared.definitions :as defs :refer [px pct alpha+ brightness+ shadow white-colour black-colour translate LOADING_RESOURCE_ID tag-and-flags]]
            [kuhut.util :refer [style timestamp contained contain-event build-flag scroll-to jq jq-find jq-animate jq-offset jq-width jq-height jq-scroll-left jq-scroll-top jq-first jq-css document jq-blur jq-text jq-prop jq-click jq-hide jq-is jq-show jq-parent jq-get] :as util]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]
            [kuhut.router :refer [path-for]]
            [cljs-time.coerce :as timec]
            [cljs-time.format :as timef]
            [cljs-time.core :as time-core]
            [accountant.core :as accountant]
            [reagent.session :as session]
            [reagent.cookies :as cookies])
  )

(set! *warn-on-infer* true)

(declare build-space)
(declare build-paragraph)
(declare build-options)
(declare build-table)
(declare build-selector)
(declare build-canvas)
(declare build-list-item)
(declare build-list)
(declare build-citation)
(declare build-text)
(declare build-image)
(declare build-audio)
(declare build-link)
(declare build-reference)
(declare build-code)
(declare build-math)
(declare build-definition)
(declare build-definitions)
(declare build-error)
(declare text-content)
(declare content)
(declare build-extras)
(declare render-worksheet-content)

(defn- element-property
  [[k v]]
  (case k
    :font-size [:font-size v]
    :text-colour [:color v]
    :background-colour [:background-color v]
    :padding [:padding v]
    :border-radius [:border-radius v]
    :text-align [:text-align v]
    :width [:width v]
    nil)
  )

(defn- common-content
  [e]
  (when (or (number? e) (string? e) (char? e) (boolean? e) (uuid? e) (inst? e) (keyword? e))
    (str e))
  )

(defn- build-extras
  [extras width move-container preview? refresh?]
  (or (common-content extras)
      (if (-> extras first (= :text))
        [build-text extras width move-container preview? refresh?]
        [build-paragraph extras width move-container preview? refresh?]))
  )

(defn- build-tooltip
  [parameters width move-container preview? refresh?]
  (when-let [tooltip (:tooltip parameters)]
    [util/build-tooltip [build-extras tooltip width move-container preview? refresh?]])
  )

(def ^:private text-flags {:bold          [:font-weight 500]
                           :italic        [:font-style :italic]
                           :underline     [:text-decoration :underline]
                           :strikethrough [:text-decoration :line-through]
                           :superscript   [:vertical-align :super :font-size :smaller]
                           :subscript     [:vertical-align :sub :font-size :smaller]})

(defn- build-space
  [[_ & [f & r :as c]] width move-container preview? refresh?]
  [:div.space (merge
                {:class (name (or (:type f) :horizontal))}
                (style (case (:type f) :vertical :width :height) (or (:size f) "0px")))]
  )

(defn- build-preview
  [path preview-position]
  (let [current-path (atom path)
        preview (reagent/atom nil)
        id (str "preview-" (random-uuid))
        {:keys [x y]} preview-position
        load #(go
                (reset! preview nil)
                (reset! preview (:body (<! (http/get (str "/preview/" (-> % last first) "?ts=" (timestamp)))))))]
    (load path)
    (reagent/create-class
      {:reagent-render
       (fn [path preview-position]
         (when (not= path @current-path)
           (reset! current-path path)
           (load path))
         (let [narrow? (-> :view session/get :narrow?)]
           [:div.preview (merge
                           {:on-click contain-event}
                           (when-not narrow?
                             (merge
                               {:on-mouse-leave #(contained % (session/remove! :preview))}
                               (style :left (px x) :top (px y) :width (min 700 (-> :view session/get :width))))))
            (when-let [navigation (-> path butlast seq)]
              (into [:div.preview-navigation {:id id}]
                    (interleave
                      (map
                        (fn [i [_ name]]
                          [:div.linked-reference
                           {:on-click #(contained % (session/update-in! [:preview :path] subvec 0 (inc i)))}
                           name])
                        (range)
                        navigation)
                      (repeat " > "))))
            (if-let [p @preview]
              [:div.worksheet
               [render-worksheet-content p (min 680 (-> :view session/get :width (- 20))) nil (reagent/atom nil) true true]]
              [:div.loading "Preview loading..."])
            (when narrow?
              [:div.close-preview-button {:on-click #(contained % (session/remove! :preview))}
               [:svg {:view-box "0 0 30 30"}
                [:path {:d "M2,2 L28,28 M2,28 L28,2"}]]])]))
       :component-did-update
       (fn [this]
         (-> this reagent/dom-node jq (jq-find "> .worksheet") (jq-animate #js {:scrollTop 0} 300))
         (when-not (-> :view session/get :narrow?)
           (let [element (-> this reagent/dom-node jq jq-first)
                 x (- (-> element jq-offset (js->clj :keywordize-keys true) :left) (-> js/window jq jq-scroll-left))
                 y (- (-> element jq-offset (js->clj :keywordize-keys true) :top) (-> js/window jq jq-scroll-top))
                 w (jq-width element)
                 h (jq-height element)
                 W (- (.-innerWidth js/window) 10)
                 H (- (.-innerHeight js/window) 10)]
             (when (-> x (+ w) (> W))
               (jq-css element "left" (px (max (- W w) 0))))
             (when (-> y (+ h) (> H))
               (jq-css element "top" (px (max (- H h) 0)))))))}))
  )

(defn- build-text
  [[_ & [f & r :as c]] width move-container preview? refresh?]
  (let [initial-text (atom nil)
        text-id (str "text-" (random-uuid))]
    (fn [[n & [f & r :as c]] width move-container preview? refresh?]
      (let [[_ flags] (tag-and-flags n)
            key (:key f)
            content (or (get @move-container key)
                        (clojure.string/join r))
            static-content (clojure.string/join (map #(common-content %) (if (map? f) r c)))]
        (when (and key (not (contains? @move-container key)))
          (swap! move-container assoc key content))
        (when (not= content @initial-text)
          (reset! initial-text content))
        (let [class (clojure.string/join " " (concat
                                               (when (:tooltip f) ["tooltipped"])
                                               (when (:preview f) ["with-preview"])
                                               (case (:assessment f)
                                                 :correct ["correct"]
                                                 :incorrect ["incorrect"]
                                                 nil)))]
          (into [:div.text (merge
                             (when (:preview f)
                               (if (or (-> :view session/get :narrow?) preview?)
                                 {:on-click #(contained % (if preview?
                                                            (session/update-in! [:preview :path] conj [(:preview f) static-content])
                                                            (session/put! :preview {:path     [[(:preview f) static-content]]
                                                                                    :position {:x (- (.-pageX ^js/MouseEvent %)
                                                                                                     (-> js/window jq jq-scroll-left)
                                                                                                     50)
                                                                                               :y (- (.-pageY ^js/MouseEvent %)
                                                                                                     (-> js/window jq jq-scroll-top)
                                                                                                     50)}})))}
                                 {:on-mouse-over #(contained % (session/put! :preview {:path     [[(:preview f) static-content]]
                                                                                       :position {:x (- (.-pageX ^js/MouseEvent %)
                                                                                                        (-> js/window jq jq-scroll-left)
                                                                                                        50)
                                                                                                  :y (- (.-pageY ^js/MouseEvent %)
                                                                                                        (-> js/window jq jq-scroll-top)
                                                                                                        50)}}))}))
                             (apply style (concat
                                            (when (map? f)
                                              (mapcat element-property f))
                                            (mapcat text-flags flags)))
                             (when (seq class) {:class class}))
                 (build-tooltip f width move-container preview? refresh?)]
                (if (or (:key f) (:assessment f))
                  (concat
                    [[:div.editable.common {:tab-index   0
                                            :id          text-id
                                            :on-click    #(let [range (.createRange (document))]
                                                            (.selectNodeContents ^js/Range range (.-target ^js/Event %))
                                                            (let [sel (.getSelection js/window)]
                                                              (.removeAllRanges ^js/Selection sel)
                                                              (.addRange ^js/Selection sel range)))
                                            :on-key-down #(let [k (.-which ^js/Event %)]
                                                            (case k
                                                              13 ;; <enter>
                                                              (contained % (-> ^js/Event % .-target jq jq-blur))
                                                              27 ;; <esc>
                                                              (contained % (do
                                                                             (-> ^js/Event % .-target jq (jq-text (or (get @move-container key) "")))
                                                                             (-> ^js/Event % .-target jq jq-blur)))
                                                              nil
                                                              ))
                                            :on-focus    #(-> ^js/Event % .-target jq (jq-prop "contenteditable" (-> f :key boolean)))
                                            :on-blur     #(do
                                                            (swap! move-container assoc key (-> ^js/Event % .-target jq jq-text))
                                                            (-> ^js/Event % .-target jq (jq-prop "contenteditable" false)))}
                      content]]
                    (when-let [solution (:solution f)]
                      [[build-text [:text {:assessment :correct} solution] width move-container preview? refresh?]]))
                  static-content))))))
  )

(defn- build-code
  [c width move-container preview? refresh?]
  (let [cm (atom nil)
        editor-config (atom c)
        output (reagent/atom nil)
        load-output #(go
                       (reset! output {:exit 0 :out "running.."})
                       (reset! output (:body (<! (http/post "/execute"
                                                            {:edn-params {:language %
                                                                          :code     %2
                                                                          :context  %3}})))))
        rebuild-editor (fn [this [n & [f & r :as c]]]
                         (let [[_ flags] (tag-and-flags n)
                               code (clojure.string/join "\n" (if (map? f) r c))
                               inline? (:inline flags)]
                           (when-let [cm @cm]
                             (.toTextArea ^js/CodeMirror cm))
                           (reset! cm (.fromTextArea js/CodeMirror
                                                     (-> this reagent/dom-node jq (jq-find "textarea") (jq-get 0))
                                                     (clj->js (merge
                                                                {:mode         (clojure.string/lower-case (name (or (:language f) :null)))
                                                                 :readOnly     (-> f :key not)
                                                                 :lineWrapping true
                                                                 :lineNumbers  (-> flags (contains? :toggle-line-numbers) (#(if inline? (identity %) (not %))))
                                                                 :minHeight    18}
                                                                (when inline?
                                                                  {:extraKeys {"Enter" (fn [cm] (when inline? (-> ^js/CodeMirror cm .getInputField jq jq-blur)))}})))))
                           (when-let [key (:key f)] (swap! move-container assoc key code))
                           (.on ^js/CodeMirror @cm "change" (fn [e _] (when-let [key (:key f)] (swap! move-container assoc key (.getValue ^js/CodeMirror e)))))
                           (when-not (:key f)
                             (.on ^js/CodeMirror @cm "touchstart" (fn [c _] (-> ^js/CodeMirror c .getInputField jq jq-click))))
                           (.setValue ^js/CodeMirror @cm code)
                           (when-let [height (:height f)]
                             (.setSize ^js/CodeMirror @cm nil height))
                           (js/setTimeout #(.refresh ^js/CodeMirror @cm) 1)
                           (reset! output nil)
                           (when (-> f :output (= :visible))
                             (load-output (or (:language f) :null) code (:executor f)))))]
    (reagent/create-class
      {:reagent-render
       (fn [[n f] width move-container preview? refresh?]
         (let [[_ flags] (tag-and-flags n)
               inline? (:inline flags)
               editable? (:key f)
               output-type (:output f)
               {:keys [exit out err]} @output
               class (clojure.string/join " " (concat
                                                (when inline? ["inline"])
                                                (when editable? ["editable"])
                                                (when-not inline? ["worksheet-block" "large"])
                                                (case (:assessment f)
                                                  :correct ["correct"]
                                                  :incorrect ["incorrect"]
                                                  nil)))]
           [:div.code (merge
                        (when (seq class) {:class class})
                        (apply style (concat
                                       [:max-width width]
                                       (when-let [font-size (:font-size f)]
                                         [:font-size font-size])
                                       (when-not inline?
                                         [:width (px width)]))))
            (when-let [language (:language f)]
              [:div.code-title (str (name language) " code")])
            [:textarea {:value "" :read-only true}]
            [:div.output
             (when (and output-type (or editable? (not exit)))
               [:div.button.no-user-select
                {:on-click #(contained % (do
                                           (when (and exit editable?)
                                             (reset! output nil))
                                           (load-output (or (:language f) :null) (.getValue ^js/CodeMirror @cm) (:executor f))))}
                (if exit (when editable? "Re-run code") (if editable? "Run code" "Show output"))])
             (when (and output-type exit)
               [:div.output-content
                [:div.output-content-title "output"]
                [:div.output-content-text (clojure.string/trim out)]
                (when (seq err)
                  [:div.output-content-error
                   [:div.output-content-error-title "error"]
                   [:div.output-content-error-text err]])])
             (when (and output-type exit)
               [:div.linked-reference {:on-click #(contained % (reset! output nil))} "hide output"])
             (when (contains? f :solution)
               (let [{:keys [code output]} (:solution f)]
                 [:div.solutions
                  (when code
                    [:div.solution
                     [:div.solution-title "correct solution"]
                     [:div.solution-text (clojure.string/join "\n" (if (vector? code) code [code]))]])
                  (when output
                    [:div.solution
                     [:div.solution-title "correct output"]
                     [:div.solution-text (clojure.string/join "\n" (if (vector? output) output [output]))]])]))]]))
       :component-did-mount
       (fn [this] (rebuild-editor this c))
       :component-will-update
       (fn [this [_ c]]
         (when (not= c @editor-config)
           (reset! editor-config c)
           (rebuild-editor this c)))}))
  )

(defn- build-math
  [c width move-container preview? refresh?]
  (let [update-component (fn [this]
                           (let [node (reagent/dom-node this)]
                             (-> node jq jq-hide)
                             (.Queue js/MathJax.Hub #js ["Typeset" (.-Hub js/MathJax) node])
                             (.Queue js/MathJax.Hub #(-> node jq jq-show))))]
    (reagent/create-class
      {:reagent-render
       (fn [[n f & [r]] width move-container preview? refresh?]
         (let [[_ flags] (tag-and-flags n)
               inline? (:inline flags)]
           [:div.math (merge
                        (let [styles (concat
                                       (when (map? f)
                                         (mapcat element-property f))
                                       (when-not inline?
                                         [:width (px width)]))]
                          (when (seq styles)
                            (apply style styles)))
                        {:class (if inline? "inline" "worksheet-block")})
            (str (if inline? "\\(" "\\[") (if (map? f) r f) (if inline? "\\)" "\\]"))]))
       :component-did-mount
       (fn [this] (update-component this))
       :component-did-update
       (fn [this] (update-component this))}))
  )

(defn- build-coverable-img
  [src width cover]
  (reset! cover true)
  [:img {:on-load #(reset! cover false)
         :src     (str "/resource/" src (when width (str "?width=" width)))}]
  )

(defn build-image-area
  [[_ & [f & r :as c]] width move-container preview? refresh?]
  (let [extra (apply style (concat
                             (when (map? f) (mapcat element-property f))
                             (let [{:keys [x y] :or {x 0 y 0}} (when (map? f) f)]
                               [:left x :top y])))
        pointer-colour (:pointer-colour f (alpha+ defs/white-colour -0.3))
        content (into [:div.image-area-content (merge
                                                 (apply style (concat
                                                                [:border-color pointer-colour :box-shadow (shadow 0 0 10 pointer-colour)]
                                                                (when (map? f) (mapcat element-property f))))
                                                 (when (:tooltip f) {:class "tooltipped"}))
                       (build-tooltip f width move-container preview? refresh?)]
                      (map
                        (fn [element]
                          (or (common-content element)
                              [(content (-> element first tag-and-flags first) build-error) element width move-container preview? refresh?]))
                        (if (map? f) r c)))]
    (case (:pointer f)
      :top-left [:div.image-area.top-left extra
                 [:svg.image-area-pointer {:width (px 8) :height (px 10)}
                  [:path {:d "M0,0 L0,10 L8,10 z" :fill pointer-colour}]]
                 content]
      :top [:div.image-area.top extra
            [:svg.image-area-pointer {:width (px 20) :height (px 6)}
             [:path {:d "M0,6 L20,6 L10,0 z" :fill pointer-colour}]]
            content]
      :top-right [:div.image-area.top-right extra
                  [:svg.image-area-pointer {:width (px 8) :height (px 10)}
                   [:path {:d "M8,0 L0,10 L8,10 z" :fill pointer-colour}]]
                  content]
      :right-top [:div.image-area.right-top extra
                  content
                  [:svg.image-area-pointer {:width (px 10) :height (px 8)}
                   [:path {:d "M0,0 L0,8 L10,0 z" :fill pointer-colour}]]]
      :right [:div.image-area.right extra
              content
              [:svg.image-area-pointer {:width (px 6) :height (px 20)}
               [:path {:d "M0,0 L0,20 L6,10 z" :fill pointer-colour}]]]
      :right-bottom [:div.image-area.right-bottom extra
                     content
                     [:svg.image-area-pointer {:width (px 10) :height (px 8)}
                      [:path {:d "M0,0 L0,8 L10,8 z" :fill pointer-colour}]]]
      :bottom-left [:div.image-area.bottom-left extra
                    content
                    [:svg.image-area-pointer {:width (px 8) :height (px 10)}
                     [:path {:d "M0,0 L0,10 L8,0 z" :fill pointer-colour}]]]
      :bottom [:div.image-area.bottom extra
               content
               [:svg.image-area-pointer {:width (px 20) :height (px 6)}
                [:path {:d "M0,0 L10,6 L20,0 z" :fill pointer-colour}]]]
      :bottom-right [:div.image-area.bottom-right extra
                     content
                     [:svg.image-area-pointer {:width (px 8) :height (px 10)}
                      [:path {:d "M0,0 L8,10 L8,0 z" :fill pointer-colour}]]]
      :left-top [:div.image-area.left-top extra
                 [:svg.image-area-pointer {:width (px 10) :height (px 8)}
                  [:path {:d "M0,0 L10,8 L10,0 z" :fill pointer-colour}]]
                 content]
      :left [:div.image-area.left extra
             [:svg.image-area-pointer {:width (px 6) :height (px 20)}
              [:path {:d "M6,0 L0,10 L6,20 z" :fill pointer-colour}]]
             content]
      :left-bottom [:div.image-area.left-bottom extra
                    [:svg.image-area-pointer {:width (px 10) :height (px 8)}
                     [:path {:d "M0,8 L10,8 L10,0 z" :fill pointer-colour}]]
                    content]
      [:div.image-area extra
       content]))
  )

(defn- build-fullview-image
  [[_ & [f & r :as c] :as i] width move-container preview? refresh? fullview?]
  (let [cover-visible? (reagent/atom true)
        fullview-visible? (reagent/atom false)]
    (fn [[_ & [f & r :as c] :as i] width move-container preview? refresh? fullview?]
      (let [scale (:scale f 0.75)
            block (:block f 100)
            real-width (-> scale (* width) (max block) (min width))]
        [:div.image (merge
                      (merge-with merge
                                  (when-let [position (-> f :position #{:left :right})]
                                    (style :float position))
                                  (when-not fullview?
                                    (style :width (px real-width))))
                      (when-let [classes (seq (concat
                                                (when (:tooltip f) ["tooltipped"])
                                                (when-not fullview? ["worksheet-block"])))]
                        {:class (clojure.string/join " " classes)}))
         (build-tooltip f width move-container preview? refresh?)
         [:div.image-with-areas (when fullview?
                                  {:on-click #(contained % (-> (jq ".popup") jq-hide))})
          (when-not fullview?
            [:div.fullview-toggle {:on-click #(contained % (reset! fullview-visible? true))}
             (let [{:keys [narrow?]} (session/get :view)]
               [:svg {:width (px (if narrow? 20 30)) :height (px (if narrow? 20 30)) :view-box "0 0 30 30"}
                [:path {:d "M3,10 L2,2 L10,3 M2,2 L15,15 L28,2 L20,3 M28,2 L27,10 M27,20 L28,28 L20,27 M28,28 L15,15 L2,28 L10,27 M2,28 L3,20"}]])])
          [build-coverable-img (:resource f) (when-not fullview? real-width) cover-visible?]
          (when (seq r)
            (into [:div.image-areas]
                  (map (fn [area] (build-image-area area width move-container preview? refresh?)) r)))]
         (when-let [title (:title f)]
           [:div.image-title {:on-click contain-event}
            [build-extras title width move-container preview? refresh?]])
         (when @cover-visible?
           [:div.cover {:on-click contain-event}
            [:img.loading {:src (str "/resource/" LOADING_RESOURCE_ID)}]])
         (when @fullview-visible?
           [:div.image-fullview {:on-click #(contained % (reset! fullview-visible? false))}
            [:div.close-fullview-button
             [:svg {:view-box "0 0 30 30"}
              [:path {:d "M2,2 L28,28 M2,28 L28,2"}]]]
            [build-fullview-image i width move-container preview? refresh? true]])])))
  )

(defn- build-image
  [[_ & [f & r :as c] :as i] width move-container preview? refresh?]
  [build-fullview-image i width move-container preview? refresh? false]
  )

(defn- build-audio
  [[_ & [f & r :as c]] width move-container preview? refresh?]
  [:div.audio.worksheet-block (when (:tooltip f) {:class "tooltipped"})
   (build-tooltip f width move-container preview? refresh?)
   [:audio (merge
             {:controls :controls :controls-list :nodownload
              :type     (str "audio/" (name (:type f :mpeg)))
              :src      (str "/resource/" (:resource f))}
             (style :max-width width))
    "Your browser doesn't support audio."]
   (when-let [title (:title f)]
     [:div.audio-title [build-extras title width move-container preview? refresh?]])]
  )

(defn- build-paragraph
  [[_ & [f & r :as c]] width move-container preview? refresh?]
  (let [real-width (- width 10)]
    (into [:div.paragraph.worksheet-block (merge
                                            (apply style (when (map? f) (mapcat element-property f)))
                                            (when (:tooltip f) {:class "tooltipped"}))
           (build-tooltip f real-width move-container preview? refresh?)]
          (map
            (fn [element]
              (or (common-content element)
                  [(content (-> element first tag-and-flags first) build-error) element real-width move-container preview? refresh?]))
            (if (map? f) r c))))
  )

(defn- build-option
  [option-type selected? disabled? inactive? parameters solution option-content margin width move-container preview? refresh? on-click]
  (let [class (clojure.string/join " " (concat
                                         (when (:tooltip parameters) ["tooltipped"])
                                         (when selected? ["selected"])
                                         (when disabled? ["disabled"])
                                         (when (seq solution)
                                           (cond
                                             (contains? solution (:value parameters)) ["correct"]
                                             selected? ["incorrect"]
                                             :else nil))))]
    [:div.option (merge (when-not inactive? {:on-click on-click})
                        (when (seq class) {:class class})
                        (style :margin-left (px margin) :margin-right (px margin)))
     (build-tooltip parameters width move-container preview? refresh?)
     [:div.option-control
      (into [:svg {:view-box "0 0 30 30"}]
            (case option-type
              :single-choice (concat
                               [[:circle {:cx (px 15) :cy (px 15) :r (px 14)}]]
                               (when selected?
                                 [[:path {:d "M9,15 L14,20 L22,10"}]]))
              :multiple-choice (concat
                                 [[:rect {:x (px 1) :y (px 1) :width (px 28) :height (px 28) :rx (px 5) :ry (px 5)}]]
                                 (when selected?
                                   [[:path {:d "M7,15 L13,21 L22,9"}]]))))]
     (into [:div.option-content (apply style (mapcat element-property parameters))]
           (map
             (fn [element]
               (or (common-content element)
                   [(content (-> element first tag-and-flags first) build-error) element width move-container preview? refresh?]))
             option-content))])
  )

(defn- build-options
  [[_ & [f & r] :as c] width move-container preview? refresh?]
  (let [key (:key f)
        tiles? (= (:style f) :tiles)
        class (clojure.string/join " " (concat
                                         (when (:tooltip f) ["tooltipped"])
                                         (when tiles? ["tiles"])))]
    (when (and key (not (contains? @move-container key)))
      (swap! move-container assoc key (or (:selection f) #{})))
    (let [option-type (:type f :multiple-choice)
          solution (:solution f)
          scale (:scale f (if tiles? 0.5 1))
          block (:block f 100)
          column-width (-> scale (* width) (max block) (min width))
          columns (max (-> width (/ column-width) int) 1)
          margin (/ (* 0.005 width) columns (if tiles? 1 3))
          real-column-width (-> width (- (if tiles? 0 20) (* margin (inc columns))) (/ columns)
                                (- (if tiles? 0 (if (-> :view session/get :narrow?) 37 47)) 4))
          selection (if solution (:selection f) (get @move-container key))]
      (if (#{:single-choice :multiple-choice} option-type)
        (into [:div.options.worksheet-block (merge (apply style (mapcat element-property f))
                                                   (when (seq class) {:class class}))
               (build-tooltip f width move-container preview? refresh?)]
              (map
                (fn [options]
                  (into [:div.option-row (style :margin-top (px (* margin 2)) :margin-bottom (px (* margin 2)))]
                        (map
                          (fn [[n & [{:keys [value] :as f} & r]]]
                            (let [[_ flags] (tag-and-flags n)]
                              [build-option option-type (contains? selection value) (:disabled flags)
                               (some? solution) f solution r margin real-column-width move-container preview? refresh?
                               (fn [_]
                                 (if (= option-type :single-choice)
                                   (swap! move-container assoc key #{value})
                                   (swap! move-container update key (fn [s] ((if (s value) disj conj) s value)))))]))
                          options)))
                (partition-all columns r)))
        [build-error (str "Unknown option type " option-type " in " c) width move-container preview? refresh?])))
  )

(defn- build-selector
  [[_ & [f & r]] width move-container preview? refresh?]
  (let [selector-id (str "selector-" (random-uuid))]
    (fn [[_ & [f & r]] width move-container preview? refresh?]
      (let [key (:key f)
            s (if-let [selection (:selection f)] selection (get @move-container key))]
        [:div.selector (merge
                         (apply style (mapcat element-property f))
                         (when-not (:key f) {:class (if (= s (:solution f)) "correct" "incorrect")}))
         (let [v (some (fn [[_ {:keys [value]} v]] (when (= s value) v)) r)]
           [:div.selection (merge
                             {:on-click #(contained % (let [visible? (-> (jq "#" selector-id) (jq-is ":visible"))]
                                                        (-> (jq ".popup") jq-hide)
                                                        (when-not visible?
                                                          (-> (jq "#" selector-id) jq-show))))}
                             (when v {:class "selected"}))
            (or v "select")
            (when (and (contains? f :solution) (not= s (:solution f)))
              [:div.correct-answer (some (fn [[_ {:keys [value]} v]] (when (= (:solution f) value) v)) r)])])
         (into [:div.selector-options.popup {:id selector-id}]
               (map
                 (fn [[_ {:keys [value]} v]]
                   [:div.selector-option (merge
                                           (when (:key f)
                                             {:on-click #(contained % (do
                                                                        (swap! move-container assoc key value)
                                                                        (-> (jq "#" selector-id) jq-hide)))})
                                           (when (= s value)
                                             {:class "selected"}))
                    v])
                 r))])))
  )

(defn- coordinates [e] (let [r (.getBoundingClientRect ^js/Element (.-target ^js/Event e))]
                         [(-> ^js/MouseEvent e .-clientX (- (.-left ^js/DOMRect r)))
                          (-> ^js/MouseEvent e .-clientY (- (.-top ^js/DOMRect r)))]))
(def ^:const PENCIL_WIDTH [3 7 11])
(def ^:const PENCIL_COLOUR ["#000000" "#ff0000" "#00ff00" "#0000ff" "#00ffff" "#ff00ff" "#ffff00"])

(defn- build-editable-canvas
  [key parameters content width move-container preview? refresh?]
  (let [redo (reagent/atom nil)
        drawing? (reagent/atom false)
        selected-pencil-width-index (reagent/atom 0)
        selected-pencil-colour-index (reagent/atom 0)
        start-drawing (fn [e key]
                        (when-not (or (not key) (-> ^js/MouseEvent e .-buttons zero?))
                          (reset! drawing? true)
                          (reset! redo nil)
                          (let [[x y] (coordinates e)]
                            (swap! move-container update key conj [(nth PENCIL_WIDTH @selected-pencil-width-index)
                                                                   (nth PENCIL_COLOUR @selected-pencil-colour-index)
                                                                   x y x y]))))
        keep-drawing (fn [e]
                       (when @drawing?
                         (let [[x y] (coordinates e)]
                           (swap! move-container update key #(update % (dec (count %)) conj x y)))))
        stop-drawing (fn [e]
                       (when @drawing?
                         (keep-drawing e)
                         (reset! drawing? false)))]
    (fn [key parameters content width move-container preview? refresh?]
      (when (and key (not (contains? @move-container key)))
        (reset! redo nil)
        (reset! drawing? false)
        (reset! selected-pencil-width-index 0)
        (reset! selected-pencil-colour-index 0)
        (swap! move-container assoc key (or content [])))
      [:div.canvas-editor
       (into [:div.tools]
             (concat
               [[:div.canvas-button (merge
                                      (if (or (not key) (and (-> @move-container (get key) seq not) (-> @redo seq not)))
                                        {:class "disabled"}
                                        {:on-click (fn [_]
                                                     (swap! move-container assoc key [])
                                                     (reset! redo nil))})
                                      (style :border-radius "6px 3px 3px 3px"))
                 [:svg {:width 61 :height 30}
                  [:path {:d "M4,12 Q4,24,16,27 L20,15 L15,11 Q9,15,4,13" :fill :none :stroke "#eeecda" :stroke-width 1.5}]
                  [:line {:x1 11 :y1 13 :x2 19 :y2 19 :fill :none :stroke "#eeecda" :stroke-width 1.5}]
                  [:path {:d "M17.4,11 L24,3 Q27,2,26,5 L19.8,12.6" :fill :none :stroke "#eeecda" :stroke-width 1.5}]
                  [:text {:x 26 :y 25 :font-size 14 :letter-spacing 0.5 :fill "#eeecda" :font-weight 300} "clear"]]]
                [:div.filler (style :width "5px")]
                [:div.canvas-button (if (or (not key) (-> @move-container (get key) seq not))
                                      {:class "disabled"}
                                      {:on-click (fn [_]
                                                   (let [item (-> @move-container (get key) last)]
                                                     (swap! move-container update key (fn [d] (subvec d 0 (dec (count d)))))
                                                     (swap! redo conj item)))})
                 [:svg {:width 30 :height 30}
                  [:path {:d "M25,26 Q23,18,14,18 L14,22 L5,15 L14,8 L14,12 Q25,14,25,26" :fill "#eeecda" :stroke :none}]]]
                [:div.canvas-button (if (or (not key) (-> @redo seq not))
                                      {:class "disabled"}
                                      {:on-click (fn [_]
                                                   (let [item (first @redo)]
                                                     (swap! move-container update key conj item)
                                                     (swap! redo next)))})
                 [:svg {:width 30 :height 30}
                  [:path {:d "M5,26 Q7,18,16,18 L16,22 L25,15 L16,8 L16,12 Q5,14,5,26" :fill "#eeecda" :stroke :none}]]]
                [:div.filler (style :width "30px")]]
               (for [i (range (count PENCIL_WIDTH))]
                 [:div.canvas-button (if (not key)
                                       {:class "disabled"}
                                       (merge
                                         {:on-click (fn [_] (reset! selected-pencil-width-index i))}
                                         (when (= @selected-pencil-width-index i)
                                           {:class "pressed"})))
                  [:svg {:width 30 :height 30}
                   [:circle {:cx 15 :cy 15 :r (* 2 (inc i)) :fill "#eeecda" :stroke :none}]]])
               [[:div.filler (style :width "30px")]]
               (for [i (range (count PENCIL_COLOUR))]
                 [:div.canvas-button (if (not key)
                                       {:class "disabled"}
                                       (merge
                                         {:on-click (fn [_] (reset! selected-pencil-colour-index i))}
                                         (when (= @selected-pencil-colour-index i)
                                           {:class "pressed"})))
                  [:svg {:width 30 :height 30}
                   [:rect {:x 6 :y 6 :width 18 :height 18 :fill (nth PENCIL_COLOUR i) :stroke :none}]]])))
       [:svg.editable (merge (select-keys parameters [:width :height])
                             {:on-mouse-down #(start-drawing % key)
                              :on-mouse-over #(start-drawing % key)
                              :on-mouse-move keep-drawing
                              :on-mouse-up   stop-drawing
                              :on-mouse-out  stop-drawing
                              :style         {:cursor (if key :crosshair :default)}})
        (into [:g.paths]
              (for [[w c x y & others] (or (get @move-container key) content)]
                [:path {:d (str "M" x "," y " L" (clojure.string/join "," others)) :stroke-width w :stroke c}]))]]))
  )

(defn- build-canvas
  [[_ & [f & r]] width move-container preview? refresh?]
  [:div.canvas.worksheet-block (when (:tooltip f) {:class "tooltipped"})
   (build-tooltip f width move-container preview? refresh?)
   (if-let [key (:key f)]
     [build-editable-canvas key f (first r) width move-container preview? refresh?]
     (into [:svg (select-keys f [:width :height])] r))]
  )

(defn- build-row
  [[n & [f & r :as c]] width columns move-container preview? refresh?]
  (let [[_ flags] (tag-and-flags n)
        columns (if (zero? columns) 1 columns)
        cell-width (- (/ (- width (inc columns)) columns) 10)]
    (into [:div.row (merge (apply style (when (map? f) (mapcat element-property f)))
                           (when-let [classes (seq (concat
                                                     (when (:tooltip f) ["tooltipped"])
                                                     (when (:header flags) ["header"])))]
                             {:class (clojure.string/join " " classes)}))
           (build-tooltip f width move-container preview? refresh?)]
          (map
            (fn [element]
              [:div.cell
               (or (common-content element)
                   [(content (-> element first tag-and-flags first) build-error) element cell-width move-container preview? refresh?])])
            (if (map? f) r c))))
  )

(defn- build-table
  [[n & [f & r :as c]] width move-container preview? refresh?]
  (let [[_ flags] (tag-and-flags n)
        rows (if (map? f) r c)
        scale (:scale f 0.75)
        block (:block f 300)
        real-width (-> scale (* width) (max block) (min width))
        columns (apply max (map (fn [[_ & [f & r :as c]]] (count (if (map? f) r c))) rows))]
    [:div.table.worksheet-block (merge (apply style (concat
                                                      (when (map? f) (mapcat element-property f))
                                                      (when-let [position (-> f :position #{:left :right})]
                                                        [:float position])
                                                      [:width (px real-width)]))
                                       (when (:tooltip f) {:class "tooltipped"}))
     (build-tooltip f width move-container preview? refresh?)
     (into [:div.table-content (merge (apply style (when (map? f) (mapcat element-property f)))
                                      (when (:borderless flags) {:class "borderless"}))]
           (map
             (fn [row]
               [build-row row (- real-width 2) columns move-container preview? refresh?])
             rows))
     (when-let [title (:title f)]
       [:div.table-title
        [build-extras title real-width move-container preview? refresh?]])])
  )

(defn- build-list-item
  [[_ & [f & r :as c]] width move-container preview? refresh?]
  [:li (into [:div.item (merge (apply style (when (map? f) (mapcat element-property f)))
                               (when (:tooltip f) {:class "tooltipped"}))
              (build-tooltip f width move-container preview? refresh?)]
             (map
               (fn [element]
                 (or (common-content element)
                     [(content (-> element first tag-and-flags first) build-error) element width move-container preview? refresh?]))
               (if (map? f) r c)))]
  )

(defn- build-list
  [[_ & [f & r :as c]] width move-container preview? refresh?] ;; TODO: make sure the lists are sized correctly
  [:div.list.worksheet-block (merge (apply style (when (map? f) (mapcat element-property f)))
                                    (when (:tooltip f) {:class "tooltipped"}))
   (build-tooltip f width move-container preview? refresh?)
   (let [list-type (:type f :bulleted)
         list-start (when (= list-type :numbered) (or (:start f) 1))]
     (into [(if (= list-type :numbered) :ol :ul) (when list-start {:start list-start})]
           (map (fn [i] [build-list-item i width move-container preview? refresh?]) (if (map? f) r c))))]
  )

(defn- build-definition
  [[_ & [f & r :as c]] width move-container preview? refresh?]
  (let [title (:title f)
        content-width (- width 30)]
    [:div.definition (merge (apply style (mapcat element-property f))
                            (when (:tooltip f) {:class "tooltipped"}))
     (build-tooltip f width move-container preview? refresh?)
     [:div.definition-title
      (or (common-content title)
          [(content (-> title first tag-and-flags first) build-error) title content-width move-container preview? refresh?])]
     (into [:div.definition-content]
           (map
             (fn [element]
               (or (common-content element)
                   [(content (-> element first tag-and-flags first) build-error) element content-width move-container preview? refresh?]))
             r))])
  )

(defn- build-definitions
  [[_ & [f & r :as c]] width move-container preview? refresh?]
  (let [real-width (- width 10)
        level (:level f 0)]
    (into [:div.definitions.worksheet-block (merge (apply style (concat
                                                                  (when (map? f) (mapcat element-property f))
                                                                  [:width (px width)]))
                                                   (when-let [classes (seq (concat
                                                                             (when (:tooltip f) ["tooltipped"])
                                                                             (when (pos? level) [(str "level-" level)])))]
                                                     {:class (clojure.string/join " " classes)}))
           (build-tooltip f real-width move-container preview? refresh?)]
          (map (fn [i] [build-definition i real-width move-container preview? refresh?]) (if (map? f) r c))))
  )

(defn- build-citation
  [[_ & [f & r :as c]] width move-container preview? refresh?]
  [:div.citation (when (:tooltip f) {:class "tooltipped"})
   (build-tooltip f width move-container preview? refresh?)
   (into [:div.citation-content (apply style (when (map? f) (mapcat element-property f)))]
         (map
           (fn [element]
             (or (common-content element)
                 [(content (-> element first tag-and-flags first) build-error) element width move-container preview? refresh?]))
           (if (map? f) r c)))
   (when-let [reference (:reference f)]
     [:div.citation-reference [build-extras reference width move-container preview? refresh?]])]
  )

(defn- build-link
  [[_ {:keys [address] :as f} & r] width move-container preview? refresh?]
  [:div.link (merge (apply style (when (map? f) (mapcat element-property f)))
                    (when (:tooltip f) {:class "tooltipped"}))
   (build-tooltip f width move-container preview? refresh?)
   (into [:div.linked-reference.external {:on-click #(do (.open js/window address) false)}]
         (map
           (fn [element]
             (or (common-content element)
                 [(content (-> element first tag-and-flags first) build-error) element width move-container preview? refresh?]))
           r))]
  )

(defn- build-reference
  [[_ & [f & [r]]] width move-container preview? refresh?]
  (let [key (if (map? f) r f)]
    [:div.reference {:on-click (fn [_]
                                 (-> "html,body" jq (jq-animate #js {:scrollTop (-> (jq "#ref-" key) jq-parent jq-offset (js->clj :keywordize-keys true) :top)} 200))
                                 (-> (jq "#ref-" key)
                                     (jq-animate #js {:background-color "#e8edb4"} 200)
                                     (jq-animate #js {:background-color "transparent"} 1000)))}
     key])
  )

(defn- build-error
  [c width move-container preview? refresh?]
  [:div.error.worksheet-block (str c)]
  )

(defn render-worksheet-content
  [[_ & [f & r :as c]] width flag move-container preview? refresh?]
  (into [:div.worksheet-content (merge
                                  (apply style (concat
                                                 (when (map? f) (mapcat element-property f))
                                                 (when flag [:min-height "200px"])))
                                  (when (:tooltip f) {:class "tooltipped"}))
         (build-tooltip f width move-container preview? refresh?)
         (when-not preview?
           (when-let [{:keys [path position]} (session/get :preview)]
             [build-preview path position]))
         flag]
        (concat
          (map
            (fn [element]
              (or (common-content element)
                  [(content (-> element first tag-and-flags first) build-error) element width move-container preview? refresh?]))
            (if (map? f) r c))
          (when-let [references (:references f)]
            [(into [:div.reference-list "References:"]
                   (map
                     (fn [[k v]]
                       [:div.reference-entry {:id (str "ref-" k)}
                        [:div.reference-key k]
                        [:div.reference-text [build-extras v width move-container preview? refresh?]]])
                     references))])))
  )

(defonce content {:text        build-text
                  :space       build-space
                  :paragraph   build-paragraph
                  :options     build-options
                  :table       build-table
                  :selector    build-selector
                  :list        build-list
                  :definitions build-definitions
                  :image       build-image
                  :audio       build-audio
                  :reference   build-reference
                  :citation    build-citation
                  :link        build-link
                  :code        build-code
                  :math        build-math
                  :canvas      build-canvas})

;; TODO: make sure this code is fully compatible with the worksheet Spec

(defn build-worksheet
  [width concept-id on-move]
  (let [current-concept-id (atom concept-id)
        worksheet (reagent/atom nil)
        refresh? (reagent/atom (random-uuid))
        move-container (reagent/atom nil)
        loading? (reagent/atom false)
        next-move-fn #(go
                        (session/remove! :preview)
                        (let [current-template-id (:template @worksheet)]
                          (when-let [id @current-concept-id]
                            (let [reset? (contains? @worksheet :flag)]
                              (reset! worksheet
                                      (:body (<! (http/post "/move/next"
                                                            {:edn-params (merge
                                                                           {:concept-id id
                                                                            :reset?     reset?}
                                                                           (when (and % (not reset?))
                                                                             {:move (into {} %)}))}))))))
                          (reset! move-container {})
                          (reset! refresh? (random-uuid))
                          (reset! loading? false)
                          (when (not= current-template-id (:template @worksheet))
                            (scroll-to "app" -44))
                          (%2)))]
    (when concept-id (next-move-fn nil on-move))
    (fn [width concept-id on-move]
      (when (not= concept-id @current-concept-id)
        (reset! current-concept-id concept-id)
        (next-move-fn nil on-move))
      (let [{:keys [content flag]} @worksheet
            submit #(do
                      (reset! loading? true)
                      (next-move-fn % on-move))
            flag-cover (when-let [{:keys [id time]} flag]
                         [:div.flag-cover
                          [:div.flag-cover-content
                           (let [t (time-core/to-default-time-zone (timec/from-long time))]
                             [:div.flag-cover-message
                              "You flagged this worksheet as " [:u "out of date"]
                              [:strong (str " at " (timef/unparse (timef/formatter "HH:mm") t)
                                            " on " (timef/unparse (timef/formatter "MMMM d, yyyy") t))]
                              "."
                              [:br] [:br]
                              "We are working hard to update it as soon as possible."])
                           [:div.flag-cover-remove.button {:on-click #(contained % (go
                                                                                     (<! (http/post "/move/remove-flag" {:edn-params {:flag id}}))
                                                                                     (swap! worksheet dissoc :flag)))}
                            [:div.flag-cover-remove-title "Remove flag"]
                            [:svg {:width (px 30) :height (px 30) :view-box "0 0 20 20"}
                             [:rect {:x (px 4) :y (px 2) :width (px 3) :height (px 16) :rx (px 2) :ry (px 2)}]
                             [:path {:d "M8,3 L18,3 L15,6.5 L18,10 L8,10 z"}]
                             [:line {:x1 (px 18) :y1 (px 2) :x2 (px 2) :y2 (px 18) :stroke defs/incorrect-colour :stroke-width (px 2)}]]]]])
            worksheet-width (- width 10)]
        (when content
          [:div.worksheet (style :width (px worksheet-width) :padding-left (px 5) :padding-right (px 5))
           [render-worksheet-content (or (:content flag) content) worksheet-width flag-cover move-container false @refresh?]
           (when-not (or flag @loading? (session/get :overlay))
             [build-flag #(go
                            (session/remove! :preview)
                            (<! (http/post "/move/flag" {:edn-params {:concept-id @current-concept-id}}))
                            (submit nil))])
           (when-not (session/get :overlay)
             [:div.next-arrow
              (into [:svg (merge
                            {:view-box "0 0 100 100"}
                            (if @loading?
                              (style :pointer-events :none)
                              {:on-click #(contained % (do
                                                         (session/remove! :preview)
                                                         (session/remove! :overlay)
                                                         (submit @move-container)))}))]
                    (if @loading?
                      (into [[:circle.main.disabled {:cx (px 50) :cy (px 50) :r (px 45)}]]
                            (map
                              (fn [i l]
                                [:circle.loading {:cx (px (+ 50 (* (dec i) 15))) :cy (px 50) :r (px 5)}
                                 [:animate {:attributeType :XML :attributeName :r :values "5;2;5" :dur (str l "s") :repeatCount :indefinite}]])
                              (range)
                              [3 2 2.5]))
                      [[:circle.main {:cx (px 50) :cy (px 50) :r (px 45)}]
                       [:polyline {:points "42,27 65,50 42,73"}]]))])
           (when (-> :help-step session/get (= 2))
             [:div (merge
                     {:on-click contain-event}
                     (style :position :absolute :top (px 0) :right (px 0) :bottom (px 0) :left (px 0) :background-color (alpha+ white-colour -0.1) :z-index 6000))
              [:div.help (style :position :absolute :top (pct 50) :left (pct 50) :transform (translate (pct -50) (pct -50)))
               [:div.help-close-all.linked-reference {:on-click #(contained % (do
                                                                                (session/put! :help-step 4)
                                                                                (cookies/set! :help-step 4 {:path "/" :max-age (* 60 60 24 365)})))}
                "close help"]
               [:div.help-title
                "Help 2 of 3"]
               [:div.help-content
                [:div.strong "No curriculum."] [:br]
                [:div.strong "No fixed time commitment."] [:br]
                [:br] "Use this " [:em [:div.strong "interactive worksheet"]] " to learn new things."
                [:div.help-extra
                 "You are welcome to just play around with it - kuhut is a safe, supportive and effective way of exploring and expanding your knowledge." [:br] [:br]
                 [:div.linked-reference {:on-click #(accountant/navigate! (path-for :principles))} "Learn more"]]]
               [:div.help-prev.button {:on-click #(contained % (do
                                                                 (session/put! :help-step 1)
                                                                 (cookies/set! :help-step 1 {:path "/" :max-age (* 60 60 24 365)})))}
                "previous"]
               [:div.help-next.button {:on-click #(contained % (do
                                                                 (session/put! :help-step 3)
                                                                 (cookies/set! :help-step 3 {:path "/" :max-age (* 60 60 24 365)})))}
                "next"]]])]))))
  )
