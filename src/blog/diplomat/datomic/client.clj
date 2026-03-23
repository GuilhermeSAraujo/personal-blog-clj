(ns blog.diplomat.datomic.client
  (:require
   [clojure.java.io :as io]
   [schema.core :as s]
   [blog.models.post :as models.post]
   [blog.models.tag :as models.tag]
   [blog.adapters.datomic.domain-to-wire :as adapters.domain->wire]
   [blog.adapters.datomic.wire-to-domain :as adapters.wire->domain]
   [blog.adapters.datomic.tag-domain-to-wire :as adapters.tag-domain->wire]
   [blog.adapters.datomic.tag-wire-to-domain :as adapters.tag-wire->domain]))

(defn- persist! [db]
  (when-let [path (::edn-path (meta db))]
    (io/make-parents path)
    (spit path (pr-str @db))))

(defprotocol IDatomic
  (transact! [db slug entity])
  (query-all! [db])
  (find-by-slug! [db slug])
  (update! [db slug entity])
  (delete! [db slug])
  (transact-tag! [db ident entity])

  (find-tag-by-ident! [db ident])
  (update-tag! [db ident entity])
  (delete-tag! [db ident]))

(extend-protocol IDatomic
  clojure.lang.Atom
  (transact! [db slug entity]
    (swap! db assoc-in [:posts slug] entity)
    (persist! db)
    slug)
  (query-all! [db]
    (vals (:posts @db)))
  (find-by-slug! [db slug]
    (get-in @db [:posts slug]))
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

  (find-tag-by-ident! [db ident]
    (when-let [attrs (get-in @db [:tags ident])]
      (assoc attrs :tag/ident ident)))
  (update-tag! [db ident entity]
    (swap! db assoc-in [:tags ident] entity)
    (persist! db)
    nil)
  (delete-tag! [db ident]
    (swap! db update :tags dissoc ident)
    (persist! db)
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
