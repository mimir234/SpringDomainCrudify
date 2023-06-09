package org.sdc.spring.domain.crudify.connector.async;

import org.sdc.spring.domain.crudify.connector.ISpringCrudifyConnector.SpringCrudifyConnectorOperation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class SpringCrudifyAsyncConnectorEnvelop <T> {
	
	private SpringCrudifyAsyncMessageType type;
	
	private String messageUuid; 
	
	private String transactionUuid;

	private String tenantId;

	private String domain;
	
	private SpringCrudifyAsyncResponseStatus status;

	private SpringCrudifyConnectorOperation operation;
	
	private T entity;
	
	private String responseDirective;
	
	private String responseMessage;
	
}
