(ns blog.models.tag
  (:require [schema.core :as s]))

(def Tag
  {:ident s/Keyword   ; e.g. :tags/clojure
   :name  s/Str       ; e.g. "Clojure"
   :color s/Str})     ; e.g. "#5881d8"
