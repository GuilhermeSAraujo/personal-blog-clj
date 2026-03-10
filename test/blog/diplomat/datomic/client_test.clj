(ns blog.diplomat.datomic.client-test
  (:require [clojure.test :refer [deftest is testing]]
            [schema.core :as s]
            [blog.diplomat.datomic.client :as datomic.client]))

(defn make-db [] (atom {}))

(deftest find-by-id-test
  (s/with-fn-validation
    (testing "returns nil when post not found"
      (let [db (make-db)]
        (is (nil? (datomic.client/find-by-id! db "nonexistent")))))

    (testing "returns stored datomic wire entity when found"
      (let [db (make-db)
            post {:title "T" :content "C" :tag-ids []}
            id (datomic.client/save-post! db post)]
        (is (= {:post/title "T" :post/content "C" :post/tag-ids []}
               (datomic.client/find-by-id! db id)))))))

(deftest update-post-test
  (s/with-fn-validation
    (testing "updates an existing post and find-by-id returns updated version"
      (let [db (make-db)
            original {:title "Old" :content "Content" :tag-ids [1]}
            id (datomic.client/save-post! db original)
            updated {:title "New" :content "Content" :tag-ids [1]}]
        (datomic.client/update-post! db id updated)
        (is (= {:post/title "New" :post/content "Content" :post/tag-ids [1]}
               (datomic.client/find-by-id! db id)))))))
