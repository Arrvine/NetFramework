package gw.netframework.core;

public interface INetSpeaker {
	void addListener(INetListener listener);
	void removeListener(INetListener listener);
}
