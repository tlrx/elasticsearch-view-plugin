package org.elasticsearch.view;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.view.mustache.MustacheViewEngineService;

import java.util.Map;
import java.util.Set;

public class ViewService extends AbstractComponent {

	private final String defaultViewLang;

	private final ImmutableMap<String, ViewEngineService> viewEngines;

	public ViewService(Settings settings) {
		this(settings, new Environment(), ImmutableSet.<ViewEngineService> builder()
				.add(new MustacheViewEngineService(settings))
				.build());
	}

	@Inject
	public ViewService(Settings settings, Environment environment, Set<ViewEngineService> viewEngines) {
		super(settings);

		this.defaultViewLang = componentSettings.get("default_view_lang", "mustache");

		ImmutableMap.Builder<String, ViewEngineService> builder = ImmutableMap.builder();
		for (ViewEngineService viewEngine : viewEngines) {
			for (String type : viewEngine.types()) {
				builder.put(type, viewEngine);
			}
		}
		this.viewEngines = builder.build();
	}

	public Object render(String view, @Nullable Map<String, Object> vars) {
		return render(defaultViewLang, view, vars);
	}

	public Object render(String lang, String view, @Nullable Map<String, Object> vars) {
        return viewEngines.get(lang).render(view, vars);
	}
}
