package org.sigmond.net;

public interface HttpCallback
{
    public void onResponse(HttpRequestInfo resp);

    public void onError(Exception e);
}
