#!/bin/bash

curl -XDELETE 'http://localhost:9200/'

curl -XPOST 'http://localhost:9200/produits' -d '{
   "settings": {
     "number_of_shards": 2,
     "number_of_replicas": 1
   },
   "mappings": {
     "produit": {
       "properties": {
         "nom": {
           "type": "multi_field",
           "fields": {
             "nom": {
               "type": "string",
               "index": "analyzed",
               "stored": "yes"
             },
             "original": {
               "type": "string",
               "index": "not_analyzed"
             }
           }
         },
         "code": {
           "type": "string",
           "index": "not_analyzed",
           "stored": "yes",
           "include_in_all": "false"
         },
         "echelle": {
           "type": "string",
           "index": "not_analyzed",
           "stored": "no",
           "include_in_all": "false"
         },
         "devise": {
           "type": "string",
           "index": "not_analyzed",
           "stored": "no",
           "include_in_all": "false"
         },
         "type": {
           "type": "string",
           "index": "not_analyzed",
           "stored": "yes",
           "include_in_all": "false"
         },
         "fournisseur": {
           "type": "object",
           "properties": {
             "nom": {
               "type": "string",
               "index": "analyzed",
               "include_in_all": "true",
               "null_value": "inconnu"
             },
             "pays": {
               "type": "string",
               "index": "not_analyzed",
               "include_in_all": "false"
             }
           }
         },
         "stock": {
           "type": "integer"
         },
         "prix": {
           "type": "double"
         },
         "annee": {
           "type": "date",
           "format": "YYYY",
           "ignore_malformed": "true"
         }
       },
         "_meta": {
           "views": {
             "full": {
               "view_lang": "mustache",
               "view": "Hello, I''m rendering the doc {{_id}} from index {{_index}} which has content {{#_source?}} {{title}} {{/_source?}}"
             }
           }
         }
     }
   }
 }'

curl -XPOST 'http://localhost:9200/produits/_bulk' --data-binary @books.json

curl -XGET 'http://localhost:9200/produits/_refresh'
