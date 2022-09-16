(ns kuhut.routes.executor
  (:require [clojure.java.shell :refer [sh]])
  )

(defn execute
  [language code context]
  (try
    (-> (sh "aws" "lambda" "invoke" "--function-name" "kuhut-coderunner" "--payload" (pr-str (pr-str {:language language :code code :context context})) "/dev/stdout" :env {"AWS_DEFAULT_REGION" "[REDACTED]" "AWS_ACCESS_KEY_ID" "[REDACTED]" "AWS_SECRET_ACCESS_KEY" "[REDACTED]"})
        :out read-string read-string)
    (catch Exception _ nil))
  )
