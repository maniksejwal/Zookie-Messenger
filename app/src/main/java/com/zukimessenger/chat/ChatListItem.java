package com.zukimessenger.chat;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by manik on 6/3/18.
 */

public class ChatListItem implements Parcelable {
    private String phoneNumber;
    private String chatID;
    private String type;
    private String name;

    public ChatListItem(String phone, String chat, String type) {
        phoneNumber = phone;
        chatID = chat;
        this.type = type;
    }

    private ChatListItem(Parcel in) {
        phoneNumber = in.readString();
        chatID = in.readString();
        type = in.readString();
        name = in.readString();
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getChatID() {
        return chatID;
    }

    public String getType(){
        return type;
    }

    public String getName(){
        return name;
    }

    public void setName(String name){
        this.name = name;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(phoneNumber);
        dest.writeString(chatID);
        dest.writeString(type);
        dest.writeString(name);
    }

    public static final Creator<ChatListItem> CREATOR = new Creator<ChatListItem>() {
        @Override
        public ChatListItem createFromParcel(Parcel in) {
            return new ChatListItem(in);
        }

        @Override
        public ChatListItem[] newArray(int size) {
            return new ChatListItem[size];
        }
    };
}
