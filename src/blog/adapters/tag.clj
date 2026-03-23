(ns blog.adapters.tag
  (:require
   [schema.core :as s]
   [blog.wire.in.tag :as wire.in.tag]
   [blog.wire.out.tag :as wire.out.tag]
   [blog.models.tag :as models.tag]))

(s/defn wire-in->model :- models.tag/Tag
  [tag :- wire.in.tag/NewTag]
  {:ident (keyword "tags" (:slug tag))
   :name  (:name tag)
   :color (:color tag)})

(s/defn model->wire-out :- wire.out.tag/Tag
  [tag :- models.tag/Tag]
  {:slug  (name (:ident tag))
   :name  (:name tag)
   :color (:color tag)})
