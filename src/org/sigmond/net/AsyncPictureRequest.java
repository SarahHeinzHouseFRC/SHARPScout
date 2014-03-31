package org.sigmond.net;

import android.graphics.drawable.Drawable;
import android.os.AsyncTask;

import java.io.InputStream;
import java.net.URL;

/**
 * Performs an asynchronous picture request.
 */
public class AsyncPictureRequest extends AsyncTask<PicRequestInfo, Integer, PicRequestInfo>
{

    @Override
    protected PicRequestInfo doInBackground(PicRequestInfo... params)
    {

        PicRequestInfo info = params[0];
        Drawable drawable = LoadImageFromWebOperations(info.url);
        info.drawable = drawable;

        return info;
    }

    protected void onPostExecute(PicRequestInfo info)
    {
        super.onPostExecute(info);
        info.finished();
    }

    protected static Drawable LoadImageFromWebOperations(String url)
    {
        try
        {
            InputStream is = (InputStream) new URL(url).getContent();
            Drawable d = Drawable.createFromStream(is, "src name");
            return d;
        }
        catch(Exception e)
        {
            return null;
        }
    }

}
