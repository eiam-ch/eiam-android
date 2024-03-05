package ch.ubique.eiam.auth.data

/**
 * Configuration data for the IDP
 */
data class IdpConfig(
	val idpDiscoveryUrl: String,
	val idpRedirectUrl: String,
	val idpClientId: String,
	val idpClientSecret: String? = null,
	val additionalOidcScopes: List<String> = emptyList(),
	val idpPromptParameter: String = "login",
)
