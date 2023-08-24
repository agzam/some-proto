(ns user
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.java.shell :as shell]
   [clojure.string :as str]
   [clojure.tools.logging :as log]
   [integrant.core :as ig]
   [integrant.repl :as ig-repl :refer [go reset halt]]
   [shadow.cljs.devtools.api :as shadow]
   [shadow.cljs.devtools.server :as server]))

(defn- shadow-cljs-watch [build-id]
  (println "starting shadow-cljs server & watch")
  (server/start!)
  (shadow/watch build-id))

(defn cljs-repl
  ([] (cljs-repl :app))
  ([build-id]
   (shadow-cljs-watch build-id)
   (shadow/nrepl-select build-id)))

(defmethod ig/init-key ::shadow-cljs-watch [_ {:keys [build-id]}]
  (shadow-cljs-watch (or build-id :app)))

(defmethod ig/halt-key! ::shadow-cljs-watch [_ _]
  (server/stop!)
  (println "shadow-cljs server stopped"))

(defmethod ig/init-key ::postcss-watch [_ _]
  []
  (let [watch-running? (->> "ps -eo comm | grep -E '(.*postcss.*)(.*watch.*)'"
                            (shell/sh "bash" "-c")
                            :out str/split-lines
                            (remove str/blank?)
                            seq boolean)]
    (when-not watch-running?
      (println "starting postcss watch process...")
      (-> (ProcessBuilder. ["npm" "run-script" "postcss:watch"]) .inheritIO .start))))

(defmethod ig/halt-key! ::postcss-watch [_ proc]
  (when proc
    (.destroy proc)
    (println "postcss watch process terminated")))

(defn read-config-file
  "Read & parse edn file with `fname`."
  [fname]
  (if (.exists (io/file fname))
    (some-> fname slurp edn/read-string)
    (log/errorf "configuration file: \"%s\" not found!" fname)))

(ig-repl/set-prep!
 (fn []
   (let [cfg (some-> "dev/config.edn"
                     read-config-file
                     (merge {::shadow-cljs-watch nil
                             ::postcss-watch nil}))]
     (ig/load-namespaces cfg)
     (ig/prep cfg))))
