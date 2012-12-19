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

Le plugin permet aussi de créer plusieurs formats de vue pour un même type de document:


Ou encore de créer une vue sur-mesure pour afficher le résultat de l'exécution de requêtes:

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
   "scale": "1:10"
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
   "scale": "1:10",
   "picture": "/9j/4AAQSkZJRgABAQAAAQABAAD//gA7..."
}'

```

And define two new views:
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
                    "view": "<div id=\"product-@{_id}\"><img src=\"/_view/catalog/product/@{_id}/logo\"/><h2>Detail of @{_source.name.toUpperCase()}</h2><p>Year: @{_source.since}, price: @{_source.price}€</p><p>@{_source.description}</p><p>@includeNamed{\"copyright\"}</p></div>"
                }
            }
        }
    }
}'
```

The URL `http://localhost:9200/_view/catalog/product/1/logo` can be used to get the picture of the product, whereas
 `http://localhost:9200/_view/catalog/product/1/full` renders the full HTML view:

image render_html.png















