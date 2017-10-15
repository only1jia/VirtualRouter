package com.only1jia.model;

import java.io.Serializable;

import com.only1jia.util.ObjectUtil;



/**
 * 路由信息。包括下一跳和距离
 *
 */
public class RouterInfo implements Serializable, Comparable<RouterInfo> {
	private static final long serialVersionUID = 1944450231426532349L;
	public Node next;
	public Distance dis;

	public RouterInfo(Node next, int dis) {
		this.next = ObjectUtil.clone(next);
		this.dis = new Distance(dis);
	}

	public RouterInfo(Node next, Distance dis) {
		this.next = ObjectUtil.clone(next);
		this.dis = ObjectUtil.clone(dis);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj instanceof RouterInfo) {
			RouterInfo o = (RouterInfo) obj;
			if (next == null)
				return o.next == null && dis.equals(o.dis);
			return next.equals(o.next) && dis.equals(o.dis);
		}
		return false;
	}

	@Override
	public String toString() {
		return "(" + next + "," + dis + ")";
	}

	public static RouterInfo getUnreachable() {
		return new RouterInfo(null, Integer.MAX_VALUE);
	}

	@Override
	public int compareTo(RouterInfo o) {
		return dis.compareTo(o.dis);
	}

	public boolean isUnreachable() {
		return dis.isUnreachable();
	}
}
