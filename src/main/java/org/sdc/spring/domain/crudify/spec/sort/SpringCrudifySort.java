package org.sdc.spring.domain.crudify.spec.sort;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpringCrudifySort {
	
	@JsonInclude(Include.NON_NULL)
	private String fieldName;

	@JsonInclude(Include.NON_NULL)
	private SpringCrudifySortDirection direction;

}
