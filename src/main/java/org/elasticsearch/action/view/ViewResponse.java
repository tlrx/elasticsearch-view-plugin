package org.elasticsearch.action.view;

import org.elasticsearch.action.ActionResponse;

public class ViewResponse extends ActionResponse {

    public String content = "";
    public String contentType = "";

    ViewResponse() {
    }

    public ViewResponse(String content, String contentType) {
        this.content = content;
        this.contentType = contentType;
    }

    public String content() {
        return this.content;
    }

    public String contentType() {
        return this.contentType;
    }
}
