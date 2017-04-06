/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerinalang.services.dispatchers.http;

import org.ballerinalang.bre.Context;
import org.ballerinalang.model.Resource;
import org.ballerinalang.model.Service;
import org.ballerinalang.services.dispatchers.ResourceDispatcher;
import org.ballerinalang.services.dispatchers.uri.QueryParamProcessor;
import org.ballerinalang.util.exceptions.BallerinaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.messaging.CarbonCallback;
import org.wso2.carbon.messaging.CarbonMessage;

import java.util.HashMap;
import java.util.Map;

/**
 * Resource level dispatchers handler for HTTP protocol.
 */
public class HTTPResourceDispatcher implements ResourceDispatcher {

    private static final Logger log = LoggerFactory.getLogger(HTTPResourceDispatcher.class);

    @Override
    public Resource findResource(Service service, CarbonMessage cMsg, CarbonCallback callback, Context balContext)
            throws BallerinaException {

        String method = (String) cMsg.getProperty(Constants.HTTP_METHOD);
        String subPath = (String) cMsg.getProperty(Constants.SUB_PATH);
        subPath = subPath.endsWith("/") ? subPath.substring(0, subPath.length() - 1) : subPath;

        try {
            Map<String, String> resourceArgumentValues = new HashMap<>();

            String rawQueryStr = cMsg.getProperty(Constants.RAW_QUERY_STR) != null
                    ? "?" + cMsg.getProperty(Constants.RAW_QUERY_STR)
                    : "";

            Resource resource = service.getUriTemplate().matches((subPath + rawQueryStr), resourceArgumentValues);
            if (resource != null
                    && (resource.getAnnotation(Constants.PROTOCOL_HTTP, method) != null)) {

                if (cMsg.getProperty(Constants.QUERY_STR) != null) {
                    QueryParamProcessor.processQueryParams
                            ((String) cMsg.getProperty(Constants.QUERY_STR))
                            .forEach((resourceArgumentValues::put));
                }
                cMsg.setProperty(org.ballerinalang.runtime.Constants.RESOURCE_ARGS, resourceArgumentValues);
                return resource;
            }
        } catch (Throwable e) {
            throw new BallerinaException(e.getMessage(), balContext);
        }

        // Throw an exception if the resource is not found.
        throw new BallerinaException("no matching resource found for Path : " + subPath + " , Method : " + method);
    }

    @Override
    public String getProtocol() {
        return Constants.PROTOCOL_HTTP;
    }
}
