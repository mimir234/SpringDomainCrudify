package org.sdc.spring.domain.crudify.engine;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.reflections.Reflections;
import org.sdc.spring.domain.crudify.connector.ISpringCrudifyConnector;
import org.sdc.spring.domain.crudify.controller.ISpringCrudifyController;
import org.sdc.spring.domain.crudify.controller.SpringCrudifyEngineController;
import org.sdc.spring.domain.crudify.repository.ISpringCrudifyRepository;
import org.sdc.spring.domain.crudify.repository.SpringCrudifyEngineRepository;
import org.sdc.spring.domain.crudify.repository.dao.ISpringCrudifyDAORepository;
import org.sdc.spring.domain.crudify.repository.dao.SpringCrudifyDao;
import org.sdc.spring.domain.crudify.repository.dao.mongodb.AbstractSpringCrudifyMongoRepository;
import org.sdc.spring.domain.crudify.repository.dao.mongodb.SpringCrudifyEngineMongoRepository;
import org.sdc.spring.domain.crudify.repository.dto.ISpringCrudifyDTOObject;
import org.sdc.spring.domain.crudify.spec.ISpringCrudifyEntity;
import org.sdc.spring.domain.crudify.spec.SpringCrudifyReadOutputMode;
import org.sdc.spring.domain.crudify.ws.AbstractSpringCrudifyService;
import org.sdc.spring.domain.crudify.ws.SpringCrudifyEngineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.pattern.PathPatternParser;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class SpringCrudifyDynamicDomainEngine implements ISpringCrudifyDynamicDomainEngine {

	@Inject
	protected MongoTemplate mongo;
	
	@Inject 
	protected ApplicationContext context;

	@Value("${spring.domain.crudify.magicTenantId}")
	protected String magicTenantId;
	
	@Value("${spring.domain.crudify.engine.packages}")
	protected String[] scanPackages;

	@Autowired
	private RequestMappingHandlerMapping requestMappingHandlerMapping;

	private ArrayList<SpringCrudifyEngineService> services;
	
	private Map<String, ISpringCrudifyDAORepository<?>> daos = new HashMap<String, ISpringCrudifyDAORepository<?>>();
	private Map<String, ISpringCrudifyRepository<?>> repositries = new HashMap<String, ISpringCrudifyRepository<?>>();
	private Map<String, ISpringCrudifyController<?>> controllers = new HashMap<String, ISpringCrudifyController<?>>();
	private Map<String, AbstractSpringCrudifyService<?>> restServices = new HashMap<String, AbstractSpringCrudifyService<?>>();
	
	@Override
	public ISpringCrudifyDAORepository<?> getDao(String name){
		return this.daos.get(name);
	}
	
	@Override
	public ISpringCrudifyRepository<?> getRepository(String name){
		return this.repositries.get(name);
	}
	
	@Override
	public ISpringCrudifyController<?> getController(String name){
		return this.controllers.get(name);
	}
	
	@Override
	public AbstractSpringCrudifyService<?> getService(String name){
		return this.restServices.get(name);
	}
	
	@Bean
	protected List<SpringCrudifyEngineService> engineServices() throws SpringCrudifyEngineException {

		log.info("== Starting Dynamic Domain Engine ==");
		this.services = new ArrayList<SpringCrudifyEngineService>();
		
		for (String pack : this.scanPackages) {
			log.info("Scanning package "+ pack);
			
			Reflections reflections = new Reflections(pack);
			
			Set<Class<?>> entities__ = reflections.getTypesAnnotatedWith(SpringCrudifyEntity.class);

			for (Class<?> clazz : entities__) {
			
				Class<?> entityClass = clazz;
				Class<?> dtoClass = null;
				
				if (!ISpringCrudifyEntity.class.isAssignableFrom(entityClass)) {
					throw new SpringCrudifyEngineException( "The class [" + entityClass.getName() + "] must implements the ISpringCrudifyEntity interface.");
				}

				SpringCrudifyEntity entityAnnotation = clazz.getAnnotation(SpringCrudifyEntity.class);
				
				boolean authorize_creation = entityAnnotation.authorize_creation();
				boolean authorize_read_all = entityAnnotation.authorize_read_all();
				boolean authorize_read_one = entityAnnotation.authorize_read_one();
				boolean authorize_update_one = entityAnnotation.authorize_update_one();
				boolean authorize_delete_one = entityAnnotation.authorize_delete_one();
				boolean authorize_delete_all = entityAnnotation.authorize_delete_all();
				boolean authorize_count = entityAnnotation.authorize_count();
				
				try {
					dtoClass = Class.forName(entityAnnotation.dto());
				} catch (ClassNotFoundException e) {
					throw new SpringCrudifyEngineException(e);
				}
				
				if (!ISpringCrudifyDTOObject.class.isAssignableFrom(dtoClass)) {
					throw new SpringCrudifyEngineException("The class [" + dtoClass.getName() + "] must implements the ISpringCrudifyDTOObject interface.");
				}
				
				SpringCrudifyDao db = entityAnnotation.db();
				
				// Dynamic Controller
				String controller__ = entityAnnotation.controller();
				ISpringCrudifyDynamicController<?> controller = null;
				
				if( controller__ != null && !controller__.isEmpty() ) {					
					controller = (ISpringCrudifyDynamicController<?>) this.getObjectFromConfiguration(controller__, ISpringCrudifyDynamicController.class);
				}
				
				// Connector 
				String connector__ = entityAnnotation.connector();
				ISpringCrudifyConnector<?, ?> connector = null;
				
				if( connector__ != null && !connector__.isEmpty() ) {					
					connector = (ISpringCrudifyConnector<?, ?>) this.getObjectFromConfiguration(connector__, ISpringCrudifyConnector.class);
				}
				
				// Repository
				String repo__ = entityAnnotation.repository();
				ISpringCrudifyRepository<?> repo = null;
				
				if( repo__ != null && !repo__.isEmpty() ) {					
					repo = (ISpringCrudifyRepository<?>) this.getObjectFromConfiguration(repo__, ISpringCrudifyRepository.class);
				}
				
				//DAO
				String dao__ = entityAnnotation.repository();
				ISpringCrudifyDAORepository<?> dao = null;
				
				if( dao__ != null && !dao__.isEmpty() ) {					
					dao = (ISpringCrudifyDAORepository<?>) this.getObjectFromConfiguration(dao__, ISpringCrudifyDAORepository.class);
				}

				try {
					this.createDynamicDomain(services, entityClass, dtoClass, db, controller, connector, repo, dao, authorize_creation, authorize_read_all, authorize_read_one, authorize_update_one, authorize_delete_one, authorize_delete_all, authorize_count);
				} catch (NoSuchMethodException e) {
					throw new SpringCrudifyEngineException(e);
				}
			}
		}
		
		return services;
	}


	private Object getObjectFromConfiguration(String objectName, Class<?> superClass) throws SpringCrudifyEngineException {
		Object obj = null; 
		
		String[] splits = objectName.split(":");
		Class<?> objClass;
		try {
			objClass = Class.forName(splits[1]);
		} catch (ClassNotFoundException e1) {
			throw new SpringCrudifyEngineException(e1);
		}
		
		if (!superClass.isAssignableFrom(objClass)) {
			throw new SpringCrudifyEngineException("The class [" + objClass.getName() + "] must implements the ["+superClass.getCanonicalName()+"] interface.");
		}

		switch(splits[0]) {
		case "bean":
			obj = this.context.getBean(objClass);
			break;
		case "class":
			try {
				Constructor<?> ctor = objClass.getConstructor();
				obj = ctor.newInstance();
			} catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new SpringCrudifyEngineException(e);
			}
			break;
		default:
			throw new SpringCrudifyEngineException("Invalid controller "+objectName+", should be bean: or class:");
		}
		
		
		return obj;
	}

	@SuppressWarnings("unchecked")
	private void createDynamicDomain(List<SpringCrudifyEngineService> services, Class<?> entityClass, Class<?> dtoClass, SpringCrudifyDao db, ISpringCrudifyDynamicController<?> dynamicController, ISpringCrudifyConnector<?, ?> connector, ISpringCrudifyRepository<?> repo, ISpringCrudifyDAORepository<?> dao, boolean authorize_creation, boolean authorize_read_all, boolean authorize_read_one, boolean authorize_update_one, boolean authorize_delete_one, boolean authorize_delete_all, boolean authorize_count) throws NoSuchMethodException {

		log.info("Creating Dynamic Domain [Entity [{}], DTO [{}], DB [{}], authorize_creation [{}], authorize_read_all [{}], authorize_read_one [{}], authorize_update_one [{}], authorize_delete_one [{}], authorize_delete_all [{}], authorize_count [{}]]",
				entityClass.getCanonicalName(), 
				dtoClass.getCanonicalName(), 
				db,
				authorize_creation,
				authorize_read_all,
				authorize_read_one,
				authorize_update_one,
				authorize_delete_one,
				authorize_delete_all,
				authorize_count);
		
		Optional<ISpringCrudifyConnector<ISpringCrudifyEntity, List<ISpringCrudifyEntity>>> connectorObj = Optional.empty();
		
		if( dao == null ) {	
			switch(db) {
			default:
			case mongo:
				dao = new SpringCrudifyEngineMongoRepository(dtoClass, this.mongo, this.magicTenantId);
				break;
			}
		}
		
		if( repo == null ) {
			repo = new SpringCrudifyEngineRepository(entityClass, dtoClass, dao);
		} else {
			repo.setDao(dao);
		}
		
		SpringCrudifyEngineController controller = new SpringCrudifyEngineController(entityClass, (ISpringCrudifyRepository<ISpringCrudifyEntity>) repo, connectorObj, dynamicController);
		SpringCrudifyEngineService service = new SpringCrudifyEngineService(entityClass, controller, authorize_creation, authorize_read_all, authorize_read_one, authorize_update_one, authorize_delete_one, authorize_delete_all, authorize_count);

		String domain = service.getDomain();
		
		services.add(service);
		this.daos.put(domain.toLowerCase()+"_dao", dao);
		this.repositries.put(domain.toLowerCase()+"_repository", repo);
		this.controllers.put(domain.toLowerCase()+"_controller", controller);
		this.restServices.put(domain.toLowerCase()+"_service", service);
		
		String baseUrl = "/"+service.getDomain().toLowerCase();
		
		RequestMappingInfo.BuilderConfiguration options = new RequestMappingInfo.BuilderConfiguration();
		options.setPatternParser(new PathPatternParser());
		
		RequestMappingInfo requestMappingInfoGetAll = RequestMappingInfo.paths(baseUrl).methods(RequestMethod.GET).options(options).build();
		RequestMappingInfo requestMappingInfoDeleteAll = RequestMappingInfo.paths(baseUrl).methods(RequestMethod.DELETE).options(options).build();
		RequestMappingInfo requestMappingInfoCreate = RequestMappingInfo.paths(baseUrl).methods(RequestMethod.POST).options(options).build();
		RequestMappingInfo requestMappingInfoCount = RequestMappingInfo.paths(baseUrl+"/count").methods(RequestMethod.DELETE).options(options).build();
		RequestMappingInfo requestMappingInfoGetOne = RequestMappingInfo.paths(baseUrl+"/{uuid}").methods(RequestMethod.GET).options(options).build();
		RequestMappingInfo requestMappingInfoUpdate = RequestMappingInfo.paths(baseUrl+"/{uuid}").methods(RequestMethod.PATCH).options(options).build();
		RequestMappingInfo requestMappingInfoDeleteOne = RequestMappingInfo.paths(baseUrl+"/{uuid}").methods(RequestMethod.DELETE).options(options).build();
		
		this.requestMappingHandlerMapping.registerMapping(requestMappingInfoGetAll, service, SpringCrudifyEngineService.class.getMethod("getEntities", String.class, SpringCrudifyReadOutputMode.class, Integer.class, Integer.class, String.class, String.class));
		this.requestMappingHandlerMapping.registerMapping(requestMappingInfoDeleteAll, service, SpringCrudifyEngineService.class.getMethod("deleteAll", String.class));
		this.requestMappingHandlerMapping.registerMapping(requestMappingInfoCreate, service, SpringCrudifyEngineService.class.getMethod("createEntity", String.class, String.class));
		this.requestMappingHandlerMapping.registerMapping(requestMappingInfoCount, service, SpringCrudifyEngineService.class.getMethod("getCount", String.class));
		this.requestMappingHandlerMapping.registerMapping(requestMappingInfoGetOne, service, SpringCrudifyEngineService.class.getMethod("getEntity", String.class, String.class));
		this.requestMappingHandlerMapping.registerMapping(requestMappingInfoUpdate, service, SpringCrudifyEngineService.class.getMethod("updateEntity", String.class, String.class, String.class));
		this.requestMappingHandlerMapping.registerMapping(requestMappingInfoDeleteOne, service, SpringCrudifyEngineService.class.getMethod("deleteEntity", String.class, String.class));
		
		return;
	}

}