(ns some-proto.backend.chatgpt
  (:refer-clojure :exclude [send])
  (:require
   [clj-http.client :as client]
   [clojure.edn :as edn]
   [clojure.java.shell :refer [sh]]
   [clojure.string :as str]
   [integrant.core :as ig]
   [some-proto.backend.html :refer [fetch-page-content]])
  (:import [java.util StringTokenizer]))

(def auth-key (atom nil))
(def openai-parameters (atom nil))

(defn- decrypt-key []
  (let [cmd ["gpg2" "-q" "--for-your-eyes-only"
             "--no-tty" "-d" "resources/creds.gpg"]]
    (some->>
     cmd
     (apply sh)
     :out
     edn/read-string
     :open-ai-key
     (reset! auth-key))))

(defmethod ig/init-key ::parameters [_ opts]
  (reset! openai-parameters opts))

(defmethod ig/halt-key! ::parameters [_ _]
  (reset! openai-parameters nil)
  (reset! auth-key nil))

(defn send [content]
  (when-not @auth-key
    (decrypt-key))
  (let [params (or @openai-parameters
                   {:model "gpt-3.5-turbo"
                    :temperature 0.8})
        res
        (client/post
         "https://api.openai.com/v1/chat/completions"
         {:content-type :json
          :as :json
          :form-params
          (merge
           params
           {:messages [{:role :user
                        :content content}]})
          :oauth-token @auth-key})]
    (some->> res :body :choices first :message :content)))


(defn abbreviate
  "Reduces given text by removing from its end, until it contains no more than max-tokens."
  [text max-tokens]
  (loop [txt text]
    (let [tokens (-> (StringTokenizer. txt)
                     (.countTokens))]
      (if (<= tokens max-tokens)
        txt
        ;; TODO: This is kinda lame, iterate with tokenizer instead, this tranformation is not lossless
        (let [new-txt (->> (str/split txt #"\W+")
                           butlast
                           (str/join " "))]
          (recur new-txt))))))

(defn trim-tokens
  "Number of allowed tokens in the prompt can exceed the max allowed.
  Let's try to reduce number of tokens by reducing content to be sent with the prompt."
  [max-tokens prompt-template page-content hn-comments]
  (let [template-size (-> (StringTokenizer. prompt-template)
                          (.countTokens))
        ;; TODO: this is not great, need to figure out better way
        max-possible (if hn-comments
                       (quot (- max-tokens template-size) 2)
                       (- max-tokens template-size))
        fmt-params (->>
                    (cond-> []
                      page-content
                      (conj (abbreviate page-content max-possible))

                      hn-comments
                      (conj (abbreviate hn-comments max-possible)))
                    (remove nil?))]
    (apply format prompt-template fmt-params)))


(defn make-summary
  [{:keys [objectID
           url
           title
           num_comments]}]
  (let [page-content (when url (fetch-page-content url))
        comments? (< 2 num_comments)
        hn-url (format "https://news.ycombinator.com/item?id=%s" objectID)
        hn-comments (when comments? (fetch-page-content hn-url))
        prompt-template (cond-> ""
                          page-content (str
                                        "Summarize information for the page: "
                                        url
                                        "Based on the title of the page: "
                                        title
                                        " and its content:\n"
                                        "--- begin content ---\n%s"
                                        "\n--- end content ---\n")
                          (and page-content hn-comments)
                          (str "Also, ")
                          comments?
                          (str "Summarize the discussion on Hackernews: "
                               hn-url " \n"
                               "using the page content:\n"
                               "--- begin NH comments---\n%s"
                               "\n--- end HN comments ---\n")
                          page-content (str "place the HN summary in a separate paragraph."))
        final-prompt (trim-tokens
                      ;; OpenAI's token counter is weird and forces me to use smaller number here
                      3100
                      prompt-template
                      page-content
                      hn-comments)]
    (if (or page-content hn-comments)
      (send final-prompt)
      "")))

(comment
  (make-summary
   {:objectID "37296400",
    :title "Optimize Java to C string conversion by avoiding double copy"
    :url "https://github.com/openjdk/panama-foreign/pull/875",
    :num_comments 0,
    :created_at "2023-08-26T00:35:06.000Z"})

  (make-summary
   {:objectID "37299856"
    :title "MMLU Benchmark Broken"
    :url "https://www.youtube.com/null"}))
