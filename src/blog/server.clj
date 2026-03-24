(ns blog.server
  (:require [blog.diplomat.http-server :as http-server]
            [blog.env :as env]
            [clojure.string :as str]
            [org.httpkit.server :as http]
            [blog.utils.request :as utils.request])
  (:gen-class))

(def ^:private cors-headers
  {"Access-Control-Allow-Origin"  (or (System/getenv "CORS_ORIGIN") "*")
   "Access-Control-Allow-Methods" "GET, POST, PATCH, PUT, DELETE, OPTIONS"
   "Access-Control-Allow-Headers" "Content-Type, Authorization"})

(defn wrap-cors [handler]
  (fn [request]
    (if (= (:request-method request) :options)
      {:status 204 :headers cors-headers}
      (let [resp (handler request)]
        (if (map? resp)
          (update resp :headers merge cors-headers)
          resp)))))

(defn assemble-handler [interceptors]
  (let [handler (last interceptors)
        mws     (butlast interceptors)]
    ;; apply middlewares so they run in listed order
    (reduce (fn [h mw] (mw h))
            handler
            (reverse mws))))

(defn path-match [path-template uri]
  (let [path-segs (str/split (or path-template "") #"/")
        uri-segs  (str/split (or uri "") #"/")]
    (when (= (count path-segs) (count uri-segs))
      (loop [params {}
             i      0]
        (if (>= i (count path-segs))
          {:path-params params}
          (let [p-seg (nth path-segs i)
                u-seg (nth uri-segs i)]
            (cond
              (str/starts-with? p-seg ":")
              (recur (assoc params (keyword (subs p-seg 1)) u-seg) (inc i))
              (= p-seg u-seg)
              (recur params (inc i))
              :else nil)))))))

(defn match-route [routes {:keys [uri request-method]}]
  (some (fn [[path method interceptors & opts]]
          (when (= method request-method)
            (if (str/includes? path ":")
              (when-let [match (path-match path uri)]
                (merge {:interceptors interceptors
                        :meta         (apply hash-map opts)}
                       match))
              (when (= path uri)
                {:interceptors interceptors
                 :meta         (apply hash-map opts)}))))
        routes))


(defn make-router [routes]
  (fn [request]
    (if-let [match (match-route routes request)]
      (let [handler   (assemble-handler (:interceptors match))
            request'  (if-let [pp (:path-params match)]
                        (assoc request :path-params pp)
                        request)]
        (handler request'))
      (utils.request/not-found request))))

(def app
  (wrap-cors (make-router http-server/routes)))

(defn -main [& _args]
  (env/load-dotenv!)
  (println "Starting server on http://localhost:8080")
  (http/run-server app {:port 8080})
  (Thread/sleep Long/MAX_VALUE))