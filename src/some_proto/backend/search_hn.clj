(ns some-proto.backend.search-hn
  (:require [clj-http.client :as client]))

(defn handler [request]
  (let [term (get-in request [:query-params "term"])
        search-url (format
                    "https://hn.algolia.com/api/v1/search?query=%s"
                    term)
        res (client/get search-url {:as :json})]
    {:status 200
     :body (some->>
            res
            :body
            :hits
            (mapv #(select-keys % [:objectID :title :url])))}))
