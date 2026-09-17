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

(def nav-links
  "The site's four pages, in nav order. Hrefs are root-relative; page-chrome
  prefixes them for pages rendered below the root."
  [{:label "Home" :href "index.html"}
   {:label "Comics" :href "comics/index.html"}
   {:label "Characters" :href "characters/index.html"}
   {:label "About" :href "about/index.html"}])

(defn- prefix
  "\"../\" for a page one directory below the root (:depth 1), else none."
  [depth]
  (if (pos? depth) "../" ""))

(defn- image-or-placeholder
  "The contents of a figure's frame: an <img> when :image is set, otherwise
  the placeholder text and no <img>. prefix adjusts a site-root-relative
  :image for pages rendered below the root."
  [image alt prefix]
  (if image
    [:img {:src (str prefix image) :alt alt}]
    [:p.placeholder-text placeholder-text]))

(defn episode-figure
  "Hiccup <figure> for one episode: a bordered frame (image or placeholder)
  followed by its figcaption. :image is site-root-relative; prefix (default
  none) adjusts it for pages rendered below the root, e.g. \"../\" for
  comics/index.html."
  ([episode] (episode-figure episode ""))
  ([{:keys [image alt caption]} prefix]
   [:figure.episode-figure
    [:div.frame {:class (when-not image "placeholder")}
     (image-or-placeholder image alt prefix)]
    [:figcaption caption]]))

(defn- episode-block [episode prefix]
  [:article.episode
   [:h2 (:title episode)]
   [:time {:datetime (:date episode)} (fmt-date (:date episode))]
   (episode-figure episode prefix)])

(defn- page-chrome
  "Full-document hiccup shared by every page: doctype, head, header (site
  title linking home, premise line), nav, the page body, footer. :depth is
  0 for index.html and 1 for any <dir>/index.html; :own-href is this page's
  root-relative href, used to render its own nav entry without a prefix."
  [{:keys [title depth own-href body]}]
  (let [p (prefix depth)]
    [(h/raw "<!DOCTYPE html>")
     [:html {:lang "en"}
      [:head
       [:meta {:charset "utf-8"}]
       [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
       [:title title]
       [:link {:rel "stylesheet" :href (str p "css/site.css")}]]
      [:body
       [:header
        [:h1 [:a {:href (str p "index.html")} site-title]]
        [:p.premise premise]
        [:nav
         [:ul
          (for [{:keys [label href]} nav-links]
            [:li [:a {:href (if (= href own-href) "index.html" (str p href))} label]])]]]
       [:main body]
       [:footer [:p footer-line]]]]]))

(defn home-page
  "Hiccup for index.html: the latest episode."
  [episodes]
  (page-chrome
   {:title site-title
    :depth 0
    :own-href "index.html"
    :body [:section.latest (episode-block (latest-episode episodes) (prefix 0))]}))

(defn comics-page
  "Hiccup for comics/index.html: every episode, ascending by :number."
  [episodes]
  (page-chrome
   {:title (str "Comics — " site-title)
    :depth 1
    :own-href "comics/index.html"
    :body [:section.comics-list
           (for [episode (sort-by :number episodes)]
             (episode-block episode (prefix 1)))]}))

(defn- character-errors [character]
  (let [{:keys [name role image alt blurb]} character]
    (cond-> []
      (blank-str? name)
      (conj {:name name :field :name :message "name must be a non-blank string"})

      (blank-str? role)
      (conj {:name name :field :role :message "role must be a non-blank string"})

      (blank-str? blurb)
      (conj {:name name :field :blurb :message "blurb must be a non-blank string"})

      (and (some? image) (blank-str? alt))
      (conj {:name name :field :alt :message "alt is required when image is set"}))))

(defn validate-characters
  "Validates a collection of character maps against the schema in
  DESIGN.md. Returns {:ok? true} or {:ok? false :errors [{:name :field
  :message} ...]} collecting every error found, not just the first."
  [characters]
  (let [errors (mapcat character-errors characters)]
    (if (empty? errors)
      {:ok? true}
      {:ok? false :errors (vec errors)})))

(defn character-card
  "Hiccup <article> for one character: name, role, a figure (frame only, no
  figcaption), and the blurb verbatim. prefix adjusts a site-root-relative
  :image for pages rendered below the root."
  [{:keys [name role image alt blurb]} prefix]
  [:article.character
   [:h2 name]
   [:p.role role]
   [:figure.character-figure
    [:div.frame {:class (when-not image "placeholder")}
     (image-or-placeholder image alt prefix)]]
   [:p.blurb blurb]])

(defn characters-page
  "Hiccup for characters/index.html: one card per character, in file order."
  [characters]
  (page-chrome
   {:title (str "Characters — " site-title)
    :depth 1
    :own-href "characters/index.html"
    :body [:section.characters
           (for [character characters]
             (character-card character (prefix 1)))]}))

(defn about-page
  "Hiccup for about/index.html: the About heading, one <p> per paragraph."
  [paragraphs]
  (page-chrome
   {:title (str "About — " site-title)
    :depth 1
    :own-href "about/index.html"
    :body [:section.about
           [:h2 "About"]
           (for [paragraph paragraphs]
             [:p paragraph])]}))

(defn render
  "Renders page-chrome hiccup (a doctype + [:html ...] pair) to an HTML
  string, starting with <!DOCTYPE html>."
  [doc]
  (str (h/html (first doc) (second doc))))
