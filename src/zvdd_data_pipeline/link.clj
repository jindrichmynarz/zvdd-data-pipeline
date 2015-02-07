(ns zvdd-data-pipeline.link
  (:require [zvdd-data-pipeline.sparql :as sparql]
            [zvdd-data-pipeline.util :refer [config exit]]))

; ----- Private functions -----

(defn- gnd-loaded?
  "Check if Gemeinsame Normdatei (GND) is loaded."
  [endpoint]
  (let [gnd-graph "http://d-nb.info/standards/elementset/gnd#"]
    (when-not (sparql/ask endpoint "gnd_loaded" :data {:gnd-graph gnd-graph})
      (exit 1 (format
                (str
                  "Please load GND "
                  "(<http://datendienst.dnb.de/cgi-bin/mabit.pl?userID=opendata&pass=opendata&cmd=login>) "
                  "into named graph <%s>.")
                gnd-graph)))))

; ----- Public functions -----

(defn link
  "Execute linking via SPARQL Update operations"
  []
  (let [endpoint (sparql/load-endpoint)
        update-fn (fn [update]
                    (sparql/execute-update endpoint
                                           update
                                           :data {:source-graph (get-in config [:data :source-graph])}))]
    (gnd-loaded? endpoint)
    (dorun (map update-fn (sparql/templates-from-dir "templates/sparql/link")))))
