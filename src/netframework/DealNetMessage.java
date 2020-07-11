package gw.netframework.core;

import java.lang.reflect.Method;

/*
 * @Arrvine
 * 根据消息分解命令并生成对应的方法名称
 * 使用反射机制获取方法并执行
 * */
public class DealNetMessage {
	
	public DealNetMessage() {
	}
	
	//根据命令生成“dealXXX”方法
	private static String getMethodName(String command) {
		StringBuffer result = new StringBuffer("deal");
		
		String[] words = command.split("_");
		int wordCount = words.length;
		for (int i = 0; i < wordCount; i++) {
			result.append(words[i].substring(0, 0+1).toUpperCase());
			result.append(words[i].substring(1).toLowerCase());
		}
		
		return result.toString();
	}
	
	//获取消息及其对象，根据得到的方法名生成方法并执行
	public static void dealCommand(Object object, NetMessage message) {
		Class<?> klass = object.getClass();
		String methodName = getMethodName(message.getCommand().name());
		Method method;
		try {
			method = klass.getMethod(methodName, NetMessage.class);
			method.invoke(object, message);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
