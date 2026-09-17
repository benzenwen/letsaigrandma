(ns lag.site
  "Pure rendering: episode data in, hiccup/HTML strings out. Knows HTML
  structure; knows nothing about the filesystem. See DESIGN.md."
  (:require [clojure.string :as str]
            [hiccup2.core :as h])
  (:import [java.time LocalDate]
           [java.time.format DateTimeFormatter]))

(def site-title "Let's AI, Grandma")
(def premise "A family, some AI, and one load-bearing comma.")
(def footer-line "© 2026 · drawn by hand, hosted on GitHub Pages")
(def placeholder-text "Not inked yet.")

(def ^:private date-fmt (DateTimeFormatter/ofPattern "MMMM d, yyyy"))
(def ^:private date-re #"^\d{4}-\d{2}-\d{2}$")

(defn- blank-str? [v]
  (or (nil? v) (not (string? v)) (str/blank? v)))

(defn- valid-date? [s]
  (and (string? s)
       (re-matches date-re s)
       (try (LocalDate/parse s) true (catch Exception _ false))))

(defn- duplicate-numbers [episodes]
  (->> episodes
       (map :number)
       frequencies
       (keep (fn [[n c]] (when (> c 1) n)))
       set))

(defn- episode-errors [dup-numbers episode]
  (let [{:keys [number title date image alt caption]} episode]
    (cond-> []
      (not (pos-int? number))
      (conj {:number number :field :number :message "number must be a positive integer"})

      (and (pos-int? number) (contains? dup-numbers number))
      (conj {:number number :field :number :message "number must be unique"})

      (blank-str? title)
      (conj {:number number :field :title :message "title must be a non-blank string"})

      (not (valid-date? date))
      (conj {:number number :field :date :message "date must be a YYYY-MM-DD string"})

      (not (string? caption))
      (conj {:number number :field :caption :message "caption must be a string"})

      (and (some? image) (blank-str? alt))
      (conj {:number number :field :alt :message "alt is required when image is set"}))))

(defn validate-episodes
  "Validates a collection of episode maps against the schema in DESIGN.md.
  Returns {:ok? true} or {:ok? false :errors [{:number :field :message} ...]}
  collecting every error found, not just the first."
  [episodes]
  (let [dups (duplicate-numbers episodes)
        errors (mapcat #(episode-errors dups %) episodes)]
    (if (empty? errors)
      {:ok? true}
      {:ok? false :errors (vec errors)})))

(defn latest-episode
  "The episode with the highest :number."
  [episodes]
  (apply max-key :number episodes))

(defn- fmt-date [iso-str]
  (.format (LocalDate/parse iso-str) date-fmt))

(defn episode-figure
  "Hiccup <figure> for one episode: image + figcaption when :image is set,
  otherwise a placeholder frame with no <img>. :image is site-root-relative;
  image-prefix (default none) adjusts it for pages rendered below the root,
  e.g. \"../\" for comics/index.html."
  ([episode] (episode-figure episode ""))
  ([{:keys [image alt caption]} image-prefix]
   (if image
     [:figure.episode-figure
      [:img {:src (str image-prefix image) :alt alt}]
      [:figcaption caption]]
     [:figure.episode-figure.placeholder
      [:p.placeholder-text placeholder-text]
      [:figcaption caption]])))

(defn- episode-block [episode image-prefix]
  [:article.episode
   [:h2 (:title episode)]
   [:time {:datetime (:date episode)} (fmt-date (:date episode))]
   (episode-figure episode image-prefix)])

(defn- page-chrome
  "Full-document hiccup shared by every page: doctype, head, header (site
  title linking home, premise line), nav, the page body, footer."
  [{:keys [title css-href home-href nav-links body]}]
  [(h/raw "<!DOCTYPE html>")
   [:html {:lang "en"}
    [:head
     [:meta {:charset "utf-8"}]
     [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
     [:title title]
     [:link {:rel "stylesheet" :href css-href}]]
    [:body
     [:header
      [:h1 [:a {:href home-href} site-title]]
      [:p.premise premise]
      [:nav
       [:ul
        (for [{:keys [label href]} nav-links]
          [:li [:a {:href href} label]])]]]
     [:main body]
     [:footer [:p footer-line]]]]])

(defn home-page
  "Hiccup for index.html: the latest episode."
  [episodes]
  (page-chrome
   {:title site-title
    :css-href "css/site.css"
    :home-href "index.html"
    :nav-links [{:label "Home" :href "index.html"}
                {:label "Comics" :href "comics/index.html"}]
    :body [:section.latest (episode-block (latest-episode episodes) "")]}))

(defn comics-page
  "Hiccup for comics/index.html: every episode, ascending by :number."
  [episodes]
  (page-chrome
   {:title (str "Comics — " site-title)
    :css-href "../css/site.css"
    :home-href "../index.html"
    :nav-links [{:label "Home" :href "../index.html"}
                {:label "Comics" :href "index.html"}]
    :body [:section.comics-list
           (for [episode (sort-by :number episodes)]
             (episode-block episode "../"))]}))

(defn render
  "Renders page-chrome hiccup (a doctype + [:html ...] pair) to an HTML
  string, starting with <!DOCTYPE html>."
  [doc]
  (str (h/html (first doc) (second doc))))
