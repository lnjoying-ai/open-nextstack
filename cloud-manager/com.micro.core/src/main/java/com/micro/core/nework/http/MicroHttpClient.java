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

import io.vertx.core.json.Json;
import org.apache.http.*;
import org.apache.http.client.methods.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MicroHttpClient
{
    private static Logger LOGGER = LogManager.getLogger();
    private static PoolingHttpClientConnectionManager connectionManager = null;
    private static HttpClientBuilder httpBulder = null;
    private static RequestConfig requestConfig = null;

    private static int MAXCONNECTION = 10;

    private static int DEFAULTMAXCONNECTION = 5;

    private  String IP ;
    private  int PORT;


    public MicroHttpClient(String host, int port)
    {
        //设置http的状态参数
        requestConfig = RequestConfig.custom()
                .setSocketTimeout(5000)
                .setConnectTimeout(5000)
                .setConnectionRequestTimeout(5000)
                .build();

        this.IP = host;
        this.PORT = port;
        HttpHost target = new HttpHost(IP, PORT);
        connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(MAXCONNECTION);
        connectionManager.setDefaultMaxPerRoute(DEFAULTMAXCONNECTION);
        connectionManager.setMaxPerRoute(new HttpRoute(target), 20);
        httpBulder = HttpClients.custom();
        httpBulder.setConnectionManager(connectionManager);
    }

    public static CloseableHttpClient getConnection()
    {
        CloseableHttpClient httpClient = httpBulder.build();
        httpClient = httpBulder.build();
        return httpClient;
    }


    public final String postMethodJson(String url, Map var2) throws Exception
    {
        HttpClient client = getConnection();
        HttpRequestBase clientMethod = createRequest("POST", url);

        String message = new String(Json.encode(var2).getBytes(),"utf-8");
        HttpEntity requestEntity = new StringEntity(message,"UTF-8");
        ((HttpEntityEnclosingRequestBase)clientMethod).setEntity(requestEntity);
        clientMethod.addHeader("Content-Type", "application/json; charset=UTF-8");
        clientMethod.addHeader("User-Agent", "Mozilla/4.0");
        String ss = clientMethod.toString();

        String rsp;
        try
        {
            HttpResponse response = client.execute(clientMethod);
            int status = response.getStatusLine().getStatusCode();
            if(status == 301 || status == 302)
            {
                Header  header;
                if((header = response.getFirstHeader("location")) != null) {
                    rsp = header.getValue();
                    LOGGER.info("The page was redirected to: {}", rsp);
                } else {
                    LOGGER.info("Location field value is null.");
                }
            }

            HttpEntity entity =  response.getEntity();
            if (null == entity)
            {
                return null;
            }
            rsp = EntityUtils.toString(entity, "utf-8");
        } catch (UnknownHostException var8) {
            LOGGER.error("UnknownHostException" + var8);
            throw var8;
        } catch (IOException var9) {
            LOGGER.error("IOException" + var9);
            throw var9;
        } catch (Exception var10) {
            LOGGER.error("Exception" + var10);
            throw var10;
        } finally {
            clientMethod.releaseConnection();
        }

        return rsp;
    }

    public final String getMethodJson(String url, List<Header> headers, Map var2) throws Exception
    {
        HttpClient client = getConnection();
        HttpRequestBase clientMethod = createRequest("GET", url);

        clientMethod.addHeader("Content-Type", "application/json; charset=UTF-8");
//        clientMethod.addHeader("User-Agent", "Mozilla/5.0");
        if (headers != null)
        {
            for (Header header:headers)
            {
                clientMethod.addHeader(header);
            }
        }
        String ss = clientMethod.toString();

        String rsp;
        try
        {
            HttpResponse response = client.execute(clientMethod);
            int status = response.getStatusLine().getStatusCode();
            if(status == 301 || status == 302)
            {
                Header  header;
                if((header = response.getFirstHeader("location")) != null) {
                    rsp = header.getValue();
                    LOGGER.info("The page was redirected to: {}", rsp);
                } else {
                    LOGGER.info("Location field value is null.");
                }
            }

            HttpEntity entity =  response.getEntity();
            if (null == entity)
            {
                return null;
            }
            rsp = EntityUtils.toString(entity, "utf-8");
            System.out.println(rsp);
        } catch (UnknownHostException var8) {
            LOGGER.error("UnknownHostException" + var8);
            throw var8;
        } catch (IOException var9) {
            LOGGER.error("IOException" + var9);
            throw var9;
        } catch (Exception var10) {
            LOGGER.error("Exception" + var10);
            throw var10;
        } finally {
            clientMethod.releaseConnection();
        }

        return rsp;
    }


    public HttpFwdRet fwd(String method, String url, List<Header> headers, InputStream stream)
    {
//        LOGGER.info("fwd message to url " + url);
//        HttpClient client = getConnection();
//        HttpRequestBase clientMethod = createRequest(method, url);
        clientMethod.setConfig(requestConfig);
        for(Header header:headers)
        {
            clientMethod.addHeader(header);
        }

        if (clientMethod instanceof HttpEntityEnclosingRequestBase)
        {
            HttpEntity requestEntity = new InputStreamEntity(stream);

            clientMethod.removeHeaders("Content-Length");
            Header content_type = clientMethod.getFirstHeader("content-type");
            clientMethod.removeHeaders("content-type");
            ((InputStreamEntity) requestEntity).setContentType(content_type);
            ((HttpEntityEnclosingRequestBase)clientMethod).setEntity(requestEntity);

        }

        try
        {
            HttpFwdRet fwdRet = new HttpFwdRet();
            HttpResponse response = client.execute(clientMethod);
            fwdRet.setStatus(response.getStatusLine().getStatusCode());
            fwdRet.setHeaders(response.getAllHeaders());
//            fwdRet.setStream(clientMethod.getResponseBody());
            if (response.getEntity().isChunked())
            {
                System.out.println("chunked");
            }

            if (response.getEntity().isStreaming())
            {
                System.out.println("streaming");
            }

            if (response.getEntity().isRepeatable())
            {
                System.out.println("repeatable");
            }
            fwdRet.setIStream(response.getEntity().getContent());

            return fwdRet;
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
//            clientMethod.releaseConnection();
        }
        return null;
    }

    public HttpFwdRet fwdReq(List<Header> headers)
    {
        for(Header header:headers)
        {
            clientMethod.addHeader(header);
        }

        if (clientMethod instanceof HttpEntityEnclosingRequestBase)
        {
            clientMethod.removeHeaders("Content-Length");
            Header content_type = clientMethod.getFirstHeader("content-type");
        }

        try
        {
            HttpFwdRet fwdRet = new HttpFwdRet();
            HttpResponse response = client.execute(clientMethod);
            fwdRet.setStatus(response.getStatusLine().getStatusCode());
            fwdRet.setHeaders(response.getAllHeaders());
            if (response.getEntity().isChunked())
            {
                System.out.println("chunked");
            }

            if (response.getEntity().isStreaming())
            {
                System.out.println("streaming");
            }

            if (response.getEntity().isRepeatable())
            {
                System.out.println("repeatable");
            }
            fwdRet.setIStream(response.getEntity().getContent());
            return fwdRet;
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    HttpRequestBase clientMethod = null;
    HttpClient client =null;

    public void createClient(String method, String url)
    {
        LOGGER.info("fwd message to url {}", url);
        client = getConnection();

        clientMethod = createRequest(method, url);
        clientMethod.setConfig(requestConfig);
    }

    public void destroyClient()
    {
        clientMethod.releaseConnection();
    }
    public HttpFwdRet fwdContent( List<Header> headers, InputStream stream)
    {

        for(Header header:headers)
        {
            clientMethod.addHeader(header);
        }

        if (clientMethod instanceof HttpEntityEnclosingRequestBase)
        {
            if (stream != null)
            {
                HttpEntity requestEntity = new InputStreamEntity(stream);

                clientMethod.removeHeaders("Content-Length");
                Header content_type = clientMethod.getFirstHeader("content-type");
                clientMethod.removeHeaders("content-type");
                ((InputStreamEntity) requestEntity).setContentType(content_type);
                ((HttpEntityEnclosingRequestBase)clientMethod).setEntity(requestEntity);
            }
        }

        try
        {
            HttpFwdRet fwdRet = new HttpFwdRet();
            HttpResponse response = client.execute(clientMethod);
            fwdRet.setStatus(response.getStatusLine().getStatusCode());
            fwdRet.setHeaders(response.getAllHeaders());
//            fwdRet.setStream(clientMethod.getResponseBody());
            if (response.getEntity().isChunked())
            {
                System.out.println("chunked");
            }

            if (response.getEntity().isStreaming())
            {
                System.out.println("streaming");
            }

            if (response.getEntity().isRepeatable())
            {
                System.out.println("repeatable");
            }
            fwdRet.setIStream(response.getEntity().getContent());
            return fwdRet;
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    HttpUriRequest createUriRequest(String method, String url)
    {
        if (method.equals("GET"))
        {
            return RequestBuilder.get().setUri(url).build();
        }

        if (method.equals("POST"))
        {
            return RequestBuilder.post().setUri(url).build();
        }

        if (method.equals("PUT"))
        {
            return RequestBuilder.put().setUri(url).build();
        }

        if (method.equals("DELETE"))
        {
            return RequestBuilder.delete().setUri(url).build();
        }

        if (method.equals("OPTION"))
        {
            return RequestBuilder.options().setUri(url).build();
        }

        if (method.equals("HEAD"))
        {
            return RequestBuilder.head().setUri(url).build();
        }
        return null;
    }

    HttpRequestBase createRequest(String method, String url)
    {
        if (method.equals("GET"))
        {
            return new HttpGet(url);
        }

        if (method.equals("POST"))
        {
            return new HttpPost(url);
        }

        if (method.equals("PUT"))
        {
            return new HttpPut(url);
        }

        if (method.equals("DELETE"))
        {
            return new HttpDelete(url);
        }

        if (method.equals("OPTIONS"))
        {
            return new HttpOptions(url);
        }

        if (method.equals("HEAD"))
        {
            return new HttpHead(url);
        }
        return null;
    }

    public static String getRandomIp() {

        // ip范围
        int[][] range = { { 607649792, 608174079 }, // 36.56.0.0-36.63.255.255
                { 1038614528, 1039007743 }, // 61.232.0.0-61.237.255.255
                { 1783627776, 1784676351 }, // 106.80.0.0-106.95.255.255
                { 2035023872, 2035154943 }, // 121.76.0.0-121.77.255.255
                { 2078801920, 2079064063 }, // 123.232.0.0-123.235.255.255
                { -1950089216, -1948778497 }, // 139.196.0.0-139.215.255.255
                { -1425539072, -1425014785 }, // 171.8.0.0-171.15.255.255
                { -1236271104, -1235419137 }, // 182.80.0.0-182.92.255.255
                { -770113536, -768606209 }, // 210.25.0.0-210.47.255.255
                { -569376768, -564133889 }, // 222.16.0.0-222.95.255.255
        };

        Random rdint = new Random();
        int index = rdint.nextInt(10);
        String ip = num2ip(range[index][0] + new Random().nextInt(range[index][1] - range[index][0]));
        return ip;
    }

    /*
     * 将十进制转换成IP地址
     */
    public static String num2ip(int ip) {
        int[] b = new int[4];
        String x = "";
        b[0] = (int) ((ip >> 24) & 0xff);
        b[1] = (int) ((ip >> 16) & 0xff);
        b[2] = (int) ((ip >> 8) & 0xff);
        b[3] = (int) (ip & 0xff);
        x = Integer.toString(b[0]) + "." + Integer.toString(b[1]) + "." + Integer.toString(b[2]) + "." + Integer.toString(b[3]);

        return x;
    }


    public static void main(String[] args) throws Exception
    {
        MicroHttpClient microHttpClient = new MicroHttpClient("127.0.0.1", 8080);
        List<String> agents = new ArrayList<>();

        agents.add("Mozilla/5.0 (iphone x Build/MXB48T; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/53.0.2785.49  Mobile MQQBrowser/6.2 TBS/043632 Safari/537.36 MicroMessenger/6.6.1.1220(0x26060135) NetType/WIFI Language/zh_CN");
        agents.add("Mozilla/5.0 (Linux; andriod 7.1.1; MI 6 Build/NMF26X; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0   Chrome/57.0.2987.132 MQQBrowser/6.2 TBS/043807 Mobile Safari/537.36 MicroMessenger/6.6.1.1220(0x26060135) NetType/4G Language/zh_CN\n");
        for (int i=1;i<738900;i++)
        {
//            Integer ttt = Utils.getRandomByRange(0, 500);
            List<Header> headers = new ArrayList<>();
            String ip = getRandomIp();
            String agent = agents.get(i % 2);
            Header headerip = new BasicHeader("x-forwarded-for", ip);
            Header headeragent = new BasicHeader("UserAgent", agent);
            headers.add(headerip);
            headers.add(headeragent);
//            System.out.println(ttt);
            microHttpClient.getMethodJson("http://", headers, null);
        }
    }
}
