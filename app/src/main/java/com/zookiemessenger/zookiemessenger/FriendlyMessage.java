package com.zookiemessenger.zookiemessenger;

/**
 * Created by manik on 1/1/18.
 */

public class FriendlyMessage {

    private String text;
    private String userPhoneNumber, contactPhoneNumber;
    private String photoUrl;

    public FriendlyMessage() {
    }

    public FriendlyMessage(String text, String user, String contact, String photoUrl) {
        this.text = text;
        this.userPhoneNumber = user;
        this.contactPhoneNumber = contact;
        this.photoUrl = photoUrl;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getName() {
        return userPhoneNumber;
    }

    public void setName(String name) {
        this.userPhoneNumber = name;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }
}