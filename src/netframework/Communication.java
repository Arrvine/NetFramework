package gw.netframework.core;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public abstract class Communication implements Runnable {
	private Socket socket;
	private DataInputStream dis;
	private DataOutputStream dos;
	private volatile boolean goon;
	private Object lock;
	
	//��֤��ִ���깹�췽��ʱ��run()����ִ�����
	Communication(Socket socket) {
		try {
			this.lock = new Object();

			this.socket = socket;
			this.dis = new DataInputStream(socket.getInputStream());
			this.dos = new DataOutputStream(socket.getOutputStream());
			this.goon = true;
			//lock���ڲ�ͬ��ʵ�����������Ҳ�ǲ�ͬ��
			//����ͬ�ͻ��������е�����״̬�ǲ�ͬ�ģ�������С�������
			//lock���õĶ������ֻ�Ǹ��̼߳������߳�
			synchronized (lock) {
				new Thread(this, "COMMUNICATION").start();
				try {
					lock.wait();
				} catch (InterruptedException e) {
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public abstract void peerAbnormalDrop();
	protected abstract void dealNetMessage(NetMessage message);
	
	@Override
	public void run() {
		String message = null;
		//���run()�����丸�̹߳�ͬ����lock(������)��
		//����lock��״̬�빹�캯����ͬ
		synchronized (lock) {
			lock.notify();
		}
	
		while (goon) {
			try {
				message = dis.readUTF();
				dealNetMessage(new NetMessage(message));
			} catch (IOException e) {
				if (goon == true) {
					goon = false;
					peerAbnormalDrop();
				}
			}
		}
		close();
	}
	

	void send(NetMessage netMessage) {
		try {
			dos.writeUTF(netMessage.toString());
		} catch (IOException e) {
			close();
		}
	}
	
	void close() {
		this.goon = false;
		try {
			if (this.dis != null) {
				this.dis.close();
			}
		} catch (IOException e) {
		} finally {
			this.dis = null;
		}
		try {
			if (this.dos != null) {
				this.dos.close();
			}
		} catch (IOException e) {
		} finally {
			this.dos = null;
		}
		try {
			if (this.socket != null && !this.socket.isClosed()) {
				this.socket.close();
			}
		} catch (IOException e) {
		} finally {
			this.socket = null;
		}
	}

}
