(ns some-proto.backend.system
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.tools.cli :as cli]
            [clojure.tools.logging :as log]
            [integrant.core :as ig]
            [muuntaja.core :as m]
            [reitit.exception]
            [reitit.ring :as ring]
            [reitit.ring.coercion :as coercion]
            [reitit.ring.middleware.exception :as exception]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.ring.middleware.parameters :as parameters]
            [ring.adapter.jetty :as jetty]
            [some-proto.backend.index-page :refer [index-page]]
            [some-proto.backend.hn-search :as hn-search])
  (:import [org.eclipse.jetty.server Server])
  (:gen-class))

(def app
  (ring/ring-handler
   (ring/router
    [["/" {:get {:handler (fn [_]
                            {:status 200
                             :body (index-page)})}}]
     ["/hn-search" {:get hn-search/handler}]
     ["/status"
      {:get {:handler (constantly
                       {:status 200
                        :body {:status :okay}})}}]]
    {:conflicts (fn [conflicts]
                  (log/error
                   (reitit.exception/format-exception :path-confilcts nil conflicts)))
     :data {:muuntaja m/instance
            :middleware [;; swagger feature
                         ;; swagger/swagger-feature
                         ;; query-params & form-params
                         parameters/parameters-middleware
                         ;; content-negotiation
                         muuntaja/format-negotiate-middleware
                         ;; encoding response body
                         muuntaja/format-response-middleware
                         ;; exception handling
                         (exception/create-exception-middleware
                          {::exception/default
                           (partial
                            exception/wrap-log-to-console
                            exception/default-handler)})
                         ;; decoding request body
                         muuntaja/format-request-middleware
                         ;; coercing response bodys
                         coercion/coerce-response-middleware
                         ;; coercing request parameters
                         coercion/coerce-request-middleware
                         ;; multipart
                         ;; multipart/multipart-middleware
                         ]}})
   (ring/routes
    (ring/create-resource-handler {:path "/"})
    (ring/create-default-handler
     {:not-found (constantly {:status 404
                              :header {"Content-Type" "text/html"}
                              :body "Page Not Found"})}))))

(defmethod ig/init-key ::server [_ opts]
  (log/info "jetty server started on port: " (:port opts))
  (jetty/run-jetty app opts))

(defmethod ig/halt-key! ::server [_ ^Server server]
  (log/info "shutting down the server")
  (.stop server))
