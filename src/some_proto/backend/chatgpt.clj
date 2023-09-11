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
  ;; java.util.StringTokenizer treats space, tab, and newline as delimiters without explicitly limiting the delimiters,
  ;; the transformation would be lossy, that may confuse chatgpt
  (let [tokenizer (StringTokenizer. text " ")]
    (str/join " "
              (take
               max-tokens
               (repeatedly #(try
                              (.nextToken tokenizer)
                              (catch Exception e nil)))))))

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

(defn compose-prompt
  "For given HN object compose a prompt for summary."
  [{:keys [objectID
           url
           title
           num_comments]}]
  (let [page-content (when url (fetch-page-content url))
        ;; makes no sense to analyze the discussion if there just a couple of comments
        comments? (some->> num_comments (< 2))
        hn-url (format "https://news.ycombinator.com/item?id=%s" objectID)
        hn-comments (when comments? (fetch-page-content hn-url))
        prompt-template (cond-> ""
                          page-content (str
                                        "Summarize information for the page: "
                                        url
                                        "\nBased on the title of the page: '"
                                        title
                                        "' and its content:\n"
                                        "--- begin content ---\n%s"
                                        "\n--- end content ---\n")

                          (and page-content hn-comments)
                          (str "Also ")

                          comments?
                          (str "summarize the discussion on Hackernews: "
                               hn-url " \n"
                               "using the page content:\n"
                               "--- begin NH comments---%s"
                               "\n--- end HN comments ---\n"
                               "HN summary should be in a separate paragraph."))
        final-prompt (trim-tokens
                      ;; OpenAI's token counter is weird and forces me to use smaller number here
                      3100
                      prompt-template
                      page-content
                      hn-comments)]
    (when (or page-content hn-comments)
      final-prompt)))

(defn make-summary
  [hn-object-map]
  (some->
   hn-object-map
   compose-prompt
   send))


(comment

  (spit "./test/some_proto/backend/sample-hn-discussion-thread.txt"
        (fetch-page-content
         (format "https://news.ycombinator.com/item?id=%s" 12167622)))

  (compose-prompt
   {:objectID "12167622",
    :title "One-sentence proof of Fermat's theorem on sums of two squares"
    :url "https://fermatslibrary.com/s/a-one-sentence-proof-of-fermats-theorem-on-sums-of-two-squares",
    :num_comments 45})

  (compose-prompt
   {:objectID "37299856"
    :title "MMLU Benchmark Broken"
    :url "https://www.youtube.com/null"})

  (let [text (compose-prompt
              {:objectID "37313183"
               :title "Griffin – A fully-regulated, API-driven bank, with Clojure"
               :url "https://www.juxt.pro/blog/clojure-in-griffin/"
               :num_comments 219})
        tokenizer (StringTokenizer. text)]
    (.countTokens tokenizer))
)
