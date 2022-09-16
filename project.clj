(defproject kuhut "0.0.1"
  :description "kuhut - the culture of learning"
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [ring-server "0.5.0"]
                 [reagent "0.8.1"]
                 [reagent-utils "0.3.2"]
                 [ring "1.7.1"]
                 [ring/ring-defaults "0.3.2"]
                 [hiccup "1.0.5"]
                 [yogthos/config "1.1.1"]
                 [org.clojure/clojurescript "1.10.520" :scope "provided"]
                 [metosin/reitit "0.3.1"]
                 [pez/clerk "1.0.0"]
                 [venantius/accountant "0.2.4" :exclusions [org.clojure/tools.reader]]
                 [cljs-http "0.1.45"]
                 [com.google.guava/guava "21.0"]
                 [com.datomic/datomic-pro "0.9.5544"]
                 [com.cognitect/transit-cljs "0.8.256"]
                 [garden "1.3.6"]
                 [clj-http "3.9.1"]
                 [thi.ng/color "1.3.0"]
                 [clojure.java-time "0.3.2"]
                 [com.taoensso/nippy "2.14.0"]
                 [com.andrewmcveigh/cljs-time "0.5.2"]
                 [lib-noir "0.9.9"]
                 [fogus/ring-edn "0.3.0"]
                 [com.draines/postal "2.0.3"]]
  :plugins [[lein-environ "1.1.0"]
            [lein-cljsbuild "1.1.7"]
            [lein-asset-minifier "0.4.6" :exclusions [org.clojure/clojure]]
            [lein-garden "0.3.0"]
            [lein-ring "0.12.4"]]
  :ring {:handler      kuhut.handler/app
         :uberwar-name "kuhut.war"}
  :min-lein-version "2.5.0"
  :uberjar-name "kuhut.jar"
  :main kuhut.server
  :repositories {"my.datomic.com" {:url      "[REDACTED]"
                                   :username "[REDACTED]"
                                   :password "[REDACTED]"}}
  :clean-targets ^{:protect false} [:target-path
                                    [:cljsbuild :builds :app :compiler :output-dir]
                                    [:cljsbuild :builds :app :compiler :output-to]]
  :source-paths ["src/clj" "src/cljc"]
  :resource-paths ["resources" "target/cljsbuild"]
  :minify-assets [[:css {:source "resources/public/css/kuhut.css"
                         :target "resources/public/css/kuhut.min.css"}]]
  :cljsbuild {:builds {:min {:source-paths ["src/cljs" "src/cljc" "env/prod/cljs"]
                             :compiler     {:output-to        "target/cljsbuild/public/js/app.js"
                                            :output-dir       "target/cljsbuild/public/js"
                                            :source-map       "target/cljsbuild/public/js/app.js.map"
                                            :optimizations    :advanced
                                            :pretty-print     false
                                            :infer-externs    true}}
                       :app {:source-paths ["src/cljs" "src/cljc" "env/dev/cljs"]
                             :figwheel     {:on-jsload "kuhut.core/mount-root"}
                             :compiler     {:main          "kuhut.dev"
                                            :asset-path    "/js/out"
                                            :output-to     "target/cljsbuild/public/js/app.js"
                                            :output-dir    "target/cljsbuild/public/js/out"
                                            :source-map    true
                                            :optimizations :none
                                            :pretty-print  true}}}}
  :garden {:builds [{:id           "kuhut"
                     :source-paths ["src/clj" "src/cljc"]
                     :stylesheet   kuhut.css.classes/kuhut
                     :compiler     {:output-to     "resources/public/css/kuhut.css"
                                    :pretty-print? false}}]}
  :figwheel {:http-server-root "public"
             :server-port      3449
             :nrepl-port       7002
             :nrepl-middleware [cider.piggieback/wrap-cljs-repl]
             :css-dirs         ["resources/public/css"]
             :ring-handler     kuhut.handler/app}
  :profiles {:dev     {:repl-options {:init-ns kuhut.repl}
                       :dependencies [[cider/piggieback "0.4.0"]
                                      [binaryage/devtools "0.9.10"]
                                      [ring/ring-mock "0.3.2"]
                                      [ring/ring-devel "1.7.1"]
                                      [prone "1.6.1"]
                                      [figwheel-sidecar "0.5.18"]
                                      [nrepl "0.6.0"]
                                      [pjstadig/humane-test-output "0.9.0"]
                                      [org.craigandera/dynne "0.4.1"]]
                       :source-paths ["env/dev/clj" "src/tools"]
                       :plugins      [[lein-figwheel "0.5.18"]]
                       :injections   [(require 'pjstadig.humane-test-output)
                                      (pjstadig.humane-test-output/activate!)]
                       :env          {:dev true}}
             :uberjar {:hooks        [minify-assets.plugin/hooks]
                       :source-paths ["env/prod/clj"]
                       :prep-tasks   ["compile" ["cljsbuild" "once" "min"]]
                       :env          {:production true}
                       :aot          :all
                       :omit-source  true}}
  :aliases {"freshdb"  ["run" "-m" "freshdb"]
            "copydb"   ["run" "-m" "copydb"]
            "checkdb"  ["run" "-m" "checkdb"]
            "resample" ["run" "-m" "resample"]}
  )
