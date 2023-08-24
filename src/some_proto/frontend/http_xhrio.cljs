(ns some-proto.frontend.http-xhrio
  "Wrapper around day8.re-frame/http-fx"
  (:require
   [ajax.core :as ajax]
   [day8.re-frame.http-fx :as http-fx]
   [re-frame.core :as rf :refer [reg-fx
                                 dispatch
                                 reg-event-fx]]))

(reg-fx :http-xhrio+
  (fn [{:keys [uri
               method
               format
               response-format] :as request}]
    (let [request*
          (merge
           request
           {:method (or method :get)
            :uri uri
            :format (or format (ajax/json-request-format))
            :response-format (or response-format
                                 (ajax/json-response-format
                                  {:keywords? true}))})]
      (dispatch [::delegate-xhrio request*]))))

(reg-event-fx ::delegate-xhrio
  (fn [_ [_ request]]
    {:http-xhrio request}))
