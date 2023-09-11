(ns some-proto.backend.chatgpt
  (:refer-clojure :exclude [send])
  (:require
   [clj-http.client :as client]
   [clojure.edn :as edn]
   [clojure.java.shell :refer [sh]]
   [clojure.string :as str]
   [integrant.core :as ig]
   [some-proto.backend.html :refer [fetch-page-content]]
   [tolkien.core :as token])
  (:import [java.util StringTokenizer]))

(def ^:private openai-parameters* (atom nil))

(defn- decrypt-key []
  (let [cmd ["gpg2" "-q" "--for-your-eyes-only"
             "--no-tty" "-d" "resources/creds.gpg"]]
    (some->>
     cmd
     (apply sh)
     :out
     edn/read-string
     :open-ai-key
     (swap! openai-parameters* assoc :api-token))))

(defn openai-parameters
  "getter/setter for openai parameters."
  ([params]
   (reset! openai-parameters* params)
   (when-not (contains? params :api-token)
     (decrypt-key))
   (openai-parameters))
  ([]
   (or @openai-parameters*
       {:model "gpt-3.5-turbo"
        :temperature 0.8
        :max-tokens 4096})))

(defmethod ig/init-key ::parameters [_ opts]
  (openai-parameters opts))

(defmethod ig/halt-key! ::parameters [_ _]
  (swap! openai-parameters* dissoc :auth-key))

(defn send [content]
  (let [{:keys [api-token] :as params} (openai-parameters)
        res
        (client/post
         "https://api.openai.com/v1/chat/completions"
         {:content-type :json
          :as :json
          :form-params
          (merge
           (select-keys params [:model :temperature])
           {:messages [{:role :user
                        :content content}]})
          :oauth-token api-token})]
    (some->> res :body :choices first :message :content)))

(defn- remove-last-word
  "Removes last word from text.
  Unlike splitting the text into words and then rejoining, this should keep indentations, tabs and linebreaks intact."
  [s]
  (let [last-delim-idx (str/last-index-of s " ")]
    (if last-delim-idx (subs s 0 last-delim-idx) "")))

(defn abbreviate
  "Reduces given text by removing from its end, until it contains no more than max-tokens."
  [text max-tokens]
  (let [{:keys [model]} (openai-parameters)
        cur-text (atom text)]
    (while (< max-tokens (token/count-tokens model @cur-text))
      (swap! cur-text remove-last-word))
    @cur-text))

(defn trim-tokens
  "Number of allowed tokens in the prompt can exceed the max allowed.
  Let's try to reduce number of tokens by reducing content to be sent with the prompt."
  [prompt-template page-content hn-comments]
  (let [{:keys [model max-tokens]} (openai-parameters)
        template-size (token/count-tokens model prompt-template)
        ;; OpenAI max tokens counted for both input and output, we can use some percentage of max available and leave
        ;; the rest for the output. If we consume too much on the input. The summary will cut too short
        ;; TODO: this is not great, need to figure out better way
        max-input-tokens (* max-tokens 0.7)
        max-page-tokens (cond-> (- max-input-tokens template-size)
                          hn-comments (*  0.5))
        max-hn-tokens (- max-input-tokens max-page-tokens)
        fmt-params (->>
                    (cond-> []
                      page-content
                      (conj (abbreviate page-content max-page-tokens))

                      hn-comments
                      (conj (abbreviate hn-comments max-hn-tokens)))
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
                               "\n--- end HN comments ---\n")

                          :always
                          (str " Use all remaining tokens for the output."))
        final-prompt (trim-tokens
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
        {:keys [model]} (openai-parameters)]
    (token/count-tokens model text))

  (fetch-page-content
   (format "https://news.ycombinator.com/item?id=%s" 37313183)
   )

  (make-summary
   {:objectID "37313183"
    :title "Griffin – A fully-regulated, API-driven bank, with Clojure"
    :url "https://www.juxt.pro/blog/clojure-in-griffin/"
    :num_comments 219}
   )


  )
