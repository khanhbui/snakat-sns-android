package com.snakat.sns.google;

import android.content.Context;
import android.content.Intent;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;

import com.snakat.sns.data.User;

import io.reactivex.Completable;
import io.reactivex.Maybe;

public interface Google {

    Maybe<User> getUser();

    Maybe<User> logIn(@NonNull ActivityResultLauncher<Intent> launcher);

    Completable logOut();

    void onActivityResult(int resultCode, Intent data);

    class Factory {
        public static Google create(@NonNull Context context) {
            return new GoogleImpl(context);
        }
    }
}
