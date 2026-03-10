(ns blog.adapters.post-test
  (:require [clojure.test :refer [deftest is testing]]
            [schema.core :as s]
            [blog.adapters.post :as adapters.post]))

(deftest wire-in-edit->partial-model-test
  (s/with-fn-validation
    (testing "passes through only provided fields"
      (is (= {:title "New Title"}
             (adapters.post/wire-in-edit->partial-model {:title "New Title"}))))

    (testing "passes through multiple fields"
      (is (= {:title "T" :tag-ids [1 2]}
             (adapters.post/wire-in-edit->partial-model {:title "T" :tag-ids [1 2]}))))

    (testing "empty map returns empty map"
      (is (= {}
             (adapters.post/wire-in-edit->partial-model {}))))))
