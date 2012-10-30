package org.elasticsearch.action.view;

import org.elasticsearch.action.ActionResponse;

public class ViewResponse extends ActionResponse {

	public String content = "";

	ViewResponse() {
	}

	public ViewResponse(String content) {
		this.content = content;
	}

	public String content() {
		return this.content;
	}

}
