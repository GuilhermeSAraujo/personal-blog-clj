# Tags Feature Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add independently-managed tags (slug, name, color) to blog posts using keyword idents as tag identity in the atom store.

**Architecture:** Tags are stored in the atom under a `:tags` partition keyed by keyword ident (e.g. `:tags/clojure`). Posts reference tags by storing those keyword idents. The atom changes from a flat post map to `{:posts {} :tags {}}`. HTTP boundaries use slug strings; the domain and storage layers use keywords.

**Tech Stack:** Clojure 1.11, Prismatic Schema, clojure.test, Leiningen (`lein test`)

---

## Task 1: Refactor atom structure — `IDatomic` and `components`

Posts currently live at the top level of the atom (`{"post-uuid" {...}}`). This task moves them into a `:posts` partition.

**Files:**
- Modify: `src/blog/components.clj`
- Modify: `src/blog/diplomat/datomic/client.clj`
- Modify: `test/blog/diplomat/datomic/client_test.clj`
- Modify: `test/blog/controllers/post_test.clj`

**Step 1: Update `make-db` in both test files**

In `test/blog/diplomat/datomic/client_test.clj` and `test/blog/controllers/post_test.clj`, change:

```clojure
(defn make-db [] (atom {}))
```
to:
```clojure
(defn make-db [] (atom {:posts {} :tags {}}))
```

**Step 2: Run tests to confirm they break**

```bash
lein test
```
Expected: errors like `NullPointerException` or wrong shape — the implementation still uses the old flat structure.

**Step 3: Update `components.clj`**

```clojure
(ns blog.components)

(defn make-components []
  {:db (atom {:posts {} :tags {}})})
```

**Step 4: Update `IDatomic` atom implementation in `client.clj`**

Replace the `extend-protocol IDatomic clojure.lang.Atom` block with:

```clojure
(extend-protocol IDatomic
  clojure.lang.Atom
  (transact! [db entity]
    (let [id (str (java.util.UUID/randomUUID))]
      (swap! db assoc-in [:posts id] (assoc entity :post/id id))
      id))
  (query-all! [db]
    (vals (:posts @db)))
  (find-by-id! [db id]
    (get-in @db [:posts id]))
  (update! [db id entity]
    (swap! db assoc-in [:posts id] entity)
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
```

Also add the new operations to the `IDatomic` protocol definition:

```clojure
(defprotocol IDatomic
  (transact! [db entity])
  (query-all! [db])
  (find-by-id! [db id])
  (update! [db id entity])
  (transact-tag! [db ident entity])
  (query-all-tags! [db])
  (find-tag-by-ident! [db ident])
  (update-tag! [db ident entity])
  (delete-tag! [db ident]))
```

**Step 5: Run tests to verify they pass**

```bash
lein test
```
Expected: all existing tests pass.

---

## Task 2: Refactor post schemas — replace `:tag-ids [s/Int]` with `:tags`

Posts now carry tag keyword idents in the domain. HTTP clients send/receive slug strings.

**Files:**
- Modify: `src/blog/wire/in/post.clj`
- Modify: `src/blog/models/post.clj`
- Modify: `src/blog/wire/datomic/post.clj`
- Create: `src/blog/wire/out/post.clj`

**Step 1: Update `wire/in/post.clj`**

```clojure
(ns blog.wire.in.post
  (:require [schema.core :as s]))

(def NewPost
  {:title   s/Str
   :content s/Str
   :tags    [s/Str]})   ; slug strings from HTTP client, e.g. ["clojure" "nodejs"]

(def EditPost
  {(s/optional-key :title)   s/Str
   (s/optional-key :content) s/Str
   (s/optional-key :tags)    [s/Str]})
```

**Step 2: Update `models/post.clj`**

```clojure
(ns blog.models.post
  (:require [schema.core :as s]))

(def Post
  {(s/optional-key :id) s/Str
   :title               s/Str
   :content             s/Str
   :tags                [s/Keyword]})  ; keyword idents, e.g. [:tags/clojure]
```

**Step 3: Update `wire/datomic/post.clj`**

```clojure
(ns blog.wire.datomic.post
  (:require [schema.core :as s]))

(def Post
  {(s/optional-key :post/id) s/Str
   :post/title               s/Str
   :post/content             s/Str
   :post/tags                [s/Keyword]})  ; keyword idents stored directly
```

**Step 4: Create `wire/out/post.clj`** (this file is missing from the codebase)

```clojure
(ns blog.wire.out.post
  (:require [schema.core :as s]))

(def Post
  {(s/optional-key :id) s/Str
   :title               s/Str
   :content             s/Str
   :tags                [s/Str]})  ; slug strings back to HTTP client
```

**Step 5: Run tests to see what breaks**

```bash
lein test
```
Expected: schema validation errors on `:tag-ids` vs `:tags` mismatches.

---

## Task 3: Refactor post adapters

**Files:**
- Modify: `src/blog/adapters/post.clj`
- Modify: `src/blog/adapters/datomic/domain_to_wire.clj`
- Modify: `src/blog/adapters/datomic/wire_to_domain.clj`
- Modify: `test/blog/adapters/post_test.clj`
- Modify: `test/blog/diplomat/datomic/client_test.clj`
- Modify: `test/blog/controllers/post_test.clj`

**Step 1: Update `adapters/post.clj`**

```clojure
(ns blog.adapters.post
  (:require
   [schema.core :as s]
   [blog.wire.in.post :as wire.in.post]
   [blog.wire.out.post :as wire.out.post]
   [blog.models.post :as models.post]))

(s/defn wire-in->model :- models.post/Post
  [new-post :- wire.in.post/NewPost]
  {:title   (:title new-post)
   :content (:content new-post)
   :tags    (mapv #(keyword "tags" %) (:tags new-post))})

(s/defn wire-in-edit->partial-model :- {s/Keyword s/Any}
  [edits :- wire.in.post/EditPost]
  (cond-> (select-keys edits [:title :content])
    (contains? edits :tags)
    (assoc :tags (mapv #(keyword "tags" %) (:tags edits)))))

(s/defn model->wire-out :- wire.out.post/Post
  [post :- models.post/Post]
  {:id      (:id post)
   :title   (:title post)
   :content (:content post)
   :tags    (mapv name (:tags post))})  ; :tags/clojure → "clojure"
```

**Step 2: Update `adapters/datomic/domain_to_wire.clj`**

```clojure
(ns blog.adapters.datomic.domain-to-wire
  (:require
   [schema.core :as s]
   [blog.models.post :as models.post]
   [blog.wire.datomic.post :as wire.datomic.post]))

(s/defn domain->wire :- wire.datomic.post/Post
  [post :- models.post/Post]
  {:post/title   (:title post)
   :post/content (:content post)
   :post/tags    (:tags post)})
```

**Step 3: Update `adapters/datomic/wire_to_domain.clj`**

```clojure
(ns blog.adapters.datomic.wire-to-domain
  (:require
   [schema.core :as s]
   [blog.models.post :as models.post]
   [blog.wire.datomic.post :as wire.datomic.post]))

(s/defn wire->domain :- models.post/Post
  [post :- wire.datomic.post/Post]
  {:id      (:post/id post)
   :title   (:post/title post)
   :content (:post/content post)
   :tags    (:post/tags post)})
```

**Step 4: Update all tests to use `:tags` instead of `:tag-ids`**

In `test/blog/adapters/post_test.clj`, replace all occurrences of `:tag-ids` with `:tags`, and integer values with keyword idents or slug strings as appropriate. For example:

```clojure
;; wire-in-edit->partial-model-test
(is (= {:title "T" :tags [:tags/foo :tags/bar]}
       (adapters.post/wire-in-edit->partial-model {:title "T" :tags ["foo" "bar"]})))

;; model->wire-out-test
(is (= {:id "abc" :title "T" :content "C" :tags ["clojure" "nodejs"]}
       (adapters.post/model->wire-out {:id "abc" :title "T" :content "C"
                                       :tags [:tags/clojure :tags/nodejs]})))
```

In `test/blog/diplomat/datomic/client_test.clj`, replace `:tag-ids []` / `:tag-ids [1]` with `:tags []` / `:tags [:tags/clojure]`, and `:post/tag-ids` with `:post/tags`:

```clojure
;; find-by-id-test
(let [post {:title "T" :content "C" :tags []}
      ...]
  (is (= {:post/title "T" :post/content "C" :post/tags []}
         (dissoc found :post/id))))

;; update-post-test
(let [original {:title "Old" :content "Content" :tags [:tags/clojure]}
      updated  {:title "New" :content "Content" :tags [:tags/clojure]}
      ...]
  (is (= {:post/title "New" :post/content "Content" :post/tags [:tags/clojure]}
         (dissoc found :post/id))))

;; find-post-by-id-test
(let [post {:title "T" :content "C" :tags [:tags/foo]}
      ...]
  (is (= {:title "T" :content "C" :tags [:tags/foo]}
         (dissoc found :id))))
```

In `test/blog/controllers/post_test.clj`, replace all `:tag-ids` with `:tags` using appropriate types:

```clojure
;; create-post-returns-id
(controllers.post/CreatePost {:title "T" :content "C" :tags []} db)

;; list-posts-returns-wire-out
(controllers.post/CreatePost {:title "T" :content "C" :tags ["clojure"]} db)
;; assert
(is (= ["clojure"] (:tags post)))

;; edit-post-test
(datomic.client/save-post! db {:title "Old" :content "Content" :tags [:tags/clojure]})
(is (= {:title "New" :content "Content" :tags ["clojure"]}
       (dissoc listed :id)))
```

**Step 5: Run tests**

```bash
lein test
```
Expected: all tests pass.

---

## Task 4: Tag schemas (no tests needed — pure data definitions)

**Files:**
- Create: `src/blog/models/tag.clj`
- Create: `src/blog/wire/in/tag.clj`
- Create: `src/blog/wire/out/tag.clj`
- Create: `src/blog/wire/datomic/tag.clj`

**Step 1: Create `models/tag.clj`**

```clojure
(ns blog.models.tag
  (:require [schema.core :as s]))

(def Tag
  {:ident s/Keyword   ; e.g. :tags/clojure
   :name  s/Str       ; e.g. "Clojure"
   :color s/Str})     ; e.g. "#5881d8"
```

**Step 2: Create `wire/in/tag.clj`**

```clojure
(ns blog.wire.in.tag
  (:require [schema.core :as s]))

(def NewTag
  {:slug  s/Str
   :name  s/Str
   :color s/Str})

(def EditTag
  {(s/optional-key :name)  s/Str
   (s/optional-key :color) s/Str})
```

**Step 3: Create `wire/out/tag.clj`**

```clojure
(ns blog.wire.out.tag
  (:require [schema.core :as s]))

(def Tag
  {:slug  s/Str
   :name  s/Str
   :color s/Str})
```

**Step 4: Create `wire/datomic/tag.clj`**

```clojure
(ns blog.wire.datomic.tag
  (:require [schema.core :as s]))

; ident is the key in the atom, NOT stored as an attribute.
; This schema represents only the stored value map.
(def Tag
  {:tag/name  s/Str
   :tag/color s/Str})
```

---

## Task 5: Tag datomic adapters (TDD)

**Files:**
- Create: `src/blog/adapters/datomic/tag_domain_to_wire.clj`
- Create: `src/blog/adapters/datomic/tag_wire_to_domain.clj`
- Create: `test/blog/adapters/datomic/tag_adapters_test.clj`

**Step 1: Write failing tests**

Create `test/blog/adapters/datomic/tag_adapters_test.clj`:

```clojure
(ns blog.adapters.datomic.tag-adapters-test
  (:require [clojure.test :refer [deftest is testing]]
            [schema.core :as s]
            [blog.adapters.datomic.tag-domain-to-wire :as domain->wire]
            [blog.adapters.datomic.tag-wire-to-domain :as wire->domain]))

(deftest domain->wire-test
  (s/with-fn-validation
    (testing "converts domain tag to datomic wire format (strips ident)"
      (is (= {:tag/name "Clojure" :tag/color "#5881d8"}
             (domain->wire/domain->wire {:ident :tags/clojure
                                         :name  "Clojure"
                                         :color "#5881d8"}))))))

(deftest wire->domain-test
  (s/with-fn-validation
    (testing "converts [ident attrs] tuple to domain tag"
      (is (= {:ident :tags/clojure :name "Clojure" :color "#5881d8"}
             (wire->domain/wire->domain [:tags/clojure
                                         {:tag/name "Clojure" :tag/color "#5881d8"}]))))))
```

**Step 2: Run tests to verify they fail**

```bash
lein test blog.adapters.datomic.tag-adapters-test
```
Expected: FAIL — namespaces not found.

**Step 3: Create `adapters/datomic/tag_domain_to_wire.clj`**

```clojure
(ns blog.adapters.datomic.tag-domain-to-wire
  (:require
   [schema.core :as s]
   [blog.models.tag :as models.tag]
   [blog.wire.datomic.tag :as wire.datomic.tag]))

(s/defn domain->wire :- wire.datomic.tag/Tag
  [tag :- models.tag/Tag]
  {:tag/name  (:name tag)
   :tag/color (:color tag)})
```

**Step 4: Create `adapters/datomic/tag_wire_to_domain.clj`**

```clojure
(ns blog.adapters.datomic.tag-wire-to-domain
  (:require
   [schema.core :as s]
   [blog.models.tag :as models.tag]
   [blog.wire.datomic.tag :as wire.datomic.tag]))

(s/defn wire->domain :- models.tag/Tag
  [[ident attrs]]   ; receives a [keyword map] tuple from (seq (:tags @db))
  {:ident ident
   :name  (:tag/name attrs)
   :color (:tag/color attrs)})
```

**Step 5: Run tests**

```bash
lein test blog.adapters.datomic.tag-adapters-test
```
Expected: PASS.

---

## Task 6: Tag HTTP adapters (TDD)

**Files:**
- Create: `src/blog/adapters/tag.clj`
- Create: `test/blog/adapters/tag_test.clj`

**Step 1: Write failing tests**

Create `test/blog/adapters/tag_test.clj`:

```clojure
(ns blog.adapters.tag-test
  (:require [clojure.test :refer [deftest is testing]]
            [schema.core :as s]
            [blog.adapters.tag :as adapters.tag]))

(deftest wire-in->model-test
  (s/with-fn-validation
    (testing "converts NewTag wire-in to domain model (slug → keyword ident)"
      (is (= {:ident :tags/clojure :name "Clojure" :color "#5881d8"}
             (adapters.tag/wire-in->model {:slug "clojure"
                                           :name "Clojure"
                                           :color "#5881d8"}))))
    (testing "handles slugs with hyphens"
      (is (= {:ident :tags/node-js :name "Node.js" :color "#68A063"}
             (adapters.tag/wire-in->model {:slug "node-js"
                                           :name "Node.js"
                                           :color "#68A063"}))))))

(deftest model->wire-out-test
  (s/with-fn-validation
    (testing "converts domain tag to wire-out (keyword ident → slug string)"
      (is (= {:slug "clojure" :name "Clojure" :color "#5881d8"}
             (adapters.tag/model->wire-out {:ident :tags/clojure
                                            :name  "Clojure"
                                            :color "#5881d8"}))))))
```

**Step 2: Run tests to verify they fail**

```bash
lein test blog.adapters.tag-test
```
Expected: FAIL — namespace not found.

**Step 3: Create `adapters/tag.clj`**

```clojure
(ns blog.adapters.tag
  (:require
   [schema.core :as s]
   [blog.wire.in.tag :as wire.in.tag]
   [blog.wire.out.tag :as wire.out.tag]
   [blog.models.tag :as models.tag]))

(s/defn wire-in->model :- models.tag/Tag
  [tag :- wire.in.tag/NewTag]
  {:ident (keyword "tags" (:slug tag))
   :name  (:name tag)
   :color (:color tag)})

(s/defn model->wire-out :- wire.out.tag/Tag
  [tag :- models.tag/Tag]
  {:slug  (name (:ident tag))
   :name  (:name tag)
   :color (:color tag)})
```

**Step 4: Run tests**

```bash
lein test blog.adapters.tag-test
```
Expected: PASS.

---

## Task 7: Tag datomic client functions (TDD)

**Files:**
- Modify: `src/blog/diplomat/datomic/client.clj`
- Modify: `test/blog/diplomat/datomic/client_test.clj`

**Step 1: Write failing tests for tag client functions**

Add to `test/blog/diplomat/datomic/client_test.clj`:

```clojure
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
      (let [db (make-db)]
        (is (nil? (datomic.client/find-tag-by-slug! db "nonexistent")))))
    (testing "returns domain model when tag exists"
      (let [db  (make-db)
            tag {:ident :tags/clojure :name "Clojure" :color "#5881d8"}
            _   (datomic.client/save-tag! db tag)]
        (is (= tag (datomic.client/find-tag-by-slug! db "clojure")))))))

(deftest update-tag-test
  (s/with-fn-validation
    (testing "updates an existing tag"
      (let [db      (make-db)
            _       (datomic.client/save-tag! db {:ident :tags/clojure
                                                   :name  "Clojure"
                                                   :color "#000"})
            updated {:ident :tags/clojure :name "Clojure" :color "#5881d8"}
            _       (datomic.client/update-tag! db "clojure" updated)]
        (is (= updated (datomic.client/find-tag-by-slug! db "clojure")))))))

(deftest delete-tag-test
  (s/with-fn-validation
    (testing "removes the tag from the store"
      (let [db (make-db)
            _  (datomic.client/save-tag! db {:ident :tags/clojure
                                              :name  "Clojure"
                                              :color "#5881d8"})
            _  (datomic.client/delete-tag! db "clojure")]
        (is (nil? (datomic.client/find-tag-by-slug! db "clojure")))
        (is (= [] (datomic.client/list-tags db)))))))
```

**Step 2: Run tests to verify they fail**

```bash
lein test blog.diplomat.datomic.client-test
```
Expected: FAIL — functions not defined.

**Step 3: Add tag functions to `client.clj`**

Add the required namespace imports:

```clojure
[blog.models.tag :as models.tag]
[blog.adapters.datomic.tag-domain-to-wire :as adapters.tag-domain->wire]
[blog.adapters.datomic.tag-wire-to-domain :as adapters.tag-wire->domain]
```

Add the functions:

```clojure
(s/defn save-tag! [db tag :- models.tag/Tag]
  (-> tag
      adapters.tag-domain->wire/domain->wire
      (->> (transact-tag! db (:ident tag))))
  (:ident tag))

(s/defn list-tags :- [models.tag/Tag]
  [db]
  (map adapters.tag-wire->domain/wire->domain
       (seq (:tags @db))))

(s/defn find-tag-by-slug! :- (s/maybe models.tag/Tag)
  [db slug :- s/Str]
  (let [ident (keyword "tags" slug)]
    (when-let [attrs (find-tag-by-ident! db ident)]
      (adapters.tag-wire->domain/wire->domain [ident (dissoc attrs :tag/ident)]))))

(s/defn update-tag! [db slug :- s/Str tag :- models.tag/Tag]
  (let [ident (keyword "tags" slug)
        wire  (adapters.tag-domain->wire/domain->wire tag)]
    (update-tag! db ident wire)
    nil))

(s/defn delete-tag! [db slug :- s/Str]
  (delete-tag! db (keyword "tags" slug))
  nil)
```

> Note: `update-tag!` and `delete-tag!` above call the protocol methods. To avoid name collision with the public `defn`, name the public functions `update-tag-by-slug!` and `delete-tag-by-slug!` if needed, or use an aliased call. The simplest fix is to prefix the public API functions: `save-tag!`, `list-tags`, `find-tag-by-slug!`, `update-tag-by-slug!`, `delete-tag-by-slug!`.

**Step 4: Run tests**

```bash
lein test blog.diplomat.datomic.client-test
```
Expected: all tests pass.

---

## Task 8: Tag controller (TDD)

**Files:**
- Create: `src/blog/controllers/tag.clj`
- Create: `test/blog/controllers/tag_test.clj`

**Step 1: Write failing tests**

Create `test/blog/controllers/tag_test.clj`:

```clojure
(ns blog.controllers.tag-test
  (:require [clojure.test :refer [deftest is testing]]
            [schema.core :as s]
            [blog.controllers.tag :as controllers.tag]
            [blog.diplomat.datomic.client :as datomic.client]))

(defn make-db [] (atom {:posts {} :tags {}}))

(deftest create-tag-test
  (s/with-fn-validation
    (testing "creates a tag and returns its slug"
      (let [db   (make-db)
            slug (controllers.tag/CreateTag {:slug "clojure"
                                             :name "Clojure"
                                             :color "#5881d8"} db)]
        (is (= "clojure" slug))
        (is (some? (datomic.client/find-tag-by-slug! db "clojure")))))))

(deftest list-tags-test
  (s/with-fn-validation
    (testing "returns all tags in wire-out format"
      (let [db   (make-db)
            _    (controllers.tag/CreateTag {:slug "clojure" :name "Clojure" :color "#5881d8"} db)
            tags (controllers.tag/ListTags db)]
        (is (= 1 (count tags)))
        (is (= {:slug "clojure" :name "Clojure" :color "#5881d8"}
               (first tags)))))))

(deftest edit-tag-test
  (s/with-fn-validation
    (testing "returns :not-found when tag does not exist"
      (let [db (make-db)]
        (is (= :not-found (controllers.tag/EditTag "nonexistent" {:name "X"} db)))))

    (testing "returns :ok and updates provided fields"
      (let [db (make-db)
            _  (controllers.tag/CreateTag {:slug "clojure" :name "Clojure" :color "#000"} db)]
        (is (= :ok (controllers.tag/EditTag "clojure" {:color "#5881d8"} db)))
        (let [tag (first (controllers.tag/ListTags db))]
          (is (= "Clojure" (:name tag)))
          (is (= "#5881d8" (:color tag))))))))

(deftest delete-tag-test
  (s/with-fn-validation
    (testing "returns :not-found when tag does not exist"
      (let [db (make-db)]
        (is (= :not-found (controllers.tag/DeleteTag "nonexistent" db)))))

    (testing "returns :ok and removes the tag"
      (let [db (make-db)
            _  (controllers.tag/CreateTag {:slug "clojure" :name "Clojure" :color "#5881d8"} db)]
        (is (= :ok (controllers.tag/DeleteTag "clojure" db)))
        (is (= [] (controllers.tag/ListTags db)))))))
```

**Step 2: Run tests to verify they fail**

```bash
lein test blog.controllers.tag-test
```
Expected: FAIL — namespace not found.

**Step 3: Create `controllers/tag.clj`**

```clojure
(ns blog.controllers.tag
  (:require
   [schema.core :as s]
   [blog.wire.in.tag :as wire.in.tag]
   [blog.wire.out.tag :as wire.out.tag]
   [blog.adapters.tag :as adapters.tag]
   [blog.diplomat.datomic.client :as datomic.client]))

(s/defn CreateTag :- s/Str
  [tag :- wire.in.tag/NewTag db]
  (let [model (adapters.tag/wire-in->model tag)]
    (datomic.client/save-tag! db model)
    (:slug tag)))

(s/defn ListTags :- [wire.out.tag/Tag]
  [db]
  (map adapters.tag/model->wire-out (datomic.client/list-tags db)))

(s/defn EditTag :- (s/enum :ok :not-found)
  [slug :- s/Str edits :- wire.in.tag/EditTag db]
  (if-let [existing (datomic.client/find-tag-by-slug! db slug)]
    (let [updated (merge existing
                         (cond-> {}
                           (:name edits)  (assoc :name (:name edits))
                           (:color edits) (assoc :color (:color edits))))]
      (datomic.client/update-tag-by-slug! db slug updated)
      :ok)
    :not-found))

(s/defn DeleteTag :- (s/enum :ok :not-found)
  [slug :- s/Str db]
  (if (datomic.client/find-tag-by-slug! db slug)
    (do (datomic.client/delete-tag-by-slug! db slug) :ok)
    :not-found))
```

**Step 4: Run tests**

```bash
lein test blog.controllers.tag-test
```
Expected: all tests pass.

**Step 5: Run the full test suite**

```bash
lein test
```
Expected: all tests pass.

---

## Task 9: Add tag HTTP routes

**Files:**
- Modify: `src/blog/diplomat/http_server.clj`

**Step 1: Add tag handlers to `http_server.clj`**

Add the require:
```clojure
[blog.wire.in.tag :as wire.in.tag]
[blog.controllers.tag :as controllers.tag]
```

Add the handlers:

```clojure
(defn create-tag [{tag :data components :components}]
  (let [slug (controllers.tag/CreateTag tag (components :db))]
    {:status 201 :body {:slug slug}}))

(defn list-tags [{components :components}]
  {:status 200 :body (controllers.tag/ListTags (components :db))})

(defn edit-tag [{edits :data path-params :path-params components :components}]
  (let [slug (:slug path-params)
        result (controllers.tag/EditTag slug edits (components :db))]
    (if (= result :not-found)
      {:status 404}
      {:status 200})))

(defn delete-tag [{path-params :path-params components :components}]
  (let [slug (:slug path-params)
        result (controllers.tag/DeleteTag slug (components :db))]
    (if (= result :not-found)
      {:status 404}
      {:status 204})))
```

Add the routes:

```clojure
["/api/tag"
 :post (conj common-interceptors
             (utils.request/wrap-schema wire.in.tag/NewTag)
             create-tag)
 :route-name :create-tag]

["/api/tag"
 :get (conj common-interceptors
            list-tags)
 :route-name :list-tags]

["/api/tag/:slug"
 :patch (conj common-interceptors
              (utils.request/wrap-schema wire.in.tag/EditTag)
              edit-tag)
 :route-name :edit-tag]

["/api/tag/:slug"
 :delete (conj common-interceptors
               delete-tag)
 :route-name :delete-tag]
```

**Step 2: Run the full test suite**

```bash
lein test
```
Expected: all tests pass.
