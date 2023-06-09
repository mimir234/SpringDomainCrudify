package org.sdc.spring.domain.crudify.security.authentication;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;

public interface ISpringCrudifyAuthenticationManager {

	HttpSecurity configureFilterChain(HttpSecurity http) throws ISpringCrudifySecurityException;

}
