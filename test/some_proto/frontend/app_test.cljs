(ns some-proto.frontend.app-test
  (:require
   [cljs.test :as t :include-macros true :refer [deftest is testing]]
   [hiccup-find.core :as hf]
   [re-frame.core :as rf :refer [subscribe
                                 reg-sub
                                 clear-sub
                                 clear-subscription-cache!]]
   [some-proto.frontend.app :as app]))

(defn fixture-clear-subscription [f]
  (clear-sub :current-route)
  (clear-subscription-cache!)
  (f))

(t/use-fixtures :each fixture-clear-subscription)

(deftest root-view-test
  (testing "verify that the main route works"
    (reg-sub :current-route
      (fn [] {:data {:view [:div#view "view only"]}}))
    (is (seq (hf/hiccup-find [:div#view] (app/root-view))))
    (is (seq (re-find #"view only" (hf/hiccup-text (app/root-view)))))))
