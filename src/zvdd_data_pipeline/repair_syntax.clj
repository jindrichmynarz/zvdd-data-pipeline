(ns zvdd-data-pipeline.repair-syntax
  (:require [clojure.java.io :as io])
  (:import [java.io File]
           [org.apache.commons.io FilenameUtils]
           [javax.xml.transform Transformer]
           [net.sf.saxon TransformerFactoryImpl]
           [net.sf.saxon.jaxp TemplatesImpl]
           [javax.xml.transform.stream StreamResult StreamSource]))

; ----- Private functions -----

(defn- transform
  "Executes XSL transformation via @cached-xslt.
  XML input is loaded from @file.
  Output is returned as string."
  [^TemplatesImpl cached-xslt
   ^File file] 
  (with-out-str (.transform (.newTransformer cached-xslt)
                            (StreamSource. file)
                            (StreamResult. *out*))))

(def ^:private
  xslt-repair
  "XSL transformer for cleaning the DDB RDF/XML."
  (let [cached-xslt (.newTemplates (TransformerFactoryImpl.)
                                   (StreamSource. (io/input-stream (io/resource "repair_syntax.xsl"))))]
    (fn [^File file]
      (transform cached-xslt file))))

; ----- Public functions -----

(defn repair-xml
  "Clean XML files from @input-dir into @output-dir using XSL transformation."
  [input-dir output-dir]
  (let [files (filter #(.isFile %) (file-seq (io/file input-dir)))
        clean-fn (fn [file]
                   (let [file-name (FilenameUtils/removeExtension (.getName file))]
                     (spit (format "%s/%s.xml" output-dir file-name) (xslt-repair file))))]
    (dorun (pmap clean-fn files))))
