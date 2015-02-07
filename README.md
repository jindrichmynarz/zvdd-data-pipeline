# ZVDD data ingestion pipeline 

Pre-processing pipeline for data from [Zentrales Verzeichnis Digitalisierter Drucke](http://www.zvdd.de/) (ZVDD) for efficient indexing in Elasticsearch.

## Configuration

In order to be able to harvest ZVDD data from the API of Deutsche Digitale Bibliothek (DDB), follow [these instructions](https://api.deutsche-digitale-bibliothek.de/doku/display/ADD/API+der+Deutschen+Digitalen+Bibliothek#APIderDeutschenDigitalenBibliothek-Registrierung) to obtain an API key. Make your API key available as value of the `DDB_API_KEY` environment variable (e.g., `export DDB_API_KEY=XYZ`).

Create a copy of the [sample configuration file](https://github.com/jindrichmynarz/zvdd-data-pipeline/blob/master/config.edn) and edit it. The configuration uses the [EDN syntax](https://github.com/edn-format/edn). If you're using the default configuration of Virtuoso RDF store running on the same machine as you will run the data ingestion pipeline, then you don't need to change the configuration. Export the path to the configuration file to the `ZVDD_CONFIG` environment variable (e.g., `export ZVDD_CONFIG=/path/to/config.edn`).

## Usage

The ZVDD data pre-processing pipeline consists of the following tasks:

* **Harvesting:** Using the `harvest` command XML files are harvested via [DDB API search](https://api.deutsche-digitale-bibliothek.de/doku/display/ADD/search).
* **Repairing syntax:** Using the `repair` command the harvested XML files are converted into syntactically correct RDF/XML files.
* **Loading data into RDF store:** Using the `load` command the repaired RDF/XML files are loaded into Virtuoso Open-Source via its [bulk loader](http://virtuoso.openlinksw.com/dataspace/doc/dav/wiki/Main/VirtBulkRDFLoader).
* **Cleaning:** The source data is cleaned using SPARQL Update operations (unfinished).
* **Linking:** The cleaned data is linked to external datasets using SPARQL Update operations (TBD).
* **Harvesting links:** The data is enriched by harvesting external linked data (TBD).
* **Transformation into JSON-LD:** The enriched data is transformed into JSON-LD trees using SPARQL CONSTRUCT queries and [JSON-LD Framing](http://json-ld.org/spec/latest/json-ld-framing/) (TBD).
* **Indexing in Elasticsearch:** The JSON-LD trees are loaded into Elasticsearch index using a specified mapping (TBD).

A more detailed description of the ZVDD data ingestion pipeline is available [here](https://docs.google.com/document/d/1RlbZSeZ3Y_cIZ5VMEc7JzK0RaiTwVWDx_-DSwpsWDZk/edit?usp=sharing).

## Compilation

The tool can be compiled using [Leiningen](http://leiningen.org/):

```bash
git clone https://github.com/jindrichmynarz/zvdd-data-pipeline.git
cd zvdd-data-pipeline
lein uberjar
```

## License

Copyright © 2015 Jindřich Mynarz

Distributed under the Eclipse Public License version 1.0.
