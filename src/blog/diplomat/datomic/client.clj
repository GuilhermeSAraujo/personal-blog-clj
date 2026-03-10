(ns blog.diplomat.datomic.client
  (:require
   [schema.core :as s]
   [blog.models.post :as models.post]
   [blog.adapters.datomic.domain-to-wire :as adapters.domain->wire]
   [blog.adapters.datomic.wire-to-domain :as adapters.wire->domain]))

(defprotocol IDatomic
  (transact! [db entity])
  (query-all! [db])
  (find-by-id! [db id])
  (update! [db id entity]))

(extend-protocol IDatomic
  clojure.lang.Atom
  (transact! [db entity]
    (let [id (str (java.util.UUID/randomUUID))]
      (swap! db assoc id entity)
      id))
  (query-all! [db]
    (vals @db))
  (find-by-id! [db id]
    (get @db id))
  (update! [db id entity]
    (swap! db assoc id entity)
    nil))

(s/defn save-post! [db post :- models.post/Post]
  (-> post
      adapters.domain->wire/domain->wire
      (->> (transact! db))))

(s/defn list-posts :- [models.post/Post]
  [db]
  (map adapters.wire->domain/wire->domain (query-all! db)))

(s/defn update-post! [db id :- s/Str post :- models.post/Post]
  (-> post
      adapters.domain->wire/domain->wire
      (->> (update! db id))))
