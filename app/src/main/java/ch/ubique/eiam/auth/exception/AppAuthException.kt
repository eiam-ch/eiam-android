package ch.ubique.eiam.auth.exception

import net.openid.appauth.AuthorizationException
import java.io.IOException

/**
 * Wrapper around [AuthorizationException] that extends from [IOException], so that an OkHttp interceptor can throw it properly
 * See: https://github.com/square/retrofit/issues/3505 and https://github.com/square/okhttp/issues/5151
 */
class AppAuthException(
	val authorizationException: AuthorizationException
) : IOException(authorizationException.message, authorizationException)