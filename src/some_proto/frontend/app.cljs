(ns some-proto.frontend.app
  (:require
   ["react-dom/client" :refer [createRoot]]
   [re-frame.core :as rf :refer [subscribe]]
   [reagent.core :as r]
   [some-proto.frontend.routing :as routing]))

(def debug? ^boolean goog.DEBUG)

(defn dev-setup []
  (when debug?
    (enable-console-print!)))

(defn root-view []
  (let [current-route @(subscribe [:current-route])]
    [:div (when current-route
            (-> current-route :data :view))]))

(defonce root (createRoot (js/document.getElementById "app")))

(defn ^:dev/after-load re-render []
  (rf/clear-subscription-cache!)
  (routing/init-routes!)
  (.render root (r/as-element [root-view])))

(defn ^:export init []
  (dev-setup)
  (re-render))
