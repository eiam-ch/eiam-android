package ch.ubique.eiam.auth.exception

import java.io.IOException

/**
 * An exception thrown by the [ch.ubique.auth.network.AuthorizationHeaderInterceptor] when an authorized request is executed without the proper AuthState
 */
class UnauthorizedException : IOException()