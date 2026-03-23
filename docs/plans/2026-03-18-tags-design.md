# Tags Feature Design

**Date:** 2026-03-18  
**Status:** Approved

## Overview

Add independently-managed tags to blog posts. Tags have their own CRUD API. Posts reference tags using Datomic-style keyword idents (e.g. `:tags/clojure`), which is the idiomatic Datomic enum/ident pattern rather than SQL-style integer foreign keys.

## Mental Model Shift (SQL → Datomic)

In SQL, tags are a separate table with an integer PK and posts join via a junction table.

In Datomic, every "thing" is an entity. Tags are entities whose `:db/ident` is the slug keyword. Posts reference tags by storing those keyword idents directly — no junction table, no integer IDs. The keyword `:tags/clojure` simultaneously serves as the slug, the identity, and the reference.

Since this project uses an `atom` to simulate Datomic, the same principle applies: the atom is partitioned into `:posts` and `:tags`, and tags are keyed by their keyword ident.

## Chosen Approach: Keyword Idents as Tag Identity

Tags are stored in the atom under the `:tags` partition, keyed by keyword ident:

```clojure
(atom {:posts {"post-uuid" {:post/title "..." :post/tags [:tags/clojure :tags/nodejs]}}
       :tags  {:tags/clojure {:tag/name "Clojure"  :tag/color "#5881d8"}
               :tags/nodejs  {:tag/name "Node.js"  :tag/color "#68A063"}}})
```

The keyword ident (e.g. `:tags/clojure`) is simultaneously:
- The slug used in HTTP API requests/responses (as its name string `"clojure"`)
- The identity/key in the atom
- The reference value stored on post entities

## Tag Entity

### Attributes

| Attribute | Type | Description |
|---|---|---|
| `slug` | string | URL-safe identifier, becomes the keyword name (e.g. `"clojure"` → `:tags/clojure`) |
| `name` | string | Display name (e.g. `"Clojure"`, `"Node.js"`) |
| `color` | string | Hex color for UI rendering (e.g. `"#5881d8"`) |

## Layer Design

### Section 1: Atom & Protocol

**Atom structure** changes from a flat post map to a partitioned map:

```clojure
;; Before
(atom {})  ; {"post-uuid" {:post/title ...}}

;; After
(atom {:posts {} :tags {}})
```

**`IDatomic` protocol** — existing post operations updated to use `:posts` partition; new tag operations added:

```clojure
(defprotocol IDatomic
  ;; posts (existing, updated internals)
  (transact! [db entity])
  (query-all! [db])
  (find-by-id! [db id])
  (update! [db id entity])
  ;; tags (new)
  (transact-tag! [db ident entity])
  (query-all-tags! [db])
  (find-tag-by-ident! [db ident])
  (update-tag! [db ident entity])
  (delete-tag! [db ident]))
```

### Section 2: Tag Layers

**`models/tag.clj`**
```clojure
(def Tag {:ident s/Keyword :name s/Str :color s/Str})
```

**`wire/in/tag.clj`**
```clojure
(def NewTag  {:slug s/Str :name s/Str :color s/Str})
(def EditTag {(s/optional-key :name) s/Str (s/optional-key :color) s/Str})
```

**`wire/out/tag.clj`**
```clojure
(def Tag {:slug s/Str :name s/Str :color s/Str})
```

**`wire/datomic/tag.clj`** (stored value in atom, ident is the key not an attribute)
```clojure
(def Tag {:tag/name s/Str :tag/color s/Str})
```

**Adapters:**
- `adapters/tag.clj` — HTTP wire ↔ domain
  - `wire-in->model`: `{:slug "clojure" ...}` → `{:ident :tags/clojure ...}`
  - `model->wire-out`: `{:ident :tags/clojure ...}` → `{:slug "clojure" ...}`
- `adapters/datomic/tag_domain_to_wire.clj` — `domain->wire`: strips `:ident`, namespaces keys
- `adapters/datomic/tag_wire_to_domain.clj` — `wire->domain`: takes `[ident attrs]` tuple, builds domain model

**`controllers/tag.clj`**
```clojure
(s/defn CreateTag :- s/Keyword [tag :- wire.in.tag/NewTag db] ...)
(s/defn ListTags  :- [wire.out.tag/Tag] [db] ...)
(s/defn EditTag   :- (s/enum :ok :not-found) [slug :- s/Str edits :- wire.in.tag/EditTag db] ...)
(s/defn DeleteTag :- (s/enum :ok :not-found) [slug :- s/Str db] ...)
```

**HTTP endpoints (additions to `http_server.clj`)**

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/tag` | Create a tag |
| `GET` | `/api/tag` | List all tags |
| `PATCH` | `/api/tag/:slug` | Edit name/color |
| `DELETE` | `/api/tag/:slug` | Delete a tag |

### Section 3: Post Model Changes

Replace `:tag-ids [s/Int]` with `:tags [s/Str]` at the HTTP boundary and `:tags [s/Keyword]` in the domain and storage layers.

| Layer | Before | After |
|---|---|---|
| `wire/in/post.clj` | `:tag-ids [s/Int]` | `:tags [s/Str]` — slug strings from client |
| `models/post.clj` | `:tag-ids [s/Int]` | `:tags [s/Keyword]` — keyword idents in domain |
| `wire/datomic/post.clj` | `:post/tag-ids [s/Int]` | `:post/tags [s/Keyword]` — stored as keywords |
| `wire/out/post.clj` | `:tag-ids [s/Int]` | `:tags [s/Str]` — slug strings in response |

**Key adapter conversions:**
- Inbound (HTTP → domain): `(map #(keyword "tags" %) (:tags wire))` — `"clojure"` → `:tags/clojure`
- Outbound (domain → HTTP): `(map name (:tags model))` — `:tags/clojure` → `"clojure"`

## Files Summary

### New files
- `src/blog/models/tag.clj`
- `src/blog/wire/in/tag.clj`
- `src/blog/wire/out/tag.clj`
- `src/blog/wire/datomic/tag.clj`
- `src/blog/adapters/tag.clj`
- `src/blog/adapters/datomic/tag_domain_to_wire.clj`
- `src/blog/adapters/datomic/tag_wire_to_domain.clj`
- `src/blog/controllers/tag.clj`

### Modified files
- `src/blog/models/post.clj`
- `src/blog/wire/in/post.clj`
- `src/blog/wire/out/post.clj` (currently missing — needs creation)
- `src/blog/wire/datomic/post.clj`
- `src/blog/adapters/post.clj`
- `src/blog/adapters/datomic/domain_to_wire.clj`
- `src/blog/adapters/datomic/wire_to_domain.clj`
- `src/blog/diplomat/datomic/client.clj`
- `src/blog/components.clj`
- `src/blog/diplomat/http_server.clj`
