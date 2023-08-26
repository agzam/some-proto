(ns some-proto.frontend.hn-search
  (:require
   [clojure.string :as str]
   [re-frame.core :as rf :refer [dispatch
                                 reg-event-fx
                                 reg-event-db
                                 reg-sub
                                 subscribe
                                 inject-cofx]]
   [some-proto.frontend.http-xhrio]
   [some-proto.frontend.svg-icons :as icons]
   [vimsical.re-frame.cofx.inject :as inject]))

(reg-event-fx ::hn-search
  (fn [{:keys [db]} [_ val]]
    (js/console.log val)
    (cond-> {:db (assoc db ::current-search-term val
                        ::hn-data nil)}

      (not (str/blank? val))
      (merge
       {:http-xhrio+ {:uri "/hn-search"
                      :params {:term val}
                      :on-failure [::hn-search-failure]
                      :on-success [::hn-search-success]}}))))

(reg-event-db ::hn-search-success
  (fn [db [_ data]]
    (assoc db ::hn-data data)))

(reg-sub ::hn-data #(get % ::hn-data))

(defn title []
  [:h1 {:class '[m-20]}
   "Hackernews search"])

(defn search-bar []
  [:div {:class '[m-20]}
   [:div.relative
    [:div
     {:class '[absolute inset-y-0 left-0 flex items-center pl-3 pointer-events-none]}
     [:svg
      {:class '[w-4 h-4 text-gray-500 "dark:text-gray-400"]
       :aria-hidden "true"
       :xmlns "http://www.w3.org/2000/svg"
       :fill "none" :viewBox "0 0 20 20"}
      [:path {:stroke "currentColor"
              :stroke-linecap
              "round" :stroke-linejoin "round"
              :stroke-width "2"
              :d "m19 19-4-4m0-7A7 7 0 1 1 1 8a7 7 0 0 1 14 0Z"}]]]
    [:input#default-search
     {:class '[block w-full p-4 pl-10 text-sm text-gray-900 border border-gray-300 bg-gray-50
               "dark:bg-gray-700"
               "dark:border-gray-600"
               "dark:placeholder-gray-400"
               "dark:text-white"
               "dark:focus:ring-orange-500"
               "dark:focus:border-orange-500"]
      :type "search"
      :placeholder "Search term"
      :on-change #(dispatch [::hn-search (-> % .-target .-value)])}]
    [:button
     {:class '[text-white absolute right-2 5 bottom-2 5 bg-orange-500
               font-medium text-sm px-4 py-2
               "hover:bg-orange-700"
               "focus:ring-4"
               "focus:outline-none"
               "focus:ring-orange-300"
               "dark:bg-orange-600"
               "dark:hover:bg-orange-700"
               "dark:focus:ring-orange-800"]
      :type "submit"}
     "Search"]]])

(defn hn-url-btn [objectID]
  [:a {:href (str
              "https://news.ycombinator.com/item?id="
              objectID)
       :target :_blank}
   [icons/hackernews {:width 32}]])

(defn url-btn [url]
  [:a {:href url
       :target :_blank}
   [icons/hyperlink {:width 16}]])

(defn hn-data-row [{:keys [title
                           url
                           objectID
                           created_at]}]
  [:tr {:class '[bg-white border-b
                 "dark:bg-gray-800"
                 "dark:border-gray-700"]}
   [:td {:class '[pl-3]}
    (subs created_at 0 10)]
   [:td {:class '[px-6 py-4]}
    title]
   [:td
    [:span {:class '[inline-flex
                     items-center]}
     [hn-url-btn objectID]
     [url-btn url]]]])

(defn results-table []
  (when-let [data @(subscribe [::hn-data])]
    [:div {:class '[relative overflow-x-auto m-20]}
     [:table {:class '[table w-full text-sm text-left
                       text-gray-500
                       "dark:text-gray-400"]}
      [:thead {:class '[text-xs text-gray-700
                        uppercase bg-gray-50
                        "dark:bg-gray-700"
                        "dark:text-gray-400"]}
       [:tr
        [:th {:class '[pl-3] :scope :col} "Date"]
        [:th {:class '[px-6 py-3] :scope :col} "Title"]
        [:th {:class '[px-6 py-3] :scope :col} ""]]]
      [:tbody
       (for [{:keys [objectID] :as row} data]
         ^{:key objectID}
         [hn-data-row row])]]]))

(defn view []
  [:div
   [title]
   [search-bar]
   [results-table]])
