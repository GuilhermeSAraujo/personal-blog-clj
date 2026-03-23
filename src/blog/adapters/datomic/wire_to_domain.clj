(ns blog.adapters.datomic.wire-to-domain
  (:require
   [schema.core :as s]
   [blog.models.post :as models.post]
   [blog.wire.datomic.post :as wire.datomic.post]))

(s/defn wire->domain :- models.post/Post
  [post :- wire.datomic.post/Post]
  {:slug (:post/slug post)
   :title (:post/title post)
   :content (:post/content post)
   :tags (:post/tags post)
   :published-at (:post/published-at post)
   :draft? (:post/draft? post)})
