(ns zvdd-data-pipeline.core
  (:gen-class)
  (:require [zvdd-data-pipeline.harvest :refer [harvest-zvdd]]
            [zvdd-data-pipeline.repair-syntax :refer [repair-xml]]
            [zvdd-data-pipeline.rdf-loader :refer [load-rdf]]
            [zvdd-data-pipeline.clean :refer [clean]]
            [zvdd-data-pipeline.link :refer [link]]
            [zvdd-data-pipeline.link-harvester :refer [harvest-links]]
            [zvdd-data-pipeline.sparql-extractor :refer [extract-rdf]]
            [zvdd-data-pipeline.transform :refer [transform]]
            [zvdd-data-pipeline.util :refer [exit]]
            [taoensso.timbre :as timbre]
            [clojure.java.io :as io]
            [clojure.string :refer [join]]
            [clojure.tools.cli :refer [parse-opts]])
  (:import [java.util.logging Level Logger]))

(declare right-trim-slash)

; ----- Private vars -----

(def ^:private
  help
  ["-h" "--help"])

(def ^:private
  input-dir
  ["-i" "--input INPUT" "Input directory"
   :parse-fn #'right-trim-slash
   :validate [#(let [f (io/file %)] (and (.exists f) (.isDirectory f)))
              "The input directory doesn't exist or isn't a directory!"]])

(def ^:private
  output-dir
  ["-o" "--output DIR" "Output directory"
   :parse-fn #'right-trim-slash])

(def ^:private
  clean-cli
  [help])

(def ^:private
  extract-rdf-cli
  [output-dir help])

(def ^:private
  harvest-cli
  [output-dir help])

(def ^:private
  harvest-links-cli
  [["-o" "--output FILE" "Output file"] help])

(def ^:private
  link-cli
  [help])

(def ^:private
  load-cli
  [input-dir help])

(def ^:private
  repair-cli
  [input-dir output-dir help])

(def ^:private
  transform-cli
  [["-f" "--frame FRAME" "JSON-LD frame for transformation"
    :validate [#(let [f (io/file %)] (and (.exists f) (.isFile f)))
               "The JSON-LD frame doesn't exist or isn't a file!"]]
   input-dir
   output-dir
   help])

; ----- Private functions -----

(defn- error-msg
  [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (join \newline errors)))

(defn- init-logger
  "Initialize logger"
  []
  (let [logfile "log/zvdd_data_pipeline.log"
        log-directory (io/file "log")]
    ; Set LDSpider log level
    (.setLevel (Logger/getLogger "com.ontologycentral.ldspider") Level/WARNING)
    ; Create log directory
    (when-not (.exists log-directory) (.mkdir log-directory))
    (timbre/set-config! [:appenders :standard-out :enabled?] false)
    (timbre/set-config! [:appenders :spit :enabled?] true)
    (timbre/set-config! [:shared-appender-config :spit-filename] logfile)))

(defn- right-trim-slash
  "Remove slash from the right of @s."
  [s]
  (if (.endsWith s "/")
    (.substring s 0 (dec (count s)))
    s))

(defn- usage
  [options-summary]
  (->> ["ZVDD data processing pipeline"
        ""
        "Usage: java -jar zvdd-data-pipeline.jar [command] [options]"
        "Supported commands: harvest, repair, load, clean, link, harvest-links, extract-rdf, transform"
        ""
        "Options:"
        options-summary]
       (join \newline)))

; ----- Public functions -----

(defn -main
  [& args]
  (init-logger)
  (let [[command & opts] args
        handle-fn (fn [callback {{:keys [help]
                                  :as options} :options
                                 :keys [errors summary]}]
                    (cond (or (and (empty? errors) (empty? options)) help) (exit 0 (usage summary))
                        errors (exit 1 (error-msg errors))
                        :else (callback)))]
    (case command
      "harvest" (let [{{:keys [output]} :options
                       :as options} (parse-opts opts harvest-cli)]
                  (handle-fn (fn []
                               (let [output-dir (io/file output)] 
                                 (when-not (.exists output-dir) (.mkdir output-dir))
                                 (harvest-zvdd output)))
                             options))
      "repair" (let [{{:keys [input output]} :options
                     :as options} (parse-opts opts repair-cli)]
                 (handle-fn (fn []
                              (let [output-dir (io/file output)]
                                (when-not (.exists output-dir) (.mkdir output-dir))
                                (repair-xml input output)))
                            options)) 
      "load" (let [{{:keys [input]} :options
                    :as options} (parse-opts opts load-cli)]
               (handle-fn (fn [] (load-rdf input))
                          options))
      "clean" (let [options (parse-opts opts clean-cli)]
                (handle-fn clean options))
      "link" (let [options (parse-opts opts link-cli)]
               (handle-fn link options))
      "harvest-links" (let [{{:keys [output]} :options
                             :as options} (parse-opts opts harvest-links-cli)]
                        (handle-fn (partial harvest-links output) options))
      "extract-rdf" (let [{{:keys [output]} :options
                           graphs :arguments
                           :as options} (parse-opts opts extract-rdf-cli)]
                      (handle-fn (partial extract-rdf output graphs) options))
      "transform" (let [{{:keys [frame input output]} :options
                         :as options} (parse-opts opts transform-cli)]
                    (handle-fn (partial transform frame input output) options))
      (exit 1 (format "Unsupported command `%s`.
                      Supported command include:
                      - `harvest`
                      - `repair`
                      - `load`
                      - `clean`
                      - `link`
                      - `harvest-links`
                      - `extract-rdf`
                      - `transform`"
                      command)))))
