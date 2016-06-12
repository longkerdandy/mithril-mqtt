package com.github.longkerdandy.mithqtt.api.auth;

/**
 * OAuth Authenticator
 */
@SuppressWarnings("unused")
public interface OAuthAuthenticator  extends Authenticator {

    /**
     * OAuth in HTTP request
     *
     * @param credentials OAuth2 bearer-token
     * @return User Name
     */
    String oauth(String credentials);

}
