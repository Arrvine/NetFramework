package gw.netframework.core;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

public class NetConnection {
	Socket socket;
	DataInputStream dis;
	DataOutputStream dos;
	
	public NetConnection() {
		// TODO 自动生成的构造函数存根
	}

	public NetConnection(Socket socket, DataInputStream dis, DataOutputStream dos) {
		this.socket = socket;
		this.dis = dis;
		this.dos = dos;
	}

	Socket getSocket() {
		return socket;
	}

	void setSocket(Socket socket) {
		this.socket = socket;
	}

	DataInputStream getDis() {
		return dis;
	}

	void setDis(DataInputStream dis) {
		this.dis = dis;
	}

	DataOutputStream getDos() {
		return dos;
	}

	void setDos(DataOutputStream dos) {
		this.dos = dos;
	}
}
