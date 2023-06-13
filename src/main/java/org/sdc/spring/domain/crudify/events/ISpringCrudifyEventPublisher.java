package org.sdc.spring.domain.crudify.events;

import org.sdc.spring.domain.crudify.spec.ISpringCrudifyEntity;

public interface ISpringCrudifyEventPublisher {
	
	public void publishEntityEvent(EntityEvent event, ISpringCrudifyEntity enity);
		
}
