(ns some-proto.backend.hackernews
  (:require
   [clj-http.client :as client]
   [some-proto.backend.utils :refer [deep-merge]]
   [clojure.core.async :as a])
  (:import
   [org.jsoup Jsoup]))

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
                                   :created_at
                                   :num_comments])))}))

(defn- get-single-story-node
  "Fetches single HN story from the API."
  [story-id]
  (let [base-url "https://hacker-news.firebaseio.com/v0/item/%s.json"]
    (some->
     (format base-url story-id)
     (client/get {:as :auto})
     :body)))

(defn- retrieve-discussion-content
  "Recursively fetches HN discussion thread from the API.

  HN Free Firebase API doesn't return content of a story in a single request. It returns a node with children IDs. You
  have to send multiple requests to fetch the whole page."
  [story-id]
  (let [in-ch (a/chan)
        out-ch (a/chan)
        fetch
        (fn [{:keys [story-id
                     parent-id
                     parents]}
             res-ch]
          (when story-id
            (let [{:keys [kids
                          id
                          text
                          type
                          by] :as _res}
                  (get-single-story-node story-id)]

              (doseq [kid kids]
                (a/put! in-ch {:story-id kid
                               :parents (conj parents id)
                               :parent-id id}))
              (when text
                (a/put! res-ch {:parent-id parent-id
                                :story-id id
                                :text text
                                :type type
                                :by by
                                :parents parents}))))
          (a/go (a/close! res-ch)))]
    ;; retrive discussion thread nodes, starting with the root story in an async pipeline recursively, feeding child
    ;; nodes (if any) into the input channel, forcing it to fetch the content for every sub-node
    (a/put! in-ch {:story-id story-id :parents []})
    (a/pipeline-async 10 out-ch fetch in-ch)

    ;; since the sub-nodes are getting fetched in parallel, the resulting list of nodes won't be sorted. let's push them
    ;; into a map where we can maintain the hierarchy
    (let [closer (a/timeout 5000)]
      (a/go-loop [discussion {}]
        (let [[val ch] (a/alts! [out-ch closer])]
          (if (or (nil? val) (= ch closer))
            discussion
            (recur
             (update-in
              discussion
              (conj (:parents val) (:story-id val))
              deep-merge val))))))))

(defn- decode-html
  "HN API returns text encoded with things like &quot; etc."
  [html] (some-> html Jsoup/parse .body .text))

(defn cleanup-discussion
  "Extracts some human readable content out of wacky map created by `retrieve-discussion-content`.
  nesting in the conversation threat is denoted using special character that can be set using `indent-symbol` string"
  [discussion-map & {:keys [indent-symbol]}]
  (loop [cs [discussion-map]
         text-str ""]
    (let [{:keys [text
                  parents
                  by] :as current} (first cs)]
      (if (not (map? current))
        text-str
        (let [ks (->> current keys (filter keyword?))
              indent (count parents)
              txt (str
                   text-str
                   (when text
                     (str
                      "\n"
                      (apply str (repeat indent (or indent-symbol "✱")))
                      " " by ": "
                      (decode-html text))))]
          (recur (concat
                  (vals (apply dissoc current ks))
                  (rest cs))
                 txt))))))

(defn fetch-discussion-content
  "Fetches the text content of HN story."
  [url]
  (when-let [story-id (re-find #"(?<=id=)\d+" url)]
    (cleanup-discussion
     (a/<!! (retrieve-discussion-content story-id)))))
