# Introducing the ElasticSearch View Plugin

Elasticsearch provides a fast and simple way to retrieve a document with the [GET API](http://www.elasticsearch.org/guide/reference/api/get.html):
```
curl -XGET 'http://localhost:9200/catalog/product/1'
```

Until now, this API only allows to get the document in JSON format:
```
{
    "_index": "catalog",
    "_type": "product",
    "_id": "1",
    "_version": 1,
    "exists": true,
    "_source": {
        "name": "1969 Harley Davidson Ultimate Chopper",
        "type": "Motorcycles",
        "brand": "Harley Davidson",
        ...
    }
}
```

Although this format is really useful, it is not directly presentable to a final user. The JSON format is more dedicated
to be used by third party applications located at client or server side. These applications are in charge of
parsing the JSON content, extracting meaningful data and rendering them in a more graphical way, for instance within a
HTML page. With ElasticSearch, anyone who wants to have a graphical rendering of these documents shall install, configure
and maintain such an application, which can become quite complex and require regular redelivery every time the graphic
content of a document is modified

The ElasticSearch View Plugin can be used when you don't want to develop a dedicated application or when you wish to
control not only the document searches but also the way the document are displayed.

This plugin allows to create views using different templating engines (for now [MVEL](http://mvel.codehaus.org/MVEL+2.0+Basic+Templating)
and [Mustache](http://mustache.github.com/) can be used) in order to generate a HTML (or XML, or anything
which is text) display of your document and access to it threw an URL.

For example, the plugin can be used to generate a HTML page that displays our product:
http://localhost:9200/_view/catalog/product/1

![HTML view of document #1](https://raw.github.com/tlrx/elasticsearch-view-plugin/gh-pages/samples/render_html.png)

The plugin can also be used to create several formats of views for a same type of document, if necessary with the help of
predefined scripts. It can also be used to generate a specific view to show the results of predefined search queries:

http://localhost:9200/_view/web/pages/home
![HTML view of document #1](https://raw.github.com/tlrx/elasticsearch-view-plugin/gh-pages/samples/render_html_list_brand.png)

In this article, we explain how to install and configure the ElasticSearch View Plugin in order to generate HTML and XML
views of documents indexed in ElasticSearch.


## Installing the plugin

The plugin can be installed as any other ElasticSearch's plugins:

```
bin/plugin -install tlrx/elasticsearch-view-plugin/0.0.1

```

The current version of the plugin is compatible with [ElasticSearch 0.20.1](http://www.elasticsearch.org/download/2012/12/07/0.20.1.html).


## Creating views for existing documents

Let's imagine that we have a `catalog` index and few documents of `product` type:
```
curl -XPUT 'http://localhost:9200/catalog/product/1' -d '
{
   "name": "1969 Harley Davidson Ultimate Chopper",
   "type": "Motorcycles",
   "brand": "Harley Davidson",
   "code": "S10_1678",
   "since": "1969",
   "price": 48.34,
   "description": "This replica features working kickstand, front suspension, gear-shift lever, footbrake lever, drive chain, wheels and steering.",
   "scale": "10"
}'

```

ElasticSearch View Plugin uses the mapping's [meta data](http://www.elasticsearch.org/guide/reference/mapping/meta.html)
to store all the views that are associated with a specific document type. Each view has a unique name, a scripting language,
a content and eventually a content type.

Be careful, as the [Update API](http://www.elasticsearch.org/guide/reference/api/update.html), the `_source` field need
to be enabled for this feature to work.

First, we can create a basic view using the [MVEL templating](http://mvel.codehaus.org/MVEL+2.0+Basic+Templating) language:
```
curl -XPUT 'http://localhost:9200/catalog/product/_mapping' -d '
{
    "product": {
        "_meta": {
            "views": {
                "default": {
                    "view_lang": "mvel",
                    "view": "Rendering the document #@{_id} in version @{_version} of type @{_type} from index @{_index}"
                }
            }
        }
    }
}'
```

The previous command creates a view called `default`. The property `view_lang` can be used to specify the templating
engine to use (default is `mvel`) whereas the `view` property holds the content of the view. When needed, a specific `content_type`
can be set. Note that the view named `default` will be used by default to render the documents of type `product`.

In MVEL, the coordinates of the document are available with `@{_id}`, `@{_type}` and `@{_index}` instructions. The original
`_source` of the document can be accessed with `@{_source._x_}` where _x_ is a document property name.

Now the view is created, opening the URL `http://localhost:9200/_view/catalog/product/1` in a web browser will trigger
the rendering of document with id 1. The result looks like:

![Default view for product #1](https://raw.github.com/tlrx/elasticsearch-view-plugin/gh-pages/samples/render_default.png)

Simple, no?


### Using multiple views

In most use cases, a unique view is not sufficient. That's why the plugins allows to define many views for the same type of document,
 allowing differents renderings of the same document:

```
curl -XPUT 'http://localhost:9200/catalog/product/_mapping' -d '
{
    "product": {
        "_meta": {
            "views": {
                "default": {
                    "view_lang": "mvel",
                    "view": "Rendering the document #@{_id} in version @{_version} of type @{_type} from index @{_index}"
                },
                "xml": {
                    "view_lang": "mvel",
                    "content_type": "text/xml",
                    "view": "<?xml version=\"1.0\" encoding=\"UTF-8\"?><product id=\"@{_id}\"><name>@{_source.name}</name><brand>@{_source.brand}</brand></product>"
                }
            }
        }
    }
}'
```


This way the URL `http://localhost:9200/_view/catalog/product/1/xml` can be used to access to the XML view of document 1:

![XML view for product #1](https://raw.github.com/tlrx/elasticsearch-view-plugin/gh-pages/samples/render_xml.png)


### Rendering binary fields

If the document contains a [binary field](http://www.elasticsearch.org/guide/reference/mapping/core-types.html), the `binary`
view language can be used to get an octet stream corresponding to the field value.

To illustrate that, we can add a new picture field to document 1 (the full JSON content is available on [gist](https://gist.github.com/4337853)):
```
curl -XPUT 'http://localhost:9200/catalog/product/1' -d '
{
   "name": "1969 Harley Davidson Ultimate Chopper",
   "type": "Motorcycles",
   "brand": "Harley Davidson",
   "code": "S10_1678",
   "since": "1969",
   "price": 48.34,
   "description": "This replica features working kickstand, front suspension, gear-shift lever, footbrake lever, drive chain, wheels and steering.",
   "scale": "10",
   "picture": "/9j/4AAQSkZJRgABAQAAAQABAAD//gA7..."
}'

```

The picture field contains a base64 encoded image of the Harley Davidson's logo.

We can now define two more views:
* **logo**: which renders the picture as binary content
* **full**: which renders the document as  HTML block

```
curl -XPUT 'http://localhost:9200/catalog/product/_mapping' -d '
{
    "product": {
        "_meta": {
            "views": {
                "default": {
                    "view_lang": "mvel",
                    "view": "Rendering the document #@{_id} in version @{_version} of type @{_type} from index @{_index}"
                },
                "xml": {
                    "view_lang": "mvel",
                    "content_type": "text/xml",
                    "view": "<?xml version=\"1.0\" encoding=\"UTF-8\"?><product id=\"@{_id}\"><name>@{_source.name}</name><brand>@{_source.brand}</brand></product>"
                },
                "logo": {
                    "view_lang": "binary",
                    "view": "_source.picture"
                },
                "full": {
                    "view_lang": "mvel",
                    "view": "<div id=\"product-@{_id}\"><img src=\"/_view/catalog/product/@{_id}/logo\"/><h2>Detail of @{_source.name.toUpperCase()}</h2><p>Year: @{_source.since}, price: @{_source.price}€</p><p>@{_source.description}</p><p>© Copyright @{_source.brand}</p></div>"
                }
            }
        }
    }
}'
```

The URL `http://localhost:9200/_view/catalog/product/1/logo` can be used to get the picture of the product, whereas
 `http://localhost:9200/_view/catalog/product/1/full` renders the full HTML view:

![HTML view of document #1](https://raw.github.com/tlrx/elasticsearch-view-plugin/gh-pages/samples/render_html.png)


## Using preloaded templates

Similar to the [scripting module](http://www.elasticsearch.org/guide/reference/modules/scripting.html), the ElasticSearch
View Plugin supports predefined templates scripts.

The scripts must be placed under the `config/views` directory and then referencing them by the script name. The way to
reference a script differs according to the view language.

For example, we can create the file  `config/views/copyright.mv` with the following content:
```
<p>© Copyright @{_source.brand}</p>
```

The `.mv` extension indicates that the file contains a template written in MVEL.

After a cluster restart, we will be able to update the `full` view in order to use the preloaded template script (note the
@includeNamed{} instruction):
```
...
     "full": {
         "view_lang": "mvel",
         "view": "<div id=\"product-@{_id}\"><img src=\"/_view/catalog/product/@{_id}/logo\"/><h2>Detail of @{_source.name.toUpperCase()}</h2><p>Year: @{_source.since}, price: @{_source.price}€</p><p>@{_source.description}</p>@includeNamed{\"copyright\"}</div>"
     }
...
 ```

Preloaded templates are great candidates for code o text that are used in mulitple views.

## Creating complete views from queries

The plugin allows to create custom views from query hits. Everytime such a view is requested, a set of predefined queries
are executed and the results are used to create the view. Such views are stored in ElasticSearch as standard documents.

This kind of view is really powerful and are a simple way to create complete web pages.

First, let's create a more complex template called `list-of-products` and stored in the file `config/views/list-of-products.mv`:
```html
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="utf-8">
    <title>@{title}</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="description" content="">
    <meta name="author" content="">
    <link href="http://twitter.github.com/bootstrap/assets/css/bootstrap.css" rel="stylesheet">
    <style>
      body {
        padding-top: 60px; /* 60px to make the container go all the way to the bottom of the topbar */
      }
    </style>

    <!-- HTML5 shim, for IE6-8 support of HTML5 elements -->
    <!--[if lt IE 9]>
      <script src="http://html5shim.googlecode.com/svn/trunk/html5.js"></script>
    <![endif]-->
  </head>

  <body>
    <div class="navbar navbar-inverse navbar-fixed-top">
      <div class="navbar-inner">
        <div class="container">
          <a class="btn btn-navbar" data-toggle="collapse" data-target=".nav-collapse">
            <span class="icon-bar"></span>
            <span class="icon-bar"></span>
            <span class="icon-bar"></span>
          </a>
          <a class="brand" href="#">Catalog</a>
          <div class="nav-collapse collapse">
            <ul class="nav">
              <li class="active"><a href="#">List of products</a></li>
            </ul>
          </div>
        </div>
      </div>
    </div>
    <div class="container">
        <h1>List of products with scale 1:10</h1>
        <table class="table table-striped">
            <thead>
                <tr>
                    <th>Code</th>
                    <th>Name</th>
                    <th>Brand</th>
                    <th>Year</th>
                </tr>
            </thead>
            <tbody>
                @foreach{item : _queries.products_with_size_1_10}
                    <tr>
                        <td>@{item._source.code}</td>
                        <td>@{item._source.name}</td>
                        <td>@{item._source.brand}</td>
                        <td>@{item._source.since}</td>
                    </tr>
                @end{}
            </tbody>
        </table>
    </div>
  </body>
</html>
```

Next, we can create a view:
```
curl -XPUT "http://localhost:9200/catalog/list-of-products-by-size/1:10" -d "{
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
                                  \"term\" : { \"scale\" : \"10\"}
                              }
                          }
                    }
                }
            },
            \"view\" : \"@includeNamed{'list-of-products'; title='List of products'}\"
        }
    }
}"
```

Note that the view is indexed in `catalog` index with the `list-of-products-by-size` document type and id `1:10`. It defines
a view called `default` (but could have another name) and uses the `list-of-products.mv` template to render a list
of products.

The list of products is defined by the `products_with_size_1_10` query in the `queries` field of the view. This query
selects 10 products that have a scale of 1:10.

If you look closely at the previous template, you can see the following code:
```
@foreach{item : _queries.products_with_size_1_10}
    <tr>
        <td>@{item._source.code}</td>
        <td>@{item._source.name}</td>
        <td>@{item._source.brand}</td>
        <td>@{item._source.since}</td>
    </tr>
@end{}
```

This code uses a MVEL templating syntax `@foreach{}...@end{}` that iterates over the hits provided by the
`products_with_size_1_10` query in order to construct a dynamic table of products that will be rendered in the
final HTML page. Of course, multiple queries can be used in the same view.

The result is available at `http://localhost:9200/_view/catalog/list-of-products-by-size/1:10` and looks like:

![List of products with scale 1:10](https://raw.github.com/tlrx/elasticsearch-view-plugin/gh-pages/samples/render_html_list.png)


## Going further...

### Using Mustache

The plugin [elasticsearch-view-mustache-plugin](https://github.com/tlrx/elasticsearch-view-mustache-plugin) adds
[Mustache](http://mustache.github.com/) as templating language for views.

Mustache is a great templating engine that supports template encapsulation. To defined views with Mustache template engine,
use `"view_lang": "mustache"`.

Some sample usage of this plugin can be found in the Github project.

### Rewriting URLs with Apache2

Apache2 server with mod_proxy and mod_rewrite can be used to redirect ElasticSearch Views Plugin URLs to better looking URLs.

The goal is to have nicer URLs like `http://www.domain.com/catalog/list-of-products-by-size/1:10` that points
to internal `http://localhost:9200/_view/catalog/list-of-products-by-size/1:10`.

Here is a basic sample of such URL rewriting:
```
RewriteEngine on
RewriteRule ^catalog/(.*)$ http://localhost:9200/_view/catalog/$1 [P,L]
```



We hope that this plugin will be as useful for you as it is for us, and we welcome your feedback and comments about this new plugin.






















