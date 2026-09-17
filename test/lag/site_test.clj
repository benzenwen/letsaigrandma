(ns lag.site-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [hiccup2.core :as h]
            [lag.site :as site]))

(def ep1
  {:number 1
   :title "Because I know Minecraft"
   :date "2026-09-20"
   :image nil
   :alt nil
   :caption "Joey catches the AI being wrong about Minecraft. Nana asks how he knew."})

(def ep2
  {:number 2
   :title "The Second One"
   :date "2026-09-27"
   :image "comics/002-second.png"
   :alt "Nana and Jane looking at a laptop, unimpressed."
   :caption "Jane is not impressed."})

(deftest validate-episodes-valid
  (is (= {:ok? true} (site/validate-episodes [ep1 ep2]))))

(deftest validate-episodes-missing-alt
  (testing "image set, alt blank"
    (let [bad (assoc ep2 :number 3 :alt "")
          result (site/validate-episodes [ep1 bad])]
      (is (false? (:ok? result)))
      (is (some #(and (= 3 (:number %)) (= :alt (:field %))) (:errors result)))))
  (testing "image set, alt missing entirely"
    (let [bad (dissoc (assoc ep2 :number 4) :alt)
          result (site/validate-episodes [bad])]
      (is (false? (:ok? result)))
      (is (some #(and (= 4 (:number %)) (= :alt (:field %))) (:errors result))))))

(deftest validate-episodes-bad-date
  (let [bad (assoc ep1 :number 4 :date "Sept 4")
        result (site/validate-episodes [bad])]
    (is (false? (:ok? result)))
    (is (some #(and (= 4 (:number %)) (= :date (:field %))) (:errors result)))))

(deftest validate-episodes-collects-all-errors
  (let [bad (assoc ep1 :number 5 :date "not-a-date" :title "")
        result (site/validate-episodes [bad])]
    (is (false? (:ok? result)))
    (is (= #{:title :date} (set (map :field (:errors result)))))))

(deftest latest-episode-selects-highest-number
  (is (= 2 (:number (site/latest-episode [ep1 ep2]))))
  (is (= 2 (:number (site/latest-episode [ep2 ep1])))))

(deftest comics-page-orders-ascending
  (let [rendered (site/render (site/comics-page [ep2 ep1]))
        i1 (.indexOf rendered "Because I know Minecraft")
        i2 (.indexOf rendered "The Second One")]
    (is (pos? i1))
    (is (pos? i2))
    (is (< i1 i2))))

(deftest episode-figure-placeholder-has-no-img
  (let [rendered (str (h/html (site/episode-figure ep1)))]
    (is (str/includes? rendered "Not inked yet."))
    (is (not (str/includes? rendered "<img")))))

(deftest episode-figure-with-image-has-img-and-no-placeholder-text
  (let [rendered (str (h/html (site/episode-figure ep2)))]
    (is (str/includes? rendered "<img"))
    (is (str/includes? rendered "comics/002-second.png"))
    (is (not (str/includes? rendered "Not inked yet.")))))

(defn- attr-values [rendered attr]
  (map second (re-seq (re-pattern (str attr "=\"([^\"]*)\"")) rendered)))

(deftest links-are-relative-on-home-page
  (let [rendered (site/render (site/home-page [ep1 ep2]))]
    (doseq [v (concat (attr-values rendered "href") (attr-values rendered "src"))]
      (is (not (str/starts-with? v "/")) v)
      (is (not (str/starts-with? v "http")) v))))

(deftest comics-page-image-src-is-depth-adjusted
  (let [rendered (site/render (site/comics-page [ep1 ep2]))]
    (is (str/includes? rendered "src=\"../comics/002-second.png\""))
    (is (not (str/includes? rendered "src=\"comics/002-second.png\"")))))

(deftest home-page-image-src-is-root-relative
  (let [rendered (site/render (site/home-page [ep2]))]
    (is (str/includes? rendered "src=\"comics/002-second.png\""))))

(deftest links-are-relative-on-comics-page
  (let [rendered (site/render (site/comics-page [ep1 ep2]))]
    (doseq [v (concat (attr-values rendered "href") (attr-values rendered "src"))]
      (is (not (str/starts-with? v "/")) v)
      (is (not (str/starts-with? v "http")) v))))

(deftest home-page-has-doctype-lang-viewport-title
  (let [rendered (site/render (site/home-page [ep1]))]
    (is (str/starts-with? rendered "<!DOCTYPE html>"))
    (is (str/includes? rendered "<html lang=\"en\">"))
    (is (str/includes? rendered "viewport"))
    (is (str/includes? rendered "<title>"))))

(deftest comics-page-has-doctype-lang-viewport-title
  (let [rendered (site/render (site/comics-page [ep1]))]
    (is (str/starts-with? rendered "<!DOCTYPE html>"))
    (is (str/includes? rendered "<html lang=\"en\">"))
    (is (str/includes? rendered "viewport"))
    (is (str/includes? rendered "<title>"))))

(deftest pages-have-no-script-tags
  (is (not (str/includes? (site/render (site/home-page [ep1])) "<script")))
  (is (not (str/includes? (site/render (site/comics-page [ep1 ep2])) "<script"))))

(deftest home-page-shows-latest-episode-details
  (let [rendered (site/render (site/home-page [ep1 ep2]))]
    (is (str/includes? rendered "The Second One"))
    (is (str/includes? rendered "<time datetime=\"2026-09-27\">September 27, 2026</time>"))
    (is (str/includes? rendered "Jane is not impressed."))))
