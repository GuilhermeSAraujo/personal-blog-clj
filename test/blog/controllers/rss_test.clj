(ns blog.controllers.rss-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [blog.controllers.rss :as controllers.rss]))

(defn make-db []
  (let [posts {"pub" {:post/slug "pub" :post/title "Published"
                      :post/content "Content" :post/tags []
                      :post/published-at "2026-03-20" :post/draft? false}
               "draft" {:post/slug "draft" :post/title "Draft"
                        :post/content "WIP" :post/tags []
                        :post/published-at "2026-03-20" :post/draft? true}}]
    (atom {:posts posts :tags {}})))

(deftest rss-feed-test
  (let [xml (controllers.rss/RssFeed (make-db))]
    (testing "returns a string"
      (is (string? xml)))
    (testing "contains published post title"
      (is (str/includes? xml "Published")))
    (testing "does not contain draft post title"
      (is (not (str/includes? xml "Draft"))))
    (testing "is valid RSS structure"
      (is (str/includes? xml "<rss"))
      (is (str/includes? xml "<channel"))
      (is (str/includes? xml "<item")))))
