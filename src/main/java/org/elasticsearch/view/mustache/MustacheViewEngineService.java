package org.elasticsearch.view.mustache;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.search.lookup.SearchLookup;
import org.elasticsearch.view.ViewEngineService;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

public class MustacheViewEngineService extends AbstractComponent implements ViewEngineService {

    @Override
    public Object render(String view, @Nullable Map<String, Object> vars) {
        Writer writer = new StringWriter();
        MustacheFactory mf = new DefaultMustacheFactory();
        Mustache mustache = mf.compile(new StringReader(view), "view.mustache");
        mustache.execute(writer, vars);
        return writer.toString();
    }

    @Inject
	public MustacheViewEngineService(Settings settings) {
		super(settings);
	}

	public String[] types() {
		return new String[] { "mustache" };
	}

}
