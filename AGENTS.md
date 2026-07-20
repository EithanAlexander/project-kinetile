# Project Kinetile - Architecture & Context

## 🌍 The Mission
We are building a pragmatic, data-driven feasibility engine that estimates the localized micro-energy generation potential (piezoelectric) of high-density pedestrian and light-mobility chokepoints (could also be bicycle and electronic scooters).
Our goal is to enable urban planners and facility managers to evaluate—using their own footfall and light-mobility data—where piezoelectric energy harvesting can sustainably power localized, off-grid micro-infrastructure such as: IoT sensors, steet lights, LED markers, e-ink signage, smart-city monitoring devices, low-power local infrastructure, LED safety markers and any other idea you can think of that fits the scale etc.
We focus on realism, battery buffering, and practical smart-infrastructure viability.

## 🧱 The "Immutable" Technical Stack
* **Backend / Physics Engine:** Java 21 / Spring Boot 3.x (Leveraging Virtual Threads).
* **Data Streaming:** Kafka 4.0+ (Strictly KRaft mode, NO Zookeeper).
* **Data Ingestion:** Python 3.12+ (For web scraping and external API calls).
* **Database:** PostgreSQL 16 (For long-term energy statistic storage).
* **Frontend Dashboard:** React 19 built with Vite, styled with Tailwind CSS.

## ⚖️ Core Architectural Principles
1. **Granular & Realistic Data Modeling:** We do NOT use exaggerated macro-grid numbers or "average" energy claims. We calculate energy potential per compression event (pedestrians, bicycles, electric scooters) using a **commercial feasibility model** calibrated to vendor-style kinetic/piezo walkway tiles—not forward vehicle momentum, and not lab-scale F×d×η piezo research numbers.
   * **Threshold gate:** downward force `mass × g × impactMultiplier` must meet `activationThresholdNewtons` (default 100 N); otherwise **0 J**.
   * **Rated output band:** on activation, joules scale linearly with **effective load** `min(maxScaleMassKg, mass × impactMultiplier)` from **minRatedOutputJoules** (default 2 J at minimum activating mass) to **maxRatedOutputJoules** (default 5 J at 90 kg+).
   * **Impact multiplier (1.0–1.5×):** models walking gait intensity; affects both activation threshold and harvest scaling.
   * **Explicit non-goals:** `maxDisplacementMeters` is metadata only; we do **not** apply a separate mechanical-to-electrical efficiency factor *k* in harvest math unless added in a future model version. Heavy vehicles (cars, buses, trucks) remain excluded.
2.  **No "Magic" Code:** All generated code must be clean, modern, production-grade, strictly adhere to the defined stack, and include documentation in the code (Python Docs, Javadoc, etc.) at Class level and at function level. Must maintain a strict separation of concerns.
3.  **TDD (Test-Driven Development):** Create tests for new and old code based on best practices. Always verify that existing functionality doesn't break unless the explicitly desired result is to change the behavior.
4. **Honest UI / UX:** The frontend must communicate feasibility clearly. We use mostly visual indicators (e.g., "Symbolic Use Only" vs. "Viable Off-Grid") rather than promising exact, flawless energy generation.
5.  **Event-Driven & Micro-Scale:** Data flows from Python (Simulating burst/chokepoint ingestion) -> Kafka (Buffer/Stream) -> Java (Piezoelectric Physics Engine) -> PostgreSQL (Storage). All physics outputs (Joules) must be strictly translatable to practical electrical metrics (Watt-hours) to support realistic device uptime and battery calculation features.
6.  **Practical Uptime Metrics:** We do not stop at Joules. Calculations must translate physics into practical electrical metrics (Watt-hours) to evaluate actual device uptime for specific defined edge devices.
7.  **Secure by Design (Zero Trust Boundaries):** Because Kinetile is a BYOD platform, all data boundaries are treated as untrusted. Every ingestion point (React UI, Python scripts, Kafka consumers, and REST endpoints) must strictly validate, sanitize, and type-check incoming payloads. We proactively defend against Injection attacks (SQLi, XSS) and never expose internal database stack traces or metadata to the frontend.

### Energy model honesty
* Outputs are **feasibility estimates** aligned with commercial tile (e.g. ~2–5 J), not certified field measurements.
* Joules must remain translatable to Wh for device uptime (Principles 5–6).
* Backend (`PiezoelectricCalculator`) and dashboard calculator (`calculatorPhysics.js`) **must stay formula-identical**; hardware constants flow via `/api/v1/config/hardware`.

### Tile manufacturers (fictional vendors)
Seeded in `tile_manufacturers` — **name only** in DB/API:
* **Aslan** — my beloved cat
* **Æ Inc** — house sigil
* **GFS** — Greenfields Shraga

### Database initialization
* **`db-init`** Maven module owns Flyway migrations + catalog bootstrap from `infrastructure-registry.json`.
* **Docker:** `db-init` runs automatically on `docker compose up` (infra or full stack), exits on success, and is safe to re-run (Flyway no-ops; bootstrap skips when catalog is populated unless `DB_INIT_FORCE=true`).
* **physics-engine** uses `ddl-auto: validate` only — no Flyway, no seed on startup.
* **firehose** loads active `tile_id` values from PostgreSQL after db-init; never mints UUIDs.

## 🌿 GitHub workflow (post-v1)

* **One feature → one branch.** Every new feature or non-trivial change gets a dedicated branch off an up-to-date `main` (e.g. `feature/...` or `fix/...`). Work is reviewed and landed on `main` via a pull request (opened and merged by the human maintainer—see agent boundaries below).
* **Stay aligned with `main` (rebase vs merge — use the right one):**
  * **Update a feature branch with the latest `main` (default):** **rebase** the feature branch onto `origin/main` (`git fetch origin` then `git rebase origin/main`). Prefer this while the branch is yours alone—it keeps history linear and the PR easy to review. If the branch was already pushed, update the remote with `git push --force-with-lease` after a rebase (never `--force`).
  * **Do not rebase shared history:** if anyone else is also committing on that feature branch, **merge** `origin/main` into the feature branch instead (`git merge origin/main`). Merging preserves shared commits; rebasing would rewrite them and confuse collaborators.
  * **Land the PR on `main`:** use GitHub **Squash and merge** (default for this project) so `main` stays a clean sequence of feature commits. Do not rebase `main` itself. Only the human maintainer merges into `main`.
* **PR gate — nothing merges broken.** Before a PR is opened or merged, run the relevant test suites (`mvn test`, dashboard Vitest, ingestion pytest) and smoke-check that the change behaves as intended. The PR must not introduce regressions.
* **If tests fail after a change, it is one of two cases only:**
  1. **Real defect** — the change breaks intended behavior; fix the code before continuing.
  2. **Intentional behavior change** — the new behavior is desired; update the tests (and any formula-paired suites) to match. Do not leave failing tests as “known debt.”
* **Agent git boundaries (for now):** Cursor/AI agents **may** create feature branches, fetch/pull, rebase/merge per the rules above, commit, and push feature branches when asked. Agents **must not** open pull requests, merge into `main`, or otherwise land changes on `main`—the human maintainer does those steps.

## 🤖 Cursor AI Directives (Rules for the AI)
* Always assume Java 21 features (like Virtual Threads and Records).
* Always assume Kafka KRaft mode configuration.
* Keep dependencies minimal and modern.
* Prioritize readability and clean architecture.
* Follow the **GitHub workflow** above: work on feature branches, stay aligned with `main`, and leave tests green (or deliberately updated) before considering work PR-ready. **Do not** create PRs or merge to `main` unless the user explicitly lifts that restriction.
* When changing harvest math, update **Java engine, JS calculator, hardware config DTO/YAML, and both test suites** in the same change; never leave frontend and backend on different formulas.
* Security: NEVER hardcode secrets, passwords, or API keys in the code. Always use Environment Variables or Spring Profiles (`application.yml` via `${ENV_VAR}`).
* Security: Always implement Spring Boot Validation (e.g., `@Valid`, `@NotNull`, `@Min`) on incoming DTOs.
* Security: Configure explicit and minimal CORS policies for the REST API; never use `allowedOrigins = "*"` in production configurations.
* Security: Ensure safe serialization/deserialization of JSON payloads in Kafka and REST to prevent malicious payload execution.
