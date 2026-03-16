(ns blog.controllers.auth-test
  (:require [clojure.test :refer [deftest is testing]]
            [schema.core :as s]
            [blog.controllers.auth :as controllers.auth]))

(deftest login-test
  (s/with-fn-validation
    (testing "returns {:ok true} when credentials match"
      (is (= {:ok true}
             (controllers.auth/Login {:username "admin" :password "secret"}
                                     "admin" "secret"))))

    (testing "returns {:error Unauthorized} when username is wrong"
      (is (= {:error "Unauthorized"}
             (controllers.auth/Login {:username "bad" :password "secret"}
                                     "admin" "secret"))))

    (testing "returns {:error Unauthorized} when password is wrong"
      (is (= {:error "Unauthorized"}
             (controllers.auth/Login {:username "admin" :password "bad"}
                                     "admin" "secret"))))))
