package org.elasticsearch.action.view;

import org.elasticsearch.action.ActionResponse;

public class ViewResponse extends ActionResponse {

    public byte[] content;
    public String contentType = "";

    ViewResponse() {
    }

    public ViewResponse(String contentType, byte[] content) {
        this.content = content;
        this.contentType = contentType;
    }

    public byte[] content() {
        return this.content;
    }

    public String contentType() {
        return this.contentType;
    }
}
