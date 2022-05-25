package com.snakat.sns.data;

public class User {

    private final String mId;
    private final String mDisplayName;
    private final String mEmail;
    private final String mAvatarUrl;

    public User(String id, String displayName, String email, String avatarUrl) {
        mId = id;
        mDisplayName = displayName;
        mEmail = email;
        mAvatarUrl = avatarUrl;
    }

    public String getId() {
        return mId;
    }

    public String getDisplayName() {
        return mDisplayName;
    }

    public String getEmail() {
        return mEmail;
    }

    public String getAvatarUrl() {
        return mAvatarUrl;
    }
}
