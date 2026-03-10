(ns blog.logic.post
  (:require [schema.core :as s]
            [blog.models.post :as models.post]))

(s/defn apply-edits :- models.post/Post
  [existing :- models.post/Post
   edits :- {s/Keyword s/Any}]
  (merge existing edits))
