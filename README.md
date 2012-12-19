### Introducing the ElasticSearch View Plugin

ElasticSearch fournit un moyen simple et rapide de récupérer un document indexé grâce à l'API Get. Jusqu'à présent, cette
API ne permet de récupérer un document qu'au format JSON:

````
curl -XGET 'http://localhost:9200/catalog/product/1'


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
````

Bien que très pratique, ce format n'est pas directement présentable à un utilisateur final. Il est plutot destiné à être
exploité par des applications tierces dont le rôle est de générer un rendu graphique d'un ou plusieurs documents, par
exemple sur une page HTML. Ces applications tierces nécessitent un environnement d'exécution dédié (serveur
d'application JEE, serveur Apache/IIS etc) qu'il faut installer, configurer et maintenir, ainsi que des livraisons
régulières de l'application lorsque le rendu graphique d'un document est modifié.

Lorsqu'il n'est pas souhaitable d'utiliser une telle application, ou lorsque l'on souhaite contrôler non seulement les
recherches de documents mais aussi leur rendu graphique, il est possible d'utiliser le plugin "ElasticSearch View Plugin".

Ce plugin permet de créer des vues en utilisant différents systèmes de templates (actuellement MVEL et Mustache sont
supportés) afin de générer des rendus HTML (ou XML, Markdown ou n'importe quoi tant que c'est du texte) et d'y accéder
par une simple URL.

Par exemple, pour accéder à un rendu HTML de notre produit:
````
````

(exemple HTML)

Le plugin permet aussi de créer plusieurs formats de vue pour un même type de document, éventuellement basé sur des
scripts prédéfinis, ou encore de créer une vue sur-mesure pour afficher le résultat de l'exécution de requêtes:

(exemple HTML).



## Installing the plugin

In order to install the plugin, simply run:

```
bin/plugin -install tlrx/elasticsearch-view-plugin/0.0.1

```

And start your ElasticSearch cluster.


## Creating views for existing documents

Let's imagine that we have a `catalog` index in which we indexed many documents of `product` type:
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
to store all the views that are associated with a specific document type. Each view has a unique name, a template language
and a template content.

Be careful, as the [Update API](http://www.elasticsearch.org/guide/reference/api/update.html), the `_source` field need to be enabled for this feature to work.

Let's create a basic view using the [MVEL templating](http://mvel.codehaus.org/MVEL+2.0+Basic+Templating) language:
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
can be set. The view named `default` will be used by default to render the documents of type `product`.

Now the view is created, opening the URL `http://localhost:9200/_view/catalog/product/1` in a web browser will trigger the rendering of document with id 1.

The result looks like:

imag render_default.png

### Using multiple views

Many views can be defined for the same type of document, allowing differents renderings of the same document:

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


The URL `http://localhost:9200/_view/catalog/product/1/xml` can be used to access to the XML view of document 1:

image render_xml.png


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

The picture field contains a base64 encoded image of Harley Davidson's logo.

We can now define two more views:
* logo: which renders the picture as binary content
* full: which renders the document as HTML content


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

image render_html.png



## Using preloaded templates

Similar to the [scripting module](http://www.elasticsearch.org/guide/reference/modules/scripting.html), the ElasticSearch
View Plugin supports predefined templates scripts.

The scripts can be placed under the `config/views` directory and then referencing them by the script name. The way to
reference a script differs according to the view language.

For example, we can create the file  `config/views/copyright.mv` with the following content:
```
<p>© Copyright @{_source.brand}</p>
```

The `.mv` extension indicates that the file contains a template in MVEL language.

After a cluster restart, we will be able to update the `full` view in order to use the preloaded template script:
```
...
     "full": {
         "view_lang": "mvel",
         "view": "<div id=\"product-@{_id}\"><img src=\"/_view/catalog/product/@{_id}/logo\"/><h2>Detail of @{_source.name.toUpperCase()}</h2><p>Year: @{_source.since}, price: @{_source.price}€</p><p>@{_source.description}</p>@includeNamed{\"copyright\"}</div>"
     }
...
 ```

## Creating complete views from queries

The plugin allows to create custom views from query hits. Everytime such a view is requested, a set of predefined queries
are executed and the results are used to create the view.

This kind of view are really powerful and are a simple way to create complete web pages.

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

    <!-- Le styles -->
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

Next, we can create the view:
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

This view is called `default` (but could have another name) and uses the `list-of-products` template to render a list
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

img render_html_list.png

These kind of view are indexed as normal ElasticSearch docs.


## Using Mustache

The plugin [elasticsearch-view-mustache-plugin](https://github.com/tlrx/elasticsearch-view-mustache-plugin) adds
[Mustache](http://mustache.github.com/) as templating language for views.

Mustache is a great templating engine that supports template encapsulation.

Hope this plugin will be as useful as it is for us :)






















