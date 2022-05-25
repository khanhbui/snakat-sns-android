package com.snakat.sns;

import android.content.Context;

import androidx.annotation.NonNull;

import com.snakat.sns.facebook.Facebook;
import com.snakat.sns.google.Google;

public final class SNS extends SNSInternal {

    private static SNS mInstance;

    public static SNS getInstance() {
        return mInstance;
    }

    public static void createInstance(@NonNull Context context) {
        createInstance(context, false);
    }

    public static void createInstance(@NonNull Context context, boolean logEnabled) {
        if (mInstance == null) {
            synchronized (SNS.class) {
                mInstance = new SNS(context, logEnabled);
            }
        }
    }

    public static void destroyInstance() {
        mInstance = null;
    }

    private SNS(@NonNull Context context, boolean logEnabled) {
        super(context, logEnabled);
    }

    @NonNull
    public Facebook facebook() {
        if (mFacebook == null) {
            mFacebook = Facebook.Factory.create(mContext.get());
        }
        return mFacebook;
    }

    @NonNull
    public Google google() {
        if (mGoogle == null) {
            mGoogle = Google.Factory.create(mContext.get());
        }
        return mGoogle;
    }
}
