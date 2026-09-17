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

(def valid-characters-edn
  "[{:name \"Nana\" :role \"Grandma\" :image nil :alt nil :blurb \"Not impressed.\"}
    {:name \"Joey\" :role \"Eleven\" :image \"images/characters/joey.png\"
     :alt \"Joey grinning at a tablet.\" :blurb \"Minecraft first.\"}]")

(def invalid-characters-edn
  "[{:name \"Joey\" :role \"Eleven\" :image \"images/characters/joey.png\"
     :alt \"\" :blurb \"Minecraft first.\"}]")

(def valid-about-edn
  "[\"First paragraph.\" \"Second paragraph.\"]")

(defn- write-episodes! [root content]
  (fs/create-dirs (fs/path root "content"))
  (spit (str (fs/path root "content" "episodes.edn")) content))

(defn- write-content!
  ([root] (write-content! root valid-episodes-edn valid-characters-edn valid-about-edn))
  ([root episodes-content characters-content about-content]
   (fs/create-dirs (fs/path root "content"))
   (spit (str (fs/path root "content" "episodes.edn")) episodes-content)
   (spit (str (fs/path root "content" "characters.edn")) characters-content)
   (spit (str (fs/path root "content" "about.edn")) about-content)))

(defn- no-output-written? [root]
  (and (not (fs/exists? (fs/path root "index.html")))
       (not (fs/exists? (fs/path root "comics" "index.html")))
       (not (fs/exists? (fs/path root "characters" "index.html")))
       (not (fs/exists? (fs/path root "about" "index.html")))
       (not (fs/exists? (fs/path root "css")))))

(deftest build-writes-expected-files-into-temp-root
  (let [root (str (fs/create-temp-dir))]
    (write-content! root)
    (let [result (build/build! {:root root})]
      (is (= ["index.html" "comics/index.html" "characters/index.html" "about/index.html"]
             (:written result)))
      (is (fs/exists? (fs/path root "index.html")))
      (is (fs/exists? (fs/path root "comics" "index.html")))
      (is (fs/exists? (fs/path root "characters" "index.html")))
      (is (fs/exists? (fs/path root "about" "index.html")))
      (is (not (fs/exists? (fs/path root "css"))) "css/site.css is hand-authored, not written by build!")
      (let [home (slurp (str (fs/path root "index.html")))
            comics (slurp (str (fs/path root "comics" "index.html")))
            characters (slurp (str (fs/path root "characters" "index.html")))
            about (slurp (str (fs/path root "about" "index.html")))]
        (is (str/includes? home "The Second One"))
        (is (str/includes? home "September 27, 2026"))
        (is (str/includes? home "Jane is not impressed."))
        (is (str/includes? comics "Because I know Minecraft"))
        (is (str/includes? comics "Not inked yet."))
        (is (str/includes? comics "The Second One"))
        (is (str/includes? characters "Nana"))
        (is (str/includes? characters "Joey"))
        (is (str/includes? about "About"))
        (is (str/includes? about "First paragraph."))))))

(deftest build-throws-and-writes-nothing-on-invalid-episodes
  (let [root (str (fs/create-temp-dir))]
    (write-content! root invalid-episodes-edn valid-characters-edn valid-about-edn)
    (testing "throws ex-info with :type :lag/invalid-episodes"
      (try
        (build/build! {:root root})
        (is false "expected build! to throw")
        (catch clojure.lang.ExceptionInfo e
          (is (= :lag/invalid-episodes (:type (ex-data e))))
          (is (some #(and (= 3 (:number %)) (= :alt (:field %))) (:errors (ex-data e)))))))
    (testing "nothing was written"
      (is (no-output-written? root)))))

(deftest build-throws-and-writes-nothing-on-invalid-characters
  (let [root (str (fs/create-temp-dir))]
    (write-content! root valid-episodes-edn invalid-characters-edn valid-about-edn)
    (testing "throws ex-info with :type :lag/invalid-characters"
      (try
        (build/build! {:root root})
        (is false "expected build! to throw")
        (catch clojure.lang.ExceptionInfo e
          (is (= :lag/invalid-characters (:type (ex-data e))))
          (is (some #(and (= "Joey" (:name %)) (= :alt (:field %))) (:errors (ex-data e)))))))
    (testing "nothing was written"
      (is (no-output-written? root)))))

(deftest build-is-deterministic
  (let [root (str (fs/create-temp-dir))]
    (write-content! root)
    (build/build! {:root root})
    (let [first-run {:index (slurp (str (fs/path root "index.html")))
                      :comics (slurp (str (fs/path root "comics" "index.html")))
                      :characters (slurp (str (fs/path root "characters" "index.html")))
                      :about (slurp (str (fs/path root "about" "index.html")))}]
      (build/build! {:root root})
      (let [second-run {:index (slurp (str (fs/path root "index.html")))
                         :comics (slurp (str (fs/path root "comics" "index.html")))
                         :characters (slurp (str (fs/path root "characters" "index.html")))
                         :about (slurp (str (fs/path root "about" "index.html")))}]
        (is (= first-run second-run))))))
