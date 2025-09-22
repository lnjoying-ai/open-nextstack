// Copyright 2024 The NEXTSTACK Authors.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.lnjoying.justice.usermanager.utils;

import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

@Component
@Slf4j
public class HttpsUtil {

//    private static final Logger log = LoggerFactory.getLogger(HttpsUtil.class);

    private boolean getResponse(HttpRequestBase httpRequest) {

        boolean res = false;
        CloseableHttpClient client = null;

        try {
            log.info("Begin sms connect");
            client = HttpClients.custom().setSSLContext(
                new SSLContextBuilder().loadTrustMaterial(null, (x509CertChain, authType) -> true).build())
                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE).build();
            try (CloseableHttpResponse response = client.execute(httpRequest)) {
                int respCode = response.getStatusLine().getStatusCode();
                if (HttpResponseStatus.OK.code() == respCode) {
                    res = true;
                } else {
                    log.error("Send sms fail. respCode={}", respCode);
                }
            }
        } catch (IOException e) {
            log.error("https io exception: {}", e.getMessage());
        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
            log.error("SSL Context exception: {}", e.getMessage());
        } finally {
            if (client != null) {
                try {
                    client.close();
                } catch (IOException e) {
                    log.error("Client close exception");
                }
            }
        }

        return res;
    }

    /**
     * https post check.
     */
    public boolean httpsPost(String url, Map<String, String> headers, String bodyParam) {
        HttpPost post = new HttpPost(url);
        if (bodyParam != null) {
            HttpEntity entity = new StringEntity(bodyParam, "UTF-8");
            post.setEntity(entity);
        }
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                post.addHeader(entry.getKey(), entry.getValue());
            }
        }
        return getResponse(post);
    }

}
