/*******************************************************************************
 * Copyright (c) 2022 Jérémy COLOMBET
 *******************************************************************************/
package org.sdc.spring.domain.crudify.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.inject.Inject;

import org.sdc.spring.domain.crudify.business.ISpringCrudifyBusiness;
import org.sdc.spring.domain.crudify.connector.ISpringCrudifyConnector;
import org.sdc.spring.domain.crudify.connector.ISpringCrudifyConnector.SpringCrudifyConnectorOperation;
import org.sdc.spring.domain.crudify.connector.SpringCrudifyConnectorException;
import org.sdc.spring.domain.crudify.events.ISpringCrudifyEventPublisher;
import org.sdc.spring.domain.crudify.events.SpringCrudifyEntityEvent;
import org.sdc.spring.domain.crudify.repository.ISpringCrudifyRepository;
import org.sdc.spring.domain.crudify.repository.dto.ISpringCrudifyDTOObject;
import org.sdc.spring.domain.crudify.spec.ISpringCrudifyDomain;
import org.sdc.spring.domain.crudify.spec.ISpringCrudifyEntity;
import org.sdc.spring.domain.crudify.spec.SpringCrudifyDomainable;
import org.sdc.spring.domain.crudify.spec.SpringCrudifyEntityException;
import org.sdc.spring.domain.crudify.spec.SpringCrudifyReadOutputMode;
import org.sdc.spring.domain.crudify.spec.filter.SpringCrudifyLiteral;
import org.sdc.spring.domain.crudify.spec.filter.SpringCrudifyLiteralException;
import org.sdc.spring.domain.crudify.spec.sort.SpringCrudifySort;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author JérémyCOLOMBET
 *
 * @param <Entity>
 * 
 *            This class implements the data treatments that have to be done one
 *            the entities during their process. If the Uuid of an entity is not
 *            set before storage, then the controller calculates one and affects
 *            it to the entity.
 * 
 * 
 */
@Slf4j
public class SpringCrudifyController<Entity extends ISpringCrudifyEntity, Dto extends ISpringCrudifyDTOObject<Entity>> extends SpringCrudifyDomainable<Entity, Dto> implements ISpringCrudifyController<Entity, Dto> {

	public SpringCrudifyController(ISpringCrudifyDomain<Entity, Dto> domain) {
		super(domain);
	}

	/**
	 * The repository used to store the entity
	 */
	@Inject
	@Setter
	protected Optional<ISpringCrudifyRepository<Entity, Dto>> repository;

	@Inject
	@Setter
	protected Optional<ISpringCrudifyConnector<Entity, List<Entity>, Dto>> connector;
	
	@Inject
	@Setter
	protected Optional<ISpringCrudifyEventPublisher> eventPublisher;
	
	@Inject
	@Setter
	protected Optional<ISpringCrudifyBusiness<Entity>> business;
	
	/**
	 * 
	 */
	@Override
	public Entity getEntity(String tenantId, String uuid, String userId) throws SpringCrudifyEntityException {
		log.info("[Tenant {}] [Domain {}] Getting entity with Uuid " + uuid, tenantId, this.domain);
		Entity entity = null;

		if (this.connector.isPresent()) {

			try {

				entity = this.entityFactory.newInstance(uuid);

				Future<Entity> entityResponse = this.connector.get().requestEntity(tenantId, entity, SpringCrudifyConnectorOperation.READ);

				while (!entityResponse.isDone()) {
					Thread.sleep(250);
				}

				entity = entityResponse.get();
			} catch (Exception e) {
				throw new SpringCrudifyEntityException(SpringCrudifyEntityException.CONNECTOR_ERROR, e);
			}

		} else if (this.repository.isPresent()) {
			entity = this.repository.get().getOneByUuid(tenantId, uuid);
		}

		if (entity == null) {
			throw new SpringCrudifyEntityException(SpringCrudifyEntityException.ENTITY_NOT_FOUND,
					"Entity does not exist");
		}
		return entity;
	}

	@Override
	public Entity createEntity(String tenantId, Entity entity, String userId) throws SpringCrudifyEntityException {

		log.info("[Tenant {}] [Domain {}] Creating entity with uuid {}", tenantId, this.domain, entity.getUuid());

		if (entity.getUuid() == null || entity.getUuid().isEmpty()) {
			entity.setUuid(UUID.randomUUID().toString());
		}

		if(this.business.isPresent()) {
			this.business.get().beforeCreate(tenantId, userId, entity);
		}

		if (this.connector.isPresent()) {
			try {

				Future<Entity> entityResponse = this.connector.get().requestEntity(tenantId, entity, SpringCrudifyConnectorOperation.CREATE);

				while (!entityResponse.isDone()) {
					Thread.sleep(250);
				}

				entity = entityResponse.get();
			} catch (Exception e) {
				throw new SpringCrudifyEntityException(SpringCrudifyEntityException.CONNECTOR_ERROR, e);
			}
		} else if (this.repository.isPresent()) {

			if (this.repository.get().doesExists(tenantId, entity)) {
				throw new SpringCrudifyEntityException(SpringCrudifyEntityException.ENTITY_ALREADY_EXISTS,
						"Entity already exists");
			}

			this.repository.get().save(tenantId, entity);
		}
		
		if( this.eventPublisher.isPresent()) {
			this.eventPublisher.get().publishEntityEvent(SpringCrudifyEntityEvent.CREATE, entity);
		}
		
		return entity;
	}

	@Override
	public List<?> getEntityList(String tenantId, int pageSize, int pageIndex, SpringCrudifyLiteral filter, SpringCrudifySort sort, SpringCrudifyReadOutputMode mode, String userId)
			throws SpringCrudifyEntityException {
		log.info("[Tenant {}] [Domain {}] Getting entities, mode {}, page size {}, page index {}, filter {}, sort {}", tenantId, this.domain, mode, pageSize, pageIndex, filter, sort);
		try {
			SpringCrudifyLiteral.validate(filter);
		} catch (SpringCrudifyLiteralException e) {
			throw new SpringCrudifyEntityException(SpringCrudifyEntityException.BAD_REQUEST, e);
		}
		ArrayList<String> entityUuids = new ArrayList<String>();

		List<Entity> entities = null;

		if (this.connector.isPresent()) {
			try {

				Future<List<Entity>> entityResponse = this.connector.get().requestList(tenantId, null, SpringCrudifyConnectorOperation.READ);
				
				while( !entityResponse.isDone() ) {
					Thread.sleep(250);
				}
				
				entities = entityResponse.get();	
			} catch (Exception e) {
				throw new SpringCrudifyEntityException(SpringCrudifyEntityException.CONNECTOR_ERROR, e);
			}
		} else if (this.repository.isPresent()) {
			entities = this.repository.get().getEntities(tenantId, pageSize, pageIndex, filter, sort);
		}

		switch (mode) {
		case full -> {return entities;}
		case id -> {entities.forEach(e -> { entityUuids.add(e.getId()); });}
		case uuid -> {entities.forEach(e -> { entityUuids.add(e.getUuid()); });}
		}
		
		return entityUuids;
	}

	@Override
	public Entity updateEntity(String tenantId, Entity entity, String userId) throws SpringCrudifyEntityException {
		log.info("[Tenant {}] [Domain {}] Updating entity with Uuid " + entity.getUuid(), tenantId, this.domain);
		Entity updated = null;
		
		if(this.business.isPresent()) {
			this.business.get().beforeUpdate(tenantId, userId, entity);
		}

		if (this.connector.isPresent()) {
			try {

				Future<Entity> entityResponse = this.connector.get().requestEntity(tenantId, entity, SpringCrudifyConnectorOperation.UPDATE);

				while (!entityResponse.isDone()) {
					Thread.sleep(250);
				}

				entity = entityResponse.get();
			} catch (Exception e) {
				throw new SpringCrudifyEntityException(SpringCrudifyEntityException.CONNECTOR_ERROR, e);
			}
		} else if (this.repository.isPresent()) {
			if (!this.repository.get().doesExists(tenantId, entity)) {
				throw new SpringCrudifyEntityException(SpringCrudifyEntityException.ENTITY_NOT_FOUND,
						"Entity does not exist");
			}

			updated = this.repository.get().update(tenantId, entity);
		}
		
		if( this.eventPublisher.isPresent()) {
			this.eventPublisher.get().publishEntityEvent(SpringCrudifyEntityEvent.UPDATE, entity);
		}

		return updated;
	}

	@Override
	public void deleteEntity(String tenantId, String uuid, String userId) throws SpringCrudifyEntityException {
		log.info("[Tenant {}] [Domain {}] Deleting entity with Uuid " + uuid, tenantId, this.domain);

		Entity entity = this.getEntity(tenantId, uuid, userId);

		if (entity == null) {
			throw new SpringCrudifyEntityException(SpringCrudifyEntityException.ENTITY_NOT_FOUND,
					"Entity does not exist");
		}
		
		if(this.business.isPresent()) {
			this.business.get().beforeDelete(tenantId, userId, entity);
		}
		
		if (this.connector.isPresent()) {
			try {

				Future<Entity> entityResponse = this.connector.get().requestEntity(tenantId, entity, SpringCrudifyConnectorOperation.DELETE);

				while (!entityResponse.isDone()) {
					Thread.sleep(250);
				}

				entity = entityResponse.get();
			} catch (Exception e) {
				throw new SpringCrudifyEntityException(SpringCrudifyEntityException.CONNECTOR_ERROR, e);
			}
		} else if (this.repository.isPresent()) {
			this.repository.get().delete(tenantId, entity);
		}
		
		if( this.eventPublisher.isPresent()) {
			this.eventPublisher.get().publishEntityEvent(SpringCrudifyEntityEvent.DELETE, entity);
		}

	}

	@Override
	public void deleteEntities(final String tenantId, String userId) throws SpringCrudifyEntityException {
		log.info("[Tenant {}] [Domain {}] Deleting all entities", tenantId, this.domain);
		List<Entity> entities = this.repository.get().getEntities(tenantId, 0, 1, null, null);

		for (Entity s : entities) {
			try {
				this.deleteEntity(tenantId, s.getUuid(), userId);
			} catch (SpringCrudifyEntityException e) {
				throw new SpringCrudifyEntityException(SpringCrudifyEntityException.UNKNOWN_ERROR,
						"Error during entities deletion");
			}
		}
	}

	@Override
	public long getEntityTotalCount(String tenantId, SpringCrudifyLiteral filter, String userId) throws SpringCrudifyEntityException {
		if (this.connector.isPresent()) {
			try {
				Future<List<Entity>> list = this.connector.get().requestList(tenantId, null, null);
				while (!list.isDone()) {
					Thread.sleep(250);
				}
				return list.get().size();
			} catch (InterruptedException | ExecutionException | SpringCrudifyConnectorException e) {
				throw new SpringCrudifyEntityException(SpringCrudifyEntityException.CONNECTOR_ERROR, e);
			}
		} else if (this.repository.isPresent()) {
			return this.repository.get().getCount(tenantId, filter);
		}
		return 0;

	}

}
