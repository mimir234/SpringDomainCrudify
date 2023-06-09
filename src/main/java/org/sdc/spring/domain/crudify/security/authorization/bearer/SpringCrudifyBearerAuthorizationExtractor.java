package org.sdc.spring.domain.crudify.security.authorization.bearer;

import java.io.IOException;

import org.sdc.spring.domain.crudify.security.authentication.dao.AbstractSpringCrudifyUserDetails;
import org.sdc.spring.domain.crudify.security.authentication.dao.ISpringCrudifyAuthenticationUserMapper;
import org.sdc.spring.domain.crudify.security.authorization.ISpringCrudifyAuthorizationProvider;
import org.sdc.spring.domain.crudify.security.authorization.token.jwt.SpringCrudifyJwtTokenProvider;
import org.sdc.spring.domain.crudify.security.keys.SpringCrudifyKeyExpiredException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class SpringCrudifyBearerAuthorizationExtractor extends OncePerRequestFilter {
	
	private ISpringCrudifyAuthorizationProvider authorizationProvider;
	
	private ISpringCrudifyAuthenticationUserMapper userMapper;
	
	private String extractUserId;
	
	@Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        String token = null;
        String username = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            try {
				username = this.authorizationProvider.getUserNameFromAuthorization(token);
			} catch (SpringCrudifyKeyExpiredException e) {
				throw new IOException(e);
			}
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
        	AbstractSpringCrudifyUserDetails userDetails = (AbstractSpringCrudifyUserDetails) this.userMapper.loadUserByUsername(username);
            try {
				if (this.authorizationProvider.validateAuthorization(token, userDetails)) {
				    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
				    
				    request.setAttribute("tenantId", userDetails.getTenantId());
				    
				    if( this.extractUserId != null && !this.extractUserId.isEmpty() && this.extractUserId.equals("enabled")) {
				    	request.setAttribute("userId", userDetails.getUuid());
				    }
				    			    
				    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
				    
				    SecurityContextHolder.getContext().setAuthentication(authToken);
				}
			} catch (SpringCrudifyKeyExpiredException e) {
				throw new IOException(e);
			}
        }
        filterChain.doFilter(request, response);
    }

}
