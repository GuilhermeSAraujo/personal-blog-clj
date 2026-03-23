(ns blog.adapters.datomic.domain-to-wire
  (:require
   [schema.core :as s]
   [blog.models.post :as models.post]
   [blog.wire.datomic.post :as wire.datomic.post]))

(s/defn domain->wire :- wire.datomic.post/Post
  [post :- models.post/Post]
  {:post/slug (:slug post)
   :post/title (:title post)
   :post/content (:content post)
   :post/tags (:tags post)
   :post/published-at (:published-at post)
   :post/draft? (:draft? post)})
