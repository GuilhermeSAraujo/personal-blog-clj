(ns blog.models.post
  (:require [schema.core :as s]))


(def Post
  {:title s/Str
   :content s/Str
   :tag-ids [s/Int]})