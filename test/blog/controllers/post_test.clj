(ns blog.controllers.post-test
  (:require [clojure.test :refer [deftest is testing]]
            [schema.core :as s]
            [blog.controllers.post :as controllers.post]
            [blog.diplomat.datomic.client :as datomic.client]))

(defn make-db [] (atom {:posts {} :tags {}}))

(def new-post
  {:slug "my-post" :title "T" :content "C"
   :tags [] :published-at "2026-03-20" :draft? false})

(def draft-post
  {:slug "my-draft" :title "Draft" :content "WIP"
   :tags [] :published-at "2026-03-20" :draft? true})

(deftest create-post-returns-slug
  (s/with-fn-validation
    (let [db (make-db)
          slug (controllers.post/CreatePost new-post db)]
      (is (= "my-post" slug)))))

(deftest list-posts-excludes-drafts
  (s/with-fn-validation
    (let [db (make-db)
          _ (controllers.post/CreatePost new-post db)
          _ (controllers.post/CreatePost draft-post db)]
      (testing "only returns published posts"
        (let [posts (controllers.post/ListPosts db {} {})]
          (is (= 1 (count posts)))
          (is (= "my-post" (:slug (first posts)))))))))

(deftest list-posts-tag-filter
  (s/with-fn-validation
    (let [db (make-db)
          clj-post (assoc new-post :slug "clj-post" :tags ["clojure"])
          js-post (assoc new-post :slug "js-post" :tags ["nodejs"])
          _ (controllers.post/CreatePost clj-post db)
          _ (controllers.post/CreatePost js-post db)]
      (testing "filters by tag"
        (is (= 1 (count (controllers.post/ListPosts db {:tag "clojure"} {})))))
      (testing "returns all when no filter"
        (is (= 2 (count (controllers.post/ListPosts db {} {}))))))))

(deftest list-posts-pagination
  (s/with-fn-validation
    (let [db (make-db)
          posts (mapv #(assoc new-post :slug (str "p" %)) (range 15))
          _ (doseq [p posts] (controllers.post/CreatePost p db))]
      (testing "default page size is 10"
        (is (= 10 (count (controllers.post/ListPosts db {} {})))))
      (testing "page 2 returns remainder"
        (is (= 5 (count (controllers.post/ListPosts db {:page "2"} {}))))))))

(deftest get-post-by-slug-test
  (s/with-fn-validation
    (let [db (make-db)
          _ (controllers.post/CreatePost new-post db)]
      (testing "returns post when found"
        (is (some? (controllers.post/GetPost "my-post" db))))
      (testing "returns :not-found when missing"
        (is (= :not-found (controllers.post/GetPost "nope" db)))))))

(deftest edit-post-test
  (s/with-fn-validation
    (testing "returns :not-found for unknown slug"
      (let [db (make-db)]
        (is (= :not-found (controllers.post/EditPost "nope" {} db)))))

    (testing "returns :ok and updates fields"
      (let [db (make-db)
            _ (controllers.post/CreatePost new-post db)]
        (is (= :ok (controllers.post/EditPost "my-post" {:title "New"} db)))
        (is (= "New" (:title (controllers.post/GetPost "my-post" db))))))))

(deftest delete-post-test
  (s/with-fn-validation
    (testing "returns :not-found for unknown slug"
      (let [db (make-db)]
        (is (= :not-found (controllers.post/DeletePost "nope" db)))))
    (testing "returns :ok and removes post"
      (let [db (make-db)
            _ (controllers.post/CreatePost new-post db)]
        (is (= :ok (controllers.post/DeletePost "my-post" db)))
        (is (= :not-found (controllers.post/GetPost "my-post" db)))))))
