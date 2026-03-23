(ns blog.wire.datomic.tag
  (:require [schema.core :as s]))

; ident is the KEY in the atom map, NOT stored as an attribute value.
; This schema represents only the stored value map.
(def Tag
  {:tag/name  s/Str
   :tag/color s/Str})
