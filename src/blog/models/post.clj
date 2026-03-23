(ns blog.models.post
  (:require [schema.core :as s]))

(def Post
  {:slug s/Str
   :title s/Str
   :content s/Str
   :tags [s/Keyword]
   :published-at s/Str
   :draft? s/Bool})
