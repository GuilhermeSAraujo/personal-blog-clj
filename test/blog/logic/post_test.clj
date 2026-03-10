(ns blog.logic.post-test
  (:require [clojure.test :refer [deftest is testing]]
            [schema.core :as s]
            [blog.logic.post :as logic.post]))

(deftest apply-edits-test
  (let [existing {:title "Old Title" :content "Old Content" :tag-ids [1 2]}]
    (s/with-fn-validation
      (testing "updates only the provided fields"
        (let [edits {:title "New Title"}]
          (is (= {:title "New Title" :content "Old Content" :tag-ids [1 2]}
                 (logic.post/apply-edits existing edits)))))

      (testing "updates multiple fields"
        (let [edits {:title "New Title" :tag-ids [3]}]
          (is (= {:title "New Title" :content "Old Content" :tag-ids [3]}
                 (logic.post/apply-edits existing edits)))))

      (testing "empty edits returns existing post unchanged"
        (let [existing {:title "Title" :content "Content" :tag-ids []}]
          (is (= existing
                 (logic.post/apply-edits existing {}))))))))
