package io.github.ackeecz.guardian.core.internal

import android.content.SharedPreferences
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.RealObject
import org.robolectric.shadow.api.Shadow
import org.robolectric.util.ReflectionHelpers

@Implements(className = "android.app.SharedPreferencesImpl")
@Suppress("ProtectedMemberInFinalClass", "ProtectedInFinal")
public class ShadowSharedPreferences {

    @RealObject
    private lateinit var realPrefs: SharedPreferences

    private val getCallCount: MutableMap<String?, Int> = mutableMapOf()

    public fun getGetCallCount(key: String?): Int = getCallCount[key] ?: 0

    @Implementation
    protected fun getString(key: String?, defValue: String?): String? {
        incrementCallCount(key)
        return Shadow.directlyOn<String?>(
            realPrefs,
            "android.app.SharedPreferencesImpl",
            "getString",
            ReflectionHelpers.ClassParameter.from(String::class.java, key),
            ReflectionHelpers.ClassParameter.from(String::class.java, defValue),
        )
    }

    private fun incrementCallCount(key: String?) {
        val current = getCallCount[key] ?: 0
        getCallCount[key] = current + 1
    }
}
