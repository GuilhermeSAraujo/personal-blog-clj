(ns blog.wire.in.auth
  (:require [schema.core :as s]))

(def Login
  {:username s/Str
   :password s/Str})
