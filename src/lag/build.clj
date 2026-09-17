(ns lag.build
  "The only namespace that touches the filesystem: reads content/*.edn,
  validates it, and writes the rendered site. See DESIGN.md."
  (:require [babashka.fs :as fs]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [lag.site :as site]))

(defn- blank-str? [v]
  (or (nil? v) (not (string? v)) (str/blank? v)))

(defn- validate-about
  "The About page is a vector of non-blank paragraph strings; validated
  inline since it needs no field-level error shape of its own."
  [paragraphs]
  (if (every? #(and (string? %) (not (str/blank? %))) paragraphs)
    {:ok? true}
    {:ok? false}))

(defn build!
  "Reads <root>/content/episodes.edn, content/characters.edn, and
  content/about.edn, validates them, and writes index.html,
  comics/index.html, characters/index.html, and about/index.html under
  root. Throws ex-info with :type :lag/invalid-episodes,
  :type :lag/invalid-characters, or :type :lag/invalid-about before writing
  anything if validation fails. Returns {:written [relative paths]}."
  [{:keys [root] :or {root "."}}]
  (let [episodes (edn/read-string (slurp (str (fs/path root "content" "episodes.edn"))))
        characters (edn/read-string (slurp (str (fs/path root "content" "characters.edn"))))
        about (edn/read-string (slurp (str (fs/path root "content" "about.edn"))))
        episodes-result (site/validate-episodes episodes)
        characters-result (site/validate-characters characters)
        about-result (validate-about about)]
    (when-not (:ok? episodes-result)
      (throw (ex-info "invalid episodes" {:type :lag/invalid-episodes :errors (:errors episodes-result)})))
    (when-not (:ok? characters-result)
      (throw (ex-info "invalid characters" {:type :lag/invalid-characters :errors (:errors characters-result)})))
    (when-not (:ok? about-result)
      (throw (ex-info "invalid about" {:type :lag/invalid-about})))
    (let [index-path (fs/path root "index.html")
          comics-path (fs/path root "comics" "index.html")
          characters-path (fs/path root "characters" "index.html")
          about-path (fs/path root "about" "index.html")]
      (fs/create-dirs (fs/parent comics-path))
      (fs/create-dirs (fs/parent characters-path))
      (fs/create-dirs (fs/parent about-path))
      (spit (str index-path) (site/render (site/home-page episodes)))
      (spit (str comics-path) (site/render (site/comics-page episodes)))
      (spit (str characters-path) (site/render (site/characters-page characters)))
      (spit (str about-path) (site/render (site/about-page about)))
      {:written ["index.html" "comics/index.html" "characters/index.html" "about/index.html"]})))
