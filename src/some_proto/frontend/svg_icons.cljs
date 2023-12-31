(ns some-proto.frontend.svg-icons)

(defn hackernews [attrs]
  [:svg
   (merge {:clip-rule "evenodd"
           :fill-rule "evenodd"
           :stroke-linejoin "round"
           :stroke-miterlimit "1.41421"
           :viewBox "0 0 560 400"}
          attrs)
   [:g {:transform "matrix(1.05469 0 0 1.05469 145 65)"}
    [:path {:d "m0 0h256v256h-256z" :fill "#fb651e"}]
    [:path {:d "m119.374 144.746-43.941-82.314h20.081l25.848 52.092c.398.928.862 1.889 1.392 2.883s.994 2.022 1.391 3.082c.266.398.464.762.597 1.094.133.331.265.629.398.894.662 1.326 1.259 2.618 1.789 3.877.53 1.26.994 2.419 1.392 3.48 1.06-2.254 2.22-4.673 3.479-7.257 1.26-2.585 2.552-5.269 3.877-8.053l26.246-52.092h18.689l-44.338 83.308v53.087h-16.9z"
            :fill "#fff"
            :fill-rule "nonzero"}]]])

(defn hyperlink [attrs]
  [:svg (merge
         {:width "48px"
          :viewBox "0 0 24 24"
          :fill "none"}
         attrs)
   [:path
    {:d "M10.0464 14C8.54044 12.4882 8.67609 9.90087 10.3494 8.22108L15.197 3.35462C16.8703 1.67483 19.4476 1.53865 20.9536 3.05046C22.4596 4.56228 22.3239 7.14956 20.6506 8.82935L18.2268 11.2626"
     :stroke "#1C274C"
     :stroke-width "1.5"
     :stroke-linecap "round"}]
   [:path
    {:d "M13.9536 10C15.4596 11.5118 15.3239 14.0991 13.6506 15.7789L11.2268 18.2121L8.80299 20.6454C7.12969 22.3252 4.55237 22.4613 3.0464 20.9495C1.54043 19.4377 1.67609 16.8504 3.34939 15.1706L5.77323 12.7373"
     :stroke "#1C274C"
     :stroke-width "1.5"
     :stroke-linecap "round"}]])
