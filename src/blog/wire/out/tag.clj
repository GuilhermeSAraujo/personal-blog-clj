(ns blog.wire.out.tag
  (:require [schema.core :as s]))

(def Tag
  {:slug  s/Str
   :name  s/Str
   :color s/Str})
