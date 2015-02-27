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
package org.elasticsearch.plugin.view;

import org.elasticsearch.action.ActionModule;
import org.elasticsearch.action.view.TransportViewAction;
import org.elasticsearch.action.view.ViewAction;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.plugins.AbstractPlugin;
import org.elasticsearch.rest.RestModule;
import org.elasticsearch.rest.action.view.RestViewAction;
import org.elasticsearch.view.ViewModule;

import java.util.Collection;
import java.util.List;


public class ViewPlugin extends AbstractPlugin {

    @Override
    public String description() {
        return "Elasticsearch View Plugin";
    }

    @Override
    public String name() {
        return "view-plugin";
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
