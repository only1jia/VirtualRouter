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
						wait(); // 用于提前创建线程，但与路由器一同开始
					} catch (InterruptedException e1) {
						// TODO InterruptedException 不知何时会发生。notify的时候并不会
						e1.printStackTrace();
					}
					while (true) {
						try {
							wait(3000);
						} catch (InterruptedException e1) {
							// TODO InterruptedException 不知何时会发生。notify的时候并不会
							e1.printStackTrace();
						}
						// 开始发送路由表
						for (Node neibour : handler.getNeighbors()) {
							try {
								RouterMessage message = new RouterMessage();
								message.sender = handler.getMe();
								message.dv = handler.getDV();
								ObjectUtil.send(neibour, message);
								// handler.debug("发送距离向量到" + neibour + "成功");
							} catch (IOException e) {
								Client.sysout("发送距离向量到" + neibour + "失败: " + e);
								// TODO 修改路由表？
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
		// 创建发送线程
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

		// 开始监听
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(handler.getMe().port);
			while (true) {
				Object obj = ObjectUtil.receive(serverSocket);
				if (obj instanceof RouterMessage) {
					RouterMessage message = (RouterMessage) obj;
					boolean changed = false; // 标记自己的路由表是否改变
					for (final Entry<Node, RouterInfo> entry : message.dv.entrySet())
						changed |= handler.refresh(message.sender, entry);
					if (changed)// 路由表改变后立即发送最新路由表给邻居
						synchronized (sender) {
							sender.notify();
						}
				} else if (obj instanceof Message) {
					Message message = (Message) obj;
					Client.sysout("收到由" + message.sender + "转发的报文");
					if (message.dst.equals(handler.getMe())) {
						Client.sysout("收到来自" + message.src + "的报文：\n" + message.text);
						continue;
					}
					message.sender = handler.getMe();
					Client.sysout(forward(message));
				}
			}
		} catch (Exception e) {
			Client.sysout("出错: " + e);
		} finally {
			if (serverSocket != null)
				try {
					serverSocket.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			Client.sysout("路由器已停止");
		}
	}

	/**
	 * 发送信息到另一个节点
	 * 
	 * @param dst
	 *            接收节点
	 * @param msg
	 *            信息
	 * @return 反馈信息
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
				return "此节点目前不可达。报文取消发送";
			message.sender = handler.getMe();
			neighbor = info.next;
			ObjectUtil.send(neighbor, message);
			return "成功发送报文到下一节点：" + neighbor;
		} catch (IOException e) {
			e.printStackTrace();
			return "可能是连接失败（请确定目标路由器正常运行）";
		}
	}

	public String change(Node neighbor, int dis) throws Exception {
		if (!handler.isNeighbor(neighbor))
			return "此节点不是邻居，不能修改距离";
		if (handler.setDis(neighbor, dis))
			synchronized (sender) {
				sender.notify();
			}
		return "修改成功";
	}

}