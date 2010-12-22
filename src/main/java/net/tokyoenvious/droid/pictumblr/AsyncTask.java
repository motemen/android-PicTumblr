package net.tokyoenvious.droid.pictumblr;

public abstract class AsyncTask<Params, Progress, Result> extends android.os.AsyncTask<Params, Progress, Result> {
    protected Result doInBackground (Params ... params) {
        return doInBackground(params[0]);
    }
    abstract protected Result doInBackground (Params p);
}
