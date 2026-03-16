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
  [db]
  (map adapters.post/model->wire-out (datomic.client/list-posts db)))

(s/defn EditPost :- (s/enum :ok :not-found)
  [id :- s/Str edits :- wire.in.post/EditPost db]
  (if-let [existing (datomic.client/find-post-by-id! db id)]
    (let [partial-model (adapters.post/wire-in-edit->partial-model edits)
          updated (logic.post/apply-edits existing partial-model)]
      (datomic.client/update-post! db id updated)
      :ok)
    :not-found))
