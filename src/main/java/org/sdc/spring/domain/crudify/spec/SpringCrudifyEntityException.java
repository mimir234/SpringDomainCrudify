/*******************************************************************************
 * Copyright (c) 2022 Jérémy COLOMBET
 *******************************************************************************/
package org.sdc.spring.domain.crudify.spec;

import lombok.Getter;

@Getter
public class SpringCrudifyEntityException extends Exception {

	private int code = BAD_REQUEST;
	
	public SpringCrudifyEntityException(String string) {
		super(string);
	}

	public SpringCrudifyEntityException(int code, String string) {
		super(string);
		this.code = code;
	}
	
	public SpringCrudifyEntityException(int code, String string, Exception e) {
		super(string, e);
		this.code = code;
	}

	public SpringCrudifyEntityException(Exception e) {
		super(e);
	}

	public SpringCrudifyEntityException(int code, Exception e) {
		super(e);
		this.code = code;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 8581388689485492204L;
	public static final int ENTITY_NOT_FOUND = 1;
	public static final int BAD_REQUEST = 2;
	public static final int UNKNOWN_ERROR = 3;
	public static final int CONNECTOR_ERROR = 4;
	public static final int ENTITY_ALREADY_EXISTS = 5;

}
