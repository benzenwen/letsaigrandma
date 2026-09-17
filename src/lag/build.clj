(ns lag.build
  "The only namespace that touches the filesystem: reads content/episodes.edn,
  validates it, and writes the rendered site. See DESIGN.md."
  (:require [babashka.fs :as fs]
            [clojure.edn :as edn]
            [lag.site :as site]))

(def ^:private stylesheet-source
  "The one hand-authored copy of the stylesheet, checked in at css/site.css
  relative to the project root (bb tasks and tests both run with that as
  the process cwd). build! copies it verbatim into <root>/css/site.css so
  the checked-in file stays the single source of truth even when :root
  points at a test's temp directory."
  "css/site.css")

(defn build!
  "Reads <root>/content/episodes.edn, validates it, and writes index.html,
  comics/index.html, and css/site.css under root. Throws ex-info with
  :type :lag/invalid-episodes before writing anything if validation fails.
  Returns {:written [relative paths]}."
  [{:keys [root] :or {root "."}}]
  (let [episodes (edn/read-string (slurp (str (fs/path root "content" "episodes.edn"))))
        {:keys [ok? errors]} (site/validate-episodes episodes)]
    (when-not ok?
      (throw (ex-info "invalid episodes" {:type :lag/invalid-episodes :errors errors})))
    (let [index-path (fs/path root "index.html")
          comics-path (fs/path root "comics" "index.html")
          css-path (fs/path root "css" "site.css")]
      (fs/create-dirs (fs/parent comics-path))
      (fs/create-dirs (fs/parent css-path))
      (spit (str index-path) (site/render (site/home-page episodes)))
      (spit (str comics-path) (site/render (site/comics-page episodes)))
      (spit (str css-path) (slurp stylesheet-source))
      {:written ["index.html" "comics/index.html" "css/site.css"]})))
