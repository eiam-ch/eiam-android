package ch.ubique.eiam.auth.exception

import java.io.IOException

/**
 * An exception thrown by the [ch.ubique.auth.network.AuthorizationHeaderInterceptor] when the IDP indicates an expired session
 */
class SessionExpiredException : IOException()