package com.unclezs.Utils;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.StandardHttpRequestRetryHandler;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HtmlUtil {
    /**
     * 相对路径转绝对路径
     *
     * @param base 根路径
     * @param url  相对路径
     * @return 绝对地址
     */
    public static String getAbsUrl(String base, String url) {
        if (!base.endsWith("/")) {
            base = base + "/";
        }
        String absurl = "";
        try {
            URL fromurl = new URL(base);
            absurl = new URL(fromurl, url).toString();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return absurl;
    }

    /**
     * 获取网页编码
     *
     * @param html 解码的网页源码（正则匹配<meta>标签的编码）
     * @return 编码格式
     */
    public static String getEncode(String html) {
        String code = "gbk";
        try {
            Pattern r = Pattern.compile("charset=[\"\']{0,1}([\\w\\-]{2,8}?)[\"\']");
            Matcher m = r.matcher(html);
            while (m.find()) {
                if (m.group(1).length() > 2) {
                    code = m.group(1);
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return code.replace("\"", "").trim();
    }

    public static String getHtmlSource(String url, String charset, String cookies, String ua) {
        StringBuffer content = new StringBuffer();
        try {
            ProxyUtil.proxyConnection();
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            if (cookies != null && !"".equals(cookies))
                connection.addRequestProperty("Cookie", cookies);
            if (ua != null && !"".equals(ua)) {
                connection.addRequestProperty("User-Agent", ua);
            } else {
                connection.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/69.0.3497.100 Safari/537.36");
            }
            connection.setReadTimeout(10000);
            connection.setConnectTimeout(10000);
            connection.setInstanceFollowRedirects(true);
            BufferedReader br=new BufferedReader(new InputStreamReader(connection.getInputStream(),charset));
            String s;
            while ((s=br.readLine())!=null){
                content.append(s);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("源码抓取失败 " + url);
        }
        return content.toString();

    }

    public static String getHtml(String url, String charset, String cookies, String ua) {
        String content = null;
        //重试5次
        HttpRequestRetryHandler retry = new StandardHttpRequestRetryHandler(5, true);
        //请求头
        List<Header> headers = new ArrayList<>();
        if (cookies != null && !"".equals(cookies))
            headers.add(new BasicHeader("Cookie", cookies));
        if (ua != null && !"".equals(ua)) {
            headers.add(new BasicHeader("User-Agent", ua));
        } else {
            headers.add(new BasicHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/69.0.3497.100 Safari/537.36"));
        }
        //延迟
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(10000)
                .setConnectionRequestTimeout(10000)
                .setSocketTimeout(10000)
                .build();
//        SystemDefaultRoutePlanner routePlanner = new SystemDefaultRoutePlanner(ProxySelector.getDefault());
        HttpClientBuilder builder = HttpClients.custom()
//                .setRoutePlanner(routePlanner)
                .setDefaultHeaders(headers)
                .setDefaultRequestConfig(config)
                .setRetryHandler(retry);
        ProxyUtil.proxyHttpClient(builder);
        try (CloseableHttpClient httpClient = builder.build()) {
            HttpGet get = new HttpGet(url);
            HttpEntity entity = httpClient.execute(get).getEntity();
            byte[] bytes = EntityUtils.toByteArray(entity);
            if (charset.contains("gb")) {
                charset = "gbk";
            }
            content = new String(bytes, charset);
        } catch (Exception e) {
            System.out.println("源码抓取失败 " + url);
        }
        return content.trim();

    }

    /**
     * 获取静态网页源码
     *
     * @param url     网页url地址
     * @param charset 网页编码
     * @return 网页源码
     */
    public static String getHtml(String url, String charset) {
        String ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/69.0.3497.100 Safari/537.36";
        return getHtml(url, charset, null, ua);
    }

    /**
     * 获取静态网页源码
     *
     * @param url     网页url地址
     * @param charset 网页编码
     * @return 网页源码
     */
    public static String getSource(String url, String charset, String referer, String ua) {
        if (ua == null) {
            ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/69.0.3497.100 Safari/537.36";
        }
        String content = null;
        //重试5次
        HttpRequestRetryHandler retry = new StandardHttpRequestRetryHandler(5, true);
        //请求头
        List<Header> headers = new ArrayList<>();
        headers.add(new BasicHeader("User-Agent", ua));
        headers.add(new BasicHeader("Referer", referer));
        //延迟
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(10000)
                .setSocketTimeout(10000)
                .setConnectionRequestTimeout(10000)
                .build();
        HttpClientBuilder builder = HttpClientBuilder.create()
                .setDefaultHeaders(headers)
                .setRetryHandler(retry)
                .setDefaultRequestConfig(config);
        ProxyUtil.proxyHttpClient(builder);
        try (CloseableHttpClient httpClient = builder.build()) {
            HttpGet get = new HttpGet(url);
            HttpEntity entity = httpClient.execute(get).getEntity();
            return EntityUtils.toString(entity, charset);
        } catch (Exception e) {
            System.out.println("源码抓取失败 " + url);
        }
        return "";
    }


}
