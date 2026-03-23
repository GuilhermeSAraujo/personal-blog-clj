# Blog Improvements Design

**Date:** 2026-03-20

## Overview

Improve the personal blog backend to be publishable as a real technical blog while keeping the architecture minimalist. The changes span the data model, API surface, persistence strategy, and developer experience.

## Data Model

Replace the existing `Post` model. `slug` becomes the primary identifier, replacing `:id`. Three new fields are added:

```clojure
(def Post
  {:slug         s/Str       ; "getting-started-with-clojure"
   :title        s/Str
   :content      s/Str
   :tags         [s/Keyword]
   :published-at s/Str       ; ISO-8601 date string, e.g. "2026-03-20"
   :draft?       s/Bool})    ; true = hidden from public list
```

`reading-time` is computed (not stored): `(Math/ceil (/ word-count 200))` minutes. It is added by the response adapter before returning to the client.

## API Routes

| Method | Path | Notes |
|--------|------|-------|
| `GET` | `/api/post` | Query params: `?tag=clojure`, `?page=1&page-size=10`. Returns only non-draft posts. |
| `POST` | `/api/post` | Requires `slug`, `published-at`, `draft?` in body. |
| `GET` | `/api/post/:slug` | Fetch single post by slug. Returns drafts (useful for preview). |
| `PATCH` | `/api/post/:slug` | Was `:id`, now `:slug`. |
| `DELETE` | `/api/post/:slug` | New endpoint. |
| `GET` | `/feed.xml` | RSS feed of 20 most recent published posts, ordered by `published-at`. |

Pagination defaults: `page=1`, `page-size=10`.

## Startup Seeder + EDN Persistence

Replace `scripts/seed.sh` with a `blog.seed` namespace containing hardcoded initial tags and posts (with all new fields).

### Persistence strategy (Option A — EDN file)

The atom is backed by a `data/blog.edn` file:

- **On startup:** if `data/blog.edn` exists, load state from it. If not, load from `blog.seed` defaults and write the file.
- **On every mutation:** update the atom and write the full atom state to `data/blog.edn`.

This gives zero-dependency file-based persistence. Any content added via the API survives restarts automatically. The seed acts as factory defaults only when no file exists.

The file path is injected via the components system so tests use a temp path and never touch real disk state.

## Testing

- **Adapter tests** — `published-at`, `slug`, `draft?`, `reading-time` wire ↔ model conversions
- **Logic tests** — pagination logic, tag filtering, reading time computation
- **Controller tests** — draft filtering on list, slug-based lookup, not-found cases
- **DB client tests** — CRUD with slug as primary key, EDN file read/write behaviour
