package org.elasticsearch.plugin.view;

import com.google.common.collect.Lists;
import org.elasticsearch.action.ActionModule;
import org.elasticsearch.action.view.TransportViewAction;
import org.elasticsearch.action.view.ViewAction;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.plugins.AbstractPlugin;
import org.elasticsearch.rest.RestModule;
import org.elasticsearch.rest.action.view.RestViewAction;
import org.elasticsearch.view.ViewModule;

import java.util.Collection;
import java.util.List;


public class ViewPlugin extends AbstractPlugin {

    public String description() {
        return "Elasticsearch View Plugin";
    }

    public String name() {
        return "elasticsearch-view-plugin";
    }

    @Override
    public void processModule(Module module) {
        if (module instanceof RestModule) {
            ((RestModule) module).addRestAction(RestViewAction.class);
        }
        if (module instanceof ActionModule) {
            ((ActionModule) module).registerAction(ViewAction.INSTANCE, TransportViewAction.class);
        }
    }

    @Override
    public Collection<Class<? extends Module>> modules() {
        List<Class<? extends Module>> modules = Lists.newArrayList();
        modules.add(ViewModule.class);
        return modules;
    }

}
