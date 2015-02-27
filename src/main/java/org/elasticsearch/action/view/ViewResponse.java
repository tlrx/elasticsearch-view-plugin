/*
 * Licensed to Elastic Search and Shay Banon under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Elastic Search licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.elasticsearch.action.view;

import java.io.IOException;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.index.get.GetResult;

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
    
    
    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        contentType = in.readString();
        int contentlength = in.readInt();
        content = new byte[contentlength];
        in.readFully(content);
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeString(contentType);
        out.writeInt(content.length);
        out.writeBytes(content);
    }
}
