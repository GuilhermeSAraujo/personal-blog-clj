(ns blog.diplomat.http-server
  (:require
   [blog.env :as env]
   [blog.wire.in.auth :as wire.in.auth]
   [blog.wire.in.post :as wire.in.post]
   [blog.utils.request :as utils.request]
   [blog.components]
   [blog.controllers.auth :as controllers.auth]
   [blog.controllers.post :as controllers.post]))

(defn login-handler [{:keys [data]}]
  (let [admin-user (env/get-env "ADMIN_USERNAME")
        admin-pass (env/get-env "ADMIN_PASSWORD")
        result (controllers.auth/Login data admin-user admin-pass)]
    (if (:ok result)
      {:status 200 :body result}
      {:status 401 :body result})))

(defn ping-handler [_request]
  {:status 200
   :body {:status "ok"}})

(defn create-post [{new-post :data
                    components :components}]
  (let [id (controllers.post/CreatePost new-post (components :db))]
    {:status 201 :body {:id id}}))

(defn list-posts [{components :components}]
  {:status 200 :body (controllers.post/ListPosts (components :db))})

(defn edit-post [{edits :data
                  path-params :path-params
                  components :components}]
  (let [id (:id path-params)
        result (controllers.post/EditPost id edits (components :db))]
    (if (= result :not-found)
      {:status 404}
      {:status 200})))

(def common-interceptors
  [(utils.request/wrap-components (blog.components/make-components))
   (utils.request/wrap-json-body)
   (utils.request/wrap-json-response)])

(def routes
  #{["/api/ping"
     :get (conj common-interceptors
                ping-handler)
     :route-name :ping]

    ["/api/post"
     :post (conj common-interceptors
                 (utils.request/wrap-schema wire.in.post/NewPost)
                 create-post)
     :route-name :create-post]

    ["/api/post"
     :get (conj common-interceptors
                list-posts)
     :route-name :list-posts]

    ["/api/post/:id"
     :patch (conj common-interceptors
                  (utils.request/wrap-schema wire.in.post/EditPost)
                  edit-post)
     :route-name :edit-post]

    ["/api/auth/login"
     :post (conj common-interceptors
                 (utils.request/wrap-schema wire.in.auth/Login)
                 login-handler)
     :route-name :login]})
