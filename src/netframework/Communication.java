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
	
	//保证在执行完构造方法时，run()函数执行完毕
	Communication(Socket socket) {
		try {
			this.lock = new Object();

			this.socket = socket;
			this.dis = new DataInputStream(socket.getInputStream());
			this.dos = new DataOutputStream(socket.getOutputStream());
			this.goon = true;
			//lock对于不同的实例化对象而言也是不同的
			//即不同客户端所持有的锁的状态是不同的，不会进行“互锁”
			//lock作用的对象仅仅只是该线程及其子线程
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
		//这个run()，与其父线程共同持有lock(对象锁)，
		//所以lock的状态与构造函数相同
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
