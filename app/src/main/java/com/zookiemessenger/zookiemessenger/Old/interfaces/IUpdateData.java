package com.zookiemessenger.zookiemessenger.Old.interfaces;
import com.zookiemessenger.zookiemessenger.Old.types.FriendInfo;
import com.zookiemessenger.zookiemessenger.Old.types.MessageInfo;


public interface IUpdateData {
	public void updateData(MessageInfo[] messages, FriendInfo[] friends, FriendInfo[] unApprovedFriends, String userKey);

}
