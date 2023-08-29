(ns some-proto.backend.system-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [muuntaja.core :as m]
   [some-proto.backend.system :as system]))

(defn- request
  ([method uri]
   (request method uri nil))
  ([method uri body]
   (-> (system/app
        {:uri uri
         :request-method method
         :body-params body})
       (select-keys [:status :body])
       (update
        :body
        (fn [b]
          (cond->> b
            (instance? java.io.InputStream b)
            (m/decode "application/json")))))))

(deftest route-patshs-test
  (testing "/status route"
    (is (= {:status 200, :body {:status "okay"}}
           (request :get "/status")))))
