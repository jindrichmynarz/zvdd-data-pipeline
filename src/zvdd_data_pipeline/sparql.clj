(ns zvdd-data-pipeline.sparql
  (:require [zvdd-data-pipeline.util :refer [config lazy-cat' url?]]
            [taoensso.timbre :as timbre]
            [com.stuartsierra.component :as component]
            [clj-http.client :as client]
            [clojure.java.io :as io]
            [clojure.xml :as xml]
            [clojure.zip :as zip]
            [clojure.data.zip.xml :as zip-xml]
            [stencil.core :refer [render-file]]
            [slingshot.slingshot :refer [throw+ try+]])
  (:import [org.apache.commons.io FilenameUtils]))

(declare ask execute-query execute-sparql execute-update render-sparql)

; ----- Private functions -----

(defn- crud
  "Use SPARQL 1.1 Graph Store to manipulate graph named with @graph-uri
  using @method (:PUT :DELETE) and additional @params."
  [sparql-endpoint method graph-uri & {:as params}]
  {:pre [(contains? #{:DELETE :GET :POST :PUT} method)
         (:authentication sparql-endpoint)
         (url? graph-uri)]}
  (let [authentication (:authentication sparql-endpoint)
        crud-url (get-in sparql-endpoint [:endpoints :crud-url])
        method-fn (method {:DELETE client/delete
                           :GET client/get
                           :POST client/post
                           :PUT client/put})
        base-params {:digest-auth authentication
                     :query-params {"graph" graph-uri}}]
    (timbre/debug (format "Sending %s request to graph <%s> using the endpoint %s."
                          (name method)
                          graph-uri
                          crud-url))
    (method-fn crud-url (merge base-params params))))

(defn- execute-sparql
  "Execute SPARQL @request-string on @endpoint-url of @sparql-endpoint using @method."
  [sparql-endpoint & {:keys [accept endpoint-url method request-string]}]
  (let [[method-fn params-key request-key] (case method
                                             :GET [client/get :query-params "query"]
                                             ; Fuseki requires form-encoded params
                                             :POST [client/post :form-params "update"])
        params (merge {params-key {request-key request-string}
                       :accept accept
                       :throw-entire-message? true}
                      (when (= method :POST) {:digest-auth (:authentication sparql-endpoint)}))]
    (try+ (timbre/debug (str "Executing SPARQL\n" request-string))
          (:body (method-fn endpoint-url params))
          (catch [:status 400] {:keys [body]}
            (timbre/error body)
            (throw+)))))

(defn- ping-endpoint
  "Send a simple SPARQL ASK query to @sparql-endpoint to test if it's available."
  [sparql-endpoint]
  (assert (ask sparql-endpoint "ping_endpoint")
          (format "SPARQL endpoint %s isn't available!" (get-in sparql-endpoint [:endpoints :query-url]))))

(defn- render-sparql
  "Render SPARQL from Mustache template on @template-path using @data."
  [template-path & {:keys [data]}]
  (let [path (if (.isAbsolute (io/as-file template-path))
               template-path
               (str "templates/sparql/" template-path ".mustache"))]
    (render-file path data)))

(defn- xml->zipper
  "Take XML string @s, parse it, and return XML zipper"
  [s]
  (->> s
       .getBytes
       java.io.ByteArrayInputStream.
       xml/parse
       zip/xml-zip))

; ----- Public functions -----

(defn ask
  "Render @template-path using @data and execute the resulting SPARQL ASK query."
  [sparql-endpoint template-path & {:keys [data]}]
  (let [results (-> (execute-query sparql-endpoint template-path :data data)
                    xml->zipper
                    (zip-xml/xml1-> :boolean zip-xml/text))]
    (boolean (Boolean/valueOf results))))

(defn construct
  "Render @template-path using @data and execute the resulting SPARQL CONSTRUCT query."
  [sparql-endpoint template-path & {:keys [data]}]
  (execute-query sparql-endpoint template-path :data data :accept "text/turtle"))

(defn execute-query
  "Render @template-path using @data and execute the resulting SPARQL query."
  [sparql-endpoint template-path & {:keys [accept data]
                                    :or {accept "application/sparql-results+xml"}}]
  (execute-sparql sparql-endpoint
                  :accept accept
                  :endpoint-url (get-in sparql-endpoint [:endpoints :query-url])
                  :method :GET
                  :request-string (render-sparql template-path :data data)))

(defn execute-update
  "Render @template-path using @data and execute the resulting SPARQL update request."
  [sparql-endpoint template-path & {:keys [data]}]
  (execute-sparql sparql-endpoint
                  :endpoint-url (get-in sparql-endpoint [:endpoints :update-url])
                  :method :POST
                  :request-string (render-sparql template-path
                                                 :data (assoc data :virtuoso? (:virtuoso? sparql-endpoint)))))

(defn post-graph
  "Use SPARQL 1.1 Graph Store to POST @payload into a graph named @graph-uri."
  [sparql-endpoint payload graph-uri]
  (crud sparql-endpoint
        :POST
        graph-uri
        :body payload))

(defn select
  "Execute SPARQL SELECT query rendered from @template-path with @data.
  Returns empty sequence when query has no results."
  [sparql-endpoint template-path & {:keys [data]}]
  (let [results (xml->zipper (execute-query sparql-endpoint template-path :data data))
        sparql-variables (map keyword (zip-xml/xml-> results :head :variable (zip-xml/attr :name)))
        sparql-results (zip-xml/xml-> results :results :result)
        get-bindings (comp (partial zipmap sparql-variables) #(zip-xml/xml-> % :binding zip-xml/text))]
    (map get-bindings sparql-results)))

(defn select-unlimited
  "Lazily stream pages of SPARQL SELECT query results
  by executing paged query from @template-path."
  [sparql-endpoint template-path & {:keys [data page-size parallel-execution?]
                                    :or {page-size 5000
                                         parallel-execution? false}}]
  (let [map-fn (if parallel-execution? pmap map)
        select-fn (fn [offset]
                    (select sparql-endpoint template-path :data (assoc data
                                                                       :limit page-size
                                                                       :offset offset)))]
    (->> (iterate (partial + page-size) 0)
         (map-fn select-fn)
         (take-while seq)
         lazy-cat')))

(defn templates-from-dir
  "List SPARQL templates in Mustache from a given @dir."
  [dir]
  (->> dir 
       io/resource
       io/as-file
       file-seq
       (filter (fn [f]
                 (and (.isFile f)
                      (= (FilenameUtils/getExtension (.getAbsolutePath f)) "mustache"))))
       (map #(.getAbsolutePath %))))

; ----- Records -----

(defrecord SparqlEndpoint [config]
  component/Lifecycle
  (start [sparql-endpoint]
    (let [{{:keys [crud-url password query-url update-url username]
            :as sparql-config} :sparql-endpoint} config 
          authentication [username password]
          new-endpoint (assoc sparql-config
                              :authentication authentication
                              :endpoints {:crud-url crud-url
                                          :query-url query-url
                                          :update-url update-url})
          server-header (-> query-url
                            client/head
                            (get-in [:headers :Server]))
          virtuoso? (when-not (nil? server-header)
                      (not= (.indexOf (clojure.string/lower-case server-header) "virtuoso") -1))]
      (assert (not-any? nil? authentication)
              "Password and username are missing from the configuration!")
      (ping-endpoint new-endpoint)
      (assoc new-endpoint :virtuoso? virtuoso?)))
  (stop [sparql-endpoint] sparql-endpoint))

(defn load-endpoint
  "SPARQL endpoint constructor"
  []
  (component/start (->SparqlEndpoint config)))
