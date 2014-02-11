/*
 * Licensed to Elastic Search and Shay Banon under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Elastic Search licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.elasticsearch.action.view;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.ElasticSearchIllegalArgumentException;
import org.elasticsearch.ElasticSearchParseException;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.TransportSearchAction;
import org.elasticsearch.action.support.single.shard.TransportShardSingleOperationAction;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.block.ClusterBlockException;
import org.elasticsearch.cluster.block.ClusterBlockLevel;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.cluster.routing.ShardIterator;
import org.elasticsearch.common.collect.ImmutableMap;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.get.GetResult;
import org.elasticsearch.index.service.IndexService;
import org.elasticsearch.index.shard.service.IndexShard;
import org.elasticsearch.indices.IndicesService;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.view.ViewContext;
import org.elasticsearch.view.ViewResult;
import org.elasticsearch.view.ViewService;
import org.elasticsearch.view.exception.ElasticSearchViewNotFoundException;


public class TransportViewAction extends TransportShardSingleOperationAction<ViewRequest, ViewResponse> {

    private final ViewService viewService;
    private final IndicesService indicesService;
    private final TransportSearchAction searchAction;

    @Inject
    public TransportViewAction(Settings settings, ThreadPool threadPool,
                               ClusterService clusterService,
                               TransportService transportService,
                               IndicesService indicesService,
                               ViewService viewService,
                               TransportSearchAction searchAction) {
        super(settings, threadPool, clusterService, transportService);
        this.indicesService = indicesService;
        this.viewService = viewService;
        this.searchAction = searchAction;
    }

    @Override
    protected String executor() {
        return ThreadPool.Names.GENERIC;
    }

    @Override
    protected ViewRequest newRequest() {
        return new ViewRequest();
    }

    @Override
    protected ViewResponse newResponse() {
        return new ViewResponse();
    }

    @Override
    protected String transportAction() {
        return ViewAction.NAME;
    }

    @Override
    protected ClusterBlockException checkGlobalBlock(ClusterState state, ViewRequest request) {
        return state.blocks().globalBlockedException(ClusterBlockLevel.READ);
    }

    @Override
    protected ClusterBlockException checkRequestBlock(ClusterState state, ViewRequest request) {
        return state.blocks().indexBlockedException(ClusterBlockLevel.READ, request.index());
    }

    @Override
    protected ShardIterator shards(ClusterState state, ViewRequest request) {
        return clusterService.operationRouting()
                .getShards(clusterService.state(), request.index(), request.type(), request.id(), null, null);
    }

    @Override
    protected ViewResponse shardOperation(ViewRequest request, int shardId) throws ElasticSearchException {

        // Get the doc first
        IndexService indexService = indicesService.indexService(request.index());
        IndexShard indexShard = indexService.shardSafe(shardId);
        GetResult getResult = indexShard.getService().get(request.type(), request.id(), null, false);

        if (!getResult.isExists()) {
            throw new ElasticSearchIllegalArgumentException("Document not found, cannot render view");
        }

        // Try to get a view stored at document level
        ViewContext viewContext = extract(getResult.sourceAsMap(), request.format());

        if (viewContext == null) {
            // Then, get the view stored in the mapping _meta field
            MappingMetaData mappingMetaData = clusterService.state().metaData().index(request.index()).mapping(request.type());
            if (mappingMetaData != null) {
                try {
                    Map<String, Object> mapping = mappingMetaData.sourceAsMap();
                    viewContext = extract(mapping, request.format());
                } catch (IOException e) {
                    throw new ElasticSearchParseException("Failed to parse mapping content to map", e);
                }
            }
        }

        if (viewContext == null) {
            throw new ElasticSearchViewNotFoundException("No view [" + request.format() + "] found for document type [" + request.type() + "]");
        }

        // Set some org.elasticsearch.test.integration.views.mappings.data required for view rendering
        viewContext.index(getResult.getIndex())
                .type(getResult.getType())
                .id(getResult.getId())
                .version(getResult.getVersion())
                .source(getResult.sourceAsMap());

        // Ok, let's render it with a ViewEngineService
        ViewResult result = viewService.render(viewContext);

        return new ViewResponse(result.contentType(), result.content());
    }

    private ViewContext extract(Map<String, Object> sourceAsMap, String format) {
        if (sourceAsMap != null) {
            for (String key : sourceAsMap.keySet()) {
                Object views = null;

                // When searching in a mapping
                if ("_meta".equals(key)) {
                    Object meta = sourceAsMap.get(key);
                    if (meta instanceof Map) {
                        views = ((Map) meta).get("views");
                    }
                }

                // When searching in the document content
                if ("views".equals(key)) {
                    views = sourceAsMap.get(key);
                }

                if ((views != null) && (views instanceof Map)) {
                    Map mapViews = (Map) views;
                    Object candidate = null;

                    // Try to load a specific view
                    if (format != null) {
                        candidate = mapViews.get(format);
                    } else if (!mapViews.isEmpty()) {
                        // Try to load the "default" view
                        Object defaultView = mapViews.get(ViewRequest.DEFAULT_VIEW);
                        if (defaultView != null) {
                            candidate = defaultView;
                        }
                    }
                    if ((candidate != null) && (candidate instanceof Map)) {
                        Map mapCandidate = (Map) candidate;

                        // ViewContext holds the org.elasticsearch.test.integration.views.mappings.data for view rendering
                        ViewContext viewContext = new ViewContext((String) mapCandidate.get("view_lang"), (String) mapCandidate.get("view"), (String) mapCandidate.get("content_type"));

                        Object queries = mapCandidate.get("queries");
                        Map<String, Object> mapQueries = null;

                        if (queries != null) {
                            if (queries instanceof List) {
                                List listQueries = (List) queries;
                                mapQueries = new HashMap<String, Object>(listQueries.size());
                                for (Object query : listQueries) {
                                    if (query instanceof Map) {
                                        Map q = (Map) query;
                                        for (Object queryName : q.keySet()) {
                                            if (queryName instanceof String) {
                                                mapQueries.put((String) queryName, q.get(queryName));
                                            }
                                        }
                                    }
                                }
                            } else if (queries instanceof Map) {
                                mapQueries = (Map) queries;
                            }
                        }

                        if (mapQueries != null) {
                            for (String queryName : mapQueries.keySet()) {
                                try {
                                    Map<String, Object> mapQuery = (Map) mapQueries.get(queryName);

                                    String[] indices = null;
                                    if (mapQuery.get("indices") instanceof List) {
                                        indices = (String[]) ((List) mapQuery.get("indices")).toArray(new String[0]);
                                    } else if (mapQuery.get("indices") instanceof String) {
                                        indices = new String[]{((String) mapQuery.get("indices"))};
                                    }

                                    String[] types = null;
                                    if (mapQuery.get("types") instanceof List) {
                                        types = (String[]) ((List) mapQuery.get("types")).toArray(new String[0]);
                                    } else if (mapQuery.get("types") instanceof String) {
                                        types = new String[]{((String) mapQuery.get("types"))};
                                    }

                                    SearchSourceBuilder searchSourceBuilder = null;
                                    if (mapQuery.get("sort") instanceof List) {
                                        if (searchSourceBuilder == null) {
                                            searchSourceBuilder = new SearchSourceBuilder();
                                        }
                                        for (Object sort : (List) mapQuery.get("sort")) {
                                            if (sort instanceof String) {
                                                searchSourceBuilder.sort((String) sort);
                                            } else if (sort instanceof Map) {
                                                for (Object field : ((Map) sort).keySet()) {
                                                    String sortField = (String) field;
                                                    String reverse = (String) ((Map) sort).get(field);
                                                    if ("asc".equals(reverse)) {
                                                        searchSourceBuilder.sort(sortField, SortOrder.ASC);
                                                    } else if ("desc".equals(reverse)) {
                                                        searchSourceBuilder.sort(sortField, SortOrder.DESC);
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (mapQuery.get("fields") instanceof List) {
                                        if (searchSourceBuilder == null) {
                                            searchSourceBuilder = new SearchSourceBuilder();
                                        }
                                        for (Object field : (List) mapQuery.get("fields")) {
                                            if (field instanceof String) {
                                                searchSourceBuilder.field((String) field);
                                            }
                                        }
                                    }

                                    SearchRequest searchRequest = new SearchRequest();
                                    if (indices != null) {
                                        searchRequest.indices(indices);
                                    }
                                    if (types != null) {
                                        searchRequest.types(types);
                                    }

                                    ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
                                    builder.put("query", (Map) mapQuery.get("query"));
                                    searchRequest.source(builder.build());

                                    if (searchSourceBuilder != null) {
                                        searchRequest.extraSource(searchSourceBuilder);
                                    }

                                    SearchResponse searchResponse = searchAction.execute(searchRequest).get();
                                    viewContext.queriesAndHits(queryName, searchResponse.getHits());

                                } catch (Exception e) {
                                    viewContext.queriesAndHits(queryName, null);
                                }
                            }
                        }
                        return viewContext;
                    }
                }
            }
        }
        return null;
    }
}
