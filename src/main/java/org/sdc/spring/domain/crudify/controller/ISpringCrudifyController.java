/*******************************************************************************
 * Copyright (c) 2022 Jérémy COLOMBET
 *******************************************************************************/
package org.sdc.spring.domain.crudify.controller;

import java.util.List;
import java.util.Optional;

import org.sdc.spring.domain.crudify.business.ISpringCrudifyBusiness;
import org.sdc.spring.domain.crudify.connector.ISpringCrudifyConnector;
import org.sdc.spring.domain.crudify.repository.ISpringCrudifyRepository;
import org.sdc.spring.domain.crudify.spec.ISpringCrudifyEntity;
import org.sdc.spring.domain.crudify.spec.SpringCrudifyEntityException;
import org.sdc.spring.domain.crudify.spec.SpringCrudifyReadOutputMode;
import org.sdc.spring.domain.crudify.spec.filter.SpringCrudifyLiteral;
import org.sdc.spring.domain.crudify.spec.sort.SpringCrudifySort;

public interface ISpringCrudifyController<Entity extends ISpringCrudifyEntity> {

	public Entity createEntity(String tenantId, Entity object) throws SpringCrudifyEntityException;

	public Entity updateEntity(String tenantId, Entity entity) throws SpringCrudifyEntityException;
	
	public void deleteEntity(String tenantId, String id) throws SpringCrudifyEntityException;

	public void deleteEntities(String tenantId) throws SpringCrudifyEntityException;

	public Entity getEntity(String tenantId, String uuid) throws SpringCrudifyEntityException;

	public long getEntityTotalCount(String tenantId, SpringCrudifyLiteral filter) throws SpringCrudifyEntityException;
	
	public Class<Entity> getEntityClazz();

	public List<?> getEntityList(String tenantId, int pageSize, int pageIndex, SpringCrudifyLiteral filter, SpringCrudifySort sort,
			SpringCrudifyReadOutputMode mode) throws SpringCrudifyEntityException;

	public void setEntityClass(Class<?> entityClass);

	public void setRepository(Optional<?> repoObj);

	public void setConnector(Optional<ISpringCrudifyConnector<ISpringCrudifyEntity, List<ISpringCrudifyEntity>>> connectorObj);

	public void setbusiness(Optional<ISpringCrudifyBusiness<ISpringCrudifyEntity>> businessObj);

}
