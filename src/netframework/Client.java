package gw.netframework.core;

import java.net.Socket;

import stu.gw.RMI.core.RMIClient;
import stu.gw.util.PropertiesParser;


public class Client {
	private static final int DEFAULT_PORT = 54188;
	private static final String DEFAULT_IP = "127.0.0.1";
	private static final int DEFAULT_RMI_PORT = 54199;

	private String ip;
	private int port;
	private Socket socket;

	private IClientAction clientAction;
	private ClientConversation conversation;
	
	private String rmiIp;
	private int rmiPort;
	private RMIClient rmiClient;
	
	public Client() {
		init();
		this.clientAction = new ClientActionAdapter();
	}
	
	public void setRmiIp(String rmiIp) {
		this.rmiIp = rmiIp;
	}

	public void setRmiPort(int rmiPort) {
		this.rmiPort = rmiPort;
	}
	
	
	//提供两种服务器连接方式，普通长连接和RMI远程调用
	
	private void init() {
		this.port = DEFAULT_PORT;
		this.ip = DEFAULT_IP;
		this.rmiIp = DEFAULT_IP;
		this.rmiPort = DEFAULT_RMI_PORT;
		readCfg("/net.cfg.properties");
	}
	
	//初始化RMI，不进行服务器连接等
	private void RMIStart() {
		if (this.rmiClient == null) {//单例
			synchronized (Client.class) {
				if (this.rmiClient == null) {
					this.rmiClient = new RMIClient();
					this.rmiClient.setRmiServerIp(rmiIp);
					this.rmiClient.setRmiServerPort(rmiPort);
				}
			}
		}
	}
	
	public <T> T getProxy(Class<?> interfaceClass) {
		RMIStart();
		return this.rmiClient.getProxy(interfaceClass);
	}
	
	//获取ip,port,strRmiIp,strRmiPort
	private void readCfg(String cfgPath) {
		PropertiesParser parser = new PropertiesParser();
		
		parser.loadProperties(cfgPath);
		String str = parser.value("port");
		if (str.length() > 0) {
			port = Integer.valueOf(str);
		}
		
		String ip = parser.value("ip");
		if (ip.length() > 0) {
			this.ip = ip;
		}
		
		String strRmiIp = parser.value("rmiServerIp");
		if (strRmiIp.length() > 0) {
			this.rmiIp = strRmiIp;
		}
		
		String strRmiPort = parser.value("rmiServerPort");
		if (strRmiPort.length() > 0) {
			this.rmiPort = Integer.valueOf(strRmiPort);
		}
	}
	
	//指定配置文件
	public void initNetConfig(String configFilePath) {
		readCfg(configFilePath);
	}
	
		
	public boolean connectToServer() {
		if (socket != null) {
			return false;
		}
		try {
			socket = new Socket(ip, port);//普通长连接
			conversation = new ClientConversation(this, socket);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	

	//提供应用层使用
	
	public void offline() {
		if (clientAction.confirmOffline() == false) {
			return;
		}
		clientAction.beforeOffline();
		conversation.offline();
		clientAction.afterOffline();
	}
	
	public void toOne(String targetId, String message) {
		// 通过调用conversation层所提供的相关功能实现
		conversation.toOne(targetId, message);
	}
	
	public void toOther(String message) {
		// 通过调用conversation层所提供的相关功能实现
		conversation.toOther(message);
	}
	
	
	
	void setId(String id) {
		conversation.setId(id);
	}
	
	public String getId() {
		return conversation.getId();
	}
	
	public Client setClientAction(IClientAction clientAction) {
		this.clientAction = clientAction;
		return this;
	}

	IClientAction getClientAction() {
		return clientAction;
	}
	
}
