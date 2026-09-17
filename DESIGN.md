# Design doctrine — Let's AI, Grandma

Injected into every plan, build, and review prompt. PRODUCT.md is *what*
we're making; this is *how*. Keep it short; every line must earn its tokens.

## Stack (decided)

- **Babashka + hiccup2, nothing else.** `bb build` renders the whole site
  from EDN data files. No quickblog (it is a blog engine; this is three
  pages and a list), no JVM, no npm, no JS unless an order proves a need.
- **Output lives at the repo root and is committed.** GitHub Pages serves
  `main` from `/ (root)` with no Actions. `index.html`, `comics/index.html`,
  `characters/index.html`, `about/index.html`, and `css/site.css` are the
  published surface. `.nojekyll` is present so nothing is preprocessed.
- **Data in `content/`, art beside its page.** `content/episodes.edn` and
  `content/characters.edn` are the source of truth. Episode art goes in
  `comics/`, character cards in `images/characters/`, site art in `images/`.
  Adding an episode is: drop one image, add one map to `episodes.edn`, run
  `bb build`, commit.
- **Layout.** `src/lag/site.clj` is pure: data in, hiccup/HTML strings out.
  `src/lag/build.clj` is the only namespace that reads or writes files.
  Tests in `test/lag/*_test.clj`; `bb test` discovers them automatically.

## Data first

The schema is the contract; the renderer serves it, not the other way
round. Episode maps (`content/episodes.edn`, a vector):

```clojure
{:number  1                              ; positive int, unique; orders episodes
 :title   "Let's AI, Grandma"            ; string
 :date    "2026-09-20"                   ; ISO-8601 date string
 :image   "comics/001-minecraft.png"     ; site-root-relative path; nil = not drawn yet
 :alt     "Nana and Joey at the table…"  ; required when :image is set; it is the transcript for readers who can't see the art
 :caption "One short dry line."}         ; string, may be empty
```

Validation is explicit and pure (`site/validate-episodes` → `{:ok? bool :errors [...]}`),
runs before rendering, and a bad file fails `bb build` with the offending
episode number and field. Never silently render a broken map.

## Modules

- **Deep modules.** Render functions take plain data and return hiccup. A
  page is `(page-name data) -> hiccup`; the HTML string wrap and the shared
  chrome (header, nav, footer) live in one place.
- **Information hiding.** Only `build.clj` knows filesystem paths; only
  `site.clj` knows HTML structure. Neither leaks into the other.
- **No pass-through layers.** A function that only calls another function
  with the same arguments is deleted, not kept.
- **Sized for today.** Two pages of content and one list. No tags, no RSS,
  no pagination, no theming system, no plugin points until an order asks.

## Errors

- Prefer semantics that make errors impossible: an episode without `:image`
  renders a placeholder frame, it does not throw. A missing `:alt` on a
  real image *does* fail validation, because that is a reader-facing defect.
- Real errors throw `ex-info` with a `:type` keyword and the data needed to
  fix them. No magic return values.

## Code

- Pure functions over plain data; side effects (file I/O, shell) at the
  edges in `build.clj`.
- Names are the first documentation. Comments carry the *why*, never a
  restatement of the next line. Public vars get a docstring that states the
  contract.
- Working code isn't enough. If a change makes the shape worse, fix the
  shape first.

## HTML and CSS

- Semantic, minimal, valid HTML5. One stylesheet, hand-written, no
  preprocessor. System font stack for body text; at most one display face
  for headings, self-hosted or system, never a webfont CDN.
- Phones first: single column, artwork at 100% width, readable at 360px
  with no horizontal scroll. Whitespace is a feature; the art dominates.
- Every image has `alt`. Every page has a `<title>`, `lang`, viewport meta.
- No inline styles, no JS, no analytics, no external requests of any kind.

## Copy

Concise, understated, a little dry. Written by a person, not a startup.
Never "exploring the exciting future of AI." When an order needs copy it
did not supply verbatim, keep it to a sentence and prefer PRODUCT.md's own
wording over invention.

## Tests

- Every behavior ships with tests that pin the acceptance criteria. A test
  that would still pass if the feature were deleted is not a test.
- Test the pure layer directly on data. Test `build.clj` by building into a
  temp directory and asserting on the files it wrote. Offline, always.
- `bb test` stays green on every commit.

## Change hygiene

- Stay inside the order's scope; unrelated improvements go in their own
  order.
- Small diffs. Regenerated HTML counts as part of the diff and must be
  committed in the same change as the source that produced it.
