package com.example.tlaichia.mobilevideocaptureanduploader;

import android.os.AsyncTask;
import android.util.Log;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.FileDataSourceImpl;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.tracks.h264.H264TrackImpl;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;

/**
 * Created by TLaiChia on 15-Oct-17.
 */


public class MP4UploaderTask extends AsyncTask<String, Void, Void> {
    protected Void doInBackground(String... mFileName) {
        try {
            // Open file
            File f = new File(mFileName[0]);

            // MP4Parser
            H264TrackImpl h264Track = new H264TrackImpl(new FileDataSourceImpl(f));
            Movie movie = new Movie();
            movie.addTrack(h264Track);
            Container mp4file = new DefaultMp4Builder().build(movie);
            FileChannel fc = new FileOutputStream(new File(mFileName[0] + ".mp4")).getChannel();
            mp4file.writeContainer(fc);
            fc.close();

            // Delete h264 file
            f.delete();
        } catch (Exception e) {
            Log.e("MP4UploaderTask.java", "Error occurred in doInBackground(String... fileName)");
            e.printStackTrace();
        }

        return null;
    }
}
