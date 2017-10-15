package com.only1jia.model;

import java.io.Serializable;

/**
 * ±¨ÎÄ
 *
 */
public class Message implements Serializable {
	private static final long serialVersionUID = 2861019874243079729L;
	public Node src;
	public Node dst;
	public Node sender;
	public String text;
}
