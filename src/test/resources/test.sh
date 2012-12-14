#!/bin/bash

ES_HOST=localhost:9200

echo Delete all
curl -XDELETE "http://$ES_HOST/"

echo
echo Creating index catalog
curl -XPOST "http://$ES_HOST/catalog"

echo
echo Updating product mapping
curl -XPUT "http://$ES_HOST/catalog/product/_mapping" --data-binary @org/elasticsearch/test/integration/views/mappings/product.json

echo
echo Bulk indexing products
curl -XPOST "http://$ES_HOST/catalog/product/_bulk" --data-binary @org/elasticsearch/test/integration/views/data/products.json


echo
echo Indexing list-of-products-by-size view
curl -XPUT "http://$ES_HOST/catalog/list-of-products-by-size/1:10" -d "{
    \"views\":{
        \"default\":{
            \"view_lang\": \"mvel\",
            \"queries\": {
                \"products_with_size_1_10\": {
                    \"indices\": \"catalog\",
                    \"types\": [\"product\"],
                    \"query\" : {
                          \"constant_score\" : {
                              \"filter\" : {
                                  \"term\" : { \"scale\" : \"1:10\"}
                              }
                          }
                    }
                }
            },
            \"view\" : \"@includeNamed{'list-of-products'; title='List of products'}\"
        }
    }
}"

echo
echo Refresh all
curl -XPOST "http://$ES_HOST/_refresh"


echo
echo ----- Views -------
curl -XGET "http://$ES_HOST/_view/catalog/product/1"

echo
curl -XGET "http://$ES_HOST/_view/catalog/product/2/full"

echo
curl -XGET "http://$ES_HOST/_view/catalog/product/1/logo" -o /dev/null -w "Size: %{size_download}\r\n"

echo
curl -XGET "http://$ES_HOST/_view/catalog/product/2/logo" -o /dev/null -w "Size: %{size_download}\r\n"

echo
curl -XGET "http://$ES_HOST/_view/catalog/list-of-products-by-size/1:10"

