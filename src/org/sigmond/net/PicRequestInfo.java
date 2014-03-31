package org.sigmond.net;

import android.graphics.drawable.Drawable;

public class PicRequestInfo
{

    public PicCallback callback;
    public Drawable drawable;
    public String url;

    public PicRequestInfo(String url, PicCallback callback)
    {
        this.callback = callback;
        this.url = url;
    }

    public void finished()
    {
        callback.onFinished(drawable);
    }
}
