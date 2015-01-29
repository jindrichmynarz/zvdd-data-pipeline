(ns zvdd-data-pipeline.harvest
  (:require [zvdd-data-pipeline.util :refer [lazy-cat']]
            [taoensso.timbre :as timbre]
            [clojure.java.io :as io]
            [environ.core :refer [env]]
            [clj-http.client :as client]))

; ----- Private vars -----

(def ^:private zvdd "zvdd – Zentrales Verzeichnis Digitalisierter Drucke")

; ----- Private functions -----

(defn- get-signed
  [path & {:keys [accept options query-params]
           :or {accept "application/json"
                options {}
                query-params {}}}]
  {:pre [(string? path)
         (map? options)
         (map? query-params)]}
  (client/get (format "http://api.deutsche-digitale-bibliothek.de%s" path)
              (merge options
                     {:headers {"Accept" accept 
                                "Authorization" (format "OAuth oauth_consumer_key=\"%s\"" (:ddb-api-key env))
                                "Host" "api.deutsche-digitale-bibliothek.de"
                                "User-Agent" "ZVDD-harvester"
                                "From" "mynarzjindrich@gmail.com"}
                      :query-params query-params})))

(defn- get-json
  "Query the DDB API for JSON output"
  [path & {:keys [query-params]
           :or {query-params {}}}]
  (:body (get-signed path
                     :options {:as :json}
                     :query-params query-params)))

(defn- search-unlimited
  "Search the DDB with pages results.
  API documentation: <https://api.deutsche-digitale-bibliothek.de/doku/display/ADD/search>"
  [& {:keys [query-params page-size parallel?]
      :or {query-params {}
           page-size 1000
           parallel? false}}]
  (let [map-fn (if parallel? pmap map)
        get-fn (fn [offset]
                 (-> (get-json "/search"
                               :query-params (merge query-params
                                                    {"offset" offset
                                                     "rows" page-size
                                                     "sort" "ALPHA_ASC"}))
                     :results
                     first))]
    (->> (iterate (partial + page-size) 0)
         (map-fn get-fn)
         (take-while (comp not zero? :numberOfDocs))
         (map-fn :docs)
         lazy-cat')))

(defn- get-doc
  "Get document in RDF/XML via its @id.
  API documentation: <https://api.deutsche-digitale-bibliothek.de/doku/display/ADD/edm>"
  [id]
  (-> (get-signed (format "/items/%s/edm" id)
                  :accept "application/xml")
      :body)) 

(defn save-doc
  "Download and save the document identified with @id."
  [output-dir id]
  (let [file-name (format "%s/%s.xml" output-dir id)]
    (when-not (.exists (io/file file-name))
      (Thread/sleep 1000)
      (timbre/debug (format "Downloading record ID %s" id))
      (spit file-name (get-doc id)))))

(def ^:private zvdd-doc-ids
  "Lazy sequence of IDs of ZVDD documents"
  (map :id (search-unlimited :query-params {"facet" "provider_fct"
                                            "provider_fct" zvdd})))

; ----- Public functions -----

(defn harvest-zvdd
  "Harvest ZVDD data from DDB into @output-dir."
  [output-dir] 
  (dorun (map (partial save-doc output-dir) zvdd-doc-ids)))
