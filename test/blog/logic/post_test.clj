(ns blog.logic.post-test
  (:require [clojure.test :refer [deftest is testing]]
            [schema.core :as s]
            [blog.logic.post :as logic.post]))

(def base-post
  {:slug "p" :title "T" :content "C" :tags []
   :published-at "2026-01-01" :draft? false})

(deftest apply-edits-test
  (let [existing (assoc base-post :title "Old" :tags [:tags/foo :tags/bar])]
    (s/with-fn-validation
      (testing "updates only provided fields"
        (is (= (assoc existing :title "New")
               (logic.post/apply-edits existing {:title "New"}))))

      (testing "updates multiple fields"
        (is (= (assoc existing :title "New" :tags [:tags/baz])
               (logic.post/apply-edits existing {:title "New" :tags [:tags/baz]}))))

      (testing "empty edits returns existing unchanged"
        (is (= existing (logic.post/apply-edits existing {})))))))

(deftest filter-by-tag-test
  (let [clj-post (assoc base-post :slug "a" :tags [:tags/clojure])
        node-post (assoc base-post :slug "b" :tags [:tags/nodejs])
        posts [clj-post node-post]]
    (testing "returns all when no tag filter"
      (is (= posts (logic.post/filter-by-tag nil posts))))
    (testing "filters to matching tag"
      (is (= [clj-post] (logic.post/filter-by-tag "clojure" posts))))
    (testing "returns empty when no match"
      (is (= [] (logic.post/filter-by-tag "rust" posts))))))

(deftest filter-published-test
  (let [pub (assoc base-post :slug "pub" :draft? false)
        draft (assoc base-post :slug "draft" :draft? true)
        posts [pub draft]]
    (testing "removes drafts"
      (is (= [pub] (logic.post/filter-published posts))))))

(deftest paginate-test
  (let [posts (mapv #(assoc base-post :slug (str "p" %)) (range 25))]
    (testing "returns first page"
      (is (= 10 (count (logic.post/paginate 1 10 posts)))))
    (testing "returns correct items on page 2"
      (is (= "p10" (:slug (first (logic.post/paginate 2 10 posts))))))
    (testing "returns remaining items on last page"
      (is (= 5 (count (logic.post/paginate 3 10 posts)))))
    (testing "returns empty for out-of-range page"
      (is (= [] (logic.post/paginate 99 10 posts))))))
