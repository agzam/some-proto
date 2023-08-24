(ns some-proto.frontend.app
  (:require
   ["react-dom/client" :refer [createRoot]]
   [reagent.core :as r]
   [re-frame.core :as rf]
   ;; [reagent.dom :as re-dom]
   ))

(def debug? ^boolean goog.DEBUG)

(defn dev-setup []
  (when debug?
    (enable-console-print!)))

(defn root-view []
  [:div
   [:h1 "Here will be dragons"]])

(defonce root (createRoot (js/document.getElementById "app")))

(defn ^:dev/after-load re-render []
  (rf/clear-subscription-cache!)
  (.render root (r/as-element [root-view])))

(defn ^:export init []
  (dev-setup)
  (re-render))
