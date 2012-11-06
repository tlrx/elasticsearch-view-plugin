package org.elasticsearch.view;

public class ViewResult {

    private String contentType;

    private byte[] content;

    public ViewResult(String contentType, byte[] content) {
        this.contentType = contentType;
        this.content = content;
    }

    public byte[] content() {
        return content;
    }

    public String contentType() {
        return contentType;
    }

    public ViewResult content(byte[] content) {
        this.content = content;
        return this;
    }

    public ViewResult contentType(String contentType) {
        this.contentType = contentType;
        return this;
    }
}
