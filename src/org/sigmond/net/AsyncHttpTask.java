package org.sigmond.net;


import android.os.AsyncTask;
import android.util.Log;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BrowserCompatSpec;
import org.apache.http.impl.cookie.CookieSpecBase;

import java.net.ConnectException;
import java.net.URI;
import java.util.List;

/**
 * Asynchronous Http request task. Performs a request and processes response in a separate thread.
 * Stores response in an HttpRequestInfo object.
 */
public class AsyncHttpTask extends AsyncTask<HttpRequestInfo, Integer, HttpRequestInfo>
{
    DefaultHttpClient client;

    public AsyncHttpTask()
    {
        super();
        client = new DefaultHttpClient();
    }

    protected HttpRequestInfo doInBackground(HttpRequestInfo... params)
    {
        HttpRequestInfo rinfo = params[0];
        try
        {
            client.setCookieStore(rinfo.getCookieStore()); //cookie handling
            HttpResponse resp = client.execute(rinfo.getRequest()); //execute request

            //store any new cookies recieved
            CookieStore store = rinfo.getCookieStore();
            Header[] allHeaders = resp.getAllHeaders();
            CookieSpecBase base = new BrowserCompatSpec();
            URI uri = rinfo.getRequest().getURI();
            int port = uri.getPort();
            if(port <= 0)
            {
                port = 80;
            }
            CookieOrigin origin = new CookieOrigin(uri.getHost(), port, uri.getPath(), false);
            for(Header header : allHeaders)
            {
                List<Cookie> parse = base.parse(header, origin);
                for(Cookie cookie : parse)
                {
                    if(cookie.getValue() != null && cookie.getValue() != "")
                    {
                        store.addCookie(cookie);
                    }
                }
            }
            rinfo.setCookieStore(store);
            //store the response
            rinfo.setResponse(resp);
            //process the response string. This is required in newer Android versions.
            //newer versions will not allow reading from a network response input stream in the main thread.
            rinfo.setResponseString(HttpUtils.responseToString(resp));
        }
        catch(ConnectException e)
        {
            if(e.getMessage().contains("ETIMEDOUT"))
            {
                Log.i("AsyncHttpTask", "Connection Timed Out");
            }
        }
        catch(Exception e)
        {
            rinfo.setException(e);
        }
        return rinfo;
    }

    protected void onPostExecute(HttpRequestInfo rinfo)
    {
        super.onPostExecute(rinfo);
        rinfo.requestFinished(); //perform callback
    }


}
