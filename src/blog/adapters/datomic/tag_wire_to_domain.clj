(ns blog.adapters.datomic.tag-wire-to-domain
  (:require
   [schema.core :as s]
   [blog.models.tag :as models.tag]
   [blog.wire.datomic.tag :as wire.datomic.tag]))

(s/defn wire->domain :- models.tag/Tag
  [[ident attrs]]   ; receives a [keyword map] tuple
  {:ident ident
   :name  (:tag/name attrs)
   :color (:tag/color attrs)})
