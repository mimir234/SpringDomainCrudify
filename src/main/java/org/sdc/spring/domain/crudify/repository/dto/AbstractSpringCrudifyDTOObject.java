/*******************************************************************************
 * Copyright (c) 2022 Jérémy COLOMBET
 *******************************************************************************/
package org.sdc.spring.domain.crudify.repository.dto;

import javax.swing.text.html.parser.Entity;

import org.sdc.spring.domain.crudify.spec.ISpringCrudifyEntity;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.Data;

/**
 * 
 * @author J.Colombet
 *
 * @param <Entity>
 */
@SuppressWarnings("hiding")
@Data
public abstract class AbstractSpringCrudifyDTOObject<Entity extends ISpringCrudifyEntity> implements ISpringCrudifyDTOObject<Entity> {
	
	@Id
	@Indexed(unique=true)
	protected String uuid;
	
	@Field
	protected String id;
	
	@Field
	protected String tenantId;
	
	protected Entity convert(Entity entity) {
		entity.setId(this.getId());
		entity.setUuid(this.uuid);
		return entity;
	}

	protected AbstractSpringCrudifyDTOObject() {
	}

	protected AbstractSpringCrudifyDTOObject(String tenantId, Entity entity){
		this.tenantId = tenantId;
		this.id = entity.getId();
		this.uuid = entity.getUuid();
		this.create(entity);
	}

}
