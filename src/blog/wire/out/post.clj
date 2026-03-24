(ns blog.wire.out.post
  (:require [schema.core :as s]))

(def Post
  {:slug s/Str
   :title s/Str
   :content s/Str
   :tags [s/Str]
   :published-at s/Str
   :draft? s/Bool
   :reading-time s/Int})
