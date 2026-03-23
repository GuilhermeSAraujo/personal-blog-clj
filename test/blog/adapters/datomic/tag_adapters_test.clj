(ns blog.adapters.datomic.tag-adapters-test
  (:require [clojure.test :refer [deftest is testing]]
            [schema.core :as s]
            [blog.adapters.datomic.tag-domain-to-wire :as domain->wire]
            [blog.adapters.datomic.tag-wire-to-domain :as wire->domain]))

(deftest domain->wire-test
  (s/with-fn-validation
    (testing "converts domain tag to datomic wire format (strips ident)"
      (is (= {:tag/name "Clojure" :tag/color "#5881d8"}
             (domain->wire/domain->wire {:ident :tags/clojure
                                         :name  "Clojure"
                                         :color "#5881d8"}))))))

(deftest wire->domain-test
  (s/with-fn-validation
    (testing "converts [ident attrs] tuple to domain tag"
      (is (= {:ident :tags/clojure :name "Clojure" :color "#5881d8"}
             (wire->domain/wire->domain [:tags/clojure
                                         {:tag/name "Clojure" :tag/color "#5881d8"}]))))))
