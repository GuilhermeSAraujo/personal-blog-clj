(ns blog.controllers.auth
  (:require
   [schema.core :as s]
   [blog.wire.in.auth :as wire.in.auth]
   [blog.logic.auth :as logic.auth]))

(s/defn Login :- (s/conditional :ok {:ok (s/eq true)}
                                :error {:error s/Str})
  [credentials :- wire.in.auth/Login
   expected-user :- s/Str
   expected-pass :- s/Str]
  (if (logic.auth/valid-credentials? expected-user expected-pass
                                     (:username credentials)
                                     (:password credentials))
    {:ok true}
    {:error "Unauthorized"}))
