(ns blog.adapters.post
  (:require
   [schema.core :as s]
   [blog.wire.in.post :as wire.in.post]
   [blog.wire.out.post :as wire.out.post]
   [blog.models.post :as models.post]))

(s/defn wire-in->model :- models.post/Post [new-post :- wire.in.post/NewPost]
  {:title (get new-post :title)
   :content (get new-post :content)
   :tag-ids (get new-post :tag-ids)})

(s/defn wire-in-edit->partial-model :- {s/Keyword s/Any}
  [edits :- wire.in.post/EditPost]
  (select-keys edits [:title :content :tag-ids]))

(s/defn model->wire-out :- wire.out.post/Post
  [post :- models.post/Post]
  {:id (:id post)
   :title (:title post)
   :content (:content post)
   :tag-ids (:tag-ids post)})