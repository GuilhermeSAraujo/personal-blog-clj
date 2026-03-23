(ns blog.wire.in.post
  (:require [schema.core :as s]))

(def NewPost
  {:slug s/Str
   :title s/Str
   :content s/Str
   :tags [s/Str]
   :published-at s/Str
   :draft? s/Bool})

(def EditPost
  {(s/optional-key :title) s/Str
   (s/optional-key :content) s/Str
   (s/optional-key :tags) [s/Str]
   (s/optional-key :published-at) s/Str
   (s/optional-key :draft?) s/Bool})
