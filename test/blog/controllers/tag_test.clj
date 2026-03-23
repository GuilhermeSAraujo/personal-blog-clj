(ns blog.controllers.tag-test
  (:require [clojure.test :refer [deftest is testing]]
            [schema.core :as s]
            [blog.controllers.tag :as controllers.tag]
            [blog.diplomat.datomic.client :as datomic.client]))

(defn make-db [] (atom {:posts {} :tags {}}))

(deftest create-tag-test
  (s/with-fn-validation
    (testing "creates a tag and returns its slug"
      (let [db   (make-db)
            slug (controllers.tag/CreateTag {:slug "clojure"
                                            :name "Clojure"
                                            :color "#5881d8"} db)]
        (is (= "clojure" slug))
        (is (some? (datomic.client/find-tag-by-slug! db "clojure")))))))

(deftest create-tag-wrong-color-test
  (s/with-fn-validation
    (testing "doesnt create the tag with invalid color"
      (let [db (make-db)]
        (is (thrown? clojure.lang.ExceptionInfo
                     (controllers.tag/CreateTag {:slug  "clojure"
                                                 :name  "Clojure"
                                                 :color 12345} db)))))))

(deftest create-tag-wrong-name-test
  (s/with-fn-validation
    (testing "doesnt create the tag with invalid name"
      (let [db (make-db)]
        (is (thrown? clojure.lang.ExceptionInfo
                     (controllers.tag/CreateTag {:slug  "clojure"
                                                 :name  123
                                                 :color "12345"} db)))))))

(deftest create-tag-wrong-slug-test
  (s/with-fn-validation
    (testing "doesnt create the tag with invalid name"
      (let [db (make-db)]
        (is (thrown? clojure.lang.ExceptionInfo
                     (controllers.tag/CreateTag {:slug  1234
                                                 :name  "Clojure"
                                                 :color "#hex"} db)))))))

(deftest list-tags-test
  (s/with-fn-validation
    (testing "returns all tags in wire-out format"
      (let [db   (make-db)
            _    (controllers.tag/CreateTag {:slug "clojure" :name "Clojure" :color "#5881d8"} db)
            tags (controllers.tag/ListTags db)]
        (is (= 1 (count tags)))
        (is (= {:slug "clojure" :name "Clojure" :color "#5881d8"}
               (first tags)))))))

(deftest edit-tag-test
  (s/with-fn-validation
    (testing "returns :not-found when tag does not exist"
      (let [db (make-db)]
        (is (= :not-found (controllers.tag/EditTag "nonexistent" {:name "X"} db)))))

    (testing "returns :ok and updates provided fields"
      (let [db (make-db)
            _  (controllers.tag/CreateTag {:slug "clojure" :name "Clojure" :color "#000"} db)]
        (is (= :ok (controllers.tag/EditTag "clojure" {:color "#5881d8"} db)))
        (let [tag (first (controllers.tag/ListTags db))]
          (is (= "Clojure" (:name tag)))
          (is (= "#5881d8" (:color tag))))))))

(deftest delete-tag-test
  (s/with-fn-validation
    (testing "returns :not-found when tag does not exist"
      (let [db (make-db)]
        (is (= :not-found (controllers.tag/DeleteTag "nonexistent" db)))))

    (testing "returns :ok and removes the tag"
      (let [db (make-db)
            _  (controllers.tag/CreateTag {:slug "clojure" :name "Clojure" :color "#5881d8"} db)]
        (is (= :ok (controllers.tag/DeleteTag "clojure" db)))
        (is (= [] (controllers.tag/ListTags db)))))))
