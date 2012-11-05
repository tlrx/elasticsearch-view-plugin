package org.elasticsearch.view.mustache;

import com.github.mustachejava.*;
import com.github.mustachejava.util.GuardException;
import com.github.mustachejava.util.Wrapper;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.concurrent.ConcurrentCollections;
import org.elasticsearch.script.CompiledScript;
import org.elasticsearch.view.ViewEngineService;

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

public class MustacheViewEngineService extends AbstractComponent implements ViewEngineService {

    private final DefaultMustacheFactory factory;

    private final ConcurrentMap<String, String> staticCache = ConcurrentCollections.newConcurrentMap();

    @Override
    public Object render(String view, @Nullable Map<String, Object> vars) {
        Writer writer = new StringWriter();
        Mustache mustache = factory.compile(new StringReader(view), "render");
        mustache.execute(writer, vars);
        return writer.toString();
    }

    @Inject
    public MustacheViewEngineService(Settings settings) {
        super(settings);
        factory = new DefaultMustacheFactory() {
            @Override
            public Reader getReader(String resourceName) {
                if(staticCache.containsKey(resourceName)){
                 return new StringReader(staticCache.get(resourceName));
                }
                return super.getReader(resourceName);
            }
        };
        factory.setObjectHandler(new DocSourceObjectHandler());
    }

    public String[] types() {
        return new String[]{"mustache"};
    }

    @Override
    public String[] extensions() {
        return new String[]{"mustache"};
    }

    @Override
    public void load(String name, String view) {
        staticCache.put(name, view);
    }

    protected class DocSourceObjectHandler implements ObjectHandler {

        @Override
        public Wrapper find(final String name, final Object[] scopes) {
            return new Wrapper() {

                @Override
                public Object call(Object[] scopes) throws GuardException {
                    for (int i = scopes.length - 1; i >= 0; i--) {
                        Object scope = scopes[i];
                        if (scope != null) {
                            int indexDot = name.indexOf(".");
                            if (indexDot == -1) {
                                if (scope instanceof Map) {
                                    Map map = (Map) scope;
                                    if (map.containsKey(name)) {
                                        return map.get(name);
                                    } else {
                                        int indexQuestionMark = name.indexOf("?");
                                        if (indexQuestionMark == (name.length() - 1)) {
                                            Object[] subscope = {scope};
                                            return find(name.substring(0, indexQuestionMark), subscope).call(subscope);
                                        }
                                    }
                                }
                            } else {
                                // Dig into the dot-notation through recursion
                                Object[] subscope = {scope};
                                Wrapper wrapper = find(name.substring(0, indexDot), subscope);
                                if (wrapper != null) {
                                    scope = wrapper.call(subscope);
                                    subscope = new Object[]{scope};
                                    return find(name.substring(indexDot + 1), new Object[]{subscope}).call(subscope);
                                }
                            }
                        }
                    }
                    return null;
                }
            };
        }

        @Override
        public Object coerce(Object object) {
            return object;
        }

        @Override
        public Writer iterate(Iteration iteration, Writer writer, Object object, Object[] scopes) {
            if (object == null) return writer;
            if (object instanceof Boolean) {
                if (!(Boolean) object) {
                    return writer;
                }
            }
            if (object instanceof String) {
                if (object.toString().equals("")) {
                    return writer;
                }
            }
            if (object instanceof Iterable) {
                for (Object next : ((Iterable) object)) {
                    writer = iteration.next(writer, coerce(next), scopes);
                }
            } else if (object instanceof Iterator) {
                Iterator iterator = (Iterator) object;
                while (iterator.hasNext()) {
                    writer = iteration.next(writer, coerce(iterator.next()), scopes);
                }
            } else if (object instanceof Object[]) {
                Object[] array = (Object[]) object;
                for (Object o : array) {
                    writer = iteration.next(writer, coerce(o), scopes);
                }
            } else {
                writer = iteration.next(writer, object, scopes);
            }
            return writer;
        }

        @Override
        public Writer falsey(Iteration iteration, Writer writer, Object object, Object[] scopes) {
            return writer;
        }

        @Override
        public Binding createBinding(final String name, TemplateContext tc, Code code) {
            return new Binding() {
                // We find the wrapper just once since only the name is needed
                private Wrapper wrapper = find(name, null);

                @Override
                public Object get(Object[] scopes) {
                    return wrapper.call(scopes);
                }
            };
        }

        @Override
        public String stringify(Object object) {
            return object.toString();
        }
    }
}
