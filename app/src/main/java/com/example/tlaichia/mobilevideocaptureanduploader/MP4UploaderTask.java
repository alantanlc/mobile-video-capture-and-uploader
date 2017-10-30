package com.example.tlaichia.mobilevideocaptureanduploader;

import android.os.AsyncTask;
import android.util.Log;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.FileDataSourceImpl;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.tracks.AACTrackImpl;
import com.googlecode.mp4parser.authoring.tracks.h264.H264TrackImpl;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.entity.mime.MultipartEntityBuilder;
import cz.msebera.android.httpclient.entity.mime.content.FileBody;
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;

/**
 * Created by TLaiChia on 15-Oct-17.
 */


public class MP4UploaderTask extends AsyncTask<String, Void, Void> {
    protected Void doInBackground(String... mFileName) {
        try {
            Log.d("MainActivity.java", mFileName[0] + " " + mFileName[1] + " " + mFileName[2]);

            // Open file
            File videoFile = new File(mFileName[2] + "/VIDEO_" + mFileName[1] + ".h264");
            //File audioFile = new File(mFileName[2] + "/AUDIO_" + mFileName[1] + ".aac");

            // MP4Parser
            H264TrackImpl h264Track = new H264TrackImpl(new FileDataSourceImpl(videoFile));
            //AACTrackImpl aacTrack = new AACTrackImpl(new FileDataSourceImpl(audioFile));
            Movie movie = new Movie();
            movie.addTrack(h264Track);
            //movie.addTrack(aacTrack);
            Container mp4file = new DefaultMp4Builder().build(movie);
            FileChannel fc = new FileOutputStream(new File(mFileName[2] + "/VIDEO_" + mFileName[1] + ".mp4")).getChannel();
            mp4file.writeContainer(fc);
            fc.close();

            Log.i("MP4UploaderTask.java", "MP4 file generated!");

            // Delete raw video and audio file
            videoFile.delete();
            //audioFile.delete();

            Log.i("MP4UploaderTask.java", "Raw video and audio files deleted!");

            // Upload to server using POST method
            HttpClient client = new DefaultHttpClient();
            HttpPost post = new HttpPost("http://monterosa.d2.comp.nus.edu.sg/~team10/server/upload_multiple.php");

            // MultipartEntityBuilder
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addTextBody("top_name", mFileName[0]);
            builder.addPart("file[]", new FileBody(new File(mFileName[2] + "/VIDEO_" + mFileName[1] + ".mp4")));
            HttpEntity entity = builder.build();
            post.setEntity(entity);

            // Execute http post
            // HttpResponse response = client.execute(post);

            Log.i("MP4UploaderTask.java", "MP4 segment uploaded!");
        } catch (Exception e) {
            Log.e("MP4UploaderTask.java", "Error occurred in doInBackground(String... fileName)");
            e.printStackTrace();
        }

        return null;
    }
}
