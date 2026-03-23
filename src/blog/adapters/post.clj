(ns blog.adapters.post
  (:require
   [clojure.string :as str]
   [schema.core :as s]
   [blog.wire.in.post :as wire.in.post]
   [blog.wire.out.post :as wire.out.post]
   [blog.models.post :as models.post]))

(defn- compute-reading-time [content]
  (let [words (remove empty? (str/split (str/trim content) #"\s+"))
        minutes (int (Math/ceil (/ (count words) 200.0)))]
    (max 1 minutes)))

(s/defn wire-in->model :- models.post/Post
  [new-post :- wire.in.post/NewPost]
  {:slug (:slug new-post)
   :title (:title new-post)
   :content (:content new-post)
   :tags (mapv #(keyword "tags" %) (:tags new-post))
   :published-at (:published-at new-post)
   :draft? (:draft? new-post)})

(s/defn wire-in-edit->partial-model :- {s/Keyword s/Any}
  [edits :- wire.in.post/EditPost]
  (cond-> (select-keys edits [:title :content :published-at :draft?])
    (contains? edits :tags)
    (assoc :tags (mapv #(keyword "tags" %) (:tags edits)))))

(s/defn model->wire-out :- wire.out.post/Post
  [post :- models.post/Post]
  {:slug (:slug post)
   :title (:title post)
   :content (:content post)
   :tags (mapv name (:tags post))
   :published-at (:published-at post)
   :draft? (:draft? post)
   :reading-time (compute-reading-time (:content post))})
