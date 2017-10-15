package com.only1jia.model;

import java.io.Serializable;




/**
 * �����ַ�Ͷ˿ڣ��൱�ڱ�ʶ����
 * 
 */
public class Node implements Comparable<Node>, Serializable {
	private static final long serialVersionUID = 837528296975168481L;
	public final static String localAddr = "127.0.0.1";
	public String addr = localAddr;
	public int port;

	/**
	 * ���ַ��������ڵ㡣
	 * 
	 * @param s
	 *            ��ʽΪ[IPv4��ַ:�˿ں�]����"127.0.0.1:8080"
	 * @throws Exception 
	 */
	public Node(String s) throws Exception {
		String[] strings = s.split(":");
		if (strings.length != 2)
			throw new Exception("�ڵ��ʽ������ð��");
		if (strings[0].length() != 0)
			this.addr = strings[0];
		this.port = Integer.parseInt(strings[1]);
	}

	/**
	 * �����ڵ㡣
	 * 
	 * @param address
	 *            �ַ�����ַ����"127.0.0.1"
	 * @param port
	 *            �˿ںš�
	 */
	public Node(String address, int port) {
		if (address != null && address.length() != 0)
			this.addr = address;
		this.port = port;
	}

	@Override
	public int compareTo(Node o) {
		if (o == null)
			return 1;
		int result = addr.compareTo(o.addr);
		if (result != 0)
			return result;
		return new Integer(port).compareTo(o.port);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj instanceof Node) {
			Node o = (Node) obj;
			return addr.equals(o.addr) && port == o.port;
		}
		return false;
	}

	@Override
	public String toString() {
		return addr + ":" + port;
	}
}
