package net.tokyoenvious.droid.pictumblr;

import android.os.AsyncTask;

public abstract class AsyncTask0<Progress, Result> extends AsyncTask<Void, Progress, Result> {
    protected Result doInBackground (Void ... params) {
        return doInBackground();
    }
    abstract protected Result doInBackground ();

//  protected void onProgressUpdate (Progress ... values) {
//      onProgressUpdate(values[0]);
//  }
//  protected void onProgressUpdate (Progress v) {
//  }
}
