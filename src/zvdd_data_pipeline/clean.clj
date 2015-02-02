(ns zvdd-data-pipeline.clean
  (:require [zvdd-data-pipeline.util :refer [config]]
            [zvdd-data-pipeline.sparql :as sparql]
            [clojure.java.io :as io])
  (:import [org.apache.commons.io FilenameUtils]))

; ----- Public functions -----

(defn clean
  "Execute SPARQL Update operations for cleaning data"
  []
  (let [endpoint (sparql/load-endpoint)
        updates (->> "templates/sparql/clean"
                     io/resource
                     io/as-file
                     file-seq
                     (filter (fn [f]
                               (and (.isFile f)
                                    (= (FilenameUtils/getExtension (.getAbsolutePath f)) "mustache"))))
                     (map #(.getAbsolutePath %)))
        update-fn (fn [update]
                    (sparql/execute-update endpoint
                                           update
                                           :data {:source-graph (get-in config [:data :source-graph])}))]
    (dorun (map update-fn updates))))
