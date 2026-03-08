(ns blog.diplomat.http-server
  (:require
   [blog.wire.in.post :as wire.in.post]
   [blog.utils.request :as utils.request]
   [blog.components]
   [blog.adapters.post :as adapters.post]
   [blog.controllers.post :as controllers.post]
   [blog.diplomat.datomic.client :as datomic.client]))

(defn ping-handler [_request]
  {:status  200
   :body    {:status "ok"}})

(defn create-post [{new-post   :data
                    components :components}]
  (-> new-post
      (adapters.post/wire-in->model)
      (controllers.post/CreatePost (components :db)))
  {:status 201})

(defn list-posts [{components :components}]
  {:status 200 :body (datomic.client/list-posts (components :db))})

(def common-interceptors
  [(utils.request/wrap-components (blog.components/make-components))
   (utils.request/wrap-json-body)
   (utils.request/wrap-json-response)])

(def routes
  #{["/api/ping"
     :get  (conj common-interceptors
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
     :route-name :list-posts]})