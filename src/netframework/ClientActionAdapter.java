package gw.netframework.core;

public class ClientActionAdapter implements IClientAction {

	@Override
	public void serverAbnormalDrop() {
	}

	@Override
	public boolean confirmOffline() {
		return true;
	}

	@Override
	public void beforeOffline() {
	}

	@Override
	public void outOfRoom() {
	}

	@Override
	public void privateConversation(String resourceId, String message) {
	}

	@Override
	public void publicConversation(String resourceId, String message) {
	}

	@Override
	public void serverForcedown() {
	}

	@Override
	public void beGoneByServer() {
	}

	@Override
	public void afterOffline() {
	}

	@Override
	public void connectSuccess() {
	}

}
