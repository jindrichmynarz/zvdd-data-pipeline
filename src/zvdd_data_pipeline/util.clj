(ns zvdd-data-pipeline.util
  (:require [clojure.edn :as edn]
            [environ.core :refer [env]])
  (:import [java.security MessageDigest]))

; ----- Public functions -----

(defn exit
  "Exit with @status and message @msg.
  @status 0 is OK, @status 1 indicates error."
  [^Integer status
   ^String msg]
  {:pre [(#{0 1} status)]}
  (println msg)
  (System/exit status))

(defn lazy-cat'
  "Lazily concatenates lazy sequence of sequences @colls.
  Taken from <http://stackoverflow.com/a/26595111/385505>."
  [colls]
  (lazy-seq
    (if (seq colls)
      (concat (first colls) (lazy-cat' (next colls))))))

(defn sha1
  "Computes SHA1 hash from @string."
  [^String string]
  (let [digest (.digest (MessageDigest/getInstance "SHA1") (.getBytes string))]
    ;; Stolen from <https://gist.github.com/kisom/1698245#file-sha256-clj-L19>
    (clojure.string/join (map #(format "%02x" (bit-and % 0xff)) digest))))

(defn url?
  "Tests if @url is valid absolute URL."
  [url]
  (try
    (java.net.URL. url)
    (catch Exception _ false)))

; ----- Public vars -----

(defonce config
  (if-let [zvdd-config (:zvdd-config env)]
    (edn/read-string (slurp zvdd-config))
    (exit 1 "Please set the environment variable ZVDD_CONFIG.")))
