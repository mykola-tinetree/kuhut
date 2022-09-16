(ns kuhut.router
  (:require [reitit.frontend :as reitit])
  )

(set! *warn-on-infer* true)

(def router
  (reitit/router
    [["/" :home]
     ["/profile" :profile]
     ["/principles" :principles]
     ["/cookie-policy" :cookie-policy]
     ["/studio" :studio]]))

(defn path-for
  [route & [params]]
  (if params
    (:path (reitit/match-by-name router route params))
    (:path (reitit/match-by-name router route)))
  )
