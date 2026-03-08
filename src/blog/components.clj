(ns blog.components)

(defn make-components []
  {:db (atom {})})  ;; pretend this is Datomic