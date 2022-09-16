(ns kuhut.css.classes
  (:require [garden.def :refer [defstyles]]
            [garden.selectors :as gs]
            [kuhut.shared.definitions :refer [primary-font-family flex px em pct vh hsl white-colour shadow border alpha+ brightness+ saturation+ translate
                                              leading-colour-0 leading-colour-1 leading-colour-2 leading-colour-3 black-colour incorrect-colour correct-colour
                                              code-font-family greyscale]])
  )

(defstyles
  kuhut
  [:body {:font-family primary-font-family :margin :auto :-webkit-text-size-adjust (pct 100) :tab-size 4}
   [:#MathJax_Message {:visibility :hidden}]
   [:.kuhut-icon
    [:path {:stroke leading-colour-0 :fill :none :stroke-width (px 4)}]
    [:circle {:stroke :none}]
    [:.motivation {:fill leading-colour-1}]
    [:.assessment {:fill leading-colour-3}]
    [:.education {:fill leading-colour-2}]]
   [:.linked-reference {:display :inline :white-space :pre-line :color (alpha+ leading-colour-1 -0.1) :cursor :pointer}]
   [:.linked-reference.external {:text-decoration :underline}]
   [:.linked-reference:hover {:opacity 0.8}]
   [:.popup {:display :none}]
   [:.no-user-select {:user-select :none :-webkit-user-select :none :-moz-user-select :none :-ms-user-select :none}]
   [:.text-input {:position :relative :background-color (alpha+ black-colour -0.98) :border (border 2 :solid (greyscale 230)) :border-radius (px 20) :display :inline-block :box-sizing :border-box :cursor :text}
    [:.text-input-title {:position :absolute :font-weight 300 :color (alpha+ black-colour -0.75) :box-sizing :border-box}]
    [:.text-input-title.smaller {:color (alpha+ black-colour -0.4) :font-weight 400 :box-sizing :border-box}]
    [:.text-input-field {:border :none :background :transparent :outline :none :width (pct 100) :padding (px 1) :box-sizing :border-box :margin (px 0) :font-family primary-font-family :color black-colour :border-radius (px 3)}]
    [:.text-input-field.error {:background-color (alpha+ incorrect-colour -0.95)}]]
   [:.text-input.focused {:border (border 2 :solid (alpha+ leading-colour-0 -0.3))}]
   [:.button {:background-color (brightness+ (saturation+ leading-colour-2 -0.4) -0.1) :color white-colour :border-radius (px 5) :display :inline-block :font-weight 300 :box-shadow (shadow 0 0 1 (alpha+ black-colour -0.5)) :border (border 1 :solid (alpha+ white-colour -0.8)) :cursor :pointer :padding (px 6 16) :font-size (px 16)}]
   [:.button:hover {:opacity 0.9}]
   [:.status-message {:border-radius (px 5) :padding (px 3 5 4 5) :border (border 1 :solid :transparent) :display :block :margin (px 5) :clear :both}]
   [:.status-message.error {:background-color (alpha+ incorrect-colour -0.8) :color incorrect-colour}]
   [:.status-message.warn {:background-color (alpha+ leading-colour-1 -0.8) :color leading-colour-1}]
   [:.status-message.ok {:background-color (alpha+ correct-colour -0.8) :color correct-colour}]
   [:.hint {:position         :absolute :white-space :pre-line :overflow :hidden :width (px 250)
            :color            (alpha+ black-colour -0.2) :font-weight 300 :line-height (px 14) :font-size (px 14)
            :background-color (saturation+ (brightness+ leading-colour-1 0.8) -0.7) :visibility :hidden :text-align :left
            :border           (border 1 :solid (saturation+ (brightness+ leading-colour-1 0.8) -0.5)) :padding (px 5) :border-radius (px 7)}
    [:.linked-reference {:font-weight 500}]]
   [:.concept-bookmark {:width     (px 20) :height (px 30) :cursor :pointer :position :relative
                        :font-size (px 14) :opacity 0.9}
    [:.hint {:right (px 10) :top (px 0)}]
    [:path {:fill (alpha+ leading-colour-0 -0.9) :stroke (alpha+ leading-colour-0 -0.85)}]
    [:polygon {:fill (alpha+ leading-colour-0 -0.7) :stroke :none}]]
   [:.concept-bookmark.selected {:opacity 1.0}
    [:path {:fill (alpha+ leading-colour-0 -0.3) :stroke :none}]
    [:polygon {:fill white-colour :stroke :none}]]
   [:.concept-bookmark:hover {:opacity 1}]
   [:.concept-bookmark.selected:hover {:opacity 0.8}]
   [:.checkbox {:display :inline-block :cursor :pointer :opacity 0.8 :line-height (px 4) :position :relative}
    [:svg {:margin-right (px 3) :display :inline-block :position :absolute
           :fill         :none :stroke (greyscale 150) :stroke-width (px 10)}]
    [:.checkbox-title {:display :inline-block :color (greyscale 100)}]]
   [:.checkbox:hover {:opacity 1.0}]
   [:.level
    [:svg
     [:circle {:stroke-width (px 1) :fill :none :stroke (alpha+ black-colour -0.95)}]
     [:path {:fill :none :stroke-width (px 2) :stroke-linecap :round}]
     [:text.title {:letter-spacing (px 0.25) :font-size (px 3.5) :text-anchor :middle}]
     [:text.value {:font-size (px 14) :text-anchor :middle}]]]
   [:.level.focus
    [:svg
     [:circle {:fill :none :stroke (brightness+ black-colour -0.95)}]
     [:path {:stroke leading-colour-0}]
     [:text {:fill leading-colour-0}]]]
   [:.level.done
    [:svg
     [:circle {:fill (hsl 0 75 60)}]
     [:text {:fill white-colour}]]]
   [:.level.done.focus
    [:svg
     [:circle {:fill leading-colour-0 :stroke leading-colour-0}]]]
   [:.level.titled
    [:svg
     [:text.value {:font-size (px 8)}]]]
   [:.concept-preview {:border        (border 1 :solid (alpha+ black-colour -0.95)) :padding (px 0 5 5 5) :border-radius (px 5)
                       :color         (greyscale 100) :text-align :left :cursor :pointer :background-color (alpha+ white-colour -0.4)
                       :margin-bottom (px 5)}
    [:.concept-preview-title {:display :flex :flex-direction :row :flex-wrap :nowrap}
     [:.concept-preview-name {:flex (flex 1 1 60) :font-size (px 20) :font-weight 400 :padding-top (px 5)}
      [:.concept-preview-tags {:color (alpha+ (saturation+ leading-colour-1 -0.3) -0.1) :vertical-align :super :font-size (px 11) :font-weight 300 :line-height (px 12) :font-style :italic}]]
     [:.concept-preview-cost {:flex (flex 0 0 40) :font-size (px 12) :padding-top (px 5)}
      [:.concept-preview-cost-free {:background-color (alpha+ correct-colour -0.1) :border-radius (px 3) :padding (px 1 5) :display :inline-block :color white-colour}]]
     [:.concept-bookmark {:flex (flex 0 0 20)}]]
    [:.concept-preview-content {:display :flex :flex-direction :row :flex-wrap :nowrap}
     [:.concept-preview-content-description {:margin-top (px 7) :flex (flex 1 1 100) :font-weight 300 :font-size (px 12)}]
     [:.concept-preview-content-level {:flex (flex 0 0 70) :display :inline-flex :align-items :center :justify-content :center}]]]
   [:.concept-preview:hover {:background-color (alpha+ white-colour -0.8)}]
   [:.interest-preview {:position :relative :display :inline-block :padding (px 0 2 2 2) :box-shadow (shadow 0 0 3 (alpha+ black-colour -0.9)) :border-radius (px 5) :width (px 100) :cursor :pointer :background-color (alpha+ white-colour -0.4) :vertical-align :top :margin (px 10 5 5 10)}
    [:.interest-preview-level]
    [:.interest-preview-name {:text-align :center :font-weight 500 :font-size (px 14) :padding (px 2)}]
    [:.concept-bookmark {:position :absolute :top (px 0) :right (px 1)}]]
   [:.interest-preview:hover {:background-color (alpha+ white-colour -0.8)}]
   [:.search-view {:position :fixed :right (px 0) :left (px 0) :background-color white-colour :color black-colour
                   :overflow :auto :display :flex :flex-direction :column :flex-wrap :nowrap :box-sizing :border-box}
    [:.search-input {:position         :relative :flex (flex 0 0 44) :display :flex :flex-direction :row :flex-wrap :nowrap :box-sizing :border-box
                     :background-color (alpha+ leading-colour-3 -0.96)}
     [:.logo {:flex (flex 0 1 38) :position :relative}
      [:.kuhut-icon {:position :absolute :top (px 4) :left (px 0)}]]
     [:.input {:flex (flex 1 1 100) :position :relative}
      [:.clear-button {:position :absolute :top (px 8) :right (px 2) :width (px 30) :height (px 30) :padding (px 7) :box-sizing :border-box}]
      [:input {:box-sizing :border-box :width (pct 100) :border-radius (px 20) :height (px 36) :font-size (px 20) :margin (px 5 0 3 0) :padding (px 0 30 0 10) :font-family primary-font-family :outline :none :border (border 2 :solid (alpha+ black-colour -0.95)) :background-color white-colour :color (alpha+ black-colour -0.3) :font-weight 300 :-webkit-appearance :none :-moz-appearance :none :appearance :none}]
      ["input::placeholder" {:color (alpha+ (saturation+ leading-colour-3 -0.5) -0.6) :font-weight 300 :font-size (px 16) :padding-top (px 2)}]
      ["input::-ms-input-placeholder" {:color (alpha+ (saturation+ leading-colour-3 -0.5) -0.6) :font-weight 300 :font-size (px 16) :padding-top (px 2)}]]
     [:.hide-search {:font-size (px 14) :box-sizing :border-box :text-align :center}]]
    [:.search-results {:overflow :auto :-webkit-overflow-scrolling :touch :flex (flex 1 1 100) :padding (px 5) :box-sizing :border-box}]]
   [:.help {:z-index 6000 :box-sizing :border-box :background-color (alpha+ (greyscale 230) -0.1) :border-radius (px 5) :border (border 1 :dotted (alpha+ leading-colour-3 -0.5)) :font-weight 400 :text-align :center}
    [:.help-close-all {:position :absolute :top (px 2) :right (px 5) :font-size (px 14) :font-weight 400}]
    [:.help-title {:padding (px 6 10 10 10) :box-sizing :border-box :font-weight 500 :background-color leading-colour-3 :display :inline-block :position :absolute :top (px 3) :left (px 3) :border-radius (px 2) :color white-colour :border (border 1 :dotted (alpha+ leading-colour-3 -0.5))}]
    [:.help-content {:border (border 2 :dashed (alpha+ black-colour -0.85)) :background-color white-colour :padding (px 5) :color (greyscale 100)}
     [:.strong {:font-weight 500 :display :inline}]
     [:.help-extra {:color (alpha+ black-colour -0.3) :margin-top (px 10) :font-weight 300}]]
    [:.help-prev {:float :left :padding-right (px 10) :border-radius (px 20 5 5 20) :box-sizing :border-box}]
    [:.help-next {:float :right :padding-left (px 10) :border-radius (px 5 20 20 5) :box-sizing :border-box}]]

   [:.page-chrome {:margin :auto :width (pct 100) :position :relative}
    [:.page-header-background {:position :fixed :top (px 0) :right (px 0) :left (px 0) :height (px 44) :background-color white-colour :z-index 5000}
     [:.page-header {:position :absolute :box-shadow (shadow 0 1 1 (alpha+ leading-colour-3 -0.85)) :background-color (alpha+ leading-colour-3 -0.96)
                     :top      (px 0) :right (px 0) :left (px 0) :height (px 44)}
      [:.page-header-content {:margin :auto :max-width (px 1010) :position :relative}
       [:.page-header-icon {:height (px 38) :cursor :pointer}]
       [:.page-header-control {:position :absolute :top (px 0) :right (px 5) :height (px 40) :cursor :pointer :display :flex :flex-direction :row :flex-wrap :nowrap :text-align :right :box-sizing :border-box :padding-top (px 5)}
        [:.page-header-name {:font-size (px 20) :font-weight 300 :color leading-colour-3 :flex (flex 1 1 50) :height (px 44) :box-sizing :border-box :position :absolute :top (px 0) :right (px 34) :padding (px 10 6)}]
        [:.page-header-name:hover {:background-color (alpha+ leading-colour-3 -0.8)}]
        [:svg.menu {:flex (flex 0 0 36) :border-radius (px 5 5 0 0)}
         [:path {:stroke-width (px 4) :stroke leading-colour-3 :fill :none}]]]
       [:.page-header-control.selected
        [:svg.menu
         [:path {:stroke-width (px 2)}]]]]
      [:.page-subheader {:z-index 5000 :cursor :pointer}
       [:.welcome-message {:font-weight 300}]
       [:.focus-panel {:position :relative}
        [:.search-icon {:position :absolute :left (px 7)}]
        [:.focus-title {:padding (px 10 2) :text-align :center :font-weight 500 :color leading-colour-0 :width (pct 100)}]
        [:.level {:position :absolute :top (px 0) :right (px 26)}]
        [:.concept-bookmark {:position :absolute :top (px 0) :right (px 5)}]]
       [:.focus-panel.locked
        [:.search-icon {:padding-top (px 4) :left (px 6)}]]]
      [:.page-subheader:hover
       [:.focus-panel
        [:.search-icon {:opacity 0.9}]
        [:.focus-title {:opacity 0.9}]
        [:.level {:opacity 0.9}]
        [:.concept-bookmark {:opacity 0.9}]]]
      [:.toolbar {:position :fixed :right (px 0) :text-align :left :cursor :default :box-sizing :border-box :color black-colour :background-color white-colour
                  :overflow :auto :display :flex :flex-direction :column :flex-wrap :nowrap}
       [:.toolbar-help {:font-weight 300 :font-size (px 16) :padding (px 10) :color (alpha+ black-colour -0.2) :background-color (alpha+ black-colour -0.9) :border-radius (px 5) :margin (px 10)}
        [:.strong {:font-weight 500 :margin (px 10 5) :color black-colour}]]
       [:.control-button {:min-height (px 60) :margin (px 0 20) :font-size (px 24) :font-weight 200 :padding-top (px 16) :box-sizing :border-box :border-bottom (border 1 :solid (alpha+ black-colour -0.95)) :position :relative :cursor :pointer}
        [:.control-button-title {:color (saturation+ leading-colour-3 -0.7)}]
        [:.control-button-subtitle {:font-size (px 12) :font-weight 300 :margin (px 3 0 0 2) :color (alpha+ leading-colour-1 -0.1)}]
        [:.control-button-arrow {:position :absolute :top (px 10) :right (px 0)}]]
       [:.control-button:hover {:opacity 0.8 :text-shadow (shadow 0 0 10 (alpha+ black-colour -0.8))}]
       [:.control-area {:margin (px 20) :font-size (px 18) :font-weight 300 :color (saturation+ leading-colour-3 -0.5)}
        [:.strong {:color (brightness+ leading-colour-1 -0.2) :display :inline-block}]]
       [:.control-area.welcome {:font-size (px 32) :height (px 70) :font-weight 300 :color (saturation+ leading-colour-3 -0.2)}]
       [:.toolbar-title {:flex          (flex 0 0 37) :font-size (px 14) :color (alpha+ black-colour -0.7) :font-weight 600 :text-align :center
                         :border-bottom (border 1 :solid (alpha+ black-colour -0.95)) :background-color (alpha+ black-colour -0.95) :padding-top (px 9) :box-sizing :border-box}
        [:.toolbar-button {:position :absolute :top (px 5) :background-color (saturation+ leading-colour-2 -0.5) :padding (px 3)}]
        [:.back {:left (px 3)}]
        [:.forth {:right (px 3)}]]
       [:.toolbar-content {:overflow :auto :-webkit-overflow-scrolling :touch :flex (flex 1 1 100) :padding (px 10) :text-align :center}
        [:.text-input {:margin-top (px 5) :width (pct 100)}]
        [:.button {:margin (px 10 0)}]
        [:.keep-logged-in {:width (pct 100) :text-align :left :margin (px 10)}
         [:svg
          [:path {:stroke-width (px 10)}]]]
        [:.forgot-password {:text-align :left :width (pct 100)}]
        [:.e-mail-reminder {:text-align :left :width (pct 100)}]
        [:.g-recaptcha {:margin-top (px 10)}]
        [:.progress {:display :flex :flex-direction :row :flex-wrap :nowrap :text-align :left}
         [:.keep-progress {:flex (flex 1 1 (pct 50)) :padding (px 10)}]
         [:.fresh-account {:flex (flex 1 1 (pct 50)) :padding (px 10)}]]
        [:.toolbar-area {:margin (px 10 5 10 5)}
         [:.toolbar-area-title {:font-size (px 22) :text-align :left :font-weight 300 :color (alpha+ black-colour -0.5) :margin-left (px 5)}]
         [:.toolbar-area-content]]
        [:.checkbox
         [:svg {:stroke (greyscale 100)}]
         [:.checkbox-title {:color (alpha+ black-colour -0.1)}]]]]]]
    [:.page-content {:margin :auto :max-width (px 1010) :margin-top (px 44) :position :relative}
     [:.linked-reference.membership {:font-size (px 20) :font-weight 300}]
     [:.home-page {:position :relative}
      [:.signup-banner {:position :absolute :width (pct 90) :background-color (-> leading-colour-2 (brightness+ 0.8) (saturation+ -0.75) (alpha+ -0.1)) :padding (px 10) :box-sizing :border-box :border (border 1 :solid (alpha+ leading-colour-3 -0.8)) :z-index 999 :box-shadow (shadow 0 0 5 (alpha+ leading-colour-2 -0.6)) :border-radius (px 5) :margin-top (px 5) :margin-left (pct 5)}]]
     [:.profile-page {:position :relative :padding (px 10 10)}
      [:.profile-area {:margin (px 20 0 30)}
       [:.profile-area-title {:font-size (px 12) :color white-colour :text-align :center :background-color (alpha+ leading-colour-3 -0.2) :padding (px 2) :font-weight 300 :border-radius (px 5 5 0 0)}]
       [:.profile-area-content {:padding (px 10 5) :background-color (alpha+ leading-colour-3 -0.95) :box-sizing :border-box :border-radius (px 0 0 5 5)}
        [:.profile-interest-search-more {:text-align :center :margin (px 15 0 5 0) :display :block}]
        [:.search-view {:position :relative :padding (px 0 0 10 0) :top :initial :right :initial :bottom :initial :left :initial :border-radius (px 10)}
         [:.search-results {:min-height (px 300)}]]]]]
     [:.policy-page
      [:.policy-group {:text-align :left :margin (px 40 5 0 5)}
       [:.policy-title {:font-weight 500}]
       [:.policy-content {:margin-top (px 10) :font-weight 300 :color (alpha+ black-colour -0.3)}]]
      [:.linked-reference.membership {:text-align :center :margin-top (px 50) :display :block}]]
     [:.principles-page
      [:.principle
       [:.principle-content
        [:.darker {:display :inline}]
        [:.principle-title {:color (greyscale 100)}]
        [:.principle-message {:text-align :left :color (greyscale 80)}]]
       [:.principle-image
        [:img {:width (pct 100)}]]]
      [:.principle.left
       [:.principle-content]]
      [:.principle.right]]
     [:#more {:color (greyscale 100) :width (pct 90) :display :inline-block}]]
    [:.page-footer {:z-index 7000}
     [:.linked-reference.page {:color (greyscale 180) :border-left (border 1 :solid (greyscale 230)) :box-sizing :border-box}]
     [:.footer-cookie-message {:color (alpha+ black-colour -0.1) :font-size (px 14) :position :fixed :bottom (px 0) :left (px 0) :padding (px 10) :background-color (saturation+ (brightness+ leading-colour-1 0.3) -0.7) :line-height (px 20)}
      [:.button {:margin (px 10)}]]]




























    (let [input-field-bottom-border (border 2 :solid (alpha+ leading-colour-2 -0.6))
          input-field-background (alpha+ (brightness+ leading-colour-2 0.5) -0.97)
          correct-border-colour (brightness+ correct-colour 0.1)
          incorrect-border-colour (brightness+ incorrect-colour 0.1)
          extras-paragraph [:.paragraph {:padding (px 0) :margin-bottom (px 0) :display :inline}]]
      [:.worksheet {:display :inline-block :font-weight 300 :position :relative}
       [:.tooltip {:position :absolute :left (px -5) :bottom (pct 102) :max-width (px 400) :min-width (px 100) :opacity 0.25 :display :block :z-index 100}
        [:.tooltip-content {:background-color (saturation+ (brightness+ leading-colour-1 0.8) -0.7) :border-radius (px 5) :font-size (px 16) :padding (px 7 10 5 10)
                            :border           (border 2 :solid (saturation+ (brightness+ leading-colour-1 0.8) -0.5)) :line-height (px 20)
                            :color            leading-colour-3 :display :block :text-decoration :none :text-transform :none}
         extras-paragraph]
        [:.tooltip-tip {:border-top   (border 15 :solid (saturation+ (brightness+ leading-colour-1 0.8) -0.5))
                        :border-right (border 10 :solid :transparent) :width (px 0) :height (px 0) :content " " :left (px 10) :position :absolute}]]
       [(gs/> :.tooltipped:hover :.tooltip) {:opacity 1.0}]
       [:.worksheet-content {:color leading-colour-3 :position :relative :display :inline-block}
        [:.preview {:position   :fixed :z-index 10000 :background-color white-colour :font-family primary-font-family
                    :box-shadow (shadow 0 0 10 (alpha+ black-colour -0.7)) :border-radius (px 5)
                    :border     (border 5 :solid (alpha+ leading-colour-3 -0.9)) :box-sizing :border-box
                    :text-align :center :font-style :normal :font-weight 300
                    :display    :flex :flex-direction :column :flex-wrap :nowrap}
         [:.close-preview-button {:position :fixed :top (px 10) :right (px 10) :opacity 0.7 :cursor :pointer :box-shadow (shadow 0 0 5 (alpha+ black-colour -0.6)) :background-color (alpha+ black-colour -0.9) :padding (px 5) :border-radius (px 5)}
          [:svg {:width (px 30) :height (px 30) :display :block}
           [:path {:fill :none :stroke (alpha+ black-colour -0.2) :stroke-width (px 1.5) :stroke-linecap :round :stroke-linejoin :round}]]]
         [:.close-preview-button:hover {:opacity 1}]
         [:.loading {:text-align :left :font-size (px 20) :margin-left (px 20)}]
         [:.preview-navigation {:flex (flex 1 0 :auto) :text-align :left :font-size (px 18) :color (greyscale 200) :padding (px 5 10 0 10) :box-sizing :border-box :background-color white-colour}]
         [:.worksheet {:border :none :margin (px 5) :-webkit-overflow-scrolling :touch}]]
        [:.flag-cover {:position         :absolute :top (px 0) :right (px 0) :bottom (px 0) :left (px 0) :z-index 2000
                       :background-color (alpha+ white-colour -0.1) :padding (px 20) :box-sizing :border-box :cursor :pointer}
         [:.flag-cover-content {:position         :absolute :top (px 50) :left (pct 50) :transform (translate (pct -50) (px 0)) :min-width (px 250)
                                :background-color (alpha+ white-colour -0.2) :padding (px 10) :box-sizing :border-box
                                :box-shadow       (shadow 0 0 50 white-colour) :font-size (px 20) :text-align :center
                                :border-radius    (px 5) :border (border 1 :solid (alpha+ black-colour -0.9))}
          [:.flag-cover-message {:font-size (px 16) :color (alpha+ black-colour -0.5)}]
          [:.flag-cover-remove {:border-radius    (px 5) :border (border 1 :solid (alpha+ leading-colour-3 -0.6))
                                :background-color (alpha+ leading-colour-3 -0.9) :padding (px 3 5) :margin (px 30 5 10 5)
                                :height           (px 32) :display :inline-flex :flex-direction :row :flex-wrap :nowrap}
           [:.flag-cover-remove-title {:font-size (px 18) :line-height (px 30) :margin-right (px 2)
                                       :flex      (flex 1 1 120) :color leading-colour-3}]
           [:svg {:flex (flex 0 0 30)}]]]]
        [:.flag-cover:hover {:background-color (alpha+ white-colour -0.9)}
         [:.flag-cover-content {:background-color (alpha+ white-colour -0.1)}]]
        [:.common {:display :inline :text-decoration :none}]
        [:.space.vertical {:display :inline-block}]
        [:.space.horizontal {:display :block :clear :both}]
        [:.paragraph {:display :block :position :relative :width (pct 100) :box-sizing :border-box :font-style :normal}]
        [:.text {:display :inline :position :relative :box-sizing :border-box :font-style :normal :color :inherit}
         [:.editable {:border-bottom input-field-bottom-border :padding (px 0 15) :background-color input-field-background
                      :color         :inherit :outline :none :display :inline :cursor :pointer :margin :auto}]]
        [:.text.incorrect {:background-color (alpha+ incorrect-colour -0.8) :border-radius (px 5) :border-bottom (border 2 :solid incorrect-colour)
                           :color            :red :padding-right (px 5)}
         [:.editable {:text-decoration :line-through :border-bottom :none :color :red :background-color :transparent :padding-right (px 5)}]
         [:.text.correct {:border-radius (px 0) :padding-right (px 0)}
          [:.editable {:border-bottom :none :text-decoration :none}]
          [:.text {:padding (px 0) :margin (px 0)}]
          [:.common {:padding (px 0) :margin (px 0)}]]]
        [:.text.correct {:background-color (saturation+ (brightness+ correct-colour 0.3) -0.5) :color correct-colour :border-radius (px 5)}
         [:.editable {:border-bottom (border 2 :solid (alpha+ correct-colour -0.4)) :color correct-colour :background-color (alpha+ correct-colour -0.9) :border-radius (px 5)}]]
        [:.text.with-preview {:border-bottom (border 1 :dashed (alpha+ leading-colour-3 -0.6))}]
        [:.text.with-preview:hover {:background-color (alpha+ leading-colour-3 -0.95)}]
        [:.tooltipped {:border (border 1 :solid :transparent) :border-radius (px 3) :cursor :pointer}]
        [:.tooltipped:hover {:border           (border 1 :solid (alpha+ black-colour -0.9)) :box-sizing :border-box
                             :background-color (alpha+ black-colour -0.98) :box-shadow (shadow 0 0 5 (alpha+ black-colour -0.8))}]
        [:.options {:display      :block :clear :both :text-align :center :position :relative :box-sizing :border-box :color (alpha+ black-colour -0.5) :margin :auto
                    :padding-left (px 20)}
         [:.option-row {:display :block :text-align :left :margin :auto}
          [:.option {:cursor :pointer :border-radius (px 8) :position :relative :display :inline-flex :align-items :center :padding (px 5 5 5 0) :box-sizing :border-box}
           [:.option-control {:display :inline-block :position :absolute :box-sizing :border-box :z-index 1}
            [:svg
             [:circle :rect {:stroke (alpha+ black-colour -0.85) :stroke-width (px 1.2) :fill (alpha+ white-colour -0.4)}]
             [:path {:fill :none :stroke (alpha+ black-colour -0.3) :stroke-width (px 2) :stroke-linecap :round}]]]
           [:.option-content
            [:.image :.audio {:margin (px 0)}]]]
          [:.option.incorrect {:background-color (alpha+ incorrect-border-colour -0.9)}
           [:.option-control
            [:circle :rect {:stroke incorrect-colour :fill (alpha+ incorrect-colour -0.6)}]
            [:path {:stroke incorrect-colour}]]]
          [:.option.correct {:background-color (alpha+ correct-border-colour -0.9)}
           [:.option-control
            [:circle :rect {:stroke correct-colour :fill (alpha+ correct-colour -0.6)}]
            [:path {:stroke correct-colour}]]
           [:.option-content
            [:img {:box-shadow (shadow 0 0 2 (alpha+ correct-colour -0.1))}]
            [:img.loading {:box-shadow :none}]]]
          [:.option.correct.selected {:background-color (alpha+ (brightness+ correct-colour 0.1) -0.8)}
           [:.option-control
            [:circle :rect {:fill correct-colour}]
            [:path {:stroke white-colour}]]]
          [:.option.incorrect.selected {:background-color (alpha+ (brightness+ incorrect-colour 0.1) -0.8)}
           [:.option-control
            [:circle :rect {:fill incorrect-colour}]
            [:path {:stroke white-colour}]]]
          [:.option.disabled {:pointer-events :none :opacity 0.5}]
          [:.option:hover {:background-color (alpha+ black-colour -0.98)}]]]
        [:.options.tiles {:padding-left (px 0)}
         [:.option-row
          [:.option {:padding (px 0)}
           [:.option-content {:margin-left (px 0) :min-width (px 50) :min-height (px 50)}]]]]
        [:.selector {:display :inline-block :position :relative :color :inherit}
         [:.selection {:background-color (greyscale 252) :padding (px 2 5) :font-weight 100
                       :color            (greyscale 150) :letter-spacing (px 1) :cursor :pointer :font-style :italic :font-size (pct 80)
                       :border           (border 1 :dashed (greyscale 230)) :border-radius (px 5)}]
         [:.selection:hover {:border (border 1 :dashed (greyscale 220)) :color (greyscale 130)}]
         [:.selection.selected {:font-weight :inherit :color :inherit :font-size :inherit
                                :font-style  :inherit :padding (px 0) :border-left :none :border-right :none :background-color white-colour :border-radius (px 0) :border-top (border 1 :dashed (greyscale 220)) :border-bottom (border 1 :dashed (greyscale 220)) :letter-spacing :inherit}]
         [:.selection.selected:hover {:background-color (greyscale 252) :border-top (border 1 :dashed (greyscale 200)) :border-bottom (border 1 :dashed (greyscale 200))}]
         [:.selector-options {:position   :absolute :background-color (greyscale 250) :white-space :nowrap
                              :padding    (px 1 2) :border-radius (px 5) :top (pct 100) :left (px -2) :transform (translate (px 0) (px -3)) :box-shadow (shadow 0 0 2 (greyscale 200)) :display :block
                              :margin-top (px 1) :z-index 500 :line-height (pct 120)}
          [:.selector-option {:cursor           :pointer :padding (px 2 25 2 5) :border-radius (px 3) :text-align :left :color (greyscale 100) :font-size (pct 80)
                              :background-color (greyscale 253) :margin-top (px 1) :margin-bottom (px 1)}]
          [:.selector-option.selected {:background-color (greyscale 240)}]
          [:.selector-option:hover {:background-color (greyscale 235)}]]
         [:.selector-options {:display :none}]]
        [:.selector.correct
         [:.selection {:border-top (border 1 :dashed correct-border-colour) :border-bottom (border 2 :solid correct-border-colour)
                       :color      correct-colour :background-color (alpha+ correct-colour -0.8)}]
         [:.selection:hover {:background-color (alpha+ correct-colour -0.9)}]]
        [:.selector.incorrect
         [:.selection {:border-top       (border 1 :dashed incorrect-border-colour) :border-bottom (border 2 :solid incorrect-border-colour)
                       :background-color (alpha+ incorrect-colour -0.8)}
          [:.correct-answer {:display :inline-block :margin-left (px 5) :background-color (saturation+ (brightness+ correct-colour 0.3) -0.5) :color correct-colour}]]
         [:.selection:hover {:background-color (alpha+ incorrect-colour -0.9)}]
         [:.selection.selected {:color :red :text-decoration :line-through}]]
        [:.list {:display :block :clear :both :position :relative :width (pct 100) :box-sizing :border-box :padding-left (px 10)}
         [:ol {:margin (px 10 0 20 0)}]
         [:.item {:text-align :left :position :relative :box-sizing :border-box :margin-top (px 8)}]]
        (let [border-colour (alpha+ black-colour -0.97)
              background-colour (alpha+ black-colour -0.99)]
          [:.citation {:display :block :clear :both :position :relative :padding-left (px 80) :margin (px 10 0 20 0)}
           [:.citation-content {:border-left (border 30 :solid border-colour) :background-color background-colour :padding (px 40 30 40 20) :text-align :left
                                :color       (alpha+ black-colour -0.4) :font-style :italic :font-size (px 24)}]
           [:.citation-reference {:border-left (border 30 :solid border-colour) :background-color background-colour :padding (px 5 10) :text-align :left
                                  :font-size   (px 14) :color (alpha+ black-colour -0.5) :border-top (border 1 :solid border-colour)}
            extras-paragraph]])
        [:.reference {:display :inline :vertical-align :super :font-size (pct 70) :color leading-colour-1 :margin-left (px 2)
                      :cursor  :pointer :font-style :normal}]
        [:.reference:hover {:color (alpha+ leading-colour-1 -0.2)}]
        [:.reference-list {:text-align       :left :display :block :width (pct 100) :box-sizing :border-box :color (alpha+ black-colour -0.3) :font-size (px 16)
                           :font-weight      700 :margin-top (px 80) :border-top (border 1 :solid (alpha+ black-colour -0.9)) :padding (px 5 5 15 5)
                           :background-color (alpha+ black-colour -0.99) :clear :both}
         [:.reference-entry {:display :block :width (pct 100) :box-sizing :border-box :margin-top (px 3) :background-color :transparent
                             :padding (px 0 2) :border-radius (px 3)}
          [:.reference-key {:margin-right (px 5) :vertical-align :super :color leading-colour-1 :font-size (pct 80) :display :inline}]
          [:.reference-text {:color       (alpha+ black-colour -0.4) :text-align :justify :display :inline
                             :font-weight 300}
           extras-paragraph]]]
        [:.image {:text-align :center :position :relative :display :block :box-sizing :border-box :margin :auto}
         [:.image-with-areas {:position :relative :display :inline-block}
          [:.fullview-toggle {:position         :absolute :z-index 3 :cursor :pointer
                              :background-color (alpha+ white-colour -0.4)
                              :padding          (px 3) :border-radius (px 5) :box-sizing :border-box :border (border 1 :solid (alpha+ black-colour -0.85))
                              :box-shadow       (shadow 0 0 5 (alpha+ black-colour -0.85))}
           [:svg {:position :absolute}
            [:path {:fill :none :stroke (alpha+ black-colour -0.5) :stroke-width (px 1.5) :stroke-linecap :round :stroke-linejoin :round}]]]
          [:img {:border     (border 1 :solid (alpha+ black-colour -0.92)) :padding (px 2) :border-radius (px 10)
                 :box-shadow (shadow 0 0 2 (alpha+ black-colour -0.9)) :vertical-align :middle :box-sizing :border-box
                 :width      (pct 100)}]
          [:.image-areas
           [:.image-area {:position :absolute :opacity 0.9 :border-radius (px 5)}
            [:.image-area-content {:display    :block :border (border 1 :solid (alpha+ white-colour -0.3))
                                   :box-sizing :border-box :border-radius (px 5) :background-color (alpha+ white-colour -0.8)
                                   :padding    (px 5 10) :box-shadow (shadow 0 0 10 white-colour) :white-space :normal}]
            [:.image-area-pointer {:position :absolute}
             [:path {:stroke :none}]]]
           [:.image-area.top-left {:transform (translate (px -5) (px 10))}
            [:.image-area-pointer {:left (px 5) :top (px -10)}]]
           [:.image-area.top {:transform (translate (pct -50) (px 6))}
            [:.image-area-pointer {:left (pct 50) :top (px -6) :transform (translate (px -10) (px 0))}]]
           [:.image-area.top-right {:transform (str (translate (pct -100) (px 10)) (translate (px 6) (px 0)))}
            [:.image-area-pointer {:left (pct 100) :top (px -10) :transform (translate (px -13) (px 0))}]]
           [:.image-area.right-top {:transform (str (translate (pct -100) (px -5)) (translate (px -9) (px 0)))}
            [:.image-area-pointer {:left (pct 100) :top (px 5)}]]
           [:.image-area.right {:transform (str (translate (pct -100) (pct -50)) (translate (px -5) (px 0)))}
            [:.image-area-pointer {:left (pct 100) :top (pct 50) :transform (translate (px 0) (px -10))}]]
           [:.image-area.right-bottom {:transform (str (translate (pct -100) (pct -100)) (translate (px -9) (px 6)))}
            [:.image-area-pointer {:left (pct 100) :top (pct 100) :transform (translate (px 0) (px -13))}]]
           [:.image-area.bottom-right {:transform (str (translate (pct -100) (pct -100)) (translate (px 6) (px -9)))}
            [:.image-area-pointer {:left (pct 100) :top (pct 100) :transform (translate (px -13) (px 0))}]]
           [:.image-area.bottom {:transform (str (translate (pct -50) (pct -100)) (translate (px 0) (px -5)))}
            [:.image-area-pointer {:left (pct 50) :top (pct 100) :transform (translate (px -10) (px 0))}]]
           [:.image-area.bottom-left {:transform (str (translate (px -5) (pct -100)) (translate (px 0) (px -9)))}
            [:.image-area-pointer {:left (px 5) :top (pct 100)}]]
           [:.image-area.left-bottom {:transform (str (translate (px 10) (pct -100)) (translate (px 0) (px 6)))}
            [:.image-area-pointer {:left (px -10) :top (pct 100) :transform (translate (px 0) (px -13))}]]
           [:.image-area.left {:transform (translate (px 6) (pct -50))}
            [:.image-area-pointer {:left (px -6) :top (pct 50) :transform (translate (px 0) (px -10))}]]
           [:.image-area.left-top {:transform (translate (px 10) (px -5))}
            [:.image-area-pointer {:left (px -10) :top (px 5)}]]
           [:.image-area:hover {:opacity 1}]]]
         [:.image-title {:font-size (px 14) :color (alpha+ black-colour -0.3) :margin (px 5 0)}
          extras-paragraph
          [:.common {:margin-right (px 2)}]]
         [:.cover {:background-color (alpha+ white-colour -0.1) :position :absolute :left (px 0) :top (px 0) :right (px 0) :bottom (px 0)}
          [:img {:width (px 30) :height (px 30) :border :none :box-shadow :none :margin-top (px 10)}]]
         [:.image-fullview {:position :fixed :top (px 0) :left (px 0) :z-index 10000 :cursor :default
                            :bottom   (px 0) :right (px 0) :background-color (alpha+ black-colour -0.1)}
          [:.close-fullview-button {:position :absolute :top (px 10) :right (px 20) :opacity 0.7 :cursor :pointer :line-height (px 20)}
           [:svg {:width (px 30) :height (px 30)}
            [:path {:fill :none :stroke (alpha+ white-colour -0.2) :stroke-width (px 1.5) :stroke-linecap :round :stroke-linejoin :round}]]]
          [:.close-fullview-button:hover {:opacity 1}]
          [:.image {:position :absolute :max-width (pct 90) :max-height (pct 90) :margin (px 0)
                    :width    (pct 100) :height (pct 100)
                    :top      (pct 50) :left (pct 50) :transform (translate (pct -50) (pct -50))}
           [:.image-with-areas {:width :auto}
            [:img {:max-width  (pct 100) :max-height (vh 90) :height :auto :width :auto
                   :box-shadow (shadow 0 0 2 (alpha+ white-colour -0.9)) :border (border 1 :solid (alpha+ white-colour -0.92))}]]
           [:.image-title {:color (alpha+ white-colour -0.3)}]]]]
        [:.audio {:text-align :center :position :relative :display :block}
         [:audio {:vertical-align :middle}]
         [:.audio-title {:font-size (px 14) :color (alpha+ black-colour -0.3) :margin (px 5 0)}
          extras-paragraph
          [:.common {:margin-right (px 2)}]]]
        [:.link {:display :inline-block}
         [:.image :.audio {:margin (px 0)}]]
        [:.canvas {:display :block :clear :both :position :relative :box-sizing :border-box :text-align :center}
         [:.canvas-editor {:background-color (alpha+ black-colour -0.35) :padding (px 3) :border-radius (px 10 10 3 3) :display :inline-block}
          [:.tools {:display :block :text-align :left}
           [:.canvas-button {:display :inline-block :border-radius (px 3) :height (px 30) :border (border 1 :solid (alpha+ black-colour -0.85)) :cursor :pointer
                             :padding (px 1 2) :background-color (alpha+ black-colour -0.8) :margin (px 1)}]
           [:.canvas-button:hover {:border (border 1 :solid (alpha+ black-colour -0.8)) :background-color (alpha+ black-colour -0.95)}]
           [:.canvas-button.disabled {:cursor :default :opacity 0.3}]
           [:.canvas-button.disabled:hover {:background-color (alpha+ black-colour -0.8) :border (border 1 :solid (alpha+ black-colour -0.8))}]
           [:.canvas-button.pressed {:border (border 1 :solid (alpha+ black-colour -0.75)) :background-color (alpha+ white-colour -0.9)}]
           [:.canvas-button.pressed:hover {:border (border 1 :solid (alpha+ black-colour -0.65))}]
           [:.filler {:display :inline-block}]]
          [:svg.editable {:border (alpha+ black-colour -0.98) :border-radius (px 3) :display :block :background-color white-colour}
           [:g.paths {:pointer-events :none :stroke :black :fill :none}
            [:path {:stroke-linecap :round :stroke-linejoin :round}]]]]]
        [:.error {:background-color incorrect-colour}]
        [:.code {:display :block :text-align :left :font-size (px 18) :line-height :initial :box-sizing :border-box :position :relative :border-radius (px 4) :padding (px 1) :box-shadow (shadow 0 0 3 (greyscale 200)) :z-index 1 :clear :both}
         [:.code-title {:font-size (px 12) :position :absolute :left (px 7) :top (px 0) :transform (translate (px 0) (pct -100)) :background-color (alpha+ (brightness+ leading-colour-0 -0.1) -0.3) :color white-colour :font-weight 400 :opacity 0 :line-height (px 14)
                        :padding   (px 0 4 1 3) :border-radius (px 6 10 2 0) :cursor :pointer :white-space :nowrap :z-index 1000}]
         [:.CodeMirror {:font-size (px 16) :line-height (px 20) :background-color (alpha+ (brightness+ leading-colour-0 -0.1) -0.98) :border-radius (px 4) :height :auto :box-sizing :border-box :border (border 2 :solid :transparent) :font-family code-font-family}
          [:.CodeMirror-lines {:cursor :pointer :padding (px 1 0)}]
          [:.CodeMirror-cursor {:display :none}]]
         [:.output {:position :relative :box-sizing :border-box :width (pct 100) :min-width (px 200)}
          [:.button {:position :absolute :top (px -4) :left (px 5) :padding (px 2 4) :font-size (px 12) :white-space :nowrap :z-index 1000 :box-shadow (shadow 0 0 2 (greyscale 150)) :border-radius (px 15) :background-color leading-colour-3 :opacity 0.4}]
          [:.linked-reference {:position :absolute :top (px 2) :right (px 7) :font-size (px 14) :text-shadow (shadow 0 0 1 white-colour)}]
          [:.output-content {:white-space :pre-wrap :box-sizing :border-box :background-color (brightness+ leading-colour-0 -0.6) :border (border 1 :solid (brightness+ leading-colour-0 -0.6)) :padding (px 20 4 4 4) :border-radius (px 3) :color (greyscale 200) :font-family code-font-family :font-size (px 14) :min-width (px 196) :position :relative}
           [:.output-content-title {:position :absolute :top (px -1) :left (pct 50) :transform (translate (pct -50) (px 0)) :font-family primary-font-family :background-color (greyscale 200) :color (brightness+ leading-colour-0 -0.6) :padding (px 0 5 2 5) :font-size (px 12) :border-radius (px 0 0 3 3) :box-shadow (shadow 0 0 3 (greyscale 200))}]
           [:.output-content-text {:max-height (px 300) :overflow :auto :-webkit-overflow-scrolling :touch}]
           [:.output-content-error {:background-color (alpha+ incorrect-colour -0.6) :position :relative :box-sizing :border-box :padding (px 15 5 5 5) :font-size (px 12) :margin-top (px 5)}
            [:.output-content-error-title {:position :absolute :top (px -1) :left (pct 50) :transform (translate (pct -50) (px 0)) :font-family primary-font-family :background-color (alpha+ incorrect-colour -0.4) :color white-colour :padding (px 0 5 2 5) :font-size (px 12) :border-radius (px 0 0 3 3) :box-shadow (shadow 0 0 3 (alpha+ incorrect-colour -0.4))}]
            [:.output-content-error-text {:max-height (px 200) :overflow :auto :-webkit-overflow-scrolling :touch}]]]
          [:.solution {:white-space :pre-wrap :box-sizing :border-box :border-radius (px 3) :background-color (saturation+ (brightness+ correct-colour 0.3) -0.5) :padding (px 22 4 4 4) :color correct-colour :font-family code-font-family :font-size (px 14) :min-width (px 196) :border (border 1 :solid correct-border-colour) :position :relative}
           [:.solution-title {:position :absolute :top (px -1) :left (pct 50) :transform (translate (pct -50) (px 0)) :font-family primary-font-family :background-color correct-border-colour :color white-colour :padding (px 0 5 2 5) :font-size (px 12) :border-radius (px 0 0 3 3) :box-shadow (shadow 0 0 3 correct-border-colour)}]
           [:.solution-text {:max-height (px 300) :overflow :auto :-webkit-overflow-scrolling :touch}]]]]
        [:.code:hover
         [:.code-title {:opacity 1}]
         [:.output
          [:.button {:opacity 1}]]]
        [:.code.inline {:display :inline-flex :margin (px 0 2) :padding (px 0)}
         [:.code-title {:left (px 5)}]
         [:.CodeMirror {:border-radius (px 3) :height :auto}]
         [:.output {:position :absolute :padding (px 0 2) :top (pct 100)}
          [:.button {:top (px -4) :left (px 0)}]
          [:.output-content {:box-shadow (shadow 0 0 5 (greyscale 100))}]
          [:.output-content
           [:.output-content-text {:max-height (px 100)}]]
          [:.solution
           [:.solution-text {:max-height (px 100)}]]]]
        [:.code.editable {:border-bottom (border 2 :solid (alpha+ (brightness+ leading-colour-0 -0.1) -0.7)) :box-shadow (shadow 0 0 5 (greyscale 200))}
         [:.CodeMirror {:background-color (alpha+ (brightness+ leading-colour-0 -0.1) -0.97)}
          [:.CodeMirror-lines {:cursor :text}]
          [:.CodeMirror-cursor {:display :block}]]]
        [:.code.correct {:box-shadow (shadow 0 0 5 correct-colour)}
         [:.CodeMirror {:border (border 2 :solid correct-colour) :background-color (alpha+ correct-colour -0.8)}]]
        [:.code.incorrect {:box-shadow (shadow 0 0 5 incorrect-colour)}
         [:.CodeMirror {:border (border 2 :solid incorrect-colour) :background-color (alpha+ incorrect-colour -0.8)}]]
        [:.math {:display :block :clear :both :font-size (em 2)}
         [:.mjx-chtml {:outline :none}]]
        [(gs/> :.math :.mjx-chtml) {:margin (px 0)}]
        [:.math.inline {:display :inline-block}]
        [:.definitions {:box-sizing :border-box :clear :both :padding (px 0 5)}
         [:.definition {:margin-top (px 10) :border-top (border 1 :dashed (alpha+ black-colour -0.9))
                        :box-sizing :border-box :clear :both :padding (px 3 5 0 5)}
          [:.definition-title {:float   :left :margin (px 0 10 10 0) :font-size (px 24) :color (alpha+ black-colour -0.1)
                               :display :inline-block :font-weight 400 :box-sizing :border-box}]
          [:.definition-content {:font-size   (px 18) :text-align :left :padding (px 5 5 5 10) :color (alpha+ black-colour -0.3)
                                 :font-weight 300 :box-sizing :border-box :border-left (border 5 :solid :transparent)} :background-color :none]]]
        [(gs/> :.definitions.level-1 :.definition) {:border-top :none :margin-top (px 5) :padding-top (px 0)}]
        [(gs/> :.definitions.level-1 :.definition :.definition-title) {:font-size (px 20) :min-width (px 75) :padding-left (px 10)}]
        [(gs/> :.definitions.level-1 :.definition :.definition-content) {:font-size (px 16) :border-left (border 5 :solid (greyscale 240)) :background-color (greyscale 253)}]
        [(gs/> :.definitions.level-2 :.definition) {:font-style :italic :border-top :none :margin-top (px 5) :padding-top (px 0)}]
        [(gs/> :.definitions.level-2 :.definition :.definition-title) {:min-width (px 50) :font-size (px 18) :font-weight 300}]
        [(gs/> :.definitions.level-2 :.definition :.definition-content) {:font-size (px 14)}]
        [:.table {:display :block :box-sizing :border-box :margin :auto}
         [:.table-content {:display :table :border-spacing (px 1) :box-sizing :border-box :border-radius (px 3) :color (greyscale 100) :font-size (px 16) :border (border 1 :solid :transparent) :background-color (alpha+ leading-colour-2 -0.7) :width (pct 100)}
          [:.row {:display :table-row :box-sizing :border-box}
           [:.cell {:display :table-cell :vertical-align :top :box-sizing :border-box :padding (px 2 5) :background-color white-colour :text-align :left :font-weight 300}]]
          [:.row.header {:font-size (px 14)}
           [:.cell {:background-color (saturation+ (brightness+ leading-colour-2 0.25) -0.55) :text-align :center :color white-colour :font-weight 400 :padding (px 4 5)}]
           [:.cell:first-child {:border-radius (px 2 0 0 0)}]
           [:.cell:last-child {:border-radius (px 0 2 0 0)}]]
          [:.row:last-child
           [:.cell:first-child {:border-radius (px 0 0 0 2)}]
           [:.cell:last-child {:border-radius (px 0 0 2 0)}]]]
         [:.table-content.borderless {:background-color :transparent}]
         [:.table-title {:font-size (px 14) :color (alpha+ black-colour -0.3) :margin (px 5 0)}
          extras-paragraph
          [:.common {:margin-right (px 2)}]]]]
       [:.flag {:cursor :pointer :position :fixed :z-index 2000 :border-radius (px 100) :box-shadow (shadow 0 0 20 white-colour)}
        [:.hint {:left (px 25) :top (px -45) :width (px 160)}]
        [:circle {:fill (alpha+ (brightness+ (saturation+ leading-colour-1 -0.8) 0.7) -0.1) :stroke (alpha+ (brightness+ leading-colour-1 -0.1) -0.9) :stroke-width (px 4)}]
        [:rect {:fill (alpha+ (brightness+ incorrect-colour -0.4) -0.6)}]
        [:path {:fill (alpha+ (brightness+ incorrect-colour -0.4) -0.6)}]]
       [:.next-arrow {:cursor :pointer :position :fixed :z-index 2000 :border-radius (px 100) :box-shadow (shadow 0 0 20 white-colour)}
        [:circle.main {:fill correct-colour :stroke (alpha+ (brightness+ correct-colour -0.2) -0.8) :stroke-width (px 3)}]
        [:circle.main.disabled {:fill (alpha+ (saturation+ correct-colour -0.5) -0.2) :stroke (alpha+ black-colour -0.9)}]
        [:polyline {:fill :none :stroke white-colour :stroke-width (px 4) :stroke-linecap :round}]
        [:circle.loading {:fill (alpha+ black-colour -0.8) :stroke :none}]]])













    [:.page-footer {:color (alpha+ black-colour -0.7) :font-size (px 12) :font-weight 300 :text-align :center :border-top (border 1 :solid (alpha+ black-colour -0.95)) :padding (px 5) :box-sizing :border-box :margin (px 0 30) :height (px 20) :line-height (px 10) :position :fixed :background-color white-colour :bottom (px 0) :left (px 0) :right (px 0)}]]

   ;; NARROW MODE
   [:.page-chrome.narrow
    [:.help {:min-width (px 250)}
     [:.help-title {:height (px 32) :font-size (px 16)}]
     [:.help-content {:font-size (px 18) :margin (px 45 10 10 10)}
      [:.help-extra {:font-size (px 14)}]]
     [:.help-prev {:margin (px 5) :font-size (px 14)}]
     [:.help-next {:margin (px 5) :font-size (px 14)}]]
    [:.page-header
     [:.page-header-content {:width (pct 100) :text-align :center}
      [:.page-header-icon {:margin-top (px 4) :display :inline-block}]
      [:.page-header-control {:width (px 38) :height (px 38) :left (px 5)}]]
     [:.page-subheader {:background-color white-colour :width (pct 100) :position :fixed :height (px 38) :top (px 44) :right (px 0) :left (px 0) :border-bottom (border 1 :solid (greyscale 240)) :border-top (border 1 :solid (greyscale 240)) :box-sizing :border-box :color (greyscale 50)}
      [:.welcome-message {:font-size (px 16) :width (pct 100) :padding-top (px 10) :text-align :center}]
      [:.focus-panel {:background-color (alpha+ leading-colour-3 -0.95)}
       [:.search-icon {:top (px 4)}]
       [:.focus-title {:font-size (px 14)}]]
      [:.search-view {:position :fixed :top (px 0) :bottom (px 0)}
       [:.logo {:margin-left (px 5)}]
       [:.input {:margin-right (px 3)}]
       [:.hide-search {:flex (flex 0 0 30) :text-align :center :margin (px 8 4 4 0) :padding (px 0)}]
       [:.search-results {:padding-bottom (px 50)}]]]
     [:.toolbar {:top (px 44) :bottom (px 16) :left (px 0) :border-top (border 1 :solid (alpha+ black-colour -0.95)) :padding (px 0 0 5 0)}
      [:.toolbar-content
       [:.text-input {:padding (px 26 10 6 10)}
        [:.text-input-title {:top (px 14) :left (px 14) :font-size (px 26)}]
        [:.text-input-title.smaller {:top (px 9) :left (px 11) :font-size (px 16)}]
        [:.text-input-field {:height (px 26) :font-size (px 20)}]]
       [:.button {:padding (px 10 20) :font-size (px 20)}]
       [:.forgot-password {:margin (px 40 5 0 5)}]
       [:.e-mail-reminder {:margin (px 40 5 0 5)}]]]]
    [:.page-header.selected
     [:.page-header-content
      [:.page-header-control
       [:svg.menu {:width (px 36) :height (px 36)}]
       [:svg.menu:hover {:background-color :transparent}]]]]
    [:.page-content {:width (pct 100)}
     [:.home-page {:padding-top (px 40) :text-align :center}]
     [:.profile-page {:padding-top (px 30)}]
     [:.policy-page {:padding (px 30 5 140 5)}
      [:.policy-group
       [:.policy-title {:font-size (px 18)}]
       [:.policy-content {:font-size (px 16)}]]]
     [:.principles-page {:margin (px 80 0 100 0) :text-align :center}
      [:.principle {:width (pct 100) :display :inline-block :padding (px 10) :box-sizing :border-box}
       [:.principle-content
        [:.principle-title {:font-size (px 26) :font-weight 100}
         [:.darker {:font-weight 300}]]
        [:.principle-message {:font-size (px 16) :font-weight 300 :margin-top (px 10)}
         [:.darker {:font-weight 400}]]]
       [:.principle-image {:text-align :center :margin-top (px 20)}]]
      [:.principle.left :.principle.right {:border-bottom (border 1 :dashed (greyscale 220))}
       [:.principle-image {:margin-top (px 10)}]]
      [:#more {:margin-top (px 50)}]]]
    [:.worksheet {:margin (px 30 0 200 0)}
     [:.worksheet-content {:font-size (px 18)}
      [:.preview {:position :fixed :top (px 0) :right (px 0) :bottom (px 0) :left (px 0) :background-color white-colour :box-shadow :none :max-height :initial :min-width :initial}
       [:.preview-navigation {:margin-right (px 40)}]
       [:.worksheet {:flex-grow 1 :overflow :auto}]]
      [:.worksheet-block {:margin-top (px 12) :margin-bottom (px 15)}]
      [:.worksheet-block.large {:margin-top (px 15) :margin-bottom (px 18)}]
      [:.text.with-preview {:cursor :pointer}]
      [:.options {:font-size (px 16) :line-height (px 18)}
       [:.option-row
        [:.option {:min-height (px 32)}
         [:.option-control {:top (px 5) :left (px 5)}
          [:svg {:width (px 22) :height (px 22)}]]
         [:.option-content {:margin-left (px 31)}
          [:.worksheet-block {:margin-top (px 0) :margin-bottom (px 0)}]]]]]
      [:.options.tiles
       [:.option-row
        [:.option {:padding-top (px 0)}
         [:.option-control {:left (px 6)}]
         [:.option-content {:margin-left (px 0) :min-width (px 50) :min-height (px 50)}]]]]
      [:.table
       [:.table-content
        [:.row
         [:.cell
          [:.worksheet-block {:margin-top (px 0) :margin-bottom (px 0)}]]]]]
      [:.definitions
       [:.definition
        [:.definition-title {:font-size (px 20)}]
        [:.definition-content {:font-size (px 16)}]]]
      [(gs/> :.definitions.level-1 :.definition :.definition-title) {:font-size (px 16) :min-width (px 75)}]
      [(gs/> :.definitions.level-1 :.definition :.definition-content) {:font-size (px 14) :padding-top (px 0)}]
      [(gs/> :.definitions.level-2 :.definition :.definition-title) {:font-size (px 16) :min-width (px 75)}]
      [(gs/> :.definitions.level-2 :.definition :.definition-content) {:font-size (px 14) :padding-top (px 0)}]
      [:.image
       [:.image-with-areas
        [:.fullview-toggle {:visibility :visible :opacity 0.6 :width (px 24) :height (px 24) :top (px 6) :right (px 6)}
         [:svg {:left (px 1) :top (px 1)}]]]
       [:.image-fullview
        [:.close-fullview-button {:top (px 5) :right (px 5)}
         [:svg {:width (px 20) :height (px 20)}]]
        [:.image
         [:.image-title {:font-size (px 12) :margin-top (px 0)}]]]]
      [:.code
       [:.code-title {:opacity 0.6 :font-size (px 10) :line-height (px 11)}]
       [:.output
        [:.button {:opacity 0.8}]]]]
     [:.flag {:bottom (px 25) :left (px 5) :width (px 40) :height (px 40)}]
     [:.next-arrow {:bottom (px 20) :right (px 5) :width (px 70) :height (px 70)}]]
    [:.page-footer {:font-size (px 10) :height (px 16) :line-height (px 5) :margin (px 0)}
     [:.linked-reference.page {:padding-left (px 5) :margin-left (px 5)}]]]


   ;; WIDE MODE
   [:.page-chrome.wide
    [:.help {:min-width (px 400)}
     [:.help-title {:height (px 36) :font-size (px 20)}]
     [:.help-content {:font-size (px 20) :margin (px 60 20 20 20)}
      [:.help-extra {:font-size (px 16)}]]
     [:.help-prev {:margin (px 10 5 5 5) :font-size (px 16)}]
     [:.help-next {:margin (px 10 5 5 5) :font-size (px 16)}]]
    [:.concept-preview
     [:.concept-preview-title
      [:.concept-preview-name
       [:.concept-preview-tags {:font-size (px 13)}]]]
     [:.concept-preview-content {:display :flex :flex-direction :row :flex-wrap :nowrap}
      [:.concept-preview-content-description {:font-size (px 14) :color (alpha+ black-colour -0.3)}]]]
    (let [toolbar-content [:.toolbar-content {:overflow :initial :padding (px 0 10)}
                           [:.text-input {:padding (px 18 7 4 7)}
                            [:.text-input-title {:top (px 10) :left (px 14) :font-size (px 18)}]
                            [:.text-input-title.smaller {:top (px 4) :left (px 8) :font-size (px 12)}]
                            [:.text-input-field {:height (px 20) :font-size (px 16)}]]
                           [:.button {:padding (px 6 16) :font-size (px 16)}]
                           [:.forgot-password {:margin (px 20 10 0 10) :font-size (px 14)}]
                           [:.e-mail-reminder {:margin (px 20 10 10 10)}]]]
      [:.page-header {:text-align :center}
       [:.page-header-content {:width (pct 90)}
        [:.page-header-icon {:position :absolute :top (px 4) :left (px 0) :width (px 200) :text-align :left}
         [:svg {:display :inline-block :float :left}]
         [:.page-header-title {:font-size (px 20) :line-height (px 20) :display :inline-block :margin (px 3 0)}
          [:.page-header-subtitle {:font-size (px 11) :font-weight 300 :line-height (px 11) :margin-left (px 4)}]]]
        [:.page-subheader {:display :inline-block :height (px 44) :padding-top (px 2)}
         [:.welcome-message {:font-size (px 18) :width (pct 100) :padding-top (px 10) :text-align :center}]
         [:.focus-panel {:padding (px 0 76 0 32) :height (px 40)}
          [:.search-icon {:top (px 6)}]
          [:.focus-title {:font-size (px 20) :font-weight 300 :line-height (px 20)}]
          [:.level {:right (px 28) :top (px 2)}]
          [:.concept-bookmark {:right (px 9) :top (px -2)}]]
         [:.focus-panel:hover {:box-shadow (shadow 0 0 3 (greyscale 200)) :border-radius (px 30)}]
         [:.search-view {:position :absolute :top (px 0) :border-radius (px 5 5 15 15) :box-shadow (shadow 0 0 5 (alpha+ black-colour -0.7)) :padding-bottom (px 10)}
          [:.search-input {:box-shadow (shadow 0 1 1 (alpha+ leading-colour-3 -0.85))}]
          [:.hide-search {:flex (flex 0 0 90) :padding (px 10 0 4 0) :text-align :center :margin (px 6 0)}]
          [:.search-results {:min-height (px 300)}]]]
        [:.page-header-control {:padding-top (px 4)}
         [:svg.menu {:width (px 36) :height (px 36) :margin-top (px 1)}]
         [:svg.menu:hover {:background-color (alpha+ leading-colour-3 -0.8) :border-radius (px 5)}]]
        [:.page-header-control.selected
         [:svg.menu {:box-shadow (shadow 0 0 3 (alpha+ black-colour -0.6))}
          [:path {:stroke leading-colour-3 :stroke-width (px 2)}]]]]
       [:.toolbar {:position   :absolute :top (px 41) :right (px 0) :min-width (px 200)
                   :color      black-colour :border-top (border 1 :solid (alpha+ black-colour -0.95))
                   :overflow   :auto :display :flex :flex-direction :column :flex-wrap :nowrap :border-radius (px 5 0 5 5)
                   :box-shadow (shadow 0 0 3 (alpha+ black-colour -0.6))}
        toolbar-content
        [:.control-button {:font-size (px 20) :min-height (px 50) :padding-top (px 12)}]]
       [:.toolbar.lock :.toolbar.settings {:width (px 500)}
        [:.toolbar-title {:padding-top (px 10)}]
        [:.toolbar-area {:margin-bottom (px 5)}
         [:.toolbar-area-title {:font-size (px 18)}]]]
       [:.toolbar.settings
        [:.toolbar-area {:width (pct 50) :float :left :margin (px 0) :box-sizing :border-box :padding (px 10)}]]
       [:.toolbar.principles {:width (px 500) :height (px 600)}
        [:img {:max-width (px 400)}]
        [:.toolbar-title {:padding-top (px 10)}]
        [:.toolbar-content {:overflow :auto :padding-bottom (px 30)}]
        [:.toolbar-area {:margin-bottom (px 5)}
         [:.toolbar-area-title {:font-size (px 18)}]]]
       [:.visitor-toolbar {:position      :absolute :top (px 40) :right (px 0) :width (px 650)
                           :color         black-colour :border-top (border 1 :solid (alpha+ white-colour -0.95))
                           :border-radius (px 5 0 5 5) :background-color white-colour
                           :box-shadow    (shadow 0 0 3 (alpha+ black-colour -0.6))}
        [:.visitor-message {:text-align :center :padding (px 20 10) :font-size (px 18) :font-weight 300 :color (alpha+ black-colour -0.2) :border-radius (px 5) :cursor :default :box-sizing :border-box :margin (px 10)}
         [:.strong {:color (brightness+ leading-colour-1 -0.2) :display :inline-block}]
         [:.linked-reference {:font-size (px 26) :border-bottom (border 1 :solid (alpha+ leading-colour-1 -0.3))}]]
        [:.toolbars {:display :flex :flex-direction :row :flex-wrap :nowrap}
         [:.toolbar {:flex (flex 1 1 100) :position :relative :padding (px 10 0) :box-shadow :none :overflow :initial :top :initial}
          toolbar-content]]]])
    [:.page-content {:width (pct 90)}
     [:.profile-page
      [:.profile-area
       [:.profile-area-title {:font-size (px 14) :padding (px 4)}]
       [:.profile-area-content {:padding (px 10)}
        [:.search-view
         [:.hide-search {:flex (flex 0 0 90) :padding (px 10 0 4 0) :text-align :center :margin (px 6 0)}]]]]]
     [:.home-page {:text-align :center :padding (px 80 0 100 0)}]
     [:.policy-page {:padding (px 10 5 140 5)}
      [:.policy-group
       [:.policy-title {:font-size (px 18)}]
       [:.policy-content {:font-size (px 16)}]]]
     [:.principles-page {:margin (px 10 0 100 0) :text-align :center}
      [:.principle {:clear :both :margin (px 50 5 5 5) :width (pct 90) :display :inline-block}
       [:.principle-content {:padding (px 10 30) :box-sizing :border-box}
        [:.principle-title {:font-size (px 30) :font-weight 100}
         [:.darker {:font-weight 400}]]
        [:.principle-message {:font-size (px 18) :font-weight 300 :margin-top (px 10)}
         [:.darker {:font-weight 500}]]]]
      [:.principle.left {:text-align :right}
       [:.principle-content {:float :left :width (pct 50) :border-right (border 1 :dashed (greyscale 220))}]
       [:.principle-image {:float :right :width (pct 50)}]]
      [:.principle.right {:text-align :left}
       [:.principle-content {:float :right :width (pct 50) :border-left (border 1 :dashed (greyscale 220))}]
       [:.principle-image {:float :left :width (pct 50)}]]
      [:#more {:margin-top (px 50)}]]
     [:.worksheet {:margin (px 20 0 100 0)}
      [:.worksheet-content {:font-size (px 24)}
       [:.preview
        [:.worksheet {:max-height (px 400) :overflow-y :auto :overflow-x :hidden}]]
       [:.worksheet-block {:margin-top (px 10) :margin-bottom (px 20)}]
       [:.worksheet-block.large {:margin-top (px 20) :margin-bottom (px 25)}]
       [:.text.with-preview {:cursor :help}]
       [:.options {:font-size (px 20) :line-height (px 24)}
        [:.option-row
         [:.option {:min-height (px 42)}
          [:.option-control {:top (px 6) :left (px 6)}
           [:svg {:width (px 30) :height (px 30)}]]
          [:.option-content {:margin-left (px 41)}
           [:.worksheet-block {:margin-top (px 0) :margin-bottom (px 0)}]]]]]
       [:.options.tiles {:padding-left (px 0)}
        [:.option-row
         [:.option {:padding-top (px 0)}
          [:.option-control {:left (px 7)}]
          [:.option-content {:margin-left (px 0) :min-width (px 50) :min-height (px 50)}]]]]
       [:.table
        [:.table-content
         [:.row
          [:.cell
           [:.worksheet-block {:margin-top (px 0) :margin-bottom (px 0)}]]]]]
       [:.image
        [:.image-with-areas
         [:.fullview-toggle {:visibility :hidden :opacity 0.9 :width (px 38) :height (px 38) :top (px 10) :right (px 10)}
          [:svg {:left (px 3) :top (px 3)}]]
         [:.fullview-toggle:hover {:opacity 1.0}]]]
       [:.image:hover
        [:.fullview-toggle {:visibility :visible}]]]
      [:.flag {:bottom (px 45) :left (pct 50) :transform (translate (px -200) (px 0)) :width (px 40) :height (px 40)}]
      [:.flag:hover
       [:svg {:opacity 0.8}]]
      [:.next-arrow {:bottom (px 40) :left (pct 50) :transform (translate (px 150) (px 0)) :width (px 70) :height (px 70)}]
      [:.next-arrow:hover {:opacity 0.8}]]]
    [:.page-footer
     [:.linked-reference.page {:padding-left (px 15) :margin-left (px 15)}]]]













   [:.studio-page {:padding (px 50 0 200 0)}
    [:.highlight {:background-color (alpha+ leading-colour-3 -0.7) :display :inline}]
    [:.button.add :.button.delete :.button.duplicate {:background-color :transparent :border-radius (px 5) :display :inline-block :font-weight 300 :box-shadow :none :border :none :cursor :pointer :padding (px 0)}]
    [:.studio-tab-list {:border-bottom (border 1 :solid (alpha+ leading-colour-3 -0.8)) :margin-bottom (px 10)}
     [:.studio-tab-button {:font-size     (px 18) :font-weight 300 :color (alpha+ black-colour -0.5) :cursor :pointer :width (px 80) :text-align :center
                           :border-radius (px 5 5 0 0) :padding (px 5) :display :inline-block :margin (px 0 5)}]
     [:.studio-tab-button:hover {:text-decoration :underline}]
     [:.studio-tab-button.selected {:font-weight 400 :background-color (alpha+ leading-colour-3 -0.2) :color white-colour}]]
    [:.content-item {:display :flex :flex-direction :row :flex-wrap :nowrap :overflow :auto :cursor :pointer}
     [:.content-delete-button {:flex (flex 0 0 50) :padding (px 10)}]
     [:.content-information {:flex     (flex 1 1 100) :display :flex :flex-direction :column :flex-wrap :nowrap :padding (px 5)
                             :overflow :auto :box-sizing :border-box}
      [:.content-title {:box-sizing :border-box :margin-bottom (px 2) :flex (flex 0 0 20)}
       [:.content-id {:display :inline-block :font-weight 500 :font-size (px 14) :color (alpha+ leading-colour-3 -0.3) :cursor :pointer}]
       [:.content-id:hover {:color leading-colour-3 :text-decoration :underline}]]
      [:.content-name {:font-weight 500 :color (alpha+ black-colour -0.3) :flex (flex 0 0 20)}
       [:.content-tag {:font-weight 300 :vertical-align :super :display :inline-block :font-size (px 12) :background-color leading-colour-1
                       :color       white-colour :padding (px 1 4) :border-radius (px 3) :margin-left (px 3)}]]
      [:.content-description {:font-weight 300 :font-size (px 14) :color (alpha+ black-colour -0.3)
                              :flex        (flex 1 1 20) :display :inline-block :word-wrap :break-word :word-break :normal :white-space :pre-line}]
      [:.template-description {:font-size  (px 12) :color (alpha+ black-colour -0.3) :font-family code-font-family
                               :flex       (flex 1 1 80) :display :inline-block :word-wrap :break-word :word-break :normal :white-space :pre
                               :max-height (px 100) :overflow :auto :border-radius (px 5)
                               :padding    (px 5) :box-shadow (shadow 0 0 5 (alpha+ black-colour -0.9)) :box-sizing :border-box}]]
     [:.content-preview {:flex (flex 0 0 150) :padding (px 3) :box-sizing :border-box :position :relative :text-align :right}
      [:.content-type {:display     :inline-block :position :absolute :color white-colour :background-color (alpha+ black-colour -0.3)
                       :padding     (px 1 3) :font-size (px 12) :font-weight 300 :border-radius (px 3) :opacity 0.5 :z-index 10 :right (px 5) :top (px 5)
                       :white-space :nowrap}]
      [:.cost {:margin (px 25 5 5 5) :color (alpha+ black-colour -0.6) :font-weight 400 :font-size (px 16)}]
      [:.weight {:margin (px 5) :color (alpha+ black-colour -0.6) :font-weight 500 :font-size (px 16)}]
      [:.flags {:margin (px 25 5) :color (alpha+ black-colour -0.5) :font-size (px 16)}]
      [:img {:border-radius (px 5) :max-width (pct 100) :max-height (pct 100) :display :inline-block}]]]
    [:.explorer-tab
     [:.explorer-area {:border  (border 1 :dashed (alpha+ leading-colour-2 -0.9)) :border-radius (px 5) :margin-top (px 10)
                       :padding (px 3) :box-sizing :border-box}
      [:.explorer-area-title {:border-bottom    (border 1 :solid (alpha+ leading-colour-2 -0.85)) :padding (px 8 2 5 2)
                              :text-align       :center :font-size (px 26) :position :relative :font-weight 100 :color (alpha+ leading-colour-3 -0.4)
                              :background-color (alpha+ black-colour -0.97) :border-radius (px 5 5 0 0)}
       [:.explorer-refresh {:position  :absolute :top (px 5) :right (px 5)
                            :font-size (px 16) :font-weight 300}]]
      [:.explorer-area-content {:padding (px 5)}
       [:.explorer-stale-templates
        [:.explorer-stale-template {:display       :flex :flex-direction :row :flex-wrap :nowrap :overflow :auto :padding (px 3)
                                    :border-bottom (border 1 :solid (alpha+ leading-colour-2 -0.95))}
         [:.explorer-stale-template-content {:flex (flex 1 1 100) :display :inline-flex :align-items :center :justify-content :center :overflow :auto}
          [:.content-item {:width (pct 100) :overflow :auto}]
          [:.content-item:hover {:background-color (alpha+ black-colour -0.98)}]]
         [:.explorer-stale-template-count {:flex      (flex 0 0 100) :display :inline-flex :align-items :center :justify-content :center
                                           :font-size (px 22)}]]]]]
     [:.explorer-area:hover {:background-color (alpha+ leading-colour-3 -0.99)}]]
    [:.content-tab {:padding (px 15 0) :text-align :center}
     [:.text-editor {:display       :inline-block :outline :none :padding (px 3)
                     :border-radius (px 5) :border (border 1 :solid (alpha+ black-colour -0.9))
                     :box-sizing    :border-box}]
     [:.text-value {:display :inline-block :padding (px 3) :border (border 1 :solid :transparent)}]
     [:.text-value:hover {:background-color (alpha+ leading-colour-2 -0.95) :border (border 1 :dashed (alpha+ leading-colour-2 -0.7))
                          :border-radius    (px 5)}]
     [:.text-value.empty {:color (alpha+ black-colour -0.8)}]
     [:.content-search-control {:position :relative :width (pct 100)}
      [:.content-search-input {:width (pct 100) :height (px 45) :display :inline-block}
       [:.delete {:position :absolute :right (px 10) :top (px 10)}]
       [:input {:width         (pct 100) :height (px 40) :display :inline-block :outline :none :font-size (px 24) :padding (px 5) :color (alpha+ black-colour -0.3)
                :margin-bottom (px 5) :border-radius (px 5) :border (border 1 :solid (alpha+ black-colour -0.9))
                :font-weight   300 :letter-spacing (px 1) :box-sizing :border-box}]
       ["input::placeholder" {:font-style :italic :font-weight 200 :color (alpha+ black-colour -0.8)
                              :font-size  (px 22) :letter-spacing (px 3) :padding-left (px 3)}]]
      [:.content-search-types {:text-align :left}
       [:.total-count {:float :right :font-weight 500 :margin-right (px 3)}]
       [:.checkbox {:margin (px 0 15 0 5)}]]
      [:.content-search-results {:background-color white-colour :border (border 1 :solid (alpha+ black-colour -0.95))
                                 :padding          (px 5) :margin-top (px 10) :width (pct 100)
                                 :display          :flex :flex-direction :column :flex-wrap :nowrap :box-sizing :border-box :text-align :left}
       [:.content-search-result {:flex (flex 1 1 100) :padding (px 0 2) :cursor :pointer :border-radius (px 3)}]
       [:.content-search-result.selected {:background-color (alpha+ leading-colour-2 -0.95)}]
       [:.content-search-result:hover {:background-color (alpha+ leading-colour-2 -0.97)}]]]
     [:.content-controls {:display :flex :flex-direction :row :flex-wrap :nowrap :margin (px 20 0)}
      [:.prev {:flex (flex 0 0 100)}]
      [:.add.button {:flex (flex 1 1 100) :margin-top (px 10)}]
      [:.next {:flex (flex 0 0 100)}]]
     [:.selected-content {:text-align :left :border (border 1 :solid (alpha+ black-colour -0.95)) :padding (px 10 0) :border-radius (px 2)}
      [:.title {:padding (px 0 10) :box-sizing :border-box}
       [:.delete.button {:margin-right (px 5)}]
       [:.id {:display :inline-block :font-weight 400 :font-size (px 24) :color (alpha+ leading-colour-3 -0.3) :cursor :pointer :margin (px 0 15 0 5)}]
       [:.id:hover {:color leading-colour-3 :text-decoration :underline}]
       [:select {:display :inline-block :font-size (px 24) :border :none :cursor :pointer :outline :none :background :none :color leading-colour-1}]]
      [:.name {:margin (px 30 5 10 5)}
       [:.text-editor {:font-weight 500 :font-size (px 20)}]
       [:.tags {:display :inline-block :vertical-align :super}
        [:.tag {:display :inline-block :margin-right (px 3)}
         [:.text-value {:background-color leading-colour-1 :color white-colour :border-radius (px 5) :display :inline-block :font-size (px 14) :margin (px 3 1 0 2) :padding (px 0 3 1 3) :font-weight 300}]
         [:.text-editor {:font-weight 300 :border-radius (px 5) :display :inline-block :font-size (px 14) :margin (px 3 1 0 2) :padding (px 0 3 1 3)}]]
        [:.text-editor {:font-weight 400 :border-radius (px 5) :display :inline-block :font-size (px 14) :margin (px 3 2 0 2) :padding (px 0 3 1 3)}]
        [:.text-value {:color correct-colour :font-weight 400}]]]
      [:.description {:word-wrap :break-word :word-break :normal :white-space :pre-line :margin (px 10 5)}
       [:.name-editor {:font-size (px 14)}]]
      [:.flags {:margin (px 20 0 10 0) :border (border 1 :solid (alpha+ leading-colour-2 -0.95)) :padding (px 10) :border-radius (px 10)}
       [:.flags-title {:display :inline-block :font-size (px 20) :margin-left (px 2) :font-weight 300}]
       [:.flags-list {:display :inline-block}
        [:.flag-user {:color   leading-colour-3 :font-size (px 18) :border (border 1 :solid (alpha+ leading-colour-3 -0.9))
                      :padding (px 5) :border-radius (px 10) :background-color (alpha+ leading-colour-3 -0.95) :display :inline-block :margin-left (px 10)}
         [:.button.delete {:margin-left (px 3) :align-items :center :justify-content :center}]]]
       [:.clear-button
        [:.button {:color   leading-colour-0 :font-size (px 24) :border (border 1 :solid (alpha+ leading-colour-0 -0.8))
                   :padding (px 5) :border-radius (px 10) :background-color (alpha+ incorrect-colour -0.9) :margin-top (px 10)}]]]
      [:.concept-cost {:font-size (px 20) :font-weight 500 :margin-left (px 5) :height (px 50)}
       [:.free {:display :inline-block :font-weight 400}]
       [:.paid {:display :inline-block :font-weight 400}
        [:.price {:display :inline-block :font-size (px 20)}
         [:.text-editor {:border (border 1 :dashed (alpha+ leading-colour-2 -0.85)) :margin-left (px 2)}]]]]
      [:.checkbox {:margin (px 20 5 5 5)}]
      [:.concept-weight {:font-size (px 20) :font-weight 500 :margin (px 20 5) :height (px 50)}]
      [:.content {:margin-top (px 30)}
       [:select {:display :inline-block :font-size (px 20) :border :none :cursor :pointer :outline :none :background :none :color leading-colour-1
                 :margin  (px 10 5)}]
       [:.image {:text-align :center}
        [:.editor {:max-width (pct 100) :box-shadow (shadow 0 0 3 (alpha+ black-colour -0.9)) :padding (px 5) :box-sizing :border-box :cursor :pointer
                   :border    (border 3 :dashed :transparent) :text-align :center}]
        [:.editor:hover {:border (border 3 :dashed (alpha+ leading-colour-2 -0.7))}]]
       [:.document {:text-align :center}
        [:.editor {:max-width (pct 100) :box-shadow (shadow 0 0 3 (alpha+ black-colour -0.9)) :padding (px 5) :box-sizing :border-box :cursor :pointer
                   :border    (border 3 :dashed :transparent) :text-align :center :color leading-colour-2
                   :word-wrap :break-word :word-break :normal :white-space :pre}]
        [:.editor:hover {:border (border 3 :dashed (alpha+ leading-colour-2 -0.7))}]
        [:.viewer {:margin-top (px 10)}]]
       [:.audio
        [:.button {:float :left :margin (px 10 5)}]]
       [:.edn {:text-align :center}
        [:.CodeMirror {:text-align :left}]
        [:.worksheet
         [:.code.inline
          [:.CodeMirror {:height :auto}]]]
        [:.button {:float :none}]
        [:.button.revert {:margin-right (px 5)}]
        [:.button.inactive {:pointer-events :none :opacity 0.3}]
        [:.template-preview {:margin-top (px 50)}]]]
      [:.connections {:border (border 1 :solid (alpha+ black-colour -0.95)) :border-radius (px 5) :margin (px 100 5 5 5)}
       [:.connection-titles {:margin (px 10) :text-align :center}
        [:.connection-title {:color (alpha+ leading-colour-2 -0.6) :display :inline-block :margin-right (px 15) :font-weight 500 :cursor :pointer :font-size (px 14)}]
        [:.connection-title:hover {:text-decoration :underline :color (alpha+ leading-colour-2 -0.2)}]
        [:.connection-title.selected {:color leading-colour-2}]]
       [:.connection-list {:margin-top (px 10) :border-top (border 1 :solid (alpha+ black-colour -0.95)) :padding-top (px 10)}
        [:.connection {:display :flex :flex-direction :row :flex-wrap :nowrap :border-radius (px 5) :margin-bottom (px 5) :padding (px 5)}
         [:.connection-controls {:flex (flex 0 0 30) :padding (px 7) :box-sizing :border-box}]
         [:.content-item {:flex (flex 1 1 100) :border-radius (px 5)}]
         [:.content-item:hover {:background-color (alpha+ leading-colour-2 -0.95)}]
         [:.connection-data {:flex (flex 0 0 100) :box-sizing :border-box :padding (px 5) :text-align :center :font-size (px 20)}]]
        [:.connection.odd {:background-color (alpha+ leading-colour-2 -0.97)}]
        [:.content-search-control {:padding (px 0 5) :box-sizing :border-box :margin (px 10 0)}]]
       [:.connection-list.relations
        [:.connection
         [:.connection-data {:flex (flex 0 0 150)}
          [:input {:max-width (px 140) :text-align :center}]]]]]]]
    [:.users-tab {:padding (px 15 0)}
     [:.button {:background-color :transparent :box-shadow :none}]
     [:.user-search-input {:position :relative}
      [:.delete {:position :absolute :right (px 10) :top (px 10)}]
      [:input {:width         (pct 100) :height (px 40) :display :inline-block :outline :none :font-size (px 24) :padding (px 5) :color (alpha+ black-colour -0.3)
               :margin-bottom (px 5) :border-radius (px 5) :border (border 1 :solid (alpha+ black-colour -0.9))
               :font-weight   300 :letter-spacing (px 1) :box-sizing :border-box}]
      ["input::placeholder" {:font-style :italic :font-weight 200 :color (alpha+ black-colour -0.8)
                             :font-size  (px 22) :letter-spacing (px 3) :padding-left (px 3)}]]
     [:.user-search-count {:float :right :margin-right (px 5) :font-weight 500}]
     [:.user-search-visitors-filter
      [:.checkbox {:margin (px 0 15 0 5)}]]
     (let [selected-border (border 1 :solid (alpha+ leading-colour-3 -0.5))]
       [:.user-search-results {:margin-top (px 20) :border (border 1 :solid (alpha+ black-colour -0.95)) :padding (px 5 2) :font-size (px 14)}
        [:.content-item {:border (border 1 :solid (alpha+ black-colour -0.95)) :margin-bottom (px 5) :border-radius (px 3)}]
        [:.content-item:hover {:background-color (alpha+ black-colour -0.97) :margin-bottom (px 5)}]
        [:.user-search-user {:margin         (px 0 0 5 5) :border-left (border 2 :solid :transparent)
                             :padding-bottom (px 2)}
         [:.user-search-user-data {:display :flex :flex-direction :row :flex-wrap :nowrap :min-height (px 30)}
          [:.button.delete {:margin (px 3 0 0 3)}]
          [:.user-search-user-info {:flex (flex 1 1 100) :display :flex :flex-direction :column :flex-wrap :nowrap :margin (px 5)}
           [:.user-search-user-info-e-mail {:flex  (flex 0 0 20) :font-size (px 14) :font-weight 500
                                            :color (alpha+ leading-colour-3 -0.3) :cursor :pointer}]
           [:.user-search-user-info-e-mail:hover {:color leading-colour-3 :text-decoration :underline}]
           (let [underline-border (border 1 :solid (alpha+ black-colour -0.7))
                 name-background (alpha+ black-colour -0.97)
                 name-colour (alpha+ black-colour -0.3)]
             [:.user-search-user-info-name {:flex (flex 0 0 24) :font-size (px 18)}
              [:.user-search-user-info-first-name {:display :inline-block :background-color name-background :border-bottom underline-border :color name-colour}]
              [:.user-search-user-info-last-name {:display :inline-block :background-color name-background :border-bottom underline-border :color name-colour :margin-left (px 5)}]])
           [:.user-search-user-info-roles {:flex (flex 0 0 18) :font-style :italic :color (alpha+ black-colour -0.5) :font-weight 300}]]
          [:.user-search-user-tab {:box-sizing :border-box :padding (px 5) :color (alpha+ black-colour -0.8) :text-align :center
                                   :border     (border 1 :solid :transparent) :margin (px 2 2 0 2)}]
          [:.user-search-user-tab.clickable {:color leading-colour-3}]
          [:.user-search-user-tab.selected {:border-top selected-border :border-right selected-border :border-left selected-border :background-color (alpha+ leading-colour-3 -0.1) :color white-colour :border-radius (px 2 2 0 0)}]]
         [:.user-search-user-view {:padding       (px 5) :border selected-border :margin (px 0 5 5 5)
                                   :border-radius (px 2) :background-color (alpha+ leading-colour-2 -0.97)}
          [:.user-search-item-field
           [:.user-search-content {:display :flex :flex-direction :row :flex-wrap :nowrap}
            [:.user-search-content-toggle {:flex (flex 0 0 30) :display :inline-flex :align-items :center :justify-content :center}
             [:.user-search-content-toggle-button {:display :inline-block}]]
            [:.user-search-content-data {:flex (flex 1 1 100) :overflow :auto}]
            [:.user-search-content-weight {:flex (flex 0 0 50) :display :inline-flex :align-items :center :justify-content :center}]
            [:.user-search-content-level {:flex (flex 0 0 70) :display :inline-flex :align-items :center :justify-content :center}]]
           [:.user-search-items {:margin-left (px 15)}]
           [:.user-search-items.open {:border-left (border 1 :solid (alpha+ leading-colour-2 -0.8))}]]
          [:.user-search-lock
           [:.user-search-lock-e-mail {:font-size (px 20) :font-weight 300 :color (alpha+ black-colour -0.4)}]
           [:.user-search-lock-pin {:color (alpha+ black-colour -0.7)}]]]]
        [:.user-search-user.open {:border-left (border 2 :solid (alpha+ leading-colour-3 -0.7))}]
        [:.user-search-user:hover {:border-left (border 2 :solid (alpha+ leading-colour-3 -0.8))}]
        [:.user-search-user.odd {:background-color (alpha+ leading-colour-2 -0.97)}]])]]]
  )
