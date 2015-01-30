(ns zvdd-data-pipeline.rdf-loader
  (:require [zvdd-data-pipeline.util :refer [config]]
            [taoensso.timbre :as timbre]
            [stencil.core :refer [render-file]]
            [clojure.java.io :as io]
            [clojure.java.shell :refer [sh]])
  (:import [java.io File]))

; ----- Private functions -----

(defn- write-to-file
  "Write string @s to @file."
  [file s]
  (with-open [f (io/writer file)]
    (.write f s)))

; ----- Public functions -----

(defn load-rdf
  "Load RDF/XML (*.xml) files from @dir-path into Virtuoso."
  [dir-path]
  {:pre [(when-let [f (io/file dir-path)] (and (.exists f) (.isDirectory f)))]}
  (let [{:keys [username password]} (get-in config [:sparql-endpoint])
        source-graph (get-in config [:data :source-graph])
        sql-file (File/createTempFile "virtuoso_bulk_load" ".sql")
        sql-content (render-file "virtuoso_bulk_load.sql.mustache"
                                 {:path (.getAbsolutePath (io/file dir-path))
                                  :source-graph source-graph})
        bash-file (doto (File/createTempFile "virtuoso_bulk_load" ".sh")
                    (.setExecutable true))
        bash-content (render-file "virtuoso_bulk_load.sh.mustache"
                                  {:username username
                                   :password password
                                   :sql-file-path (.getAbsolutePath sql-file)})]
    (write-to-file sql-file sql-content)
    (write-to-file bash-file bash-content)
    (try
      (sh (.getAbsolutePath bash-file))
      (finally (.delete sql-file)
               (.delete bash-file)))))
