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
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.action.ValidateActions;
import org.elasticsearch.action.support.single.shard.SingleShardOperationRequest;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;


public class ViewRequest extends SingleShardOperationRequest<ViewRequest> {

    private String type;
    private String id;
    private String format;
    public static final String DEFAULT_VIEW = "default";

    ViewRequest() {
      this.type = "_all";
    }

    /**
    * Constructs a new view request against the specified index. The {@link #type(String)} and {@link #id(String)}
    * must be set.
    */
    public ViewRequest(String index) {
        super(index);
        this.type = "_all";
    }

    /*
    * Copy constructor that creates a new view request that is a copy of the one provided as an argument. 
    * The new request will inherit though headers and context from the original request that caused it. 
    */
     public   ViewRequest(ViewRequest viewRequest, ActionRequest originalRequest) {

         super(originalRequest);
         this.index = viewRequest.index;
         this.type = viewRequest.type;
         this.id = viewRequest.id;
     }
     
    /**
    * Constructs a new view request starting from the provided request, meaning that it will
    * inherit its headers and context, and against the specified index.
     */
    public ViewRequest(ActionRequest request, String index) {
        super(request, index);
    }

    /**
     * Constructs a new view request against the specified index with the type and id.
     *
     * @param index The index to view the document from
     * @param type  The type of the document
     * @param id    The id of the document
     */
    public ViewRequest(String index, String type, String id) {
        super(index);
        this.type = type;
        this.id = id;
    }

    /**
     * Sets the type of the document to fetch.
     */
    public ViewRequest type(@Nullable String type) {
        if (type == null) {
            type = "_all";
        }
        this.type = type;
        return this;
    }

    /**
     * Sets the id of the document to fetch.
     */
    public ViewRequest id(String id) {
        this.id = id;
        return this;
    }

    /**
     * Sets the format of the document
     */
    public ViewRequest format(String format) {
        this.format = format;
        return this;
    }

    public String type() {
        return type;
    }

    public String id() {
        return id;
    }

    public String format() {
        return format;
    }
    @Override
    public ActionRequestValidationException validate() {
        ActionRequestValidationException validationException = super.validate();
        if (type == null) {
            validationException = ValidateActions.addValidationError("type is missing", validationException);
        }
        if (id == null) {
            validationException = ValidateActions.addValidationError("id is missing", validationException);
        }
        return validationException;
    }
    
    @Override
    public void   readFrom(StreamInput in) throws IOException 
    {
        super.readFrom(in);
        type = in.readSharedString();
        id = in.readString();
    }
    
    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeSharedString(type);
        out.writeString(id);
    }
    
    @Override
    public String toString() {
        return "view [" + index + "][" + type + "][" + id + "]";
    }
   
}
