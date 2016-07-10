package com.github.longkerdandy.mithqtt.http.exception;

import com.github.longkerdandy.mithqtt.http.entity.ErrorEntity;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

/**
 * Authorize Exception
 */
public class AuthorizeException extends WebApplicationException {

	private static final long serialVersionUID = -8336814815115724806L;

	/**
     * Create a HTTP 401 (Unauthorized) exception.
     */
    public AuthorizeException() {
        super(401);
    }

    /**
     * Create a HTTP 401 (Unauthorized) exception.
     *
     * @param entity the error response entity
     */
    @SuppressWarnings("rawtypes") 
    public AuthorizeException(ErrorEntity entity) {
        super(Response.status(401).entity(entity).type("application/json").build());
    }
}