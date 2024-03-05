package ch.ubique.eiam

import android.app.Application
import kotlinx.coroutines.MainScope

class EiamApplication : Application() {
	companion object {
		val applicationScope = MainScope()
	}
}