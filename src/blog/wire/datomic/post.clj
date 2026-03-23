(ns blog.wire.datomic.post
  (:require [schema.core :as s]))

(def Post
  {:post/slug s/Str
   :post/title s/Str
   :post/content s/Str
   :post/tags [s/Keyword]
   :post/published-at s/Str
   :post/draft? s/Bool})
