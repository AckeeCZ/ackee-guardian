package io.github.ackeecz.guardian.core.internal

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.ackeecz.guardian.core.internal.junit.rule.AndroidFakeKeyStoreRule
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * This class relies on the runtime Java vendor not to check the signature of the provider. If
 * you're getting this exception:
 *
 * java.security.NoSuchProviderException: JCE cannot authenticate the provider AndroidKeyStore
 *
 * Then switch to a different Java vendor.
 * Known vendors that don't work:
 *      - Oracle JDK
 * Known vendors that work:
 *      - JetBrains Runtime
 *      - Corretto
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.R], shadows = [ShadowSharedPreferences::class])
public abstract class AndroidTestWithKeyStore {

    @get:Rule
    public val fakeKeyStoreRule: AndroidFakeKeyStoreRule = AndroidFakeKeyStoreRule()
}
