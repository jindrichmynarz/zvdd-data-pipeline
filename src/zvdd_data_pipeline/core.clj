(ns zvdd-data-pipeline.core
  (:gen-class)
  (:require [zvdd-data-pipeline.harvest :refer [harvest-zvdd]]
            [zvdd-data-pipeline.repair-syntax :refer [repair-xml]]
            [zvdd-data-pipeline.rdf-loader :refer [load-rdf]]
            [taoensso.timbre :as timbre]
            [clojure.java.io :as io]
            [clojure.string :refer [join]]
            [clojure.tools.cli :refer [parse-opts]]))

(declare right-trim-slash)

; ----- Private vars -----

(def ^:private
  clean-cli
  [["-i" "--input INPUT" "Input directory"
    :parse-fn #'right-trim-slash
    :validate [#(let [f (io/file %)] (and (.exists f) (.isDirectory f)))
               "The input directory doesn't exist or isn't a directory!"]]
   ["-o" "--output OUTPUT" "Output directory"
    :parse-fn #'right-trim-slash]
   ["-h" "--help"]])

(def ^:private
  harvest-cli
  [["-o" "--output DIR" "Output directory"
    :parse-fn #'right-trim-slash]
   ["-h" "--help"]])

(def ^:private
  load-cli
  [["-i" "--input DIR" "Input directory"
    :parse-fn #'right-trim-slash]
   ["-h" "--help"]])

; ----- Private functions -----

(defn- error-msg
  [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (join \newline errors)))

(defn- exit
  "Exit with @status and message @msg.
  @status 0 is OK, @status 1 indicates error."
  [^Integer status
   ^String msg]
  {:pre [(#{0 1} status)]}
  (println msg)
  (System/exit status))

(defn- init-logger
  "Initialize logger"
  []
  (let [logfile "log/ddb_harvest.log"
        log-directory (io/file "log")]
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
        "Supported commands: harvest, clean"
        ""
        "Options:"
        options-summary]
       (join \newline)))

; ----- Public functions -----

(defn -main
  [& args]
  (init-logger)
  (let [[command & opts] args]
    (case command
      "harvest" (let [{{:keys [help output]
                        :as options} :options
                       :keys [errors summary]} (parse-opts opts harvest-cli)]
                  (cond (or (and (empty? errors) (empty? options)) help) (exit 0 (usage summary))
                        errors (exit 1 (error-msg errors))
                        :else (let [output-dir (io/file output)] 
                                (when-not (.exists output-dir) (.mkdir output-dir))
                                (harvest-zvdd output))))
      "clean" (let [{{:keys [help input output]
                      :as options} :options
                     :keys [errors summary]
                     :as all} (parse-opts opts clean-cli)]
                (cond (or (and (empty? errors) (empty? options)) help) (exit 0 (usage summary))
                      errors (exit 1 (error-msg errors))
                      :else (let [output-dir (io/file output)]
                              (when-not (.exists output-dir) (.mkdir output-dir))
                              (repair-xml input output))))
      "load" (let [{{:keys [help input]
                     :as options} :options
                    :keys [errors summary]} (parse-opts opts load-cli)]
                (cond (or (and (empty? errors) (empty? options)) help) (exit 0 (usage summary))
                      errors (exit 1 (error-msg errors))
                      :else (load-rdf input)))
      (exit 1 (format "Unsupported command `%s`. Supported command include `harvest`, `clean`, and `load`."
                      command)))))
