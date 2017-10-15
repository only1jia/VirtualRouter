package com.only1jia.router;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.Map.Entry;

import com.only1jia.client.Client;
import com.only1jia.util.ObjectUtil;
import com.only1jia.model.RouterMessage;
import com.only1jia.model.Message;
import com.only1jia.model.Node;
import com.only1jia.model.RouterInfo;



public class Router extends Thread {
	
	 class Sender extends Thread {

			private final DVHandler handler = DVHandler.getInstance();

			@Override
			public void run() {
				synchronized (this) {
					try {
						wait(); // ������ǰ�����̣߳�����·����һͬ��ʼ
					} catch (InterruptedException e1) {
						// TODO InterruptedException ��֪��ʱ�ᷢ����notify��ʱ�򲢲���
						e1.printStackTrace();
					}
					while (true) {
						try {
							wait(3000);
						} catch (InterruptedException e1) {
							// TODO InterruptedException ��֪��ʱ�ᷢ����notify��ʱ�򲢲���
							e1.printStackTrace();
						}
						// ��ʼ����·�ɱ�
						for (Node neibour : handler.getNeighbors()) {
							try {
								RouterMessage message = new RouterMessage();
								message.sender = handler.getMe();
								message.dv = handler.getDV();
								ObjectUtil.send(neibour, message);
								// handler.debug("���;���������" + neibour + "�ɹ�");
							} catch (IOException e) {
								Client.sysout("���;���������" + neibour + "ʧ��: " + e);
								// TODO �޸�·�ɱ�
							}
						}
					}
				}

			}

		}


	private final Sender sender;
	private final static DVHandler handler = DVHandler.getInstance();

	public Router(File file) throws Exception {
		handler.init(file);
		// ���������߳�
		sender = new Sender();
		sender.start();
	}

	public void shutdown() {
		handler.shutdown();
		synchronized (sender) {
			sender.notify();
		}
	}

	@Override
	public void run() {
		synchronized (sender) {
			sender.notify();
		}

		// ��ʼ����
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(handler.getMe().port);
			while (true) {
				Object obj = ObjectUtil.receive(serverSocket);
				if (obj instanceof RouterMessage) {
					RouterMessage message = (RouterMessage) obj;
					boolean changed = false; // ����Լ���·�ɱ��Ƿ�ı�
					for (final Entry<Node, RouterInfo> entry : message.dv.entrySet())
						changed |= handler.refresh(message.sender, entry);
					if (changed)// ·�ɱ�ı��������������·�ɱ���ھ�
						synchronized (sender) {
							sender.notify();
						}
				} else if (obj instanceof Message) {
					Message message = (Message) obj;
					Client.sysout("�յ���" + message.sender + "ת���ı���");
					if (message.dst.equals(handler.getMe())) {
						Client.sysout("�յ�����" + message.src + "�ı��ģ�\n" + message.text);
						continue;
					}
					message.sender = handler.getMe();
					Client.sysout(forward(message));
				}
			}
		} catch (Exception e) {
			Client.sysout("����: " + e);
		} finally {
			if (serverSocket != null)
				try {
					serverSocket.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			Client.sysout("·������ֹͣ");
		}
	}

	/**
	 * ������Ϣ����һ���ڵ�
	 * 
	 * @param dst
	 *            ���սڵ�
	 * @param msg
	 *            ��Ϣ
	 * @return ������Ϣ
	 * @throws IOException
	 * @throws UnknownHostException
	 */
	public String send(Node dst, String msg) {
		Message message = new Message();
		message.src = handler.getMe();
		message.dst = dst;
		message.text = msg;
		return forward(message);
	}

	private String forward(Message message) {
		try {
			Node neighbor = null;
			RouterInfo info = handler.get(message.dst);
			if (info == null || info.isUnreachable())
				return "�˽ڵ�Ŀǰ���ɴ����ȡ������";
			message.sender = handler.getMe();
			neighbor = info.next;
			ObjectUtil.send(neighbor, message);
			return "�ɹ����ͱ��ĵ���һ�ڵ㣺" + neighbor;
		} catch (IOException e) {
			e.printStackTrace();
			return "����������ʧ�ܣ���ȷ��Ŀ��·�����������У�";
		}
	}

	public String change(Node neighbor, int dis) throws Exception {
		if (!handler.isNeighbor(neighbor))
			return "�˽ڵ㲻���ھӣ������޸ľ���";
		if (handler.setDis(neighbor, dis))
			synchronized (sender) {
				sender.notify();
			}
		return "�޸ĳɹ�";
	}

}