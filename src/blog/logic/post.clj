(ns blog.logic.post
  (:require [schema.core :as s]
            [blog.models.post :as models.post]))

(s/defn apply-edits :- models.post/Post
  [existing :- models.post/Post
   edits :- {s/Keyword s/Any}]
  (merge existing edits))

(defn filter-by-tag [tag-slug posts]
  (if (nil? tag-slug)
    posts
    (let [tag-kw (keyword "tags" tag-slug)]
      (filter #(some #{tag-kw} (:tags %)) posts))))

(defn filter-published [posts]
  (remove :draft? posts))

(defn paginate [page page-size posts]
  (let [offset (* (dec page) page-size)]
    (vec (take page-size (drop offset posts)))))
