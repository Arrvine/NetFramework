package gw.netframework.core;

public interface IClientAction {
	void serverAbnormalDrop();
	void connectSuccess();
	boolean confirmOffline();
	void beforeOffline();
	void outOfRoom();
	void privateConversation(String resourceId, String message);
	void publicConversation(String resourceId, String message);
	void serverForcedown();
	void beGoneByServer();
	void afterOffline();
}
