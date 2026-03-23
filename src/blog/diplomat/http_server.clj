(ns blog.diplomat.http-server
  (:require
   [blog.env :as env]
   [blog.wire.in.auth :as wire.in.auth]
   [blog.wire.in.post :as wire.in.post]
   [blog.wire.in.tag :as wire.in.tag]
   [blog.utils.request :as utils.request]
   [blog.components]
   [blog.controllers.auth :as controllers.auth]
   [blog.controllers.post :as controllers.post]
   [blog.controllers.tag :as controllers.tag]
   [blog.controllers.rss :as controllers.rss]))

(defn login-handler [{:keys [data]}]
  (let [admin-user (env/get-env "ADMIN_USERNAME")
        admin-pass (env/get-env "ADMIN_PASSWORD")
        result (controllers.auth/Login data admin-user admin-pass)]
    (if (:ok result)
      {:status 200 :body result}
      {:status 401 :body result})))

(defn ping-handler [_request]
  {:status 200 :body {:status "ok"}})

(defn create-post [{new-post :data components :components}]
  (let [slug (controllers.post/CreatePost new-post (components :db))]
    {:status 201 :body {:slug slug}}))

(defn list-posts [{components :components query-params :query-params}]
  {:status 200
   :body (controllers.post/ListPosts (components :db) query-params {})})

(defn get-post [{path-params :path-params components :components}]
  (let [slug (:slug path-params)
        result (controllers.post/GetPost slug (components :db))]
    (if (= result :not-found)
      {:status 404}
      {:status 200 :body result})))

(defn edit-post [{edits :data path-params :path-params components :components}]
  (let [slug (:slug path-params)
        result (controllers.post/EditPost slug edits (components :db))]
    (if (= result :not-found)
      {:status 404}
      {:status 200})))

(defn delete-post [{path-params :path-params components :components}]
  (let [slug (:slug path-params)
        result (controllers.post/DeletePost slug (components :db))]
    (if (= result :not-found)
      {:status 404}
      {:status 204})))

(defn create-tag [{tag :data components :components}]
  (let [slug (controllers.tag/CreateTag tag (components :db))]
    {:status 201 :body {:slug slug}}))

(defn list-tags [{components :components}]
  {:status 200 :body (controllers.tag/ListTags (components :db))})

(defn edit-tag [{edits :data path-params :path-params components :components}]
  (let [slug (:slug path-params)
        result (controllers.tag/EditTag slug edits (components :db))]
    (if (= result :not-found)
      {:status 404}
      {:status 200})))

(defn delete-tag [{path-params :path-params components :components}]
  (let [slug (:slug path-params)
        result (controllers.tag/DeleteTag slug (components :db))]
    (if (= result :not-found)
      {:status 404}
      {:status 204})))

(defn rss-feed [{components :components}]
  {:status 200
   :headers {"Content-Type" "application/rss+xml; charset=utf-8"}
   :body (controllers.rss/RssFeed (components :db))})

(def common-interceptors
  [(utils.request/wrap-components (blog.components/make-components))
   (utils.request/wrap-query-params)
   (utils.request/wrap-json-body)
   (utils.request/wrap-json-response)])

(def routes
  #{["/api/ping"
     :get (conj common-interceptors ping-handler)
     :route-name :ping]

    ["/api/post"
     :post (conj common-interceptors
                 (utils.request/wrap-schema wire.in.post/NewPost)
                 create-post)
     :route-name :create-post]

    ["/api/post"
     :get (conj common-interceptors list-posts)
     :route-name :list-posts]

    ["/api/post/:slug"
     :get (conj common-interceptors get-post)
     :route-name :get-post]

    ["/api/post/:slug"
     :patch (conj common-interceptors
                  (utils.request/wrap-schema wire.in.post/EditPost)
                  edit-post)
     :route-name :edit-post]

    ["/api/post/:slug"
     :delete (conj common-interceptors delete-post)
     :route-name :delete-post]

    ["/api/auth/login"
     :post (conj common-interceptors
                 (utils.request/wrap-schema wire.in.auth/Login)
                 login-handler)
     :route-name :login]

    ["/api/tag"
     :post (conj common-interceptors
                 (utils.request/wrap-schema wire.in.tag/NewTag)
                 create-tag)
     :route-name :create-tag]

    ["/api/tag"
     :get (conj common-interceptors list-tags)
     :route-name :list-tags]

    ["/api/tag/:slug"
     :patch (conj common-interceptors
                  (utils.request/wrap-schema wire.in.tag/EditTag)
                  edit-tag)
     :route-name :edit-tag]

    ["/api/tag/:slug"
     :delete (conj common-interceptors delete-tag)
     :route-name :delete-tag]

    ["/feed.xml"
     :get (conj common-interceptors rss-feed)
     :route-name :rss-feed]})
