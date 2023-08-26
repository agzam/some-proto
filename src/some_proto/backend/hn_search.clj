(ns some-proto.backend.hn-search
  (:require
   [clj-http.client :as client]))

(defn handler [request]
  (let [term (get-in request [:query-params "term"])
        search-url
        (format
         "https://hn.algolia.com/api/v1/search_by_date?tags=story&query=%s"
         term)
        res (client/get search-url {:as :json})]

    #_(clojure.pprint/pprint
     (some->>
      res
      :body
      :hits))

    {:status 200
     :body (some->>
            res
            :body
            :hits
            (mapv #(select-keys % [:objectID
                                   :title
                                   :url
                                   :created_at])))}))
