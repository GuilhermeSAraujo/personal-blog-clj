(defproject blog "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [http-kit/http-kit "2.7.0"]
                 [cheshire/cheshire "5.11.0"]
                 [prismatic/schema "1.4.1"]]
  :source-paths ["src"]
  :test-paths ["test"]
  :main blog.server
  :profiles {:uberjar {:aot :all
                       :uberjar-name "blog-standalone.jar"}})