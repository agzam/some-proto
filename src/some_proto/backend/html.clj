(ns some-proto.backend.html
  (:require
   [clj-http.client :as client])
  (:import
   [org.jsoup Jsoup]))

(defn fetch-page-content
  "Retrieves text content of a page for given url."
  [url]
  (some->
   url
   (client/get {:content-type :html})
   :body
   Jsoup/parse
   (.select "body")
   (.text)))
