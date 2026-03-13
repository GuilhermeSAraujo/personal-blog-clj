(ns blog.controllers.post-test
  (:require [clojure.test :refer [deftest is testing]]
            [schema.core :as s]
            [blog.controllers.post :as controllers.post]
            [blog.diplomat.datomic.client :as datomic.client]))

(defn make-db [] (atom {}))

(deftest edit-post-test
  (s/with-fn-validation
    (testing "returns :not-found when post does not exist"
      (let [db (make-db)]
        (is (= :not-found (controllers.post/EditPost "nonexistent" {} db)))))

    (testing "returns :ok and updates only the provided fields"
      (let [db (make-db)
            id (datomic.client/save-post! db {:title "Old" :content "Content" :tag-ids [1]})]
        (is (= :ok (controllers.post/EditPost id {:title "New"} db)))
        (let [listed (first (datomic.client/list-posts db))]
          (is (= id (:id listed)))
          (is (= {:title "New" :content "Content" :tag-ids [1]}
                 (dissoc listed :id))))))

    (testing "returns :ok with empty edits, post unchanged"
      (let [db (make-db)
            id (datomic.client/save-post! db {:title "Title" :content "Content" :tag-ids []})]
        (is (= :ok (controllers.post/EditPost id {} db)))
        (let [listed (first (datomic.client/list-posts db))]
          (is (= id (:id listed)))
          (is (= {:title "Title" :content "Content" :tag-ids []}
                 (dissoc listed :id))))))))

(deftest create-post-returns-id
  (s/with-fn-validation
    (let [db (make-db)
          id (controllers.post/CreatePost {:title "T" :content "C" :tag-ids []} db)]
      (is (string? id))
      (is (< 0 (count id)))
      (is (some? (datomic.client/find-by-id! db id))))))
