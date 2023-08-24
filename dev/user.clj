(ns user
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.tools.logging :as log]
   [integrant.core :as ig]
   [integrant.repl :as ig-repl :refer [go halt]]
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

(defn read-config-file
  "Read & parse edn file with `fname`."
  [fname]
  (if (.exists (io/file fname))
    (some-> fname slurp edn/read-string)
    (log/errorf "configuration file: \"%s\" not found!" fname)))

(ig-repl/set-prep!
 (fn []
   (let [cfg (some-> "dev/config.edn"
                     read-config-file)]
     (ig/load-namespaces cfg)
     (ig/prep cfg))))
