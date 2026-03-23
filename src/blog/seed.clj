(ns blog.seed)

(def initial-state
  {:tags
   {:tags/clojure {:tag/name "Clojure" :tag/color "#5881D8" :tag/slug "clojure"}
    :tags/nodejs {:tag/name "Node.js" :tag/color "#68A063" :tag/slug "nodejs"}}
   :posts
   {"getting-started-with-clojure"
    {:post/slug "getting-started-with-clojure"
     :post/title "Getting Started with Clojure"
     :post/content "Clojure is a dynamic, functional Lisp dialect running on the JVM."
     :post/tags [:tags/clojure]
     :post/published-at "2026-03-20"
     :post/draft? false}
    "building-rest-apis-with-nodejs"
    {:post/slug "building-rest-apis-with-nodejs"
     :post/title "Building REST APIs with Node.js"
     :post/content "Node.js makes it easy to build scalable network applications."
     :post/tags [:tags/nodejs]
     :post/published-at "2026-03-20"
     :post/draft? false}}})
