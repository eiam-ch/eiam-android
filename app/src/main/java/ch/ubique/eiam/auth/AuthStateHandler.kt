package ch.ubique.eiam.auth

interface AuthStateHandler {
	/**
	 * Notify the handler that the session has expired
	 */
	fun sessionHasExpired()
}