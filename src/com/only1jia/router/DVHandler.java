package com.only1jia.router;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import com.only1jia.client.Client;
import com.only1jia.util.ObjectUtil;
import com.only1jia.model.DV;
import com.only1jia.model.Distance;
import com.only1jia.model.Node;
import com.only1jia.model.RouterInfo;



public class DVHandler {
	
	

	private Node me = null;
	private DV myDV;
	private Map<Node, DV> neighborDVs = new TreeMap<>();
	private Map<Node, Distance> cost = new TreeMap<>();
	private static DV initDV;
	private final static DVHandler instance = new DVHandler();

	public static DVHandler getInstance() {
		return instance;
	}

	public synchronized void init(File file) throws Exception {
		// TODO 初始化判断
		// 读取静态路由信息
		// 文件格式：每一行：[地址:端口] [距离（代价）]，距离为0表示自己，距离为-1表示非邻居（当前不可达）
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
		String lineText;

		// 从文件读取所有节点，记录自己合邻居，初始化距离为无穷
		initDV = new DV();
		while ((lineText = bufferedReader.readLine()) != null) {
			String[] tokens = lineText.split(" ");
			if (tokens.length != 2) {
				bufferedReader.close();
				throw new Exception("文件格式错误：无空格分隔");
			}
			Node dst = new Node(tokens[0]);
			int dis = Integer.parseInt(tokens[1]);
			if (dis == 0) { // 自己
				me = ObjectUtil.clone(dst);
			} else if (dis != -1) { // 邻居
				// 对邻居的距离向量，初始化所有节点为无穷
				cost.put(dst, new Distance(dis));
			}
			initDV.put(dst, RouterInfo.getUnreachable());
		}
		bufferedReader.close();
		if (me == null)
			throw new Exception("配置文件错误：没有本地节点");
		for (Node neighbor : cost.keySet()) {
			DV dv = ObjectUtil.clone(initDV);
			dv.replace(neighbor, new RouterInfo(neighbor, new Distance(0)));
			neighborDVs.put(neighbor, dv);
		}

		// 自己的距离向量。更新邻居的距离
		myDV = ObjectUtil.clone(initDV);
		for (Entry<Node, Distance> entry : cost.entrySet())
			myDV.replace(entry.getKey(), new RouterInfo(entry.getKey(), entry.getValue()));
		myDV.replace(me, new RouterInfo(me, 0));
		Client.sysout("初始化完成");
		
		Client.port.setText(Integer.toString(me.port));
	}

	public Node getMe() {
		return ObjectUtil.clone(me);
	}

	public synchronized DV getDV() {
		return ObjectUtil.clone(myDV);
	}

	public synchronized RouterInfo get(Node dst) {
		return ObjectUtil.clone(myDV.get(dst));
	}

	public boolean isNeighbor(Node node) {
		return cost.containsKey(node);
	}

	public Set<Node> getNeighbors() {
		return cost.keySet();
	}

	public synchronized void shutdown() {
		myDV = ObjectUtil.clone(initDV);
	}

	/**
	 * 从上帝视角修改自己到邻居的距离
	 * 
	 * @param neighbor
	 *            邻居节点
	 * @param dis
	 *            距离
	 * @return 自身的距离向量是否变更
	 * @throws Exception 
	 * @throws MyException
	 */
	public synchronized boolean setDis(Node neighbor, int dis) throws Exception {
		if (!isNeighbor(neighbor))
			throw new Exception("不是邻居，无法修改");
		if (dis <= 0 && dis != -1)
			throw new Exception("邻居距离必须是正数或-1表示无穷");
		Distance newDis = new Distance(dis);
		cost.replace(neighbor, newDis);
		Client.sysout("更新自己到邻居" + neighbor + "直接距离为" + newDis);
		RouterInfo oldInfo = myDV.get(neighbor);
		RouterInfo minInfo = getMin(neighbor);
		if (oldInfo.equals(minInfo))
			return false;
		myDV.replace(neighbor, minInfo);
		Client.sysout("更新自己经由" + minInfo.next + "到" + neighbor + "的代价为" + minInfo.dis);
		return true;
	}

	public synchronized boolean refresh(Node neighbor, Entry<Node, RouterInfo> entry) throws Exception {
		Node dst = entry.getKey();
		RouterInfo newInfo = entry.getValue();
		if (!isNeighbor(neighbor))
			throw new Exception("不是邻居，无法更新");
		if (dst.equals(neighbor)) // 忽略邻居到邻居自身的条目
			return false;
		// boolean changed = false;
		final DV neighborDV = neighborDVs.get(neighbor);
		final Distance dis = newInfo.dis;

		final RouterInfo oldInfo = neighborDV.get(dst);
		if (oldInfo == null || !oldInfo.equals(newInfo)) {
			neighborDVs.get(neighbor).replace(dst, newInfo);
			Client.sysout("更新从邻居" + neighbor + "到" + dst + "的代价为" + dis);
		}

		boolean changed = false;
		final RouterInfo myInfo = myDV.get(dst);
		final Distance totalDis = Distance.add(myDV.get(neighbor).dis, dis);
		if (myInfo.dis.compareTo(totalDis) > 0) { // 代价变小
			myDV.replace(dst, new RouterInfo(neighbor, totalDis));
			Client.sysout("更新自己经由" + neighbor + "到" + dst + "的代价为" + totalDis);
		} else if (neighbor.equals(myInfo.next) && myInfo.dis.compareTo(totalDis) < 0) { // 原路径代价增加
			RouterInfo minInfo = getMin(dst);
			if (!minInfo.dis.equals(myInfo.dis)) // 需要更新距离向量
				changed = true;
			myDV.replace(dst, minInfo);
			Client.sysout("更新自己经由" + minInfo.next + "到" + dst + "的代价为" + minInfo.dis);
		}
		return changed;
	}

	/**
	 * DV算法中计算到目的节点最短路由的方法。
	 * 
	 * @param dst
	 *            目的节点
	 * @return 最短路由信息。节点不可达时返回距离为最大值的信息。
	 */
	private synchronized RouterInfo getMin(Node dst) {
		RouterInfo minInfo = RouterInfo.getUnreachable();
		for (Node neibour : getNeighbors()) { // 遍历邻居
			Distance totalDis = Distance.add(cost.get(neibour), neighborDVs.get(neibour).get(dst).dis);
			if (minInfo.dis.compareTo(totalDis) > 0)
				minInfo = new RouterInfo(neibour, totalDis);
		}
		return minInfo;
	}

}
