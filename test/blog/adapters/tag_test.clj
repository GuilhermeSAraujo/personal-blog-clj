(ns blog.adapters.tag-test
  (:require [clojure.test :refer [deftest is testing]]
            [schema.core :as s]
            [blog.adapters.tag :as adapters.tag]))

(deftest wire-in->model-test
  (s/with-fn-validation
    (testing "converts NewTag wire-in to domain model (slug → keyword ident)"
      (is (= {:ident :tags/clojure :name "Clojure" :color "#5881d8"}
             (adapters.tag/wire-in->model {:slug "clojure"
                                           :name "Clojure"
                                           :color "#5881d8"}))))
    (testing "handles slugs with hyphens"
      (is (= {:ident :tags/node-js :name "Node.js" :color "#68A063"}
             (adapters.tag/wire-in->model {:slug "node-js"
                                           :name "Node.js"
                                           :color "#68A063"}))))))

(deftest model->wire-out-test
  (s/with-fn-validation
    (testing "converts domain tag to wire-out (keyword ident → slug string)"
      (is (= {:slug "clojure" :name "Clojure" :color "#5881d8"}
             (adapters.tag/model->wire-out {:ident :tags/clojure
                                            :name  "Clojure"
                                            :color "#5881d8"}))))))
