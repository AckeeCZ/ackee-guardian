package io.github.ackeecz.guardian.jetpack

import android.content.SharedPreferences
import androidx.annotation.VisibleForTesting
import io.github.ackeecz.guardian.core.internal.WeakReferenceFactory
import kotlinx.coroutines.runBlocking
import java.lang.ref.WeakReference
import kotlin.apply

private typealias PrefsListener = SharedPreferences.OnSharedPreferenceChangeListener
private typealias EncryptedPrefsListener = EncryptedSharedPreferences.OnSharedPreferenceChangeListener

internal class EncryptedSharedPreferencesAdapter @VisibleForTesting internal constructor(
    private val adaptee: EncryptedSharedPreferences,
    private val weakReferenceFactory: WeakReferenceFactory,
) : SharedPreferences {

    private val listeners: MutableMap<PrefsListener, WeakReference<EncryptedPrefsListener>> = mutableMapOf()

    constructor(adaptee: EncryptedSharedPreferences) : this(
        adaptee = adaptee,
        weakReferenceFactory = WeakReferenceFactory(),
    )

    override fun getAll(): Map<String?, *> = withCoroutine {
        adaptee.getAll()
    }

    override fun getString(key: String?, defValue: String?): String? = withCoroutine {
        adaptee.getString(key, defValue)
    }

    override fun getStringSet(key: String?, defValues: Set<String?>?): Set<String?>? = withCoroutine {
        adaptee.getStringSet(key, defValues)
    }

    override fun getInt(key: String?, defValue: Int): Int = withCoroutine {
        adaptee.getInt(key, defValue)
    }

    override fun getLong(key: String?, defValue: Long): Long = withCoroutine {
        adaptee.getLong(key, defValue)
    }

    override fun getFloat(key: String?, defValue: Float): Float = withCoroutine {
        adaptee.getFloat(key, defValue)
    }

    override fun getBoolean(key: String?, defValue: Boolean): Boolean = withCoroutine {
        adaptee.getBoolean(key, defValue)
    }

    override fun contains(key: String?): Boolean = withCoroutine {
        adaptee.contains(key)
    }

    override fun edit(): SharedPreferences.Editor = Editor(adaptee.edit())

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
        if (listener == null) return

        val encryptedPrefsListener = EncryptedPrefsListener { _, key ->
            listener.onSharedPreferenceChanged(this, key)
        }
        listeners[listener] = weakReferenceFactory.create(encryptedPrefsListener)
        adaptee.registerOnSharedPreferenceChangeListener(encryptedPrefsListener)
    }

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
        if (listener == null) return

        listeners.remove(listener)?.let { weakEncryptedPrefsListener ->
            val encryptedPrefsListener = weakEncryptedPrefsListener.get()
            if (encryptedPrefsListener != null) {
                adaptee.unregisterOnSharedPreferenceChangeListener(encryptedPrefsListener)
            }
        }
    }

    private class Editor(private val adaptee: EncryptedSharedPreferences.Editor) : SharedPreferences.Editor {

        override fun putString(key: String?, value: String?) = applyWithCoroutine {
            adaptee.putString(key, value)
        }

        private fun <T : Any?> applyWithCoroutine(block: suspend () -> T) = apply { withCoroutine(block) }

        override fun putStringSet(key: String?, values: Set<String?>?) = applyWithCoroutine {
            adaptee.putStringSet(key, values)
        }

        override fun putInt(key: String?, value: Int) = applyWithCoroutine {
            adaptee.putInt(key, value)
        }

        override fun putLong(key: String?, value: Long) = applyWithCoroutine {
            adaptee.putLong(key, value)
        }

        override fun putFloat(key: String?, value: Float) = applyWithCoroutine {
            adaptee.putFloat(key, value)
        }

        override fun putBoolean(key: String?, value: Boolean) = applyWithCoroutine {
            adaptee.putBoolean(key, value)
        }

        override fun remove(key: String?) = applyWithCoroutine {
            adaptee.remove(key)
        }

        override fun clear() = apply {
            adaptee.clear()
        }

        override fun commit(): Boolean = withCoroutine {
            adaptee.commit()
        }

        override fun apply() = withCoroutine {
            adaptee.apply()
        }
    }
}

private fun <T : Any?> withCoroutine(block: suspend () -> T): T {
    return runBlocking { block() }
}

private const val DEPRECATION_MESSAGE = "Using this adapter is highly discouraged. " +
    "Prefer EncryptedSharedPreferences directly when possible to avoid blocking of the thread."

/**
 * Adapts [EncryptedSharedPreferences] to [SharedPreferences] interface. It is highly discouraged
 * to use this adapter, because [EncryptedSharedPreferences] contains heavy suspend operations that
 * do not block thread when called directly from a coroutine, but can block thread when called
 * using this adapter, because it has to block the thread to wait for a wrapped suspend operation
 * to finish. You should always prefer [EncryptedSharedPreferences] interface to avoid blocking a
 * thread and this adapter should be used only in situations, when you can't easily migrate from
 * [SharedPreferences] to [EncryptedSharedPreferences] interface, such as:
 * - temporary usage to ease migration from Jetpack Security EncryptedSharedPreferences to Ackee
 * [EncryptedSharedPreferences]
 * - if you need to pass [EncryptedSharedPreferences] to the code you can't change
 * (such as third-party library) and it requires [SharedPreferences] interface
 */
@Suppress("DeprecatedCallableAddReplaceWith")
@Deprecated(message = DEPRECATION_MESSAGE)
public fun EncryptedSharedPreferences.adaptToSharedPreferences(): SharedPreferences {
    return EncryptedSharedPreferencesAdapter(this)
}
