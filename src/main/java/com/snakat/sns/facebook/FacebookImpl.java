package com.snakat.sns.facebook;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.LoginStatusCallback;
import com.facebook.Profile;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.share.Sharer;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.widget.ShareDialog;
import com.snakat.sns.data.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import io.reactivex.Completable;
import io.reactivex.CompletableEmitter;
import io.reactivex.CompletableOnSubscribe;
import io.reactivex.CompletableSource;
import io.reactivex.Maybe;
import io.reactivex.MaybeSource;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;

class FacebookImpl implements Facebook {

    private final Context mContext;
    private final CallbackManager mCallbackManager;

    private final List<CompletableEmitter> mLoginEmitters = Collections.synchronizedList(new ArrayList<>());

    public FacebookImpl(@NonNull Context context) {
        mContext = context;
        mCallbackManager = CallbackManager.Factory.create();

        LoginManager.getInstance()
                .registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        Profile.fetchProfileForCurrentAccessToken();

                        while (mLoginEmitters.size() > 0) {
                            CompletableEmitter emitter = mLoginEmitters.remove(0);
                            emitter.onComplete();
                        }
                    }

                    @Override
                    public void onCancel() {
                        while (mLoginEmitters.size() > 0) {
                            CompletableEmitter emitter = mLoginEmitters.remove(0);
                            emitter.onComplete();
                        }
                    }

                    @Override
                    public void onError(@NonNull FacebookException exception) {
                        while (mLoginEmitters.size() > 0) {
                            CompletableEmitter emitter = mLoginEmitters.remove(0);
                            emitter.tryOnError(exception);
                        }
                    }
                });
    }

    private boolean isLoggedIn() {
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        return accessToken != null && !accessToken.isExpired();
    }

    private void tryLogin(@NonNull Fragment fragment) {
        LoginManager.getInstance().logIn(fragment, Collections.singletonList("public_profile"));
    }

    @NonNull
    @Override
    public Maybe<User> getUser() {
        return Maybe.defer(new Callable<MaybeSource<? extends User>>() {
            @Override
            public MaybeSource<? extends User> call() throws Exception {
                if (isLoggedIn()) {
                    Profile profile = Profile.getCurrentProfile();
                    if (profile != null) {
                        Uri pictureUri = profile.getProfilePictureUri(256, 256);
                        User user = new User(profile.getId(), profile.getName(), null, pictureUri != null ? pictureUri.toString() : null);
                        return Maybe.just(user);
                    }
                }
                return Maybe.empty();
            }
        });
    }

    @NonNull
    @Override
    public Maybe<User> logIn(@NonNull Fragment fragment) {
        return Completable
                .create(new CompletableOnSubscribe() {
                    @Override
                    public void subscribe(CompletableEmitter emitter) throws Exception {
                        LoginManager.getInstance().retrieveLoginStatus(mContext, new LoginStatusCallback() {
                            @Override
                            public void onCompleted(@NonNull AccessToken accessToken) {
                                emitter.onComplete();
                            }

                            @Override
                            public void onFailure() {
                                mLoginEmitters.add(emitter);
                                tryLogin(fragment);
                            }

                            @Override
                            public void onError(@NonNull Exception e) {
                                mLoginEmitters.add(emitter);
                                tryLogin(fragment);
                            }
                        });
                    }
                })
                .delay(1000, TimeUnit.MILLISECONDS) // wait a little bit for sdk fetches and saves profile.
                .andThen(getUser());
    }

    @NonNull
    @Override
    public Completable logOut() {
        return Completable.defer(new Callable<CompletableSource>() {
            @Override
            public CompletableSource call() throws Exception {
                LoginManager.getInstance().logOut();
                return Completable.complete();
            }
        });
    }

    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        return mCallbackManager.onActivityResult(requestCode, resultCode, data);
    }

    @NonNull
    @Override
    public Single<Boolean> shareLink(@NonNull Fragment fragment, @NonNull String quote, @NonNull String url) {
        return Single.create(new SingleOnSubscribe<Boolean>() {
            @Override
            public void subscribe(SingleEmitter<Boolean> emitter) throws Exception {
                ShareLinkContent content = new ShareLinkContent.Builder()
                        .setQuote(quote)
                        .setContentUrl(Uri.parse(url))
                        .build();

                ShareDialog shareDialog = new ShareDialog(fragment);
                shareDialog.registerCallback(mCallbackManager, new FacebookCallback<Sharer.Result>() {
                    @Override
                    public void onSuccess(Sharer.Result result) {
                        emitter.onSuccess(true);
                    }

                    @Override
                    public void onCancel() {
                        emitter.onSuccess(false);
                    }

                    @Override
                    public void onError(@NonNull FacebookException e) {
                        emitter.tryOnError(e);
                    }
                });

                shareDialog.show(content);
            }
        });
    }
}
