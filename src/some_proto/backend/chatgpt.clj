(ns some-proto.backend.chatgpt
  (:refer-clojure :exclude [send])
  (:require
   [clj-http.client :as client]
   [clojure.edn :as edn]
   [clojure.java.shell :refer [sh]]
   [integrant.core :as ig]))

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

