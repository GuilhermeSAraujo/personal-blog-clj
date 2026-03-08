(ns blog.server
  (:require [blog.diplomat.http-server :as http-server]
            [org.httpkit.server :as http]
            [blog.utils.request :as utils.request]))

(defn assemble-handler [interceptors]
  (let [handler (last interceptors)
        mws     (butlast interceptors)]
    ;; apply middlewares so they run in listed order
    (reduce (fn [h mw] (mw h))
            handler
            (reverse mws))))

(defn match-route [routes {:keys [uri request-method]}]
  (some (fn [[path method interceptors & opts]]
          (when (and (= path uri)
                     (= method request-method))
            {:interceptors interceptors
             :meta         (apply hash-map opts)}))
        routes))


(defn make-router [routes]
  (fn [request]
    (if-let [{:keys [interceptors]} (match-route routes request)]
      (let [handler (assemble-handler interceptors)]
        (handler request))
      (utils.request/not-found request))))

(def app
  (make-router http-server/routes))

(defn -main [& _args]
  (println "Starting server on http://localhost:8080")
  (http/run-server app {:port 8080})
  (Thread/sleep Long/MAX_VALUE))