(ns blog.server-test
  (:require [clojure.test :refer [deftest is testing]]
            [blog.server :as server]
            [blog.diplomat.http-server :as http-server]))

(deftest path-match-test
  (testing "path-match with param returns path-params"
    (let [m (server/path-match "/api/post/:id" "/api/post/uuid-here")]
      (is (some? m))
      (is (= "uuid-here" (get-in m [:path-params :id])))))

  (testing "path-match \"/api/post/:id\" \"/api/post/some-id\" returns :path-params {:id \"some-id\"}"
    (is (= {:path-params {:id "some-id"}}
           (server/path-match "/api/post/:id" "/api/post/some-id"))))

  (testing "path-match returns nil when segment count differs (uri shorter)"
    (is (nil? (server/path-match "/api/post/:id" "/api/post"))))

  (testing "path-match returns nil when segment count differs (uri longer)"
    (is (nil? (server/path-match "/api/post/:id" "/api/post/foo/bar")))))

(deftest match-route-path-params-test
  (testing "match-route returns :path-params for PATCH /api/post/:id"
    (let [request {:uri "/api/post/some-id" :request-method :patch}
          match   (server/match-route http-server/routes request)]
      (is (some? match))
      (is (= "some-id" (get-in match [:path-params :id]))))))
