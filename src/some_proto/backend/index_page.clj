(ns some-proto.backend.index-page
  (:require
   [hiccup.page :refer [html5
                        include-css
                        include-js]]))

(def metas
  (list
   [:meta {:name "copyright" :content "Ag Ibragimov. All registered trademarks belong to their respective owners"}]
   [:meta {:name "description" :content "FIXME"}]
   [:meta {:http-equiv "cache-control" :content "no-cache"}]
   [:meta {:name "viewport" :content "initial-scale=1, width=device-width"}]) )

(defn index-page []
  (html5
   [:head
    metas
    [:title "FIXME: I have no title"]
    #_(include-css "")
    [:body
     [:noscript
      [:p [:b "This is a JavaScript app. Please enable JavaScript to continue."]]]
     [:div#app]
     (include-js "out/app.js")]]))
