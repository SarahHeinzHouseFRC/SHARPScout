package org.sigmond.net;

import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpUriRequest;

import java.util.Map;

public class HttpRequestInfo
{
    private HttpUriRequest request_;
    private HttpCallback callback_;
    private Exception exception_;
    private HttpResponse response_;
    private CookieStore cookieStore;
    private String response;
    private Map<String, String> params;

    public HttpRequestInfo(HttpUriRequest request, HttpCallback callback)
    {
        super();
        request_ = request;
        callback_ = callback;
    }

    public HttpUriRequest getRequest()
    {
        return request_;
    }

    public void setRequest(HttpUriRequest request)
    {
        request_ = request;
    }

    public HttpCallback getCallback()
    {
        return callback_;
    }

    public void setCallback(HttpCallback callback)
    {
        callback_ = callback;
    }

    public Exception getException()
    {
        return exception_;
    }

    public void setException(Exception exception)
    {
        exception_ = exception;
    }

    public HttpResponse getResponse()
    {
        return response_;
    }

    public void setResponse(HttpResponse response)
    {
        response_ = response;
    }

    public void requestFinished()
    {
        if(exception_ != null)
        {
            callback_.onError(exception_);
        }
        else
        {
            callback_.onResponse(this);
        }
    }

    public void setCookieStore(CookieStore store)
    {
        cookieStore = store;
    }

    public CookieStore getCookieStore()
    {
        return cookieStore;
    }

    public void setResponseString(String response)
    {
        this.response = response;
    }

    public String getResponseString()
    {
        return response;
    }

    public void setParams(Map<String, String> param)
    {
        params = param;
    }

    public Map<String, String> getParams()
    {
        return params;
    }
}
