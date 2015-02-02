# ZVDD data ingestion pipeline 

Pre-processing pipeline for data from [Zentrales Verzeichnis Digitalisierter Drucke](http://www.zvdd.de/) for efficient indexing in Elasticsearch.

## Usage

The ZVDD data pre-processing pipeline consists of the following tasks:

* **Harvesting:** Using the `harvest` command XML files are harvested via [DDB API search](https://api.deutsche-digitale-bibliothek.de/doku/display/ADD/search).
* **Repairing syntax:** Using the `repair` command the harvested XML files are converted into syntactically correct RDF/XML files.
* **Loading data into RDF store:** Using the `load` command the repaired RDF/XML files are loaded into Virtuoso Open-Source via its [bulk loader](http://virtuoso.openlinksw.com/dataspace/doc/dav/wiki/Main/VirtBulkRDFLoader).
* **Cleaning:** The source data is cleaned using SPARQL Update operations (TBD).
* **Linking:** The cleaned data is linked to external datasets using SPARQL Update operations (TBD).
* **Harvesting links:** The data is enriched by harvesting external linked data (TBD).
* **Transformation into JSON-LD:** The enriched data is transformed into JSON-LD trees using SPARQL CONSTRUCT queries and [JSON-LD Framing](http://json-ld.org/spec/latest/json-ld-framing/) (TBD).
* **Indexing in Elasticsearch:** The JSON-LD trees are loaded into Elasticsearch index using a specified mapping (TBD).

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
