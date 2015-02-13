(ns zvdd-data-pipeline.transform
  (:require [clojure.java.io :as io])
  (:import [java.io File]
           [java.util LinkedHashMap]
           [org.apache.commons.io FilenameUtils]
           [com.hp.hpl.jena.rdf.model ModelFactory]
           [org.apache.jena.riot RDFFormat RDFDataMgr]
           [com.github.jsonldjava.core JsonLdOptions JsonLdProcessor]
           [com.github.jsonldjava.utils JsonUtils]))

; ----- Private vars -----

(defonce ^:private
  json-ld-options
  (doto (JsonLdOptions.) (.setUseNativeTypes true)))

; ----- Private functions -----

(defn- parse-frame
  "Parse JSON-LD frame from @file-name."
  [file-name]
  (-> file-name 
      io/resource
      io/input-stream
      JsonUtils/fromInputStream))

(defn- turtle->json-ld
  "Convert RDF string @rdf into JSON-LD string."
  [^String rdf]
  (let [model (.read (ModelFactory/createDefaultModel) (io/input-stream (.getBytes rdf)) "" "TURTLE")]
    (with-out-str (RDFDataMgr/write *out* model RDFFormat/JSONLD_PRETTY))))

(defn- frame-json-ld
  "Frame JSON-LD with @frame using optional @options."
  [^LinkedHashMap json-ld
   ^LinkedHashMap frame
   & {:keys [options]
      :or {options json-ld-options}}]
  (JsonLdProcessor/frame json-ld frame options))

(defn- convert-json-ld
  "Frame @json-ld using @frame and get rid of @graph."
  [^LinkedHashMap frame
   ^LinkedHashMap json-ld]
  (let [framed-json-ld (frame-json-ld json-ld frame)
        triples (.get framed-json-ld "@graph")]
    (when (seq triples)
      (JsonUtils/toPrettyString (first triples))))) 

(defn- transform-json-ld
  "Transform RDF/Turtle file into JSON-LD using @frame."
  [^LinkedHashMap frame
   ^File file]
  (->> file
       slurp
       turtle->json-ld
       JsonUtils/fromString
       (convert-json-ld frame)))

; ----- Public functions -----

(defn transform
  "Transform RDF/Turtle files from @input directory
  to JSON-LD-framed JSON files in the @output directory
  using a JSON-LD @frame."
  [^String frame
   ^String input
   ^String output]
  (let [files (filter #(.isFile %) (file-seq (io/file input)))
        output-dir (io/file output)
        parsed-frame (parse-frame frame)
        transform-fn (fn [file]
                       (let [file-name (FilenameUtils/removeExtension (.getName file))]
                         (spit (format "%s/%s.jsonld" output file-name)
                               (transform-json-ld parsed-frame file))))]
    ; Create output directory if it doesn't exist.
    (when-not (.exists output-dir) (.mkdir output-dir))
    (dorun (pmap transform-fn files))))
