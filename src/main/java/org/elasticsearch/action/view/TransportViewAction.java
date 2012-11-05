package org.elasticsearch.action.view;

import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.ElasticSearchIllegalArgumentException;
import org.elasticsearch.ElasticSearchParseException;
import org.elasticsearch.action.support.single.shard.TransportShardSingleOperationAction;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.block.ClusterBlockException;
import org.elasticsearch.cluster.block.ClusterBlockLevel;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.cluster.routing.ShardIterator;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.get.GetResult;
import org.elasticsearch.index.service.IndexService;
import org.elasticsearch.index.shard.service.IndexShard;
import org.elasticsearch.indices.IndicesService;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.view.ViewContext;
import org.elasticsearch.view.ViewService;

import java.io.IOException;
import java.util.Map;


public class TransportViewAction extends TransportShardSingleOperationAction<ViewRequest, ViewResponse> {

    private final ViewService viewService;
    private final IndicesService indicesService;

    @Inject
    public TransportViewAction(Settings settings, ThreadPool threadPool,
                               ClusterService clusterService,
                               TransportService transportService,
                               IndicesService indicesService,
                               ViewService viewService) {
        super(settings, threadPool, clusterService, transportService);
        this.indicesService = indicesService;
        this.viewService = viewService;
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

        // Then, get the view stored in the mapping _meta field
        MappingMetaData mappingMetaData = clusterService.state().metaData().index(request.index()).mapping(request.type());
        ViewContext viewContext = null;
        try {
            Map<String, Object> mapping = mappingMetaData.sourceAsMap();
            for (String key : mapping.keySet()) {
                if ("_meta".equals(key)) {
                    Object meta = mapping.get(key);
                    if (meta instanceof Map) {
                        Object views = ((Map) meta).get("views");
                        if ((views != null) && (views instanceof Map)) {
                            Map mapViews = (Map) views;

                            Object candidate = mapViews.get(request.format());
                            if ((candidate == null) && (!mapViews.isEmpty())) {
                                candidate = mapViews.values().iterator().next();
                            }
                            if ((candidate != null) && (candidate instanceof Map)) {
                                Map mapCandidate = (Map) candidate;

                                // ViewContext holds the data for view rendering
                                viewContext = new ViewContext((String) mapCandidate.get("view_lang"), (String) mapCandidate.get("view"));
                            }
                        }
                        break;
                    }
                }
            }

        } catch (IOException e) {
            throw new ElasticSearchParseException("Failed to parse mapping content to map", e);
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
        Object render = viewService.render(viewContext);
                //todo gérer le content type correctement boudiou!
        return new ViewResponse((String)render, "text/html;charset=utf8");
    }
}
