package gw.netframework.core;

import java.net.Socket;

public class ServerConversation extends Communication {
	private String id;
	private String ip;
	
	private Server server;
	
	ServerConversation(Socket socket) {
		super(socket);
		this.ip = socket.getInetAddress().getHostAddress();
	}
	
	String getIp() {
		return ip;
	}
	
	String getId() {
		return id;
	}
	
	void setServer(Server server) {
		this.server = server;
	}
	
	void setId(String id) {
		this.id = id;
	}
	
	void toOne(String resourceId, String message) {
		send(new NetMessage()
				.setCommand(ENetCommand.TO_ONE)
				.setAction(resourceId)
				.setPara(message));
	}
	
	void toOther(String resourceId, String message) {
		send(new NetMessage()
				.setCommand(ENetCommand.TO_OTHER)
				.setAction(resourceId)
				.setPara(message));
	}
	
	void killConversation() {
		send(new NetMessage()
				.setCommand(ENetCommand.GONE));
		close();
	}
	
	void serverForcedown() {
		send(new NetMessage()
				.setCommand(ENetCommand.FORCE_DOWN));
		close();
	}

	public void dealOffline(NetMessage message) {
		server.removeConversation(id);
		server.speakOut("客户端[" + id + "]下线！");
		close();
	}
	
	public void dealToOther(NetMessage message) {
		server.toOther(this.id, message.getPara());
	}
	
	public void dealToOne(NetMessage message) {
		server.toOne(this.id, message.getAction(), message.getPara());
	}
	
	@Override
	protected void dealNetMessage(NetMessage message) {
		DealNetMessage.dealCommand(this, message);
	}

	@Override
	public void peerAbnormalDrop() {
		server.removeConversation(id);
		server.speakOut("客户端[" + id + "]异常下线！");
	}

}
