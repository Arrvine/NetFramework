package gw.netframework.core;

import java.net.Socket;

public class ClientConversation extends Communication {
	private String id;
	private Client client;

	public ClientConversation(Client client, Socket socket) {
		super(socket);
		this.client = client;
	}
	
	void setId(String id) {
		this.id = id;
	}
	
	String getId() {
		return id;
	}
	
	void toOne(String targetId, String message) {
		send(new NetMessage()
				.setCommand(ENetCommand.TO_ONE)
				.setAction(targetId)
				.setPara(message));
	}
	
	void toOther(String message) {
		send(new NetMessage()
				.setCommand(ENetCommand.TO_OTHER)
				.setPara(message));
	}
	
	void offline() {
		send(new NetMessage()
				.setCommand(ENetCommand.OFFLINE));
		close();
	}

	public void dealForceDown(NetMessage message) {
		 client.getClientAction().serverForcedown();
		close();
	}
	
	public void dealToOne(NetMessage message) {
		String action = message.getAction();
		String para = message.getPara();
		client.getClientAction().privateConversation(action, para);
	}
	
	public void dealOutOfRoom(NetMessage message) {
		client.getClientAction().outOfRoom();
		close();
	}
	
	public void dealId(NetMessage message) {
		setId(message.getPara());
		client.getClientAction().connectSuccess();
	}
	
	@Override
	protected void dealNetMessage(NetMessage message) {
		DealNetMessage.dealCommand(this, message);
	}

	@Override
	public void peerAbnormalDrop() {
		client.getClientAction().serverAbnormalDrop();
	}

}
