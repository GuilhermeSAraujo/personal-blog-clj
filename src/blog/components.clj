(ns blog.components
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]
            [blog.seed :as seed]))

(def edn-path "data/blog.edn")

(defn- load-state [path]
  (if (.exists (io/file path))
    (edn/read-string (slurp path))
    seed/initial-state))

(defn make-components []
  (let [state (load-state edn-path)
        db (atom state :meta {::edn-path edn-path})]
    {:db db}))

(defn make-test-components []
  {:db (atom {:posts {} :tags {}})})
