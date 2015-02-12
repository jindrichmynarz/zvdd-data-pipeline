(ns zvdd-data-pipeline.link
  (:require [zvdd-data-pipeline.sparql :as sparql]
            [zvdd-data-pipeline.util :refer [config]]))

; ----- Public functions -----

(defn link
  "Execute linking via SPARQL Update operations"
  []
  (let [endpoint (sparql/load-endpoint)
        update-fn (fn [update]
                    (sparql/execute-update endpoint
                                           update
                                           :data {:source-graph (get-in config [:data :source-graph])}))]
    (sparql/gnd-loaded? endpoint)
    (dorun (map update-fn (sparql/templates-from-dir "templates/sparql/link")))))
