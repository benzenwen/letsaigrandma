(ns lag.test-runner
  "Discovers and runs every test namespace under test/. Adding a test file is
  the whole job: any test/**/*_test.clj is picked up without touching this
  file. Exits non-zero on any failure or error so it can serve as a gate."
  (:require [babashka.fs :as fs]
            [clojure.string :as str]
            [clojure.test :as t]))

(defn test-namespaces
  "Namespace symbols for every *_test.clj under root, derived from the path:
  test/lag/site_test.clj -> lag.site-test."
  [root]
  (->> (fs/glob root "**_test.clj")
       (map #(str (fs/relativize root %)))
       (map #(-> % (str/replace #"\.clj$" "") (str/replace "/" ".") (str/replace "_" "-")))
       (map symbol)
       sort
       vec))

(defn run []
  (let [nses (test-namespaces "test")]
    (if (empty? nses)
      (println "no test namespaces under test/ — nothing to run")
      (do (doseq [ns nses] (require ns))
          (let [{:keys [fail error]} (apply t/run-tests nses)]
            (when (pos? (+ (or fail 0) (or error 0)))
              (System/exit 1)))))))
