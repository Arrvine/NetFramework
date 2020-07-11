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
	private List<Socket> socketPool;//accept�󣬷���pool;�Ƚ��ȳ�
	
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
			// �������������������ٴ�����
			speakOut("�������������������ٴ�������");
			return;
		}
		try {
			speakOut(TimeDate.getCurrentTime(TimeDate.DATE_TIME) + " �����������С���");
			conversationList = new ConcurrentHashMap<>();//�̰߳�ȫMap
			serverSocket = new ServerSocket(port);
			
			goon = true;
			new Thread(new ProcessClientConnect(), "�ͻ��������������߳�").start();
			new Thread(this, "�����������߳�").start();
			speakOut(TimeDate.getCurrentTime(TimeDate.DATE_TIME) + " �����������ɹ���");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void shutdown() {
		if (goon == false) {
			speakOut("������δ������");
			return;
		}
		if (!conversationList.isEmpty()) {
			speakOut("�������߿ͻ��ˣ�����崻���");
			return;
		}
		speakOut(TimeDate.getCurrentTime(TimeDate.DATE_TIME) + " RMI�������ѹرգ�");
		closeConversation();
		close();
		speakOut(TimeDate.getCurrentTime(TimeDate.DATE_TIME) + " �������������رգ�");
	}
	
	public void forcedown() {
		if (goon == false) {
			speakOut("������δ������");
			return;
		}
		if (!conversationList.isEmpty()) {
			// ��֪���пͻ��ˣ�������ǿ��崻�����������£�
			for (ServerConversation conversation : conversationList.values()) {
				conversation.serverForcedown();
			}
		}
		closeConversation();
		close();
		speakOut(TimeDate.getCurrentTime(TimeDate.DATE_TIME) + " ǿ�ƹرշ�������");
	}
	
	void toOne(String resourceId, String targetId, String message) {
		// �����ָ���ͻ���ת����Ϣ
		ServerConversation conversation = conversationList.get(targetId);
		conversation.toOne(resourceId, message);
	}
	
	void toOther(String resourceId, String message) {
		// ����������������߿ͻ���ת����Ϣ
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
			speakOut("�ͻ���[" + id + "]�����ڣ�");
			return;
		}
		conversation.killConversation();
		synchronized (conversationList) {
			conversationList.remove(id);
		}
		speakOut("�ͻ���[" + id + "]��ǿ�����ߣ�");
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
		speakOut(TimeDate.getCurrentTime(TimeDate.DATE_TIME) + " ��ʼ�����ͻ����������󡭡�");
		while (goon) {
			// �����ͻ������ӣ������ɡ�ά��һ���Ự�߳�
			try {
				Socket socket = serverSocket.accept();
				synchronized (socketPool) {
					socketPool.add(socket);
					speakOut(TimeDate.getCurrentTime(TimeDate.DATE_TIME) + "�ͻ���[" 
					+ socket.getInetAddress() + "]�������ӡ���");
				}
			} catch (IOException e) {
				goon = false;
			}
		}
		close();
		
	}
	
	//����ͻ�������
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
						socket = socketPool.remove(0);//ȡ��ջ��socket
					}
					String id;
					// Ӧ����socket�����Ự������ServerConversation����
					ServerConversation conversation = new ServerConversation(socket);
					if (conversationList.size() >= maxClientCount) {
						conversation.send(new NetMessage()
								.setCommand(ENetCommand.OUT_OF_ROOM));
						conversation.close();
						speakOut(TimeDate.getCurrentTime(TimeDate.DATE_TIME) 
								+ "�ͻ���[" + socket.getInetAddress() + "]����������������ӣ�");
					} else {
						long curTime = System.currentTimeMillis();
						id = conversation.getIp() + ":" + curTime;//ʹ��ip�͵�ǰʱ������id
						conversation.setServer(Server.this);
						conversation.setId(id);
						conversation.send(new NetMessage()
								.setCommand(ENetCommand.ID)
								.setPara(id));
						conversationList.put(id, conversation);
						speakOut(TimeDate.getCurrentTime(TimeDate.DATE_TIME) 
								+ "�ͻ���[" + id + "]���ӳɹ���");
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
