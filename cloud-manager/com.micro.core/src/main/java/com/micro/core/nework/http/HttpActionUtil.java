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

package com.micro.core.nework.http;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.concurrent.TimeUnit;

public class HttpActionUtil
{
    private static final RestTemplate restTemplate;

    private static final ClientHttpRequestFactory requestFactory;

    public static PoolingHttpClientConnectionManager connectionManager;
    static {
        connectionManager = new PoolingHttpClientConnectionManager(getRegistry());
        requestFactory =  new HttpComponentsClientHttpRequestFactory(httpClient(connectionManager));
        restTemplate = new RestTemplate(requestFactory);
    }

    private static  Registry<ConnectionSocketFactory> getRegistry()
    {
        return RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", SSLConnectionSocketFactory.getSocketFactory())
                .build();
    }


    private static HttpClient httpClient( PoolingHttpClientConnectionManager connectionManager)
    {
//        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(registry);
        connectionManager.setMaxTotal(100);
        connectionManager.closeExpiredConnections();
        //MaxPerRoute是对maxtotal的细分，每个主机的并发最大是50，route是指域名
        connectionManager.setDefaultMaxPerRoute(50);
        RequestConfig requestConfig = RequestConfig.custom()
                //返回数据的超时时间
                .setSocketTimeout(3000)
                //连接上服务器的超时时间
                .setConnectTimeout(2000)
                //从连接池中获取连接的超时时间
                .setConnectionRequestTimeout(1000)
                .build();
        return  HttpClientBuilder.create().setDefaultRequestConfig(requestConfig)
                .setConnectionManager(connectionManager)
                .build();
    }


    private static SimpleClientHttpRequestFactory setRequestFactory()
    {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(3000);
        requestFactory.setReadTimeout(3000);
        return requestFactory;
    }



    public static String get(String url) {
        restTemplate.setErrorHandler(new CustomErrorHandler());
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        connectionManager.closeExpiredConnections();
        connectionManager.closeIdleConnections(60, TimeUnit.SECONDS);
        return response.getBody();
    }

    public static <T> T get(String url, Class<T> responseType) {
        restTemplate.setErrorHandler(new CustomErrorHandler());
        ResponseEntity<T> response = restTemplate.getForEntity(url, responseType);
        connectionManager.closeExpiredConnections();
        connectionManager.closeIdleConnections(60, TimeUnit.SECONDS);
        return response.getBody();
    }

    public static String post(String url, String json){
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> requestEntity = new HttpEntity<>(json, requestHeaders);
        restTemplate.setErrorHandler(new CustomErrorHandler());
        ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);
        connectionManager.closeExpiredConnections();
        connectionManager.closeIdleConnections(60, TimeUnit.SECONDS);
        return response.getBody();
    }

    public static <T> T post(String url, String json, Class<T> responseType){
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> requestEntity = new HttpEntity<>(json, requestHeaders);
        restTemplate.setErrorHandler(new CustomErrorHandler());
        ResponseEntity<T> response = restTemplate.postForEntity(url, requestEntity, responseType);
        connectionManager.closeExpiredConnections();
        connectionManager.closeIdleConnections(60, TimeUnit.SECONDS);
        return response.getBody();
    }

    public static String put(String url, String json)
    {
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> requestEntity = new HttpEntity<>(json, requestHeaders);
        restTemplate.setErrorHandler(new CustomErrorHandler());
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT,requestEntity,String.class);
        connectionManager.closeExpiredConnections();
        connectionManager.closeIdleConnections(60, TimeUnit.SECONDS);
        return response.getBody();
    }

    public static <T> T  put(String url, String json, Class<T> responseType)
    {
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> requestEntity = new HttpEntity<>(json, requestHeaders);
        restTemplate.setErrorHandler(new CustomErrorHandler());
        ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.PUT,requestEntity,responseType);
        connectionManager.closeExpiredConnections();
        connectionManager.closeIdleConnections(60, TimeUnit.SECONDS);
        return response.getBody();
    }

    public static String delete(String url){
        restTemplate.setErrorHandler(new CustomErrorHandler());
        ResponseEntity<String> response = restTemplate.exchange(url,HttpMethod.DELETE, null,String.class);
        connectionManager.closeExpiredConnections();
        connectionManager.closeIdleConnections(60, TimeUnit.SECONDS);
        return response.getBody();
    }

    public static <T> T delete(String url, Class<T> responseType){
        restTemplate.setErrorHandler(new CustomErrorHandler());
        ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.DELETE, null, responseType);
        connectionManager.closeExpiredConnections();
        connectionManager.closeIdleConnections(60, TimeUnit.SECONDS);
        return response.getBody();
    }

    public static <T> T getObject(String url,  Class<T> responseType)
    {
        restTemplate.setErrorHandler(new CustomErrorHandler());
        connectionManager.closeExpiredConnections();
        connectionManager.closeIdleConnections(60, TimeUnit.SECONDS);
        return restTemplate.getForObject(url, responseType);
    }

    public static <T> T getObject(String url,  Class<T> responseType, Object... objects)
    {
        restTemplate.setErrorHandler(new CustomErrorHandler());

        T result = restTemplate.getForObject(url, responseType, objects);
        connectionManager.closeExpiredConnections();
        connectionManager.closeIdleConnections(60, TimeUnit.SECONDS);

        return result;
    }

    public static <T> T getObject(URI url,  Class<T> responseType)
    {
        restTemplate.setErrorHandler(new CustomErrorHandler());
        T result = restTemplate.getForObject(url, responseType);
        connectionManager.closeExpiredConnections();
        connectionManager.closeIdleConnections(60, TimeUnit.SECONDS);
        return result;
    }

    public static  <T>ResponseEntity<T> postForEntity(String url, Object request,
                                                Class<T> responseType, Object... uriVariables)
    {
        restTemplate.setErrorHandler(new CustomErrorHandler());
        HttpHeaders headers = new HttpHeaders();
        MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");
        headers.setContentType(type);
        HttpEntity<Object> requestEntity = new HttpEntity<>(request, headers);
        ResponseEntity<T> result = restTemplate.postForEntity(url, requestEntity, responseType, uriVariables);
        connectionManager.closeExpiredConnections();
        connectionManager.closeIdleConnections(60, TimeUnit.SECONDS);
        return result;
    }

 }
