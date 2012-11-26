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
package org.elasticsearch.view.mvel;

import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.concurrent.ConcurrentCollections;
import org.elasticsearch.view.ViewEngineService;
import org.mvel2.templates.CompiledTemplate;
import org.mvel2.templates.TemplateCompiler;
import org.mvel2.templates.TemplateRegistry;
import org.mvel2.templates.TemplateRuntime;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

public class MvelViewEngineService extends AbstractComponent implements ViewEngineService {

    private final StaticCacheTemplateRegistry registry;

    @Inject
    public MvelViewEngineService(Settings settings) {
        super(settings);
        registry = new StaticCacheTemplateRegistry();
    }

    @Override
    public String[] types() {
        return new String[]{"mvel"};
    }

    @Override
    public String[] extensions() {
        return new String[]{"mv"};
    }

    @Override
    public String contentType() {
        return "text/html;charset=utf8";
    }

    @Override
    public void load(String name, String view) {
        // compile the template
        CompiledTemplate compiled = TemplateCompiler.compileTemplate(view);
        registry.addNamedTemplate(name, compiled);
    }

    @Override
    public byte[] render(String view, @Nullable Map<String, Object> vars) {
        String output = (String) TemplateRuntime.eval(view, vars, registry);
        return output.getBytes();
    }

    private class StaticCacheTemplateRegistry implements TemplateRegistry {

        private final ConcurrentMap<String, CompiledTemplate> staticCache = ConcurrentCollections.newConcurrentMap();

        @Override
        public Iterator iterator() {
            return staticCache.values().iterator();
        }

        @Override
        public Set<String> getNames() {
            return staticCache.keySet();
        }

        @Override
        public boolean contains(String name) {
            return staticCache.containsKey(name);
        }

        @Override
        public void addNamedTemplate(String name, CompiledTemplate compiledTemplate) {
            staticCache.put(name, compiledTemplate);
        }

        @Override
        public CompiledTemplate getNamedTemplate(String name) {
            return staticCache.get(name);
        }
    }
}
