(ns blog.adapters.datomic.wire-to-domain
  (:require
   [schema.core :as s]
   [blog.models.post :as models.post]
   [blog.wire.datomic.post :as wire.datomic.post]))

(s/defn wire->domain :- models.post/Post
  [post :- wire.datomic.post/Post]
  {:title    (:post/title post)
   :content  (:post/content post)
   :tag-ids  (:post/tag-ids post)})
