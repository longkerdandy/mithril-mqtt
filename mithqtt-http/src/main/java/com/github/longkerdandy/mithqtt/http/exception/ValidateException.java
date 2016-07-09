package com.github.longkerdandy.mithqtt.http.exception;

import com.github.longkerdandy.mithqtt.http.entity.ErrorEntity;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

/**
 * Validate Exception
 */
public class ValidateException extends WebApplicationException {

	private static final long serialVersionUID = -2897180088846699127L;

	/**
     * Create a HTTP 422 (UnProcessable Entity) exception.
     */
    public ValidateException() {
        super(422);
    }

    /**
     * Create a HTTP 422 (UnProcessable Entity) exception.
     *
     * @param entity the error response entity
     */
    @SuppressWarnings("rawtypes")
	public ValidateException(ErrorEntity entity) {
        super(Response.status(422).entity(entity).type("application/json").build());
    }
}