# Blog Improvements Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Upgrade the personal blog backend to use slug-based post identity, add draft/published workflow, pagination, tag filtering, reading time, RSS feed, and replace the shell seed script with an EDN-backed Clojure seeder.

**Architecture:** Changes flow from inside-out through the Diplomat layers: model → datomic wire schema → datomic adapters → datomic client → HTTP adapters → logic → controllers → HTTP server. EDN persistence is added as a write-through layer in `components` and the datomic client. Each task has a narrow scope to a single layer.

**Tech Stack:** Clojure 1.11, Leiningen, http-kit, Prismatic Schema, in-memory atom + EDN file persistence.

---

### Task 1: Update the `Post` domain model

**Files:**
- Modify: `src/blog/models/post.clj`

Replace `:id` with `:slug` and add `:published-at` and `:draft?`.

**Step 1: Update the model**

```clojure
(ns blog.models.post
  (:require [schema.core :as s]))

(def Post
  {:slug         s/Str
   :title        s/Str
   :content      s/Str
   :tags         [s/Keyword]
   :published-at s/Str
   :draft?       s/Bool})
```

**Step 2: Run all tests to see what breaks**

```
lein test
```

Expected: several test failures across adapters and client — this is the guided list of what to fix next.

---

### Task 2: Update the datomic wire schema for posts

**Files:**
- Modify: `src/blog/wire/datomic/post.clj`

Replace `:post/id` with `:post/slug` and add the two new fields.

**Step 1: Update the wire schema**

```clojure
(ns blog.wire.datomic.post
  (:require [schema.core :as s]))

(def Post
  {:post/slug         s/Str
   :post/title        s/Str
   :post/content      s/Str
   :post/tags         [s/Keyword]
   :post/published-at s/Str
   :post/draft?       s/Bool})
```

**Step 2: Run tests**

```
lein test
```

Expected: failures now cascade into the datomic adapters.

---

### Task 3: Update datomic adapters (domain ↔ wire)

**Files:**
- Modify: `src/blog/adapters/datomic/domain_to_wire.clj`
- Modify: `src/blog/adapters/datomic/wire_to_domain.clj`
- Modify: `test/blog/adapters/datomic/tag_adapters_test.clj` (check if post adapter tests exist here too)
- Test: `test/blog/adapters/datomic/` (create `post_adapters_test.clj` if not present)

**Step 1: Write failing tests for the datomic adapters**

Create `test/blog/adapters/datomic/post_adapters_test.clj`:

```clojure
(ns blog.adapters.datomic.post-adapters-test
  (:require [clojure.test :refer [deftest is testing]]
            [schema.core :as s]
            [blog.adapters.datomic.domain-to-wire :as d->w]
            [blog.adapters.datomic.wire-to-domain :as w->d]))

(def sample-post
  {:slug         "getting-started"
   :title        "Getting Started"
   :content      "Hello world"
   :tags         [:tags/clojure]
   :published-at "2026-03-20"
   :draft?       false})

(def sample-wire
  {:post/slug         "getting-started"
   :post/title        "Getting Started"
   :post/content      "Hello world"
   :post/tags         [:tags/clojure]
   :post/published-at "2026-03-20"
   :post/draft?       false})

(deftest domain->wire-test
  (s/with-fn-validation
    (testing "maps all fields to wire namespaced keys"
      (is (= sample-wire (d->w/domain->wire sample-post))))))

(deftest wire->domain-test
  (s/with-fn-validation
    (testing "maps all wire fields back to domain keys"
      (is (= sample-post (w->d/wire->domain sample-wire))))))
```

**Step 2: Run to verify it fails**

```
lein test blog.adapters.datomic.post-adapters-test
```

Expected: FAIL — functions don't handle new fields yet.

**Step 3: Update `domain_to_wire.clj`**

```clojure
(ns blog.adapters.datomic.domain-to-wire
  (:require
   [schema.core :as s]
   [blog.models.post :as models.post]
   [blog.wire.datomic.post :as wire.datomic.post]))

(s/defn domain->wire :- wire.datomic.post/Post
  [post :- models.post/Post]
  {:post/slug         (:slug post)
   :post/title        (:title post)
   :post/content      (:content post)
   :post/tags         (:tags post)
   :post/published-at (:published-at post)
   :post/draft?       (:draft? post)})
```

**Step 4: Update `wire_to_domain.clj`**

```clojure
(ns blog.adapters.datomic.wire-to-domain
  (:require
   [schema.core :as s]
   [blog.models.post :as models.post]
   [blog.wire.datomic.post :as wire.datomic.post]))

(s/defn wire->domain :- models.post/Post
  [post :- wire.datomic.post/Post]
  {:slug         (:post/slug post)
   :title        (:post/title post)
   :content      (:post/content post)
   :tags         (:post/tags post)
   :published-at (:post/published-at post)
   :draft?       (:post/draft? post)})
```

**Step 5: Run adapter tests**

```
lein test blog.adapters.datomic.post-adapters-test
```

Expected: PASS.

---

### Task 4: Update datomic client — slug-based storage

**Files:**
- Modify: `src/blog/diplomat/datomic/client.clj`
- Modify: `test/blog/diplomat/datomic/client_test.clj`

The atom now stores posts keyed by slug string. Remove UUID generation. Add `find-post-by-slug!`, `delete-post!`, tag-filter query, and pagination.

**Step 1: Rewrite the client tests**

Replace `test/blog/diplomat/datomic/client_test.clj` with:

```clojure
(ns blog.diplomat.datomic.client-test
  (:require [clojure.test :refer [deftest is testing]]
            [schema.core :as s]
            [blog.diplomat.datomic.client :as datomic.client]))

(defn make-db [] (atom {:posts {} :tags {}}))

(def post-a
  {:slug "post-a" :title "A" :content "Content A"
   :tags [:tags/clojure] :published-at "2026-01-01" :draft? false})

(def post-b
  {:slug "post-b" :title "B" :content "Content B"
   :tags [:tags/nodejs] :published-at "2026-02-01" :draft? false})

(def draft-post
  {:slug "draft" :title "Draft" :content "WIP"
   :tags [] :published-at "2026-03-01" :draft? true})

(deftest save-and-find-by-slug-test
  (s/with-fn-validation
    (testing "returns nil for unknown slug"
      (is (nil? (datomic.client/find-post-by-slug! (make-db) "nope"))))
    (testing "returns domain model after save"
      (let [db (make-db)
            _  (datomic.client/save-post! db post-a)]
        (is (= post-a (datomic.client/find-post-by-slug! db "post-a")))))))

(deftest save-post-returns-slug-test
  (s/with-fn-validation
    (let [db   (make-db)
          slug (datomic.client/save-post! db post-a)]
      (is (= "post-a" slug)))))

(deftest list-posts-test
  (s/with-fn-validation
    (let [db (make-db)
          _  (datomic.client/save-post! db post-a)
          _  (datomic.client/save-post! db post-b)]
      (testing "returns all posts"
        (is (= 2 (count (datomic.client/list-posts db))))))))

(deftest update-post-test
  (s/with-fn-validation
    (let [db      (make-db)
          _       (datomic.client/save-post! db post-a)
          updated (assoc post-a :title "Updated")]
      (datomic.client/update-post! db "post-a" updated)
      (is (= "Updated" (:title (datomic.client/find-post-by-slug! db "post-a")))))))

(deftest delete-post-test
  (s/with-fn-validation
    (let [db (make-db)
          _  (datomic.client/save-post! db post-a)]
      (datomic.client/delete-post! db "post-a")
      (is (nil? (datomic.client/find-post-by-slug! db "post-a"))))))

(deftest save-and-list-tags-test
  (s/with-fn-validation
    (testing "save-tag! stores tag and list-tags returns it"
      (let [db  (make-db)
            tag {:ident :tags/clojure :name "Clojure" :color "#5881d8"}
            _   (datomic.client/save-tag! db tag)
            all (datomic.client/list-tags db)]
        (is (= 1 (count all)))
        (is (= tag (first all)))))))

(deftest find-tag-by-slug-test
  (s/with-fn-validation
    (testing "returns nil when tag does not exist"
      (is (nil? (datomic.client/find-tag-by-slug! (make-db) "nonexistent"))))
    (testing "returns domain model when tag exists"
      (let [db  (make-db)
            tag {:ident :tags/clojure :name "Clojure" :color "#5881d8"}
            _   (datomic.client/save-tag! db tag)]
        (is (= tag (datomic.client/find-tag-by-slug! db "clojure")))))))

(deftest update-tag-by-slug-test
  (s/with-fn-validation
    (testing "updates an existing tag"
      (let [db      (make-db)
            _       (datomic.client/save-tag! db {:ident :tags/clojure :name "Clojure" :color "#000"})
            updated {:ident :tags/clojure :name "Clojure" :color "#5881d8"}
            _       (datomic.client/update-tag-by-slug! db "clojure" updated)]
        (is (= updated (datomic.client/find-tag-by-slug! db "clojure")))))))

(deftest delete-tag-by-slug-test
  (s/with-fn-validation
    (testing "removes the tag from the store"
      (let [db (make-db)
            _  (datomic.client/save-tag! db {:ident :tags/clojure :name "Clojure" :color "#5881d8"})
            _  (datomic.client/delete-tag-by-slug! db "clojure")]
        (is (nil? (datomic.client/find-tag-by-slug! db "clojure")))
        (is (= [] (datomic.client/list-tags db)))))))
```

**Step 2: Run to see failures**

```
lein test blog.diplomat.datomic.client-test
```

Expected: FAIL — no `find-post-by-slug!`, `delete-post!`, slug-based transact.

**Step 3: Rewrite `src/blog/diplomat/datomic/client.clj`**

```clojure
(ns blog.diplomat.datomic.client
  (:require
   [schema.core :as s]
   [blog.models.post :as models.post]
   [blog.models.tag :as models.tag]
   [blog.adapters.datomic.domain-to-wire :as adapters.domain->wire]
   [blog.adapters.datomic.wire-to-domain :as adapters.wire->domain]
   [blog.adapters.datomic.tag-domain-to-wire :as adapters.tag-domain->wire]
   [blog.adapters.datomic.tag-wire-to-domain :as adapters.tag-wire->domain]))

(defprotocol IDatomic
  (transact! [db slug entity])
  (query-all! [db])
  (find-by-slug! [db slug])
  (update! [db slug entity])
  (delete! [db slug])
  (transact-tag! [db ident entity])
  (query-all-tags! [db])
  (find-tag-by-ident! [db ident])
  (update-tag! [db ident entity])
  (delete-tag! [db ident]))

(extend-protocol IDatomic
  clojure.lang.Atom
  (transact! [db slug entity]
    (swap! db assoc-in [:posts slug] entity)
    slug)
  (query-all! [db]
    (vals (:posts @db)))
  (find-by-slug! [db slug]
    (get-in @db [:posts slug]))
  (update! [db slug entity]
    (swap! db assoc-in [:posts slug] entity)
    nil)
  (delete! [db slug]
    (swap! db update :posts dissoc slug)
    nil)
  (transact-tag! [db ident entity]
    (swap! db assoc-in [:tags ident] entity)
    ident)
  (query-all-tags! [db]
    (map (fn [[ident attrs]] (assoc attrs :tag/ident ident))
         (:tags @db)))
  (find-tag-by-ident! [db ident]
    (when-let [attrs (get-in @db [:tags ident])]
      (assoc attrs :tag/ident ident)))
  (update-tag! [db ident entity]
    (swap! db assoc-in [:tags ident] entity)
    nil)
  (delete-tag! [db ident]
    (swap! db update :tags dissoc ident)
    nil))

(s/defn save-post! :- s/Str
  [db post :- models.post/Post]
  (let [slug (:slug post)]
    (transact! db slug (adapters.domain->wire/domain->wire post))
    slug))

(s/defn list-posts :- [models.post/Post]
  [db]
  (map adapters.wire->domain/wire->domain (query-all! db)))

(s/defn update-post! [db slug :- s/Str post :- models.post/Post]
  (update! db slug (adapters.domain->wire/domain->wire post))
  nil)

(s/defn find-post-by-slug! :- (s/maybe models.post/Post)
  [db slug :- s/Str]
  (when-let [wire-entity (find-by-slug! db slug)]
    (adapters.wire->domain/wire->domain wire-entity)))

(s/defn delete-post! [db slug :- s/Str]
  (delete! db slug)
  nil)

(s/defn save-tag! [db tag :- models.tag/Tag]
  (transact-tag! db (:ident tag) (adapters.tag-domain->wire/domain->wire tag))
  (:ident tag))

(s/defn list-tags :- [models.tag/Tag]
  [db]
  (mapv adapters.tag-wire->domain/wire->domain
        (seq (:tags @db))))

(s/defn find-tag-by-slug! :- (s/maybe models.tag/Tag)
  [db slug :- s/Str]
  (let [ident (keyword "tags" slug)
        attrs (get-in @db [:tags ident])]
    (when attrs
      (adapters.tag-wire->domain/wire->domain [ident attrs]))))

(s/defn update-tag-by-slug! [db slug :- s/Str tag :- models.tag/Tag]
  (update-tag! db (keyword "tags" slug) (adapters.tag-domain->wire/domain->wire tag))
  nil)

(s/defn delete-tag-by-slug! [db slug :- s/Str]
  (delete-tag! db (keyword "tags" slug))
  nil)
```

**Step 4: Run tests**

```
lein test blog.diplomat.datomic.client-test
```

Expected: PASS.

---

### Task 5: Add EDN persistence to the datomic client

**Files:**
- Modify: `src/blog/diplomat/datomic/client.clj`
- Modify: `src/blog/components.clj`

Every mutation writes the atom state to `data/blog.edn`. On startup, load from file if it exists.

**Step 1: Add EDN write-through helper and wrap all mutations**

Add these to `src/blog/diplomat/datomic/client.clj`:

```clojure
;; at the top of the file, add:
;   [clojure.java.io :as io]
;   [clojure.edn :as edn]

(defn- persist! [db]
  (when-let [path (::edn-path (meta db))]
    (io/make-parents path)
    (spit path (pr-str @db))))
```

Then update all three atom mutation methods in the `extend-protocol` block to call `(persist! db)` after `swap!`:

```clojure
  (transact! [db slug entity]
    (swap! db assoc-in [:posts slug] entity)
    (persist! db)
    slug)
  (update! [db slug entity]
    (swap! db assoc-in [:posts slug] entity)
    (persist! db)
    nil)
  (delete! [db slug]
    (swap! db update :posts dissoc slug)
    (persist! db)
    nil)
  (transact-tag! [db ident entity]
    (swap! db assoc-in [:tags ident] entity)
    (persist! db)
    ident)
  (update-tag! [db ident entity]
    (swap! db assoc-in [:tags ident] entity)
    (persist! db)
    nil)
  (delete-tag! [db ident]
    (swap! db update :tags dissoc ident)
    (persist! db)
    nil)
```

**Step 2: Update `src/blog/components.clj`**

```clojure
(ns blog.components
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]
            [blog.seed :as seed]))

(def edn-path "data/blog.edn")

(defn- load-state [path]
  (if (.exists (io/file path))
    (edn/read-string (slurp path))
    (seed/initial-state)))

(defn make-components []
  (let [state (load-state edn-path)
        db    (with-meta (atom state) {::edn-path edn-path})]
    {:db db}))

(defn make-test-components []
  {:db (atom {:posts {} :tags {}})})
```

Note: `make-test-components` returns a plain atom with no `::edn-path` metadata, so `persist!` is a no-op in tests.

**Step 3: Run all tests — EDN must not interfere with tests**

```
lein test
```

Expected: PASS — tests use `make-test-components` or bare atoms with no EDN path.

---

### Task 6: Create the seed namespace

**Files:**
- Create: `src/blog/seed.clj`

**Step 1: Create the seed file**

```clojure
(ns blog.seed)

(def initial-state
  {:tags
   {:tags/clojure {:tag/name "Clojure" :tag/color "#5881D8" :tag/slug "clojure"}
    :tags/nodejs  {:tag/name "Node.js" :tag/color "#68A063" :tag/slug "nodejs"}}
   :posts
   {"getting-started-with-clojure"
    {:post/slug         "getting-started-with-clojure"
     :post/title        "Getting Started with Clojure"
     :post/content      "Clojure is a dynamic, functional Lisp dialect running on the JVM."
     :post/tags         [:tags/clojure]
     :post/published-at "2026-03-20"
     :post/draft?       false}
    "building-rest-apis-with-nodejs"
    {:post/slug         "building-rest-apis-with-nodejs"
     :post/title        "Building REST APIs with Node.js"
     :post/content      "Node.js makes it easy to build scalable network applications."
     :post/tags         [:tags/nodejs]
     :post/published-at "2026-03-20"
     :post/draft?       false}}})
```

Note: this stores data in datomic wire format (with `post/` and `tag/` namespaced keys) because `components.clj` loads it directly into the atom, which stores wire-format data.

**Step 2: Run all tests**

```
lein test
```

Expected: PASS.

---

### Task 7: Update `wire.in/post` and `wire.out/post`

**Files:**
- Modify: `src/blog/wire/in/post.clj`
- Modify: `src/blog/wire/out/post.clj`

**Step 1: Update `wire.in/post`**

```clojure
(ns blog.wire.in.post
  (:require [schema.core :as s]))

(def NewPost
  {:slug         s/Str
   :title        s/Str
   :content      s/Str
   :tags         [s/Str]
   :published-at s/Str
   :draft?       s/Bool})

(def EditPost
  {(s/optional-key :title)        s/Str
   (s/optional-key :content)      s/Str
   (s/optional-key :tags)         [s/Str]
   (s/optional-key :published-at) s/Str
   (s/optional-key :draft?)       s/Bool})
```

**Step 2: Update `wire.out/post`**

```clojure
(ns blog.wire.out.post
  (:require [schema.core :as s]))

(def Post
  {:slug         s/Str
   :title        s/Str
   :content      s/Str
   :tags         [s/Str]
   :published-at s/Str
   :draft?       s/Bool
   :reading-time s/Int})
```

**Step 3: Run tests**

```
lein test
```

Expected: failures in `adapters.post` — that's next.

---

### Task 8: Update the HTTP adapter for posts

**Files:**
- Modify: `src/blog/adapters/post.clj`
- Modify: `test/blog/adapters/post_test.clj`

**Step 1: Write failing tests**

Replace `test/blog/adapters/post_test.clj`:

```clojure
(ns blog.adapters.post-test
  (:require [clojure.test :refer [deftest is testing]]
            [schema.core :as s]
            [blog.adapters.post :as adapters.post]))

(def sample-new-post
  {:slug "my-post" :title "My Post" :content "Hello world"
   :tags ["clojure"] :published-at "2026-03-20" :draft? false})

(def sample-model
  {:slug "my-post" :title "My Post" :content "Hello world"
   :tags [:tags/clojure] :published-at "2026-03-20" :draft? false})

(deftest wire-in->model-test
  (s/with-fn-validation
    (testing "converts all fields including new ones"
      (is (= sample-model
             (adapters.post/wire-in->model sample-new-post))))))

(deftest wire-in-edit->partial-model-test
  (s/with-fn-validation
    (testing "passes through only provided fields"
      (is (= {:title "New Title"}
             (adapters.post/wire-in-edit->partial-model {:title "New Title"}))))

    (testing "passes through tags converting to keywords"
      (is (= {:tags [:tags/foo :tags/bar]}
             (adapters.post/wire-in-edit->partial-model {:tags ["foo" "bar"]}))))

    (testing "passes through published-at and draft?"
      (is (= {:published-at "2026-04-01" :draft? true}
             (adapters.post/wire-in-edit->partial-model {:published-at "2026-04-01" :draft? true}))))

    (testing "empty map returns empty map"
      (is (= {} (adapters.post/wire-in-edit->partial-model {}))))))

(deftest model->wire-out-test
  (s/with-fn-validation
    (testing "converts domain post to wire.out format"
      (is (= {:slug "my-post" :title "My Post" :content "Hello world"
              :tags ["clojure"] :published-at "2026-03-20" :draft? false
              :reading-time 1}
             (adapters.post/model->wire-out sample-model))))

    (testing "reading time rounds up (200 words = 1 min)"
      (let [post (assoc sample-model :content (clojure.string/join " " (repeat 201 "word")))]
        (is (= 2 (:reading-time (adapters.post/model->wire-out post))))))

    (testing "empty content gives reading time of 1"
      (let [post (assoc sample-model :content "")]
        (is (= 1 (:reading-time (adapters.post/model->wire-out post))))))))
```

**Step 2: Run to verify failures**

```
lein test blog.adapters.post-test
```

Expected: FAIL.

**Step 3: Update `src/blog/adapters/post.clj`**

```clojure
(ns blog.adapters.post
  (:require
   [clojure.string :as str]
   [schema.core :as s]
   [blog.wire.in.post :as wire.in.post]
   [blog.wire.out.post :as wire.out.post]
   [blog.models.post :as models.post]))

(defn- compute-reading-time [content]
  (let [word-count (count (str/split (str/trim content) #"\s+"))
        minutes    (int (Math/ceil (/ (max 1 word-count) 200)))]
    minutes))

(s/defn wire-in->model :- models.post/Post
  [new-post :- wire.in.post/NewPost]
  {:slug         (:slug new-post)
   :title        (:title new-post)
   :content      (:content new-post)
   :tags         (mapv #(keyword "tags" %) (:tags new-post))
   :published-at (:published-at new-post)
   :draft?       (:draft? new-post)})

(s/defn wire-in-edit->partial-model :- {s/Keyword s/Any}
  [edits :- wire.in.post/EditPost]
  (cond-> (select-keys edits [:title :content :published-at :draft?])
    (contains? edits :tags)
    (assoc :tags (mapv #(keyword "tags" %) (:tags edits)))))

(s/defn model->wire-out :- wire.out.post/Post
  [post :- models.post/Post]
  {:slug         (:slug post)
   :title        (:title post)
   :content      (:content post)
   :tags         (mapv name (:tags post))
   :published-at (:published-at post)
   :draft?       (:draft? post)
   :reading-time (compute-reading-time (:content post))})
```

**Step 4: Run adapter tests**

```
lein test blog.adapters.post-test
```

Expected: PASS.

---

### Task 9: Update post logic — filtering and pagination

**Files:**
- Modify: `src/blog/logic/post.clj`
- Modify: `test/blog/logic/post_test.clj`

**Step 1: Write failing tests**

Replace `test/blog/logic/post_test.clj`:

```clojure
(ns blog.logic.post-test
  (:require [clojure.test :refer [deftest is testing]]
            [schema.core :as s]
            [blog.logic.post :as logic.post]))

(def base-post
  {:slug "p" :title "T" :content "C" :tags []
   :published-at "2026-01-01" :draft? false})

(deftest apply-edits-test
  (let [existing (assoc base-post :title "Old" :tags [:tags/foo :tags/bar])]
    (s/with-fn-validation
      (testing "updates only provided fields"
        (is (= (assoc existing :title "New")
               (logic.post/apply-edits existing {:title "New"}))))

      (testing "updates multiple fields"
        (is (= (assoc existing :title "New" :tags [:tags/baz])
               (logic.post/apply-edits existing {:title "New" :tags [:tags/baz]}))))

      (testing "empty edits returns existing unchanged"
        (is (= existing (logic.post/apply-edits existing {})))))))

(deftest filter-by-tag-test
  (let [clj-post  (assoc base-post :slug "a" :tags [:tags/clojure])
        node-post (assoc base-post :slug "b" :tags [:tags/nodejs])
        posts     [clj-post node-post]]
    (testing "returns all when no tag filter"
      (is (= posts (logic.post/filter-by-tag posts nil))))
    (testing "filters to matching tag"
      (is (= [clj-post] (logic.post/filter-by-tag posts "clojure"))))
    (testing "returns empty when no match"
      (is (= [] (logic.post/filter-by-tag posts "rust"))))))

(deftest filter-published-test
  (let [pub   (assoc base-post :slug "pub" :draft? false)
        draft (assoc base-post :slug "draft" :draft? true)
        posts [pub draft]]
    (testing "removes drafts"
      (is (= [pub] (logic.post/filter-published posts))))))

(deftest paginate-test
  (let [posts (mapv #(assoc base-post :slug (str "p" %)) (range 25))]
    (testing "returns first page"
      (is (= 10 (count (logic.post/paginate posts 1 10)))))
    (testing "returns correct items on page 2"
      (is (= "p10" (:slug (first (logic.post/paginate posts 2 10))))))
    (testing "returns remaining items on last page"
      (is (= 5 (count (logic.post/paginate posts 3 10)))))
    (testing "returns empty for out-of-range page"
      (is (= [] (logic.post/paginate posts 99 10))))))
```

**Step 2: Run to verify failures**

```
lein test blog.logic.post-test
```

Expected: FAIL — `filter-by-tag`, `filter-published`, `paginate` don't exist.

**Step 3: Update `src/blog/logic/post.clj`**

```clojure
(ns blog.logic.post
  (:require [schema.core :as s]
            [blog.models.post :as models.post]))

(s/defn apply-edits :- models.post/Post
  [existing :- models.post/Post
   edits :- {s/Keyword s/Any}]
  (merge existing edits))

(defn filter-by-tag [posts tag-slug]
  (if (nil? tag-slug)
    posts
    (let [tag-kw (keyword "tags" tag-slug)]
      (filter #(some #{tag-kw} (:tags %)) posts))))

(defn filter-published [posts]
  (remove :draft? posts))

(defn paginate [posts page page-size]
  (let [offset (* (dec page) page-size)]
    (vec (take page-size (drop offset posts)))))
```

**Step 4: Run logic tests**

```
lein test blog.logic.post-test
```

Expected: PASS.

---

### Task 10: Update post controller

**Files:**
- Modify: `src/blog/controllers/post.clj`
- Modify: `test/blog/controllers/post_test.clj`

**Step 1: Rewrite controller tests**

Replace `test/blog/controllers/post_test.clj`:

```clojure
(ns blog.controllers.post-test
  (:require [clojure.test :refer [deftest is testing]]
            [schema.core :as s]
            [blog.controllers.post :as controllers.post]
            [blog.diplomat.datomic.client :as datomic.client]))

(defn make-db [] (atom {:posts {} :tags {}}))

(def new-post
  {:slug "my-post" :title "T" :content "C"
   :tags [] :published-at "2026-03-20" :draft? false})

(def draft-post
  {:slug "my-draft" :title "Draft" :content "WIP"
   :tags [] :published-at "2026-03-20" :draft? true})

(deftest create-post-returns-slug
  (s/with-fn-validation
    (let [db   (make-db)
          slug (controllers.post/CreatePost new-post db)]
      (is (= "my-post" slug)))))

(deftest list-posts-excludes-drafts
  (s/with-fn-validation
    (let [db (make-db)
          _  (controllers.post/CreatePost new-post db)
          _  (controllers.post/CreatePost draft-post db)]
      (testing "only returns published posts"
        (let [posts (controllers.post/ListPosts db {} {})]
          (is (= 1 (count posts)))
          (is (= "my-post" (:slug (first posts)))))))))

(deftest list-posts-tag-filter
  (s/with-fn-validation
    (let [db       (make-db)
          clj-post (assoc new-post :slug "clj-post" :tags ["clojure"])
          js-post  (assoc new-post :slug "js-post"  :tags ["nodejs"])
          _        (controllers.post/CreatePost clj-post db)
          _        (controllers.post/CreatePost js-post db)]
      (testing "filters by tag"
        (is (= 1 (count (controllers.post/ListPosts db {:tag "clojure"} {})))))
      (testing "returns all when no filter"
        (is (= 2 (count (controllers.post/ListPosts db {} {}))))))))

(deftest list-posts-pagination
  (s/with-fn-validation
    (let [db    (make-db)
          posts (mapv #(assoc new-post :slug (str "p" %)) (range 15))
          _     (doseq [p posts] (controllers.post/CreatePost p db))]
      (testing "default page size is 10"
        (is (= 10 (count (controllers.post/ListPosts db {} {})))))
      (testing "page 2 returns remainder"
        (is (= 5 (count (controllers.post/ListPosts db {} {:page "2"}))))))))

(deftest get-post-by-slug-test
  (s/with-fn-validation
    (let [db (make-db)
          _  (controllers.post/CreatePost new-post db)]
      (testing "returns post when found"
        (is (some? (controllers.post/GetPost "my-post" db))))
      (testing "returns :not-found when missing"
        (is (= :not-found (controllers.post/GetPost "nope" db)))))))

(deftest edit-post-test
  (s/with-fn-validation
    (testing "returns :not-found for unknown slug"
      (let [db (make-db)]
        (is (= :not-found (controllers.post/EditPost "nope" {} db)))))

    (testing "returns :ok and updates fields"
      (let [db (make-db)
            _  (controllers.post/CreatePost new-post db)]
        (is (= :ok (controllers.post/EditPost "my-post" {:title "New"} db)))
        (is (= "New" (:title (controllers.post/GetPost "my-post" db))))))))

(deftest delete-post-test
  (s/with-fn-validation
    (testing "returns :not-found for unknown slug"
      (let [db (make-db)]
        (is (= :not-found (controllers.post/DeletePost "nope" db)))))
    (testing "returns :ok and removes post"
      (let [db (make-db)
            _  (controllers.post/CreatePost new-post db)]
        (is (= :ok (controllers.post/DeletePost "my-post" db)))
        (is (= :not-found (controllers.post/GetPost "my-post" db)))))))
```

**Step 2: Run to verify failures**

```
lein test blog.controllers.post-test
```

Expected: FAIL — `GetPost`, `DeletePost` missing; `ListPosts` signature changed.

**Step 3: Rewrite `src/blog/controllers/post.clj`**

```clojure
(ns blog.controllers.post
  (:require
   [schema.core :as s]
   [blog.wire.in.post :as wire.in.post]
   [blog.wire.out.post :as wire.out.post]
   [blog.logic.post :as logic.post]
   [blog.adapters.post :as adapters.post]
   [blog.diplomat.datomic.client :as datomic.client]))

(s/defn CreatePost :- s/Str
  [post :- wire.in.post/NewPost db]
  (datomic.client/save-post! db (adapters.post/wire-in->model post)))

(s/defn ListPosts :- [wire.out.post/Post]
  [db query-params path-params]
  (let [tag       (:tag query-params)
        page      (Integer/parseInt (get query-params :page "1"))
        page-size (Integer/parseInt (get query-params :page-size "10"))]
    (->> (datomic.client/list-posts db)
         (logic.post/filter-published)
         (logic.post/filter-by-tag tag)
         (logic.post/paginate page page-size)
         (map adapters.post/model->wire-out))))

(s/defn GetPost :- (s/either wire.out.post/Post (s/eq :not-found))
  [slug :- s/Str db]
  (if-let [post (datomic.client/find-post-by-slug! db slug)]
    (adapters.post/model->wire-out post)
    :not-found))

(s/defn EditPost :- (s/enum :ok :not-found)
  [slug :- s/Str edits :- wire.in.post/EditPost db]
  (if-let [existing (datomic.client/find-post-by-slug! db slug)]
    (let [partial-model (adapters.post/wire-in-edit->partial-model edits)
          updated       (logic.post/apply-edits existing partial-model)]
      (datomic.client/update-post! db slug updated)
      :ok)
    :not-found))

(s/defn DeletePost :- (s/enum :ok :not-found)
  [slug :- s/Str db]
  (if (datomic.client/find-post-by-slug! db slug)
    (do (datomic.client/delete-post! db slug) :ok)
    :not-found))
```

**Step 4: Run controller tests**

```
lein test blog.controllers.post-test
```

Expected: PASS.

---

### Task 11: Update the HTTP server routes

**Files:**
- Modify: `src/blog/diplomat/http_server.clj`

Update all post handlers to use slug, add `get-post`, `delete-post`, and pass query params to `ListPosts`.

**Step 1: Update `src/blog/diplomat/http_server.clj`**

```clojure
(ns blog.diplomat.http-server
  (:require
   [blog.env :as env]
   [blog.wire.in.auth :as wire.in.auth]
   [blog.wire.in.post :as wire.in.post]
   [blog.wire.in.tag :as wire.in.tag]
   [blog.utils.request :as utils.request]
   [blog.components]
   [blog.controllers.auth :as controllers.auth]
   [blog.controllers.post :as controllers.post]
   [blog.controllers.tag :as controllers.tag]
   [blog.controllers.rss :as controllers.rss]))

(defn login-handler [{:keys [data]}]
  (let [admin-user (env/get-env "ADMIN_USERNAME")
        admin-pass (env/get-env "ADMIN_PASSWORD")
        result     (controllers.auth/Login data admin-user admin-pass)]
    (if (:ok result)
      {:status 200 :body result}
      {:status 401 :body result})))

(defn ping-handler [_request]
  {:status 200 :body {:status "ok"}})

(defn create-post [{new-post :data components :components}]
  (let [slug (controllers.post/CreatePost new-post (components :db))]
    {:status 201 :body {:slug slug}}))

(defn list-posts [{components :components query-params :query-params}]
  {:status 200
   :body (controllers.post/ListPosts (components :db) query-params {})})

(defn get-post [{path-params :path-params components :components}]
  (let [slug   (:slug path-params)
        result (controllers.post/GetPost slug (components :db))]
    (if (= result :not-found)
      {:status 404}
      {:status 200 :body result})))

(defn edit-post [{edits :data path-params :path-params components :components}]
  (let [slug   (:slug path-params)
        result (controllers.post/EditPost slug edits (components :db))]
    (if (= result :not-found)
      {:status 404}
      {:status 200})))

(defn delete-post [{path-params :path-params components :components}]
  (let [slug   (:slug path-params)
        result (controllers.post/DeletePost slug (components :db))]
    (if (= result :not-found)
      {:status 404}
      {:status 204})))

(defn create-tag [{tag :data components :components}]
  (let [slug (controllers.tag/CreateTag tag (components :db))]
    {:status 201 :body {:slug slug}}))

(defn list-tags [{components :components}]
  {:status 200 :body (controllers.tag/ListTags (components :db))})

(defn edit-tag [{edits :data path-params :path-params components :components}]
  (let [slug   (:slug path-params)
        result (controllers.tag/EditTag slug edits (components :db))]
    (if (= result :not-found)
      {:status 404}
      {:status 200})))

(defn delete-tag [{path-params :path-params components :components}]
  (let [slug   (:slug path-params)
        result (controllers.tag/DeleteTag slug (components :db))]
    (if (= result :not-found)
      {:status 404}
      {:status 204})))

(defn rss-feed [{components :components}]
  {:status  200
   :headers {"Content-Type" "application/rss+xml; charset=utf-8"}
   :body    (controllers.rss/RssFeed (components :db))})

(def common-interceptors
  [(utils.request/wrap-components (blog.components/make-components))
   (utils.request/wrap-json-body)
   (utils.request/wrap-json-response)])

(def routes
  #{["/api/ping"
     :get (conj common-interceptors ping-handler)
     :route-name :ping]

    ["/api/post"
     :post (conj common-interceptors
                 (utils.request/wrap-schema wire.in.post/NewPost)
                 create-post)
     :route-name :create-post]

    ["/api/post"
     :get (conj common-interceptors list-posts)
     :route-name :list-posts]

    ["/api/post/:slug"
     :get (conj common-interceptors get-post)
     :route-name :get-post]

    ["/api/post/:slug"
     :patch (conj common-interceptors
                  (utils.request/wrap-schema wire.in.post/EditPost)
                  edit-post)
     :route-name :edit-post]

    ["/api/post/:slug"
     :delete (conj common-interceptors delete-post)
     :route-name :delete-post]

    ["/api/auth/login"
     :post (conj common-interceptors
                 (utils.request/wrap-schema wire.in.auth/Login)
                 login-handler)
     :route-name :login]

    ["/api/tag"
     :post (conj common-interceptors
                 (utils.request/wrap-schema wire.in.tag/NewTag)
                 create-tag)
     :route-name :create-tag]

    ["/api/tag"
     :get (conj common-interceptors list-tags)
     :route-name :list-tags]

    ["/api/tag/:slug"
     :patch (conj common-interceptors
                  (utils.request/wrap-schema wire.in.tag/EditTag)
                  edit-tag)
     :route-name :edit-tag]

    ["/api/tag/:slug"
     :delete (conj common-interceptors delete-tag)
     :route-name :delete-tag]

    ["/feed.xml"
     :get (conj common-interceptors rss-feed)
     :route-name :rss-feed]})
```

**Step 2: Check that `wrap-components` receives query params** — inspect `src/blog/utils/request.clj` and ensure the request map passed to handlers includes `:query-params`. If not, add extraction there.

**Step 3: Run all tests**

```
lein test
```

Expected: failures only in RSS — that's next.

---

### Task 12: Add RSS feed controller

**Files:**
- Create: `src/blog/controllers/rss.clj`
- Create: `test/blog/controllers/rss_test.clj`

**Step 1: Write failing test**

Create `test/blog/controllers/rss_test.clj`:

```clojure
(ns blog.controllers.rss-test
  (:require [clojure.test :refer [deftest is testing]]
            [blog.controllers.rss :as controllers.rss]))

(defn make-db []
  (let [posts {"pub"   {:post/slug "pub"   :post/title "Published"
                        :post/content "Content" :post/tags []
                        :post/published-at "2026-03-20" :post/draft? false}
               "draft" {:post/slug "draft" :post/title "Draft"
                        :post/content "WIP" :post/tags []
                        :post/published-at "2026-03-20" :post/draft? true}}]
    (atom {:posts posts :tags {}})))

(deftest rss-feed-test
  (let [xml (controllers.rss/RssFeed (make-db))]
    (testing "returns a string"
      (is (string? xml)))
    (testing "contains published post title"
      (is (clojure.string/includes? xml "Published")))
    (testing "does not contain draft post title"
      (is (not (clojure.string/includes? xml "Draft"))))
    (testing "is valid RSS structure"
      (is (clojure.string/includes? xml "<rss"))
      (is (clojure.string/includes? xml "<channel"))
      (is (clojure.string/includes? xml "<item")))))
```

**Step 2: Run to verify failures**

```
lein test blog.controllers.rss-test
```

Expected: FAIL — namespace doesn't exist.

**Step 3: Create `src/blog/controllers/rss.clj`**

```clojure
(ns blog.controllers.rss
  (:require
   [clojure.string :as str]
   [blog.diplomat.datomic.client :as datomic.client]
   [blog.logic.post :as logic.post]))

(defn- escape-xml [s]
  (-> s
      (str/replace "&" "&amp;")
      (str/replace "<" "&lt;")
      (str/replace ">" "&gt;")))

(defn- post->item [{:keys [slug title content published-at]}]
  (str "<item>"
       "<title>" (escape-xml title) "</title>"
       "<link>https://yourblog.com/posts/" slug "</link>"
       "<pubDate>" published-at "</pubDate>"
       "<description>" (escape-xml (subs content 0 (min 200 (count content)))) "</description>"
       "</item>"))

(defn RssFeed [db]
  (let [posts (->> (datomic.client/list-posts db)
                   (logic.post/filter-published)
                   (sort-by :published-at)
                   (reverse)
                   (take 20))]
    (str "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
         "<rss version=\"2.0\">"
         "<channel>"
         "<title>My Blog</title>"
         "<link>https://yourblog.com</link>"
         "<description>Technical articles</description>"
         (str/join (map post->item posts))
         "</channel>"
         "</rss>")))
```

**Step 4: Run RSS tests**

```
lein test blog.controllers.rss-test
```

Expected: PASS.

---

### Task 13: Run the full test suite and fix any remaining failures

**Step 1: Run all tests**

```
lein test
```

**Step 2: Fix any remaining failures** — at this point they will likely be in `test/blog/server_test.clj` or integration tests referencing old `:id` fields. Update assertions to use `:slug` instead of `:id`.

**Step 3: Run all tests again**

```
lein test
```

Expected: all PASS.

---

### Task 14: Verify EDN persistence end-to-end

**Step 1: Start the server**

```
lein run
```

**Step 2: Check that `data/blog.edn` was created on startup**

```
cat data/blog.edn
```

Expected: EDN map with seed posts and tags.

**Step 3: Add a post and verify it persists**

```bash
curl -s -X POST http://localhost:8080/api/post \
  -H "Content-Type: application/json" \
  -d '{"slug":"test-edn","title":"Test EDN","content":"Testing persistence","tags":[],"published-at":"2026-03-20","draft?":false}' | jq .
```

**Step 4: Restart the server and verify the post survives**

Stop with Ctrl-C, then:

```
lein run
curl -s http://localhost:8080/api/post/test-edn | jq .
```

Expected: post is returned — state survived restart.

---

### Task 15: Verify RSS feed

**Step 1: Hit the RSS endpoint**

```
curl -s http://localhost:8080/feed.xml
```

Expected: valid XML with `<rss>`, `<channel>`, and `<item>` elements for published posts only.
