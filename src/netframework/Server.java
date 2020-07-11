package gw.netframework.core;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import stu.gw.RMI.core.RMIFactory;
import stu.gw.util.PropertiesParser;
import stu.gw.util.TimeDate;

public class Server implements INetSpeaker, Runnable {
	private static final int DEFAULT_PORT = 54188;
	private static final int MAX_CLIENT_COUNT = 50;
	
	private ServerSocket serverSocket;
	private int port;
	private volatile boolean goon;
	private Map<String, ServerConversation> conversationList;
	private int maxClientCount;
	
	private List<INetListener> listenerList;
	private List<Socket> socketPool;//accept后，放入pool;先进先出
	
	public Server() {
		listenerList = new ArrayList<>();
		socketPool = new ArrayList<>();
		init();
	}
	
	public void setPort(int port) {
		this.port = port;
	}

	public boolean isStartup() {
		return goon;
	}
	
	public void startup() {
		if (goon == true) {
			// 服务器已启动，无需再次启动
			speakOut("服务器已启动，无需再次启动！");
			return;
		}
		try {
			speakOut(TimeDate.getCurrentTime(TimeDate.DATE_TIME) + " 服务器启动中……");
			conversationList = new ConcurrentHashMap<>();//线程安全Map
			serverSocket = new ServerSocket(port);
			
			goon = true;
			new Thread(new ProcessClientConnect(), "客户端连接请求处理线程").start();
			new Thread(this, "服务器侦听线程").start();
			speakOut(TimeDate.getCurrentTime(TimeDate.DATE_TIME) + " 服务器启动成功！");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void shutdown() {
		if (goon == false) {
			speakOut("服务器未启动！");
			return;
		}
		if (!conversationList.isEmpty()) {
			speakOut("尚有在线客户端，不能宕机！");
			return;
		}
		speakOut(TimeDate.getCurrentTime(TimeDate.DATE_TIME) + " RMI服务器已关闭！");
		closeConversation();
		close();
		speakOut(TimeDate.getCurrentTime(TimeDate.DATE_TIME) + " 服务器已正常关闭！");
	}
	
	public void forcedown() {
		if (goon == false) {
			speakOut("服务器未启动！");
			return;
		}
		if (!conversationList.isEmpty()) {
			// 告知所有客户端，服务器强制宕机，并处理后事！
			for (ServerConversation conversation : conversationList.values()) {
				conversation.serverForcedown();
			}
		}
		closeConversation();
		close();
		speakOut(TimeDate.getCurrentTime(TimeDate.DATE_TIME) + " 强制关闭服务器！");
	}
	
	void toOne(String resourceId, String targetId, String message) {
		// 完成向指定客户端转发消息
		ServerConversation conversation = conversationList.get(targetId);
		conversation.toOne(resourceId, message);
	}
	
	void toOther(String resourceId, String message) {
		// 完成向其它所有在线客户端转发消息
		for (String id : conversationList.keySet()) {
			if (id.equals(resourceId)) {
				continue;
			}
			ServerConversation conversation = conversationList.get(id);
			conversation.toOther(resourceId, message);
		}
	}
	
	void killClient(String id) {
		ServerConversation conversation = conversationList.get(id);
		if (conversation == null) {
			speakOut("客户端[" + id + "]不存在！");
			return;
		}
		conversation.killConversation();
		synchronized (conversationList) {
			conversationList.remove(id);
		}
		speakOut("客户端[" + id + "]被强制下线！");
	}
	
	public List<String> getOnlineClient() {
		List<String> result = new ArrayList<>();
		
		synchronized (conversationList) {
			for (String id : conversationList.keySet()) {
				result.add(id);
			}
		}
		
		return result;
	}
	
	void removeConversation(String id) {
		if (!conversationList.containsKey(id)) {
			return;
		}
		conversationList.remove(id);
	}
	
	void addConversation(String id, ServerConversation conversation) {
		if (conversationList.containsKey(id)) {
			return;
		}
		conversationList.put(id, conversation);
	}
	
	private void closeConversation() {
		conversationList.clear();
		conversationList = null;
	}
	
	public void parseRmiMapping(String rmiMappingPath) {
		RMIFactory.scanRMIMapping(rmiMappingPath);
	}
	
	private void init() {
		this.port = DEFAULT_PORT;
		this.maxClientCount = MAX_CLIENT_COUNT;
		readCfg("/net.cfg.properties");
	}
	
	private void readCfg(String cfgPath) {
		PropertiesParser parser = new PropertiesParser();
		parser.loadProperties(cfgPath);
		String str = parser.value("port");
		if (str.length() > 0) {
			this.port = Integer.valueOf(str);
		}
		
		str = parser.value("max_client_count");
		if (str.length() > 0) {
			this.maxClientCount = Integer.valueOf(str);
		}
	}
	
	public void initNetConfig(String configFilePath) {
		readCfg(configFilePath);
	}
	
	@Override
	public void run() {
		speakOut(TimeDate.getCurrentTime(TimeDate.DATE_TIME) + " 开始侦听客户端连接请求……");
		while (goon) {
			// 侦听客户端连接，并构成、维持一个会话线程
			try {
				Socket socket = serverSocket.accept();
				synchronized (socketPool) {
					socketPool.add(socket);
					speakOut(TimeDate.getCurrentTime(TimeDate.DATE_TIME) + "客户端[" 
					+ socket.getInetAddress() + "]请求连接……");
				}
			} catch (IOException e) {
				goon = false;
			}
		}
		close();
		
	}
	
	//处理客户端连接
		class ProcessClientConnect implements Runnable {
			public ProcessClientConnect() {
			}
			
			@Override
			public void run() {
				Socket socket = null;
				while (goon) {
					synchronized (socketPool) {
						if (socketPool.isEmpty()) {
							continue;
						}
						socket = socketPool.remove(0);//取出栈顶socket
					}
					String id;
					// 应该用socket产生会话，即，ServerConversation对象！
					ServerConversation conversation = new ServerConversation(socket);
					if (conversationList.size() >= maxClientCount) {
						conversation.send(new NetMessage()
								.setCommand(ENetCommand.OUT_OF_ROOM));
						conversation.close();
						speakOut(TimeDate.getCurrentTime(TimeDate.DATE_TIME) 
								+ "客户端[" + socket.getInetAddress() + "]因服务器满放弃连接！");
					} else {
						long curTime = System.currentTimeMillis();
						id = conversation.getIp() + ":" + curTime;//使用ip和当前时间生成id
						conversation.setServer(Server.this);
						conversation.setId(id);
						conversation.send(new NetMessage()
								.setCommand(ENetCommand.ID)
								.setPara(id));
						conversationList.put(id, conversation);
						speakOut(TimeDate.getCurrentTime(TimeDate.DATE_TIME) 
								+ "客户端[" + id + "]连接成功！");
					}
				}
			}
			
		}

	
	
	void speakOut(String message) {
		for (INetListener listener : listenerList) {
			listener.dealMessage(message);
		}
	}
	
	@Override
	public void addListener(INetListener listener) {
		if (!this.listenerList.contains(listener)) {
			this.listenerList.add(listener);
		}
	}

	@Override
	public void removeListener(INetListener listener) {
		if (this.listenerList.contains(listener)) {
			this.listenerList.remove(listener);
		}
	}
	
	private void close() {
		goon = false;
		try {
			if (serverSocket != null && !serverSocket.isClosed()) {
				serverSocket.close();
			}
		} catch (IOException e) {
		} finally {
			serverSocket = null;
		}
	}
	
}
