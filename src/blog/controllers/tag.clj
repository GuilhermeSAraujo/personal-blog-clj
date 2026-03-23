(ns blog.controllers.tag
  (:require
   [schema.core :as s]
   [blog.wire.in.tag :as wire.in.tag]
   [blog.wire.out.tag :as wire.out.tag]
   [blog.adapters.tag :as adapters.tag]
   [blog.diplomat.datomic.client :as datomic.client]))

(s/defn CreateTag :- s/Str
  [tag :- wire.in.tag/NewTag db]
  (let [model (adapters.tag/wire-in->model tag)]
    (datomic.client/save-tag! db model)
    (:slug tag)))

(s/defn ListTags :- [wire.out.tag/Tag]
  [db]
  (mapv adapters.tag/model->wire-out (datomic.client/list-tags db)))

(s/defn EditTag :- (s/enum :ok :not-found)
  [slug :- s/Str edits :- wire.in.tag/EditTag db]
  (if-let [existing (datomic.client/find-tag-by-slug! db slug)]
    (let [updated (merge existing
                         (select-keys edits [:name :color]))]
      (datomic.client/update-tag-by-slug! db slug updated)
      :ok)
    :not-found))

(s/defn DeleteTag :- (s/enum :ok :not-found)
  [slug :- s/Str db]
  (if (datomic.client/find-tag-by-slug! db slug)
    (do (datomic.client/delete-tag-by-slug! db slug) :ok)
    :not-found))
