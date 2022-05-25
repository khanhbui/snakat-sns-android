package com.snakat.sns;

import android.content.Context;

import androidx.annotation.NonNull;

import com.snakat.sns.facebook.Facebook;
import com.snakat.sns.google.Google;

import java.lang.ref.WeakReference;

abstract class SNSInternal {

    static boolean LOG_ENABLED = true;

    protected Facebook mFacebook;
    protected Google mGoogle;

    protected final WeakReference<Context> mContext;

    protected SNSInternal(@NonNull Context context, boolean logEnabled) {
        LOG_ENABLED = logEnabled;

        mContext = new WeakReference<>(context);
    }

    protected void destroy() {
        mContext.clear();
    }
}
