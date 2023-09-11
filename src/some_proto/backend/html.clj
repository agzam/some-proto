(ns some-proto.backend.html
  (:require
   [clj-http.client :as client]
   [clojure.pprint]
   [clojure.string :as str]
   [some-proto.backend.hackernews :as hackernews])
  (:import
   [org.jsoup Jsoup]))

(defmulti fetch-page-content
  "Not all websites allow straightforward html data scraping. Sometimes we need to
   dispatch different method to get data."
  (fn [url]
    (let [patterns {"https://news.ycombinator.com.*" :hackernews}]
      (or
       (->> patterns keys
            (filter #(re-matches (re-pattern %) url))
            first
            (get patterns))
       :default))))

(defmethod fetch-page-content :default
  [url]
  (try
    (some->
     url
     (client/get {:content-type :html})
     :body
     Jsoup/parse
     (.select "body")
     (.text))
    (catch Exception e
      (clojure.pprint/pprint
       (.getMessage e))
      nil)))

(defmethod fetch-page-content :hackernews
  [url]
  (hackernews/fetch-discussion-content url))


(defn gather-page-urls
  "Scans the given html page url to collect all urls on the page."
  [url]
  (some->
   url
   (client/get {:content-type :html})
   :body
   Jsoup/parse))

(defn get-title
  "Returns document.title of a page."
  [url]
  (try
    (->
     (Jsoup/connect url)
     (.get)
     (.title))
    (catch Exception _
      nil)))

