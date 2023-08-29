(ns some-proto.backend.html
  (:require
   [clojure.pprint]
   [clj-http.client :as client])
  (:import
   [org.jsoup Jsoup]))

(defn fetch-page-content
  "Retrieves text content of a page for given url."
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



(comment
  (fetch-page-content
   "https://www.thelancet.com/article/S0140-6736(22)02465-5/fulltext"
   )
  (fetch-page-content
   "https://cacm.acm.org/news/275841-behind-the-ai-boom-an-army-of-overseas-workers-in-digital-sweatshops/fulltext"
   )

  (fetch-page-content "https://www.youtube.com/null")
  )
