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
         },
         "image" : {
            "type": "string",
            "index": "not_analyzed",
            "store": "yes"
         }
       },
         "_meta": {
           "views": {
             "light": {
               "view_lang": "mustache",
               "view": "Hello, Im rendering the doc {{_type}}:{{_id}} in version {{_version}} from index {{_index}} which has content {{#_source.languages}}{{language}}{{/_source.languages}} and {{_source.code}} or {{_source.fournisseur.pays}} and {{#_source.devise?}}with a currency!{{/_source.devise?}}{{^_source.devise}}and no currency{{/_source.devise}}"
             },
              "full": {
                "view_lang": "mustache",
                "view": "{{<html}} {{$title}}Detail of {{_type}} #{{_id}}{{/title}} {{$content}} {{_source.nom}}:{{_source.code}}<p>{{_source.fournisseur.nom}}</p> {{<image}}{{$imageBase64}}{{_source.image}}{{/imageBase64}}{{/image}}  {{/content}} {{/html}}"
              }
           }
         }
     }
   }
 }'


curl -XPUT 'http://localhost:9200/catalog/list-of-products-by-size/1:10' -d '{
    "views":{
        "default":{
            "view_lang": "mustache",
            "queries": {
                "products_with_size_1_10": {
                    "index": "produits",
                    "query" : {
                          "constant_score" : {
                              "filter" : {
                                  "term" : { "echelle" : "1:10"}
                              }
                          }
                    }
                },
                "with_errors" : {
                    "index": "shadow",
                    "query":{
                        "match_all":{}
                    }
                }
            },
            "view" : "{{<html}} {{$title}}List of products with a size of 1:10{{/title}} {{$content}} {{#_queries.products_with_size_1_10}} {{type}}:{{id}} {{source.nom}}/{{_source.code}} {{/_queries.products_with_size_1_10}}  {{/content}} {{/html}}"
        }
    }
}'


curl -XPOST 'http://localhost:9200/produits/_bulk' --data-binary @books.json

curl -XGET 'http://localhost:9200/produits/_refresh'
