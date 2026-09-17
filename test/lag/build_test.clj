(ns lag.build-test
  (:require [babashka.fs :as fs]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [lag.build :as build]))

(def valid-episodes-edn
  "[{:number 1
     :title \"Because I know Minecraft\"
     :date \"2026-09-20\"
     :image nil
     :alt nil
     :caption \"Joey catches the AI being wrong about Minecraft. Nana asks how he knew.\"}
    {:number 2
     :title \"The Second One\"
     :date \"2026-09-27\"
     :image \"comics/002-second.png\"
     :alt \"Nana and Jane looking at a laptop, unimpressed.\"
     :caption \"Jane is not impressed.\"}]")

(def invalid-episodes-edn
  "[{:number 3
     :title \"Missing Alt\"
     :date \"2026-10-01\"
     :image \"comics/003.png\"
     :alt \"\"
     :caption \"A caption.\"}]")

(defn- write-episodes! [root content]
  (fs/create-dirs (fs/path root "content"))
  (spit (str (fs/path root "content" "episodes.edn")) content))

(deftest build-writes-expected-files-into-temp-root
  (let [root (str (fs/create-temp-dir))]
    (write-episodes! root valid-episodes-edn)
    (let [result (build/build! {:root root})]
      (is (= ["index.html" "comics/index.html" "css/site.css"] (:written result)))
      (is (fs/exists? (fs/path root "index.html")))
      (is (fs/exists? (fs/path root "comics" "index.html")))
      (is (fs/exists? (fs/path root "css" "site.css")))
      (let [home (slurp (str (fs/path root "index.html")))
            comics (slurp (str (fs/path root "comics" "index.html")))]
        (is (str/includes? home "The Second One"))
        (is (str/includes? home "September 27, 2026"))
        (is (str/includes? home "Jane is not impressed."))
        (is (str/includes? comics "Because I know Minecraft"))
        (is (str/includes? comics "Not inked yet."))
        (is (str/includes? comics "The Second One"))))))

(deftest build-throws-and-writes-nothing-on-invalid-episodes
  (let [root (str (fs/create-temp-dir))]
    (write-episodes! root invalid-episodes-edn)
    (testing "throws ex-info with :type :lag/invalid-episodes"
      (try
        (build/build! {:root root})
        (is false "expected build! to throw")
        (catch clojure.lang.ExceptionInfo e
          (is (= :lag/invalid-episodes (:type (ex-data e))))
          (is (some #(and (= 3 (:number %)) (= :alt (:field %))) (:errors (ex-data e)))))))
    (testing "nothing was written"
      (is (not (fs/exists? (fs/path root "index.html"))))
      (is (not (fs/exists? (fs/path root "comics" "index.html"))))
      (is (not (fs/exists? (fs/path root "css" "site.css")))))))

(deftest build-is-deterministic
  (let [root (str (fs/create-temp-dir))]
    (write-episodes! root valid-episodes-edn)
    (build/build! {:root root})
    (let [first-run {:index (slurp (str (fs/path root "index.html")))
                      :comics (slurp (str (fs/path root "comics" "index.html")))
                      :css (slurp (str (fs/path root "css" "site.css")))}]
      (build/build! {:root root})
      (let [second-run {:index (slurp (str (fs/path root "index.html")))
                         :comics (slurp (str (fs/path root "comics" "index.html")))
                         :css (slurp (str (fs/path root "css" "site.css")))}]
        (is (= first-run second-run))))))
