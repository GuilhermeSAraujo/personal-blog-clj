(ns blog.wire.datomic.post
  (:require [schema.core :as s]))

(def Post
  {:post/title    s/Str
   :post/content  s/Str
   :post/tag-ids  [s/Int]})
