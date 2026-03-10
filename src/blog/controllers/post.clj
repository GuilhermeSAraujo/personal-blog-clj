(ns blog.controllers.post
  (:require
   [schema.core :as s]
   [blog.models.post :as models.post]
   [blog.logic.post :as logic.post]
   [blog.adapters.datomic.wire-to-domain :as adapters.wire->domain]
   [blog.diplomat.datomic.client :as datomic.client]))

(s/defn CreatePost [post :- models.post/Post db]
  (datomic.client/save-post! db post))

(s/defn ListPosts :- [models.post/Post]
  [db]
  (datomic.client/list-posts db))

(s/defn EditPost
  [id :- s/Str edits :- {s/Keyword s/Any} db]
  (if-let [wire-entity (datomic.client/find-by-id! db id)]
    (let [existing (adapters.wire->domain/wire->domain wire-entity)
          updated (logic.post/apply-edits existing edits)]
      (datomic.client/update-post! db id updated)
      :ok)
    :not-found))
