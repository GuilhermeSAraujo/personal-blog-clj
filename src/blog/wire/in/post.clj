(ns blog.wire.in.post
  (:require [schema.core :as s]))

(def NewPost
  {:title s/Str
   :content s/Str
   :tag-ids [s/Int]})