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
  (let [tag (:tag query-params)
        page (Integer/parseInt (get query-params :page "1"))
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
          updated (logic.post/apply-edits existing partial-model)]
      (datomic.client/update-post! db slug updated)
      :ok)
    :not-found))

(s/defn DeletePost :- (s/enum :ok :not-found)
  [slug :- s/Str db]
  (if (datomic.client/find-post-by-slug! db slug)
    (do (datomic.client/delete-post! db slug) :ok)
    :not-found))
