(ns some-proto.frontend.app-test
  (:require
   [some-proto.frontend.app :as app]

   [cljs.test :as t :include-macros true :refer [deftest is testing]]
   [re-frame.core :as rf :refer [subscribe
                                 reg-sub
                                 clear-sub
                                 clear-subscription-cache!]]))

(defn fixture-clear-subscription [f]
  (clear-sub :current-route)
  (clear-subscription-cache!)
  (f))

(t/use-fixtures :each fixture-clear-subscription)

(deftest root-view-test
  (testing "verify that the main route works"
    (reg-sub :current-route
      (fn [] {:data {:view [:div#view "view only"]}}))

    (js/console.log
     (app/root-view))
    ))
