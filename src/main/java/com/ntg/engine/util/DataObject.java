package com.ntg.engine.util;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlElement;

public class DataObject implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@XmlElement
	public Long recid = 0L;
	@XmlElement
	public String name;

}