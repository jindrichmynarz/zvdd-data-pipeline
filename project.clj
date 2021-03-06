(defproject zvdd-data-pipeline "0.2.0-SNAPSHOT"
  :description "Data ingestion pipeline for ZVDD data in RDF from DDB"
  :url "http://github.com/jindrichmynarz/zvdd-data-pipeline"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [com.taoensso/timbre "3.3.1"]
                 [org.clojure/tools.cli "0.3.1"]
                 [environ "1.0.0"]
                 [clj-http "1.0.1"]
                 [log4j/log4j "1.2.17" :exclusions [javax.mail/mail
                                                    javax.jms/jms
                                                    com.sun.jmdk/jmxtools
                                                    com.sun.jmx/jmxri]]
                 [commons-io/commons-io "2.4"]
                 [net.sf.saxon/Saxon-HE "9.6.0-4"]
                 [org.apache.jena/jena-core "2.12.1"]
                 [org.apache.jena/jena-arq "2.12.1"]
                 [com.github.jsonld-java/jsonld-java "0.5.1"]
                 [com.stuartsierra/component "0.2.2"]
                 [stencil "0.3.3"]
                 [org.clojure/data.zip "0.1.1"]
                 [slingshot "0.10.3"]
                 [com.ontologycentral/ldspider "1.3"]
                 [org.semanticweb.yars/nxparser "1.2.6"]]
  :main zvdd-data-pipeline.core
  :profiles {:uberjar {:aot :all}}
  :repositories [["ldspider" "http://ldspider.googlecode.com/svn/repository"]
                 ["nxparser" "http://nxparser.googlecode.com/svn/repository"]]
  :jvm-opts ["-Xmx4g"]
  :uberjar-name "zvdd-data-pipeline.jar")
