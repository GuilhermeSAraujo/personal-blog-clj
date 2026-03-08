(ns blog.adapters.post
  (:require
   [schema.core :as s]
   [blog.wire.in.post :as wire.in.post]
   [blog.models.post :as models.post]))

(s/defn wire-in->model :- models.post/Post [new-post :- wire.in.post/NewPost]
  {:title (get new-post :title)
   :content (get new-post :content)
   :tag-ids (get new-post :tag-ids)})