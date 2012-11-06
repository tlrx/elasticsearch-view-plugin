package org.elasticsearch.action.view;

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
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.view.ViewContext;
import org.elasticsearch.view.ViewResult;
import org.elasticsearch.view.ViewService;

import java.io.IOException;
import java.util.Map;


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

        if (!getResult.exists()) {
            throw new ElasticSearchIllegalArgumentException("Document not found, cannot render view");
        }

        // Try to get a view stored at document level
        ViewContext viewContext = extract(getResult.sourceAsMap(), request.format());

        if (viewContext == null) {
            // Then, get the view stored in the mapping _meta field
            MappingMetaData mappingMetaData = clusterService.state().metaData().index(request.index()).mapping(request.type());
            try {
                Map<String, Object> mapping = mappingMetaData.sourceAsMap();
                viewContext = extract(mapping, request.format());
            } catch (IOException e) {
                throw new ElasticSearchParseException("Failed to parse mapping content to map", e);
            }
        }

        if (viewContext == null) {
            throw new ElasticSearchIllegalArgumentException("No view defined in the mapping for document type [" + request.type() + "]");
        }

        // Set some data required for view rendering
        viewContext.index(getResult.index())
                    .type(getResult.type())
                    .id(getResult.id())
                    .version(getResult.version())
                    .source(getResult.sourceAsMap());

        // Ok, let's render it with a ViewEngineService
        ViewResult result = viewService.render(viewContext);

        return new ViewResponse(result.contentType(), result.content());
    }

    private ViewContext extract(Map<String, Object> sourceAsMap, String format) {
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

                Object candidate = mapViews.get(format);
                if ((candidate == null) && (!mapViews.isEmpty())) {
                    candidate = mapViews.values().iterator().next();
                }
                if ((candidate != null) && (candidate instanceof Map)) {
                    Map mapCandidate = (Map) candidate;

                    // ViewContext holds the data for view rendering
                    ViewContext viewContext = new ViewContext((String) mapCandidate.get("view_lang"), (String) mapCandidate.get("view"));

                    Object queries = mapCandidate.get("queries");
                    if ((queries != null) && (queries instanceof Map)) {
                        Map<String, Object> mapQueries = (Map) queries;

                        for (String queryName : mapQueries.keySet()) {
                            try {
                                Map<String, Object> mapQuery = (Map) mapQueries.get(queryName);
                                SearchRequest searchRequest = new SearchRequest((String) mapQuery.get("index")); //todo gérer les types et les multi indexs

                                ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
                                builder.put("query", (Map) mapQuery.get("query"));
                                searchRequest.source(builder.build());

                                SearchResponse searchResponse = searchAction.execute(searchRequest).get();
                                viewContext.queriesAndHits(queryName, searchResponse.hits());

                            } catch (Exception e) {
                                //todo gérer les exceptions
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
