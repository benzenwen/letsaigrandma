# Let's AI, Grandma — product brief

A satirical webcomic about a multigenerational family making sense of AI as
it moves from novelty into everyday life. This file is the brief the site is
built from; DESIGN.md is how we build it; `orders/` are the individual jobs.

## The joke

"Let's eat, Grandma" vs "Let's eat Grandma." The comma is the difference
between dinner with her and dinner *of* her. Likewise, "Let's AI, Grandma"
is using AI together with Grandma; "Let's AI Grandma" is doing something to
her. That ambiguity is the comic's whole premise, and the site should never
explain it more than once.

## Stance

Not "AI good." Not "AI bad." Curious, affectionate, skeptical, satirical.
The recurring question is not "What can AI do?" but "What are we choosing
to delegate, and what happens to us when we do?" Subject matter comes from
current AI news: absurdities, hype, genuine breakthroughs, education,
science, work, agents, hallucinations, anthropomorphism, children using AI,
strange incentives, and situations where two contradictory things are true
at once.

## Characters

- **Nana** — Grandma. One of the early women to earn a Ph.D. in
  biochemistry. Has lived through many waves of scientific and technological
  hype, so she is neither impressed by marketing nor reflexively
  anti-technology. Dryly funny, observant, a little cynical, loves her
  grandchildren. Typical lines: "That is not the same question." /
  "Interesting. Now how would we know?" / "It may be useful. That doesn't
  make it true."
- **Joey** — 11. Minecraft kid. Enthusiastic, inventive, a little AI-pilled.
  Treats AI as a creative tool and playground, not a political or economic
  issue. Often the first to try something ridiculous, and sometimes finds
  genuinely interesting uses because adults would never ask his questions.
  Weakness: fluent answers feel true to him.
- **Jane** — high-school-aged older sibling. Skeptical the way teenagers
  are: suspicious of hype, tired of adults pretending to understand youth
  culture, worried that AI makes schoolwork, creative work, and authenticity
  weird. Not anti-AI; worried that opting out may itself become a
  disadvantage.
- **Mom** — warm and practical. Recurring supporting character.
- **Dad** — distinctive round yellow glasses. Recurring supporting
  character. With Mom, brings workplace, parenting, school, and adult social
  pressure into the strip.

## Visual style (the artwork, and what the site must not fight)

Physical medium: Pelikan opaque watercolor, 24-color tray, over pen-and-ink.
Black ink outlines, simplified abstract cartoon forms, very simple faces,
dot or oval eyes (no exaggerated chevron closed eyes), soft slightly uneven
opaque washes, limited palette, textured paper, loose handmade borders,
intentionally imperfect linework. It should feel like an intelligent
handmade sketchbook comic.

Avoid: polished digital illustration, gradients, slick vector aesthetics,
glossy AI-art rendering, over-detailed portraits.

## Site

Extremely small, fast, static, hosted on GitHub Pages. Artwork dominates;
lots of whitespace; simple readable type, at most one hand-drawn-feeling
display face for headings and only if it doesn't look gimmicky. Design
references: sketchbook, Sunday comic, notebook margin, science-lab notebook,
family-refrigerator drawing but intellectually sharp. Phones first.

Pages:

- **Home** — title/logo, one-sentence premise, latest episode (or the
  Episode 1 placeholder), simple navigation.
- **Comics** — chronological list of episodes. Adding an episode is one
  image plus title, date, and a short caption. Nothing else.
- **Characters** — Nana, Joey, Jane, Mom, Dad, each with a character-card
  image.
- **About** — the punctuation joke in a few lines; what the comic is for;
  that it investigates the absurdities, promises, contradictions, and
  unintended consequences of AI through one family.

Copy voice: concise, understated, a little dry. No corporate AI copy. Never
"exploring the exciting future of artificial intelligence." It should sound
like a smart comic made by a person, not a startup landing page.

## Episode 1 (planned)

Nana and Joey. Joey asks an AI about Minecraft, something he knows cold,
and catches a wrong answer immediately. Nana names the interesting problem:
AI is easiest to evaluate in the domains where you already know enough to
catch its mistakes.

> Joey: "It says it can."
> Nana: "Is that true?"
> Joey: "Well… no."
> Nana: "How did you know?"
> Joey: "Because I know Minecraft."
> Nana: "So it works best when you already know the answer?"
> Joey: "…that seems bad."
> Nana: "Not bad. Interesting."

## Deliverables requested

- Clean repository structure for GitHub Pages
- `index.html`, minimal CSS, tiny CLJS/JS only if genuinely needed
- Folders for `/images`, `/comics`, and character art
- An obvious pattern for adding new episodes
- Short README: local preview and GitHub Pages deployment

## Inputs not yet in the repo

The brief refers to character reference images, character-card images, and
an AI-generated rough sketch of Episode 1 that the artist will re-render in
ink and watercolor. **None of these are in the repository or were available
to the session that wrote this file.** The site therefore ships with a
documented drop location for each image and visible placeholders until the
artwork lands. Nothing in the layout should depend on image dimensions
being known in advance.

## Roadmap of slices (each is one work order)

1. Site generator + Home + Comics from an episode data file (order 0001)
2. Characters and About pages from a character data file
3. README with local preview and GitHub Pages deployment; `.nojekyll`
4. Episode 1 artwork drop-in (content, not code — foreman/artist task)
