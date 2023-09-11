(ns some-proto.backend.utils)

(defn deep-merge
  [& maps]
  (let [merge-fn (fn *merge* [& args]
                   (if (every? map? args)
                     (apply merge-with *merge* args)
                     (last args)))]
    (apply merge-with merge-fn maps)))
