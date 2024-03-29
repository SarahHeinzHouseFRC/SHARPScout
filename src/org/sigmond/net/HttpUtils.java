package org.sigmond.net;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HttpUtils
{

    CookieStore cookies;

    public HttpUtils()
    {
        cookies = new BasicCookieStore();
    }

    public void doGet(String url, HttpCallback callback)
    {
        HttpGet get = new HttpGet(url);
        HttpRequestInfo rinfo = new HttpRequestInfo(get, callback);
        rinfo.setCookieStore(cookies);
        AsyncHttpTask task = new AsyncHttpTask();
        task.execute(rinfo);
    }

    public void doPost(String url, Map<String, String> params,
                       HttpCallback callback)
    {
        try
        {

            HttpPost post = new HttpPost(url);

            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(params.size());

            for(String key : params.keySet())
            {
                nameValuePairs.add(new BasicNameValuePair(key, params.get(key)));
            }

            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(nameValuePairs);
            post.setEntity(entity);

            HttpRequestInfo rinfo = new HttpRequestInfo(post, callback);
            rinfo.setParams(params);
            rinfo.setCookieStore(cookies);
            AsyncHttpTask task = new AsyncHttpTask();
            task.execute(rinfo);
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public static String responseToString(HttpResponse response) throws IOException
    {
        InputStream in = response.getEntity().getContent();
        InputStreamReader ir = new InputStreamReader(in);
        BufferedReader bin = new BufferedReader(ir);
        String line = null;
        StringBuffer buff = new StringBuffer();
        while((line = bin.readLine()) != null)
        {
            buff.append(line + "\n");
        }
        bin.close();
        return buff.toString();
    }

    public void setCookieStore(CookieStore store)
    {
        cookies = store;
    }
}
