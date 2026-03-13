(ns blog.models.post
  (:require [schema.core :as s]))


(def Post
  {(s/optional-key :id) s/Str
   :title                s/Str
   :content              s/Str
   :tag-ids              [s/Int]})
