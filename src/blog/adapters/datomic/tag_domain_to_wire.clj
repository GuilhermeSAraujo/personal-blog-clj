(ns blog.adapters.datomic.tag-domain-to-wire
  (:require
   [schema.core :as s]
   [blog.models.tag :as models.tag]
   [blog.wire.datomic.tag :as wire.datomic.tag]))

(s/defn domain->wire :- wire.datomic.tag/Tag
  [tag :- models.tag/Tag]
  {:tag/name  (:name tag)
   :tag/color (:color tag)})
