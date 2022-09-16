(ns kuhut.middleware
  (:require
   [ring.middleware.defaults :refer [site-defaults wrap-defaults]]))

(def middleware
  [#(wrap-defaults % (assoc-in site-defaults [:security :anti-forgery] false))])
