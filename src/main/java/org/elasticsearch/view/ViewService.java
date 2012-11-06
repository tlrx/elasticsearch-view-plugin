package org.elasticsearch.view;

import org.elasticsearch.common.collect.ImmutableMap;
import org.elasticsearch.common.collect.ImmutableSet;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.io.Streams;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.concurrent.ConcurrentCollections;
import org.elasticsearch.env.Environment;
import org.elasticsearch.script.CompiledScript;
import org.elasticsearch.script.ScriptEngineService;
import org.elasticsearch.view.mustache.MustacheViewEngineService;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

public class ViewService extends AbstractComponent {

    private final String defaultViewLang;

    private final ImmutableMap<String, ViewEngineService> viewEngines;

    private final ConcurrentMap<String, CompiledScript> staticCache = ConcurrentCollections.newConcurrentMap();

    public ViewService(Settings settings) {
        this(settings, new Environment(), ImmutableSet.<ViewEngineService>builder()
                .add(new MustacheViewEngineService(settings))
                .build());
    }

    @Inject
    public ViewService(Settings settings, Environment environment, Set<ViewEngineService> viewEngines) {
        super(settings);

        // todo vérifier que ça marche ce truc
        this.defaultViewLang = componentSettings.get("default_view_lang", "mustache");

        ImmutableMap.Builder<String, ViewEngineService> builder = ImmutableMap.builder();
        for (ViewEngineService viewEngine : viewEngines) {
            for (String type : viewEngine.types()) {
                builder.put(type, viewEngine);
            }
        }
        this.viewEngines = builder.build();

        // compile static scripts
        File viewsFile = new File(environment.configFile(), "views");
        if (viewsFile.exists()) {
            processViewsDirectory("", viewsFile);
        }
    }

    private void processViewsDirectory(String prefix, File dir) {
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                processViewsDirectory(prefix + file.getName() + "_", file);
            } else {
                int extIndex = file.getName().lastIndexOf('.');
                if (extIndex != -1) {
                    String ext = file.getName().substring(extIndex + 1);
                    String viewName = prefix + file.getName().substring(0, extIndex);
                    boolean found = false;
                    for (ViewEngineService viewEngine : viewEngines.values()) {
                        for (String s : viewEngine.extensions()) {
                            if (s.equals(ext)) {
                                found = true;
                                try {
                                    String view = Streams.copyToString(new InputStreamReader(new FileInputStream(file), "UTF-8"));
                                    viewEngine.load(viewName, view);
                                } catch (Exception e) {
                                    logger.warn("failed to load/compile view [{}]", e, viewName);
                                }
                                break;
                            }
                        }
                        if (found) {
                            break;
                        }
                    }
                    if (!found) {
                        logger.warn("no view engine found for [{}]", ext);
                    }
                }
            }
        }
    }

    public ViewResult render(ViewContext context) {
        ViewEngineService viewEngineService = viewEngines.get(context.lang() == null ? defaultViewLang : context.lang());
        return new ViewResult(viewEngineService.contentType(), viewEngineService.render(context.view(), context.varsAsMap()));
    }
}
