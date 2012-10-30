package org.elasticsearch.view;

import org.elasticsearch.common.Nullable;

import java.util.Map;

public interface ViewEngineService {

	String[] types();
	
    Object render(String view, @Nullable Map<String, Object>vars);
}
