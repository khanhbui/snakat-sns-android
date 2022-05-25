package com.snakat.sns.facebook;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.snakat.sns.data.User;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;

public interface Facebook {

    @NonNull
    Maybe<User> getUser();

    @NonNull
    Maybe<User> logIn(@NonNull Fragment fragment);

    @NonNull
    Completable logOut();

    boolean onActivityResult(int requestCode, int resultCode, @Nullable Intent data);

    @NonNull
    Single<Boolean> shareLink(@NonNull Fragment fragment, @NonNull String quote, @NonNull String url);

    class Factory {
        @NonNull
        public static Facebook create(@NonNull Context context) {
            return new FacebookImpl(context);
        }
    }
}
