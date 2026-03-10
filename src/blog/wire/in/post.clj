(ns blog.wire.in.post
  (:require [schema.core :as s]))

(def NewPost
  {:title s/Str
   :content s/Str
   :tag-ids [s/Int]})

(def EditPost
  {(s/optional-key :title) s/Str
   (s/optional-key :content) s/Str
   (s/optional-key :tag-ids) [s/Int]})