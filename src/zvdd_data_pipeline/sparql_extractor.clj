(ns zvdd-data-pipeline.sparql-extractor
  (:require [zvdd-data-pipeline.sparql :refer [construct load-endpoint select-unlimited]]
            [zvdd-data-pipeline.util :refer [config sha1]]
            [clojure.java.io :as io]))

; ----- Private vars -----

(def ^:private source-graph
  (get-in config [:data :source-graph]))

; ----- Private functions -----

(defn- get-resources
  "Lazy sequence of ZVDD resource URIs"
  [sparql-endpoint]
  (map :resource
       (select-unlimited sparql-endpoint
                         "get_resources"
                         :data {:source-graph source-graph})))

(defn- get-resource-representation
  "Get RDF representation of a given @resource
  using data from named @source-graphs."
  [sparql-endpoint source-graphs resource]
  (construct sparql-endpoint
             "resource_representation"
             :data {:resource resource
                    :source-graphs source-graphs}))

; ----- Public functions -----

(defn extract-rdf
  "Extract representations of ZVDD resources in RDF from named @graphs
  into the @output directory."
  [output graphs]
  {:pre [(seq? graphs)]}
  (let [output-dir (io/file output)
        endpoint (load-endpoint) 
        source-graphs (concat [source-graph "http://d-nb.info/standards/elementset/gnd#"] graphs)
        extract-fn (fn [resource]
                     (spit (format "%s/%s.ttl" output (sha1 resource)) 
                           (get-resource-representation endpoint source-graphs resource)))]
    ; Create output directory if it doesn't exist.
    (when-not (.exists output-dir) (.mkdir output-dir))
    (dorun (map extract-fn (get-resources endpoint)))))
