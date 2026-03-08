(ns blog.controllers.post
  (:require
   [schema.core :as s]
   [blog.models.post :as models.post]
   [blog.diplomat.datomic.client :as datomic.client]))

(s/defn CreatePost [post :- models.post/Post db]
  (datomic.client/save-post! db post))