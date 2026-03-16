(ns blog.logic.auth-test
  (:require [clojure.test :refer [deftest is testing]]
            [schema.core :as s]
            [blog.logic.auth :as logic.auth]))

(deftest valid-credentials-test
  (s/with-fn-validation
    (testing "returns true when username and password match"
      (is (true? (logic.auth/valid-credentials? "admin" "secret" "admin" "secret"))))

    (testing "returns false when username does not match"
      (is (false? (logic.auth/valid-credentials? "admin" "secret" "wrong" "secret"))))

    (testing "returns false when password does not match"
      (is (false? (logic.auth/valid-credentials? "admin" "secret" "admin" "wrong"))))

    (testing "returns false when both are wrong"
      (is (false? (logic.auth/valid-credentials? "admin" "secret" "x" "y"))))))
