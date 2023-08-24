(ns some-proto.frontend.routing
  (:require
   [re-frame.core :as rf :refer [dispatch
                                 reg-event-db
                                 reg-event-fx
                                 reg-fx
                                 reg-sub]]
   [reitit.coercion.schema]
   [reitit.frontend]
   [reitit.frontend.controllers :as rfc]
   [reitit.frontend.easy :as rfe]
   [schema.core :as s]))

(def routes
  [""
   ["/"
    {:name :routes/root
     :view [:h1 "ROOT"]
     :controllers [{:start (fn [] (println "entering root route"))
                    :stop (fn [] (println "leaving root route"))}]}]])

(reg-event-fx :navigate (fn [_ [_ & route]] {:navigate! route}))

(reg-fx :navigate! (fn [route] (apply rfe/push-state route)))

(reg-event-db
  :navigated
  (fn [db [_ new-match]]
    (let [old-match (:current-route db)
          controllers (rfc/apply-controllers (:controllers old-match) new-match)]
      (assoc db :current-route (assoc new-match :controllers controllers)))))

(reg-sub :current-route (fn [db] (:current-route db)))

(def router
  (reitit.frontend/router
   routes
   {:data {:coercion reitit.coercion.schema/coercion}}))

(defn on-navigate [new-match]
  (when new-match
    (dispatch [:navigated new-match])))

(defn init-routes! []
  (js/console.log "initializing routes")
  (rfe/start!
   router
   on-navigate {:use-fragment true}))
