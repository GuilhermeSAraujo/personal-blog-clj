(ns blog.adapters.datomic.post-adapters-test
  (:require [clojure.test :refer [deftest is testing]]
            [schema.core :as s]
            [blog.adapters.datomic.domain-to-wire :as d->w]
            [blog.adapters.datomic.wire-to-domain :as w->d]))

(def sample-post
  {:slug "getting-started"
   :title "Getting Started"
   :content "Hello world"
   :tags [:tags/clojure]
   :published-at "2026-03-20"
   :draft? false})

(def sample-wire
  {:post/slug "getting-started"
   :post/title "Getting Started"
   :post/content "Hello world"
   :post/tags [:tags/clojure]
   :post/published-at "2026-03-20"
   :post/draft? false})

(deftest domain->wire-test
  (s/with-fn-validation
    (testing "maps all fields to wire namespaced keys"
      (is (= sample-wire (d->w/domain->wire sample-post))))))

(deftest wire->domain-test
  (s/with-fn-validation
    (testing "maps all wire fields back to domain keys"
      (is (= sample-post (w->d/wire->domain sample-wire))))))
