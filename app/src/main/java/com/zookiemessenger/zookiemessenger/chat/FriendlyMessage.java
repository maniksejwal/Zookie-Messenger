package com.zookiemessenger.zookiemessenger.chat;

import java.util.List;

/**
 * Created by manik on 1/1/18.
 */

public class FriendlyMessage {

    private String text;
    private String userPhoneNumber;
    private String url;
    private String type;
    private List<String> tags;

    public FriendlyMessage() {
    }

    public FriendlyMessage(String text, String user, String type, String url, List<String> tags) {
        this.text = text;
        this.userPhoneNumber = user;
        this.url = url;
        this.type = type;
        this.tags = tags;
    }

    public String getType() {
        return type;
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

    public String getUrl() {
        return url;
    }

    public void setPhotoUrl(String url) {
        this.url = url;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<String> getTags(){
        return tags;
    }
}