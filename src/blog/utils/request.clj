(ns blog.utils.request
  (:require
   [cheshire.core :as json]
   [schema.core :as s]))

(defn wrap-json-body []
  (fn [handler]
    (fn [request]
      (let [body        (:body request)
            body-str    (when body (slurp body))
            content-typ (get-in request [:headers "content-type"])
            data        (when (and body-str
                                   (some? (re-find #"application/json"
                                                   (or content-typ ""))))
                          (json/parse-string body-str true))
            request'    (cond-> request
                          data (assoc :data data))]
        (handler request')))))

(defn wrap-schema [schema]
  (fn [handler]
    (fn [request]
      (try
        (let [data       (:data request)
              valid-data (s/validate schema data)
              request'   (assoc request :data valid-data)]
          (handler request'))
        (catch Exception e
          {:status  400
           :headers {"Content-Type" "application/json"}
           :body    (json/generate-string
                     {:error   "Invalid request body"
                      :message (.getMessage e)})})))))

(defn wrap-json-response []
  (fn [handler]
    (fn [request]
      (let [{:keys [status headers body] :as resp} (handler request)]
        (if (map? resp)
          {:status  (or status 200)
           :headers (merge {"Content-Type" "application/json"} headers)
           :body    (json/generate-string body)}
          resp)))))


(defn wrap-components [components]
  (fn [handler]
    (fn [request]
      (handler (assoc request :components components)))))

(defn not-found [_]
  {:status  404
   :headers {"Content-Type" "application/json"}
   :body    (json/generate-string {:error "Not found"})})