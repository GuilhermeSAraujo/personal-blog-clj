(ns blog.seed
  (:require [clojure.java.io :as io]))

(defn- post-content [slug]
  (-> (str "seed/posts/" slug ".md") io/resource slurp))

(def initial-state
  {:tags
   {:tags/clojure {:tag/name "Clojure" :tag/color "#5881D8" :tag/slug "clojure"}
    :tags/nodejs {:tag/name "Node.js" :tag/color "#68A063" :tag/slug "nodejs"}
    :tags/javascript {:tag/name "JavaScript" :tag/color "#68A063" :tag/slug "javascript"}
    }
   :posts
   {"common-mistakes-of-a-developer-starting-with-meteorjs"
    {:post/slug "common-mistakes-of-a-developer-starting-with-meteorjs"
     :post/title "Common Mistakes of a Developer Starting with Meteor.js"
     :post/content (post-content "common-mistakes-of-a-developer-starting-with-meteorjs")
     :post/tags [:tags/nodejs :tags/javascript]
     :post/published-at "2024-10-28"
     :post/draft? false}
    "handling-promises-with-higher-order-functions-in-javascript"
    {:post/slug "handling-promises-with-higher-order-functions-in-javascript"
     :post/title "Handling Promises with Higher-Order Functions in JavaScript"
     :post/content (post-content "handling-promises-with-higher-order-functions-in-javascript")
     :post/tags [:tags/javascript]
     :post/published-at "2024-12-17"
     :post/draft? false}
    "optimizing-end-to-end-testing-strategies-for-speed-reliability-and-efficiency"
    {:post/slug "optimizing-end-to-end-testing-strategies-for-speed-reliability-and-efficiency"
     :post/title "Optimizing End-to-End Testing: Strategies for Speed, Reliability, and Efficiency"
     :post/content (post-content "optimizing-end-to-end-testing-strategies-for-speed-reliability-and-efficiency")
     :post/tags [:tags/javascript]
     :post/published-at "2025-01-27"
     :post/draft? false}
    "hosting-web-app-on-your-old-android-phone"
    {:post/slug "hosting-web-app-on-your-old-android-phone"
     :post/title "Hosting web app on your old Android phone"
     :post/content (post-content "hosting-web-app-on-your-old-android-phone")
     :post/tags [:tags/javascript]
     :post/published-at "2025-01-27"
     :post/draft? false}
    "understanding-lighthouse-the-metrics-that-actually-matter-for-performance-and-seo"
    {:post/slug "understanding-lighthouse-the-metrics-that-actually-matter-for-performance-and-seo"
     :post/title "Understanding Lighthouse: The Metrics That Actually Matter for Performance and SEO"
     :post/content (post-content "understanding-lighthouse-the-metrics-that-actually-matter-for-performance-and-seo")
     :post/tags [:tags/javascript]
     :post/published-at "2025-01-27"
     :post/draft? false}}})
