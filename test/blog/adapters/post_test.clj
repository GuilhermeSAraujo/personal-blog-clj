(ns blog.adapters.post-test
  (:require [clojure.test :refer [deftest is testing]]
            [schema.core :as s]
            [blog.adapters.post :as adapters.post]))

(def sample-new-post
  {:slug "my-post" :title "My Post" :content "Hello world"
   :tags ["clojure"] :published-at "2026-03-20" :draft? false})

(def sample-model
  {:slug "my-post" :title "My Post" :content "Hello world"
   :tags [:tags/clojure] :published-at "2026-03-20" :draft? false})

(deftest wire-in->model-test
  (s/with-fn-validation
    (testing "converts all fields including new ones"
      (is (= sample-model
             (adapters.post/wire-in->model sample-new-post))))))

(deftest wire-in-edit->partial-model-test
  (s/with-fn-validation
    (testing "passes through only provided fields"
      (is (= {:title "New Title"}
             (adapters.post/wire-in-edit->partial-model {:title "New Title"}))))

    (testing "passes through tags converting to keywords"
      (is (= {:tags [:tags/foo :tags/bar]}
             (adapters.post/wire-in-edit->partial-model {:tags ["foo" "bar"]}))))

    (testing "passes through published-at and draft?"
      (is (= {:published-at "2026-04-01" :draft? true}
             (adapters.post/wire-in-edit->partial-model {:published-at "2026-04-01" :draft? true}))))

    (testing "empty map returns empty map"
      (is (= {} (adapters.post/wire-in-edit->partial-model {}))))))

(deftest model->wire-out-test
  (s/with-fn-validation
    (testing "converts domain post to wire.out format"
      (is (= {:slug "my-post" :title "My Post" :content "Hello world"
              :tags ["clojure"] :published-at "2026-03-20" :draft? false
              :reading-time 1}
             (adapters.post/model->wire-out sample-model))))

    (testing "reading time rounds up (201 words = 2 min)"
      (let [post (assoc sample-model :content (clojure.string/join " " (repeat 201 "word")))]
        (is (= 2 (:reading-time (adapters.post/model->wire-out post))))))

    (testing "empty content gives reading time of 1"
      (let [post (assoc sample-model :content "")]
        (is (= 1 (:reading-time (adapters.post/model->wire-out post))))))))
