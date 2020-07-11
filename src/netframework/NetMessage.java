package gw.netframework.core;

public class NetMessage {
	private ENetCommand command;
	private String action;
	private String para;
	
	NetMessage() {
	}

	NetMessage(String message) {
		int dotIndex;
		
		dotIndex = message.indexOf('.');
		if (dotIndex < 0) {
			return;
		}
		String str = message.substring(0, dotIndex);
		this.command = ENetCommand.valueOf(str);
		
		message = message.substring(dotIndex + 1);
		dotIndex = message.indexOf('.');
		if (dotIndex < 0) {
			this.command = null;
			return;
		}
		str = message.substring(0, dotIndex); 
		this.action = str.equals(" ") ? null : str;
		
		message = message.substring(dotIndex + 1);
		this.para = message;
	}
	
	ENetCommand getCommand() {
		return command;
	}

	NetMessage setCommand(ENetCommand command) {
		this.command = command;
		return this;
	}

	String getAction() {
		return action;
	}

	NetMessage setAction(String action) {
		this.action = action;
		return this;
	}

	String getPara() {
		return para;
	}

	NetMessage setPara(String para) {
		this.para = para;
		return this;
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer(command.name());
		result.append('.');
		result.append(action == null ? " " : action).append('.');
		result.append(para);
		
		return result.toString();
	}
	
}
