(ns blog.server-test
  (:require [clojure.test :refer [deftest is testing]]
            [blog.server :as server]
            [blog.diplomat.http-server :as http-server]))

(deftest path-match-test
  (testing "path-match with param returns path-params"
    (let [m (server/path-match "/api/post/:slug" "/api/post/my-post")]
      (is (some? m))
      (is (= "my-post" (get-in m [:path-params :slug])))))

  (testing "path-match \"/api/post/:slug\" \"/api/post/some-slug\" returns :path-params {:slug \"some-slug\"}"
    (is (= {:path-params {:slug "some-slug"}}
           (server/path-match "/api/post/:slug" "/api/post/some-slug"))))

  (testing "path-match returns nil when segment count differs (uri shorter)"
    (is (nil? (server/path-match "/api/post/:slug" "/api/post"))))

  (testing "path-match returns nil when segment count differs (uri longer)"
    (is (nil? (server/path-match "/api/post/:slug" "/api/post/foo/bar")))))

(deftest match-route-path-params-test
  (testing "match-route returns :path-params for PATCH /api/post/:slug"
    (let [request {:uri "/api/post/some-slug" :request-method :patch}
          match (server/match-route http-server/routes request)]
      (is (some? match))
      (is (= "some-slug" (get-in match [:path-params :slug]))))))
