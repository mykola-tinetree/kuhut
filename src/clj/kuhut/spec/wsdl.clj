(ns kuhut.spec.wsdl
  (:require [clojure.spec.alpha :as s]
            [kuhut.shared.definitions :refer [tag-and-flags]])
  )

(def id-regex #"^[a-z][a-z0-9-]*$")
(def size-regex #"^(auto|0)$|^[+-]?[0-9]+.?([0-9]+)?(px|em|ex|%|in|cm|mm|pt|pc)$")
(def colour-regex #"^(#[0-9a-f]{3}|#(?:[0-9a-f]{2}){2,4}|(rgb|hsl)a?\((-?\d+%?[,\s]+){2,3}\s*[\d\.]+%?\))$")
(def url-regex #"^(?:(?:https?|ftp)://)(?:\S+(?::\S*)?@)?(?:(?!10(?:\.\d{1,3}){3})(?!127(?:\.\d{1,3}){3})(?!169\.254(?:\.\d{1,3}){2})(?!192\.168(?:\.\d{1,3}){2})(?!172\.(?:1[6-9]|2\d|3[0-1])(?:\.\d{1,3}){2})(?:[1-9]\d?|1\d\d|2[01]\d|22[0-3])(?:\.(?:1?\d{1,2}|2[0-4]\d|25[0-5])){2}(?:\.(?:[1-9]\d?|1\d\d|2[0-4]\d|25[0-4]))|(?:(?:[a-z\x{00a1}-\x{ffff}0-9]+-?)*[a-z\x{00a1}-\x{ffff}0-9]+)(?:\.(?:[a-z\x{00a1}-\x{ffff}0-9]+-?)*[a-z\x{00a1}-\x{ffff}0-9]+)*(?:\.(?:[a-z\x{00a1}-\x{ffff}]{2,})))(?::\d{2,5})?(?:/[^\s]*)?$")

(defn- tag-and-flags-fit
  [k correct-tag allowed-flags]
  (let [[tag flags] (tag-and-flags k)]
    (and (= tag correct-tag)
         (every? allowed-flags (map keyword flags))))
  )

(s/def :kuhut/size (s/or :number (s/and number? #(not (neg? %)))
                         :string (s/and string? #(re-matches size-regex %))))

(s/def :kuhut/colour (s/and string? #(re-matches colour-regex %)))

(s/def :kuhut/url (s/and string? #(re-matches url-regex %)))

(s/def :kuhut/id (s/and string? #(< (count %) 100) #(re-matches id-regex %)))

(s/def :kuhut/uuid uuid?)

(s/def :kuhut/key (s/and keyword? #(re-matches id-regex (name %))))

(s/def :kuhut/tags (s/coll-of :kuhut/key :kind set?))

(s/def :kuhut/scale (s/and double? #(<= 0 % 1)))
(s/def :kuhut/block (complement neg-int?))

(s/def :kuhut/start (s/and number? (complement neg?)))

(s/def :kuhut/font-size :kuhut/size)

(s/def :kuhut/text-align #{:left :right :center :justify})

(s/def :kuhut/position #{:left :right :middle})

(s/def :kuhut/text-colour :kuhut/colour)
(s/def :kuhut/background-colour :kuhut/colour)

(s/def :kuhut/width :kuhut/size)
(s/def :kuhut/height :kuhut/size)
(s/def :kuhut/padding :kuhut/size)
(s/def :kuhut/border-radius :kuhut/size)

(s/def :kuhut/assessment #{:correct :incorrect})

(s/def :kuhut.common/properties (s/keys :opt-un [:kuhut/id
                                                 :kuhut/tags
                                                 :kuhut/tooltip
                                                 :kuhut/font-size
                                                 :kuhut/text-colour
                                                 :kuhut/background-colour
                                                 :kuhut/padding
                                                 :kuhut/border-radius]))

(s/def :kuhut.common/content (s/or :number number?
                                   :string string?
                                   :char char?
                                   :boolean boolean?
                                   :uuid uuid?
                                   :inst inst?
                                   :keyword keyword?))

(s/def :kuhut.space/type #{:vertical :horizontal})
(s/def :kuhut.space/element (s/cat :tag #{:space}
                                   :properties (s/? (s/keys :opt-un [:kuhut.space/type
                                                                     :kuhut/size]))))

(s/def :kuhut.text/preview :kuhut/uuid)
(s/def :kuhut.text/solution string?)
(s/def :kuhut.text/element (s/cat :tag #(tag-and-flags-fit % :text #{:bold :italic :underline :strikethrough :superscript :subscript})
                                  :properties (s/? (s/merge :kuhut.common/properties
                                                            (s/keys :opt-un [:kuhut.text/preview
                                                                             :kuhut.text/solution
                                                                             :kuhut/assessment
                                                                             :kuhut/key])))
                                  :content (s/* :kuhut.common/content)))

(s/def :kuhut/content (s/alt :common :kuhut.common/content
                             :element (s/or :text :kuhut.text/element
                                            :paragraph :kuhut.paragraph/element
                                            :space :kuhut.space/element
                                            :options :kuhut.options/element
                                            :table :kuhut.table/element
                                            :selector :kuhut.selector/element
                                            :list :kuhut.list/element
                                            :definitions :kuhut.definitions/element
                                            :image :kuhut.image/element
                                            :audio :kuhut.audio/element
                                            :reference :kuhut.reference/element
                                            :citation :kuhut.citation/element
                                            :link :kuhut.link/element
                                            :code :kuhut.code/element
                                            :math :kuhut.math/element
                                            :canvas :kuhut.canvas/element)))

(s/def :kuhut.link/address :kuhut/url)
(s/def :kuhut.link/element (s/cat :tag #{:link}
                                  :properties (s/merge :kuhut.paragraph/properties
                                                       (s/keys :req-un [:kuhut.link/address]))
                                  :content (s/* :kuhut/content))) ;; TODO: really everything?

(s/def :kuhut.reference/element (s/cat :tag #{:reference}
                                       :properties (s/? :kuhut.common/properties)
                                       :content :kuhut/id))

(s/def :kuhut/extras (s/alt :common :kuhut.common/content
                            :text :kuhut.text/element
                            :paragraph :kuhut.paragraph/element))

(s/def :kuhut/title :kuhut/extras)

(s/def :kuhut.citation/reference :kuhut/extras)
(s/def :kuhut.citation/element (s/cat :tag #{:citation}
                                      :properties (s/? (s/merge :kuhut.paragraph/properties
                                                                (s/keys :opt-un [:kuhut.citation/reference])))
                                      :content (s/* :kuhut/content)))

(s/def :kuhut.canvas/element (s/cat :tag #{:canvas}
                                    :properties (s/merge :kuhut.common/properties
                                                         (s/keys :req-un [:kuhut/width
                                                                          :kuhut/height]
                                                                 :opt-un [:kuhut/assessment
                                                                          :kuhut/key]))
                                    ;:content #(or (= % true) (not= % true))
                                    ))                      ;; this is just SVG content - no checking of that for now

(s/def :kuhut.area/x :kuhut/size)
(s/def :kuhut.area/y :kuhut/size)
(s/def :kuhut.area/pointer #{:top-left :top :top-right :right-top :right :right-bottom :bottom-right :bottom :bottom-left :left-bottom :left :left-top})
(s/def :kuhut.area/pointer-colour :kuhut/colour)
(s/def :kuhut.area/element (s/cat :tag #{:area}
                                  :properties (s/? (s/merge :kuhut.common/properties
                                                            (s/keys :opt-un [:kuhut.area/x
                                                                             :kuhut.area/y
                                                                             :kuhut.area/pointer
                                                                             :kuhut.area/pointer-colour])))
                                  :content (s/* :kuhut/content)))

(s/def :kuhut.image/resource :kuhut/uuid)
(s/def :kuhut.image/element (s/cat :tag #{:image}
                                   :properties (s/? (s/merge :kuhut.common/properties
                                                             (s/keys :req-un [:kuhut.image/resource]
                                                                     :opt-un [:kuhut/position
                                                                              :kuhut/title
                                                                              :kuhut/scale])))
                                   :content (s/* (s/spec :kuhut.area/element))))

(s/def :kuhut.audio/title :kuhut/extras)
(s/def :kuhut.audio/type #{:mpeg})
(s/def :kuhut.audio/element (s/cat :tag #{:audio}
                                   :properties (s/? (s/merge :kuhut.common/properties
                                                             (s/keys :opt-un [:kuhut.audio/title
                                                                              :kuhut.audio/type])))
                                   :content :kuhut/uuid))

(s/def :kuhut.code/language #{:clojure :python})
(s/def :kuhut.code/value (s/or :value :kuhut.common/content
                               :lines (s/coll-of :kuhut.common/content :kind vector?)))
(s/def :kuhut.code.executor/prefix :kuhut.code/value)
(s/def :kuhut.code.executor/indentation (complement neg-int?))
(s/def :kuhut.code.executor/suffix :kuhut.code/value)
(s/def :kuhut.code.executor/print :kuhut.code/value)
(s/def :kuhut.code/executor (s/keys :opt-un [:kuhut.code.executor/prefix
                                             :kuhut.code.executor/indentation
                                             :kuhut.code.executor/suffix
                                             :kuhut.code.executor/print]))
(s/def :kuhut.code/output #{nil :hidden :visible})
(s/def :kuhut.code.answer/code :kuhut.code/value)
(s/def :kuhut.code.answer/output :kuhut.code/value)
(s/def :kuhut.code/solution (s/keys :opt-un [:kuhut.code.answer/code
                                             :kuhut.code.answer/output]))
(s/def :kuhut.code/answer (s/keys :opt-un [:kuhut.code.answer/code
                                           :kuhut.code.answer/output]))
(s/def :kuhut.code/element (s/cat :tag #(tag-and-flags-fit % :code #{:inline :toggle-line-numbers})
                                  :properties (s/* (s/keys :opt-un [:kuhut.code/language
                                                                    :kuhut.code/answer
                                                                    :kuhut.code/solution
                                                                    :kuhut.code/executor
                                                                    :kuhut.code/output
                                                                    :kuhut/assessment
                                                                    :kuhut/font-size
                                                                    :kuhut/height
                                                                    :kuhut/key]))
                                  :content (s/* :kuhut.common/content)))

(s/def :kuhut.math/element (s/cat :tag #(tag-and-flags-fit % :math #{:inline})
                                  :properties (s/? :kuhut.common/properties)
                                  :content string?))

(s/def :kuhut.item/element (s/cat :tag #{:item}
                                  :properties (s/? (s/merge :kuhut.common/properties))
                                  :content (s/* :kuhut/content)))

(s/def :kuhut.list/type #{:numbered :bulleted})
(s/def :kuhut.list/element (s/cat :tag #{:list}
                                  :properties (s/? (s/& (s/merge :kuhut.common/properties
                                                                 (s/keys :opt-un [:kuhut.list/type
                                                                                  :kuhut/start]))
                                                        #(or (not (:start %)) (= (:type %) :numbered))))
                                  :content (s/* (s/spec :kuhut.item/element))))

(s/def :kuhut.option/value any?)
(s/def :kuhut.option/element (s/cat :tag #(tag-and-flags-fit % :option #{:disabled})
                                    :properties (s/merge :kuhut.common/properties
                                                         (s/keys :req-un [:kuhut.option/value]))
                                    :content (s/* :kuhut/content)))

(s/def :kuhut.options/type #{:multiple-choice :single-choice})
(s/def :kuhut.options/style #{:tiles})
(s/def :kuhut.options/selection (s/coll-of :kuhut.option/value :kind set?))
(s/def :kuhut.options/solution (s/coll-of :kuhut.option/value :kind set?))
(s/def :kuhut.options/element (s/cat :tag #{:options}
                                     :properties (s/? (s/merge :kuhut.common/properties
                                                               (s/keys :opt-un [:kuhut/key
                                                                                :kuhut.options/type
                                                                                :kuhut.options/style
                                                                                :kuhut.options/selection
                                                                                :kuhut.options/solution
                                                                                :kuhut/scale])))
                                     :content (s/* (s/spec :kuhut.option/element))))

(s/def :kuhut.row/element (s/cat :tag #(tag-and-flags-fit % :row #{:header})
                                 :properties :kuhut.common/properties
                                 :content (s/* :kuhut/content)))

(s/def :kuhut.table/element (s/cat :tag #(tag-and-flags-fit % :table #{:borderless})
                                   :properties (s/? (s/merge :kuhut.common/properties
                                                             (s/keys :opt-un [:kuhut/position
                                                                              :kuhut/title
                                                                              :kuhut/scale
                                                                              :kuhut/block])))
                                   :content (s/* (s/spec :kuhut.row/element))))

(s/def :kuhut.selection/element (s/cat :tag #{:selection}
                                       :properties (s/keys :req-un [:kuhut.option/value])
                                       :content :kuhut.common/content))

(s/def :kuhut.selector/selection :kuhut.option/value)
(s/def :kuhut.selector/solution :kuhut.option/value)
(s/def :kuhut.selector/element (s/cat :tag #{:selector}
                                      :properties (s/? (s/merge :kuhut.common/properties
                                                                (s/keys :opt-un [:kuhut/key
                                                                                 :kuhut.selector/selection
                                                                                 :kuhut.selector/solution])))
                                      :content (s/* (s/spec :kuhut.selection/element))))

(s/def :kuhut.definition/title :kuhut/content)
(s/def :kuhut.definition/element (s/cat :tag #{:definition}
                                        :properties (s/? (s/merge :kuhut.common/properties
                                                                  (s/keys :req-un [:kuhut.definition/title])))
                                        :content (s/* :kuhut/content)))

(s/def :kuhut.definitions/level #{0 1 2})
(s/def :kuhut.definitions/element (s/cat :tag #{:definitions}
                                         :properties (s/? (s/merge :kuhut.common/properties
                                                                   (s/keys :req-un [:kuhut.definitions/level])))
                                         :content (s/* (s/spec :kuhut.definition/element))))


(s/def :kuhut.paragraph/properties (s/merge :kuhut.common/properties
                                            (s/keys :opt-un [:kuhut/text-align])))

(s/def :kuhut.paragraph/element (s/cat :tag #{:paragraph}
                                       :properties (s/? :kuhut.paragraph/properties)
                                       :content (s/* :kuhut/content)))

(s/def :kuhut/tooltip :kuhut/extras)

(s/def :kuhut.worksheet/references (s/map-of :kuhut/id :kuhut/extras))
(s/def :kuhut.worksheet/element (s/cat :tag #{:worksheet}
                                       :properties (s/? (s/merge :kuhut.common/properties
                                                                 (s/keys :opt-un [:kuhut/width
                                                                                  :kuhut.worksheet/references])))
                                       :content (s/* :kuhut/content)))

;; TODO: add timed worksheets, e.g. for timed questions or for showing the results of an answer for 5 seconds etc.
;; TODO: add full template, exercise, worksheet definitions
