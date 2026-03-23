(ns blog.controllers.rss
  (:require
   [clojure.string :as str]
   [blog.diplomat.datomic.client :as datomic.client]
   [blog.logic.post :as logic.post]))

(defn- escape-xml [s]
  (-> s
      (str/replace "&" "&amp;")
      (str/replace "<" "&lt;")
      (str/replace ">" "&gt;")))

(defn- post->item [{:keys [slug title content published-at]}]
  (str "<item>"
       "<title>" (escape-xml title) "</title>"
       "<link>https://yourblog.com/posts/" slug "</link>"
       "<pubDate>" published-at "</pubDate>"
       "<description>" (escape-xml (subs content 0 (min 200 (count content)))) "</description>"
       "</item>"))

(defn RssFeed [db]
  (let [posts (->> (datomic.client/list-posts db)
                   (logic.post/filter-published)
                   (sort-by :published-at)
                   (reverse)
                   (take 20))]
    (str "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
         "<rss version=\"2.0\">"
         "<channel>"
         "<title>My Blog</title>"
         "<link>https://yourblog.com</link>"
         "<description>Technical articles</description>"
         (str/join (map post->item posts))
         "</channel>"
         "</rss>")))
