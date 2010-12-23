package net.tokyoenvious.droid.pictumblr;

import android.os.AsyncTask;

public abstract class AsyncTask1<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> {
    protected Result doInBackground (Params ... params) {
        return doInBackground(params[0]);
    }
    abstract protected Result doInBackground (Params p);

//  protected void onProgressUpdate (Progress ... values) {
//      onProgressUpdate(values[0]);
//  }
//  protected void onProgressUpdate (Progress v) {
//  }
}
