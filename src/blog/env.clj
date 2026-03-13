(ns blog.env
  "Load .env file into a map; get-env returns dotenv value or System/getenv."
  (:require [clojure.string :as str]
            [clojure.java.io :as io]))

(def ^:private dotenv
  (atom {}))

(defn- parse-line [line]
  (let [line (str/trim line)]
    (when (and (not (str/blank? line))
               (not (str/starts-with? line "#")))
      (let [i (.indexOf line "=")]
        (when (pos? i)
          (let [k (str/trim (subs line 0 i))
                v (str/trim (subs line (inc i)))]
            ;; remove surrounding quotes if present
            [k
             (if (and (>= (count v) 2)
                      (or (and (= \" (first v)) (= \" (last v)))
                          (and (= \' (first v)) (= \' (last v)))))
               (subs v 1 (dec (count v)))
               v)]))))))

(defn load-dotenv!
  "Load .env from dir (default: current working directory). Merges into internal map."
  ([] (load-dotenv! (System/getProperty "user.dir")))
  ([dir]
   (let [f (io/file dir ".env")]
     (when (.exists f)
       (let [lines (str/split-lines (slurp f))
             pairs (into {} (keep parse-line lines))]
         (swap! dotenv merge pairs))))
   @dotenv))

(defn get-env
  "Return value for key from .env (if loaded) or from System/getenv. key is string."
  [k]
  (or (get @dotenv k)
      (System/getenv k)))
