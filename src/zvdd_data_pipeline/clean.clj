(ns zvdd-data-pipeline.clean
  (:require [zvdd-data-pipeline.util :refer [config]]
            [zvdd-data-pipeline.sparql :as sparql]))

; ----- Public functions -----

(defn clean
  "Execute SPARQL Update operations for cleaning data"
  []
  (let [endpoint (sparql/load-endpoint)
        updates (sparql/templates-from-dir "templates/sparql/clean") 
        update-fn (fn [update]
                    (sparql/execute-update endpoint
                                           update
                                           :data {:source-graph (get-in config [:data :source-graph])}))]
    (dorun (map update-fn updates))))
