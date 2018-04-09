package com.example.tlaichia.mobilevideocaptureanduploader;

import android.os.AsyncTask;
import android.util.Log;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.entity.mime.MultipartEntityBuilder;
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;

/**
 * Created by TLaiChia on 15-Nov-17.
 */

public class UploadLiveTask extends AsyncTask<String, Void, Void> {
    protected Void doInBackground(String... mFileName) {
        HttpResponse response = null;
        try {
            // Upload to server using POST method
            HttpClient client = new DefaultHttpClient();
            HttpPost post = new HttpPost("http://monterosa.d2.comp.nus.edu.sg/~team10/server/upload_live.php");

            // MultipartEntityBuilder
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addTextBody("top_name", mFileName[0]);
            HttpEntity entity = builder.build();
            post.setEntity(entity);
            response = client.execute(post);

            Log.e("MP4UploaderTask.java", "requestUploadLive Response: " + response);
        } catch (Exception e) {
            Log.e("MP4UploaderTask.java", "requestUploadLive Response: " + response);
            e.printStackTrace();
        }

        return null;
    }
}
