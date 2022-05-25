package com.snakat.sns.google;

import static android.app.Activity.RESULT_OK;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.snakat.sns.data.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import io.reactivex.Completable;
import io.reactivex.CompletableEmitter;
import io.reactivex.CompletableOnSubscribe;
import io.reactivex.Maybe;
import io.reactivex.MaybeEmitter;
import io.reactivex.MaybeOnSubscribe;
import io.reactivex.MaybeSource;

class GoogleImpl implements Google {

    private final Context mContext;

    private final GoogleSignInClient mGoogleSignInClient;

    private List<MaybeEmitter<User>> mLoginEmitters = Collections.synchronizedList(new ArrayList<>());

    public GoogleImpl(@NonNull Context context) {
        mContext = context;

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(context, gso);
    }

    @Override
    public Maybe<User> getUser() {
        return Maybe.defer(new Callable<MaybeSource<? extends User>>() {
            @Override
            public MaybeSource<? extends User> call() throws Exception {
                GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(mContext);
                if (account == null) {
                    return Maybe.empty();
                }
                Uri photoUrl = account.getPhotoUrl();
                User user = new User(account.getId(), account.getDisplayName(), account.getEmail(), photoUrl == null ? null : photoUrl.toString());
                return Maybe.just(user);
            }
        });
    }

    @Override
    public Maybe<User> logIn(@NonNull ActivityResultLauncher<Intent> launcher) {
        return Maybe
                .create(new MaybeOnSubscribe<User>() {
                    @Override
                    public void subscribe(MaybeEmitter<User> emitter) throws Exception {
                        mLoginEmitters.add(emitter);

                        Intent intent = mGoogleSignInClient.getSignInIntent();
                        launcher.launch(intent);
                    }
                });
    }

    @Override
    public Completable logOut() {
        return Completable.create(new CompletableOnSubscribe() {
            @Override
            public void subscribe(CompletableEmitter emitter) throws Exception {
                mGoogleSignInClient.signOut()
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                emitter.onComplete();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                emitter.tryOnError(e);
                            }
                        });
            }
        });
    }

    @Override
    public void onActivityResult(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Uri photoUrl = account.getPhotoUrl();
                User user = new User(account.getId(), account.getDisplayName(), account.getEmail(), photoUrl == null ? null : photoUrl.toString());

                while (mLoginEmitters.size() > 0) {
                    MaybeEmitter<User> emitter = mLoginEmitters.remove(0);
                    emitter.onSuccess(user);
                }
            } catch (ApiException e) {
                e.printStackTrace();
                while (mLoginEmitters.size() > 0) {
                    MaybeEmitter<User> emitter = mLoginEmitters.remove(0);
                    emitter.tryOnError(e);
                }
            }
        } else {
            while (mLoginEmitters.size() > 0) {
                MaybeEmitter<User> emitter = mLoginEmitters.remove(0);
                emitter.onComplete();
            }
        }
    }
}
