(ns blog.wire.in.tag
  (:require [schema.core :as s]))

(def NewTag
  {:slug  s/Str
   :name  s/Str
   :color s/Str})

(def EditTag
  {(s/optional-key :name)  s/Str
   (s/optional-key :color) s/Str})
