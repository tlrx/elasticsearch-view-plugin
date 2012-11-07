package org.elasticsearch.view.binary;

import org.elasticsearch.common.Base64;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.view.ViewEngineService;

import java.io.IOException;
import java.util.Map;

public class BinaryViewEngineService extends AbstractComponent implements ViewEngineService {

    @Inject
    public BinaryViewEngineService(Settings settings) {
        super(settings);
    }

    @Override
    public String[] types() {
        return new String[]{"binary"};
    }

    @Override
    public String[] extensions() {
        return new String[]{};
    }

    @Override
    public String contentType() {
        return "application/octet-stream";
    }

    @Override
    public void load(String name, String view) {
        // Nothing to load
    }

    @Override
    public byte[] render(String view, @Nullable Map<String, Object> vars) {
        byte[] result = new byte[0];
        if (vars != null) {
            Object binary = getBinaryField(view, vars);
            if (binary != null) {
                if (binary instanceof String) {
                    try {
                        result = Base64.decode((String) binary);
                    } catch (IOException e) {
                        //todo gérer exception
                    }
                }
            }
        }
        return result;
    }

    private Object getBinaryField(String name, Object scope) {
        int indexDot = name.indexOf(".");
        if (indexDot == -1) {
            if (scope instanceof Map) {
                Object binary = ((Map) scope).get(name);
                return binary;
            }
        } else {
            String accessor = name.substring(0, indexDot);
            if (scope instanceof Map) {
                scope = ((Map) scope).get(accessor);
                return getBinaryField(name.substring(indexDot + 1), scope);
            }
        }
        return null;
    }
}
