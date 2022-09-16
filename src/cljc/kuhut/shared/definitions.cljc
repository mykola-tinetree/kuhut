(ns kuhut.shared.definitions
  (:require [thi.ng.color.core :as col])
  )

(def ^:const FAVICON_RESOURCE_ID "[REDACTED]")
(def ^:const HUMAN_RIGHTS_RESOURCE_ID "[REDACTED]")
(def ^:const GOALS_OF_KUHUT_RESOURCE_ID "[REDACTED]")
(def ^:const TF_IDF_SEARCH_STATS_RESOURCE_ID "[REDACTED]")
(def ^:const LOADING_RESOURCE_ID "[REDACTED]")
(def ^:const PRINCIPLES_LIFELONG_RESOURCE_ID "[REDACTED]")
(def ^:const PRINCIPLES_SAFE_RESOURCE_ID "[REDACTED]")
(def ^:const PRINCIPLES_HELPOTHERS_RESOURCE_ID "[REDACTED]")
(def ^:const PRINCIPLES_AI_RESOURCE_ID "[REDACTED]")
(def ^:const PRINCIPLES_EQUALITY_RESOURCE_ID "[REDACTED]")
(def ^:const DEFAULT_TOPIC "[REDACTED]")

(defn keyname [k] (when k (str (when-let [n (namespace k)] (str n "/")) (name k))))
(defn tag-and-flags [k] (let [[tag & flags] (map keyword (clojure.string/split (name k) #"\."))] [tag (set flags)]))

(defn px [& x] (clojure.string/join " " (map #(str % "px") x)))
(defn em [& x] (clojure.string/join " " (map #(str % "em") x)))
(defn vh [& x] (clojure.string/join " " (map #(str % "vh") x)))
(defn pct [& x] (clojure.string/join " " (map #(str % "%") x)))
(defn border [w t c] (str (px w) " " (keyname t) " " (if (keyword? c) (keyname c) c)))
(defn shadow [x y s c] (str (px x) " " (px y) " " (px s) " " (if (keyword? c) (keyname c) c)))
(defn linear-gradient [a & c] (str "linear-gradient(" a "deg," (clojure.string/join "," c) ")"))
(defn flex [a b c] (str a " " b " " (px c)))
(defn alpha+ [c v] (-> c col/css (col/adjust-alpha v) col/as-css deref))
(defn brightness+ [c v] (-> c col/css (col/adjust-brightness v) col/as-css deref))
(defn translate [x y] (str "translate(" x "," y ")"))
(defn saturation+ [c v] (-> c col/css (col/adjust-saturation v) col/as-css deref))
(defn hsl [h s l] (str "hsl(" h "," s "%," l "%)"))
(defn greyscale [g] (str "rgb(" g "," g "," g ")"))

(def ^:const primary-font-family "Roboto,sans-serif")
(def ^:const code-font-family "Menlo,monospace")
(def ^:const leading-colour-0 "#cd6e13")
(def ^:const leading-colour-1 "#cd9313")
(def ^:const leading-colour-2 "#1b358b")
(def ^:const leading-colour-3 "#0e6d7e")
(def ^:const correct-colour "#4dc167")
(def ^:const incorrect-colour "#ff4444")
(def ^:const white-colour "#ffffff")
(def ^:const black-colour "#000000")
