(ns blog.diplomat.datomic.client-test
  (:require [clojure.test :refer [deftest is testing]]
            [schema.core :as s]
            [blog.diplomat.datomic.client :as datomic.client]))

(defn make-db [] (atom {:posts {} :tags {}}))

(def post-a
  {:slug "post-a" :title "A" :content "Content A"
   :tags [:tags/clojure] :published-at "2026-01-01" :draft? false})

(def post-b
  {:slug "post-b" :title "B" :content "Content B"
   :tags [:tags/nodejs] :published-at "2026-02-01" :draft? false})

(deftest save-and-find-by-slug-test
  (s/with-fn-validation
    (testing "returns nil for unknown slug"
      (is (nil? (datomic.client/find-post-by-slug! (make-db) "nope"))))
    (testing "returns domain model after save"
      (let [db (make-db)
            _ (datomic.client/save-post! db post-a)]
        (is (= post-a (datomic.client/find-post-by-slug! db "post-a")))))))

(deftest save-post-returns-slug-test
  (s/with-fn-validation
    (let [db (make-db)
          slug (datomic.client/save-post! db post-a)]
      (is (= "post-a" slug)))))

(deftest list-posts-test
  (s/with-fn-validation
    (let [db (make-db)
          _ (datomic.client/save-post! db post-a)
          _ (datomic.client/save-post! db post-b)]
      (testing "returns all posts"
        (is (= 2 (count (datomic.client/list-posts db))))))))

(deftest update-post-test
  (s/with-fn-validation
    (let [db (make-db)
          _ (datomic.client/save-post! db post-a)
          updated (assoc post-a :title "Updated")]
      (datomic.client/update-post! db "post-a" updated)
      (is (= "Updated" (:title (datomic.client/find-post-by-slug! db "post-a")))))))

(deftest delete-post-test
  (s/with-fn-validation
    (let [db (make-db)
          _ (datomic.client/save-post! db post-a)]
      (datomic.client/delete-post! db "post-a")
      (is (nil? (datomic.client/find-post-by-slug! db "post-a"))))))

(deftest save-and-list-tags-test
  (s/with-fn-validation
    (testing "save-tag! stores tag and list-tags returns it"
      (let [db (make-db)
            tag {:ident :tags/clojure :name "Clojure" :color "#5881d8"}
            _ (datomic.client/save-tag! db tag)
            all (datomic.client/list-tags db)]
        (is (= 1 (count all)))
        (is (= tag (first all)))))))

(deftest find-tag-by-slug-test
  (s/with-fn-validation
    (testing "returns nil when tag does not exist"
      (is (nil? (datomic.client/find-tag-by-slug! (make-db) "nonexistent"))))
    (testing "returns domain model when tag exists"
      (let [db (make-db)
            tag {:ident :tags/clojure :name "Clojure" :color "#5881d8"}
            _ (datomic.client/save-tag! db tag)]
        (is (= tag (datomic.client/find-tag-by-slug! db "clojure")))))))

(deftest update-tag-by-slug-test
  (s/with-fn-validation
    (testing "updates an existing tag"
      (let [db (make-db)
            _ (datomic.client/save-tag! db {:ident :tags/clojure :name "Clojure" :color "#000"})
            updated {:ident :tags/clojure :name "Clojure" :color "#5881d8"}
            _ (datomic.client/update-tag-by-slug! db "clojure" updated)]
        (is (= updated (datomic.client/find-tag-by-slug! db "clojure")))))))

(deftest delete-tag-by-slug-test
  (s/with-fn-validation
    (testing "removes the tag from the store"
      (let [db (make-db)
            _ (datomic.client/save-tag! db {:ident :tags/clojure :name "Clojure" :color "#5881d8"})
            _ (datomic.client/delete-tag-by-slug! db "clojure")]
        (is (nil? (datomic.client/find-tag-by-slug! db "clojure")))
        (is (= [] (datomic.client/list-tags db)))))))
