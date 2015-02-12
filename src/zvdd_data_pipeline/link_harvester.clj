(ns zvdd-data-pipeline.link-harvester
  (:require [zvdd-data-pipeline.util :refer [config]]
            [zvdd-data-pipeline.sparql :as sparql]
            [clojure.java.io :as io])
  (:import [java.io File]
           [java.net URI]
           [com.ontologycentral.ldspider Crawler]
           [com.ontologycentral.ldspider.frontier BasicFrontier]
           [com.ontologycentral.ldspider.hooks.sink SinkCallback]
           [org.semanticweb.yars.util CallbackRDFXMLOutputStream]
           [com.ontologycentral.ldspider.seen HashSetSeen]
           [com.ontologycentral.ldspider.queue HashTableRedirects]))

; ----- Public functions -----

(defn harvest-links
  "Harvest links into @output RDF/XML file."
  [^String output]
  (let [; Last parameter of SinkCallback is a boolean flag indicating if header data should be included.
        sink (SinkCallback. (CallbackRDFXMLOutputStream. (io/output-stream output)) false)
        frontier (BasicFrontier.)
        endpoint (sparql/load-endpoint)
        source-graph (get-in config [:data :source-graph])
        get-links (fn [template]
                    (map :link (sparql/select-unlimited endpoint
                                                        template
                                                        :data {:source-graph source-graph})))
        links (apply concat
                     (map get-links (sparql/templates-from-dir "templates/sparql/link_harvesting")))
        ; Run the crawler on threads = 2 + number of CPUs
        crawler (doto (Crawler. (+ 2 (.. Runtime getRuntime availableProcessors)))
                  (.setOutputCallback sink))]
    (sparql/gnd-loaded? endpoint)
    (dorun (map #(.add frontier (URI. %)) links))
    (try
      (.evaluateBreadthFirst crawler
                             frontier
                             (HashSetSeen.) ; Cache seen URIs
                             (HashTableRedirects.) ; Cache URI redirects
                             0 ; How many hops beyond the provided URI should the crawler go?
                             -1 ; Unlimited maximum number of processed URIs
                             -1 ; Unlimited maximum number of processed pay-level domains (PLDs)
                             -1 ; Minimum number of active PLDs
                             false) ; Don't take minimum number of active PLDs into account
      (finally (.close crawler)))
    (println (format "Harvested data saved in %s." output))))
