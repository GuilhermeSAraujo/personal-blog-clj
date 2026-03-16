(ns blog.logic.auth
  (:require [schema.core :as s]))

(s/defn valid-credentials? :- s/Bool
  [expected-username :- s/Str
   expected-password :- s/Str
   username :- s/Str
   password :- s/Str]
  (boolean (and (= expected-username username)
                (= expected-password password))))
