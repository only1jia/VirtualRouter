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
		// TODO ��ʼ���ж�
		// ��ȡ��̬·����Ϣ
		// �ļ���ʽ��ÿһ�У�[��ַ:�˿�] [���루���ۣ�]������Ϊ0��ʾ�Լ�������Ϊ-1��ʾ���ھӣ���ǰ���ɴ
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
		String lineText;

		// ���ļ���ȡ���нڵ㣬��¼�Լ����ھӣ���ʼ������Ϊ����
		initDV = new DV();
		while ((lineText = bufferedReader.readLine()) != null) {
			String[] tokens = lineText.split(" ");
			if (tokens.length != 2) {
				bufferedReader.close();
				throw new Exception("�ļ���ʽ�����޿ո�ָ�");
			}
			Node dst = new Node(tokens[0]);
			int dis = Integer.parseInt(tokens[1]);
			if (dis == 0) { // �Լ�
				me = ObjectUtil.clone(dst);
			} else if (dis != -1) { // �ھ�
				// ���ھӵľ�����������ʼ�����нڵ�Ϊ����
				cost.put(dst, new Distance(dis));
			}
			initDV.put(dst, RouterInfo.getUnreachable());
		}
		bufferedReader.close();
		if (me == null)
			throw new Exception("�����ļ�����û�б��ؽڵ�");
		for (Node neighbor : cost.keySet()) {
			DV dv = ObjectUtil.clone(initDV);
			dv.replace(neighbor, new RouterInfo(neighbor, new Distance(0)));
			neighborDVs.put(neighbor, dv);
		}

		// �Լ��ľ��������������ھӵľ���
		myDV = ObjectUtil.clone(initDV);
		for (Entry<Node, Distance> entry : cost.entrySet())
			myDV.replace(entry.getKey(), new RouterInfo(entry.getKey(), entry.getValue()));
		myDV.replace(me, new RouterInfo(me, 0));
		Client.sysout("��ʼ�����");
		
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
	 * ���ϵ��ӽ��޸��Լ����ھӵľ���
	 * 
	 * @param neighbor
	 *            �ھӽڵ�
	 * @param dis
	 *            ����
	 * @return ����ľ��������Ƿ���
	 * @throws Exception 
	 * @throws MyException
	 */
	public synchronized boolean setDis(Node neighbor, int dis) throws Exception {
		if (!isNeighbor(neighbor))
			throw new Exception("�����ھӣ��޷��޸�");
		if (dis <= 0 && dis != -1)
			throw new Exception("�ھӾ��������������-1��ʾ����");
		Distance newDis = new Distance(dis);
		cost.replace(neighbor, newDis);
		Client.sysout("�����Լ����ھ�" + neighbor + "ֱ�Ӿ���Ϊ" + newDis);
		RouterInfo oldInfo = myDV.get(neighbor);
		RouterInfo minInfo = getMin(neighbor);
		if (oldInfo.equals(minInfo))
			return false;
		myDV.replace(neighbor, minInfo);
		Client.sysout("�����Լ�����" + minInfo.next + "��" + neighbor + "�Ĵ���Ϊ" + minInfo.dis);
		return true;
	}

	public synchronized boolean refresh(Node neighbor, Entry<Node, RouterInfo> entry) throws Exception {
		Node dst = entry.getKey();
		RouterInfo newInfo = entry.getValue();
		if (!isNeighbor(neighbor))
			throw new Exception("�����ھӣ��޷�����");
		if (dst.equals(neighbor)) // �����ھӵ��ھ��������Ŀ
			return false;
		// boolean changed = false;
		final DV neighborDV = neighborDVs.get(neighbor);
		final Distance dis = newInfo.dis;

		final RouterInfo oldInfo = neighborDV.get(dst);
		if (oldInfo == null || !oldInfo.equals(newInfo)) {
			neighborDVs.get(neighbor).replace(dst, newInfo);
			Client.sysout("���´��ھ�" + neighbor + "��" + dst + "�Ĵ���Ϊ" + dis);
		}

		boolean changed = false;
		final RouterInfo myInfo = myDV.get(dst);
		final Distance totalDis = Distance.add(myDV.get(neighbor).dis, dis);
		if (myInfo.dis.compareTo(totalDis) > 0) { // ���۱�С
			myDV.replace(dst, new RouterInfo(neighbor, totalDis));
			Client.sysout("�����Լ�����" + neighbor + "��" + dst + "�Ĵ���Ϊ" + totalDis);
		} else if (neighbor.equals(myInfo.next) && myInfo.dis.compareTo(totalDis) < 0) { // ԭ·����������
			RouterInfo minInfo = getMin(dst);
			if (!minInfo.dis.equals(myInfo.dis)) // ��Ҫ���¾�������
				changed = true;
			myDV.replace(dst, minInfo);
			Client.sysout("�����Լ�����" + minInfo.next + "��" + dst + "�Ĵ���Ϊ" + minInfo.dis);
		}
		return changed;
	}

	/**
	 * DV�㷨�м��㵽Ŀ�Ľڵ����·�ɵķ�����
	 * 
	 * @param dst
	 *            Ŀ�Ľڵ�
	 * @return ���·����Ϣ���ڵ㲻�ɴ�ʱ���ؾ���Ϊ���ֵ����Ϣ��
	 */
	private synchronized RouterInfo getMin(Node dst) {
		RouterInfo minInfo = RouterInfo.getUnreachable();
		for (Node neibour : getNeighbors()) { // �����ھ�
			Distance totalDis = Distance.add(cost.get(neibour), neighborDVs.get(neibour).get(dst).dis);
			if (minInfo.dis.compareTo(totalDis) > 0)
				minInfo = new RouterInfo(neibour, totalDis);
		}
		return minInfo;
	}

}
