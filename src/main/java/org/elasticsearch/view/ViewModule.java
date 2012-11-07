package org.elasticsearch.view;

import java.util.List;

import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.multibindings.Multibinder;
import org.elasticsearch.view.binary.BinaryViewEngineService;
import org.elasticsearch.view.mustache.MustacheViewEngineService;

import com.google.common.collect.Lists;

public class ViewModule extends AbstractModule {

	private final List<Class<? extends ViewEngineService>> viewEngines = Lists.newArrayList();

	public void addViewsEngine(Class<? extends ViewEngineService> viewEngine) {
		viewEngines.add(viewEngine);
	}

	@Override
	protected void configure() {
		Multibinder<ViewEngineService> multibinder = Multibinder.newSetBinder(binder(), ViewEngineService.class);
		multibinder.addBinding().to(MustacheViewEngineService.class);
        multibinder.addBinding().to(BinaryViewEngineService.class);
		for (Class<? extends ViewEngineService> viewEngine : viewEngines) {
			multibinder.addBinding().to(viewEngine);
		}

		bind(ViewService.class).asEagerSingleton();
	}
}
