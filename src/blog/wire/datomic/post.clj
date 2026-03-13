(ns blog.wire.datomic.post
  (:require [schema.core :as s]))

(def Post
  {(s/optional-key :post/id) s/Str
   :post/title                s/Str
   :post/content              s/Str
   :post/tag-ids              [s/Int]})
