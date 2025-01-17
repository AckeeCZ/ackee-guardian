package io.github.ackeecz.guardian.core.internal

import android.content.Context
import com.google.crypto.tink.KeyTemplate
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * Decorator of [AndroidKeysetManager.Builder] that synchronizes Android KeyStore operations using
 * [semaphore].
 */
public class AndroidKeysetManagerSynchronizedBuilder(private val semaphore: Semaphore) {

    private val delegate = AndroidKeysetManager.Builder()

    public fun withKeyTemplate(template: KeyTemplate): AndroidKeysetManagerSynchronizedBuilder = apply {
        delegate.withKeyTemplate(template)
    }

    public fun withSharedPref(
        context: Context,
        keysetName: String,
        prefFileName: String,
    ): AndroidKeysetManagerSynchronizedBuilder = apply {
        delegate.withSharedPref(context, keysetName, prefFileName)
    }

    public fun withMasterKeyUri(uri: String): AndroidKeysetManagerSynchronizedBuilder = apply {
        delegate.withMasterKeyUri(uri)
    }

    public suspend fun build(): AndroidKeysetManager {
        return semaphore.withPermit { delegate.build() }
    }
}
