/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This file is based on the original EncryptedFile from Jetpack Security Crypto library
 * https://developer.android.com/reference/kotlin/androidx/security/crypto/EncryptedFile
 */
package io.github.ackeecz.guardian.jetpack

import android.content.Context
import com.google.crypto.tink.KeyTemplate
import com.google.crypto.tink.StreamingAead
import com.google.crypto.tink.streamingaead.AesGcmHkdfStreamingKeyManager
import com.google.crypto.tink.streamingaead.StreamingAeadConfig
import io.github.ackeecz.guardian.core.MasterKey
import io.github.ackeecz.guardian.core.internal.AndroidKeysetManagerSynchronizedBuilder
import io.github.ackeecz.guardian.core.internal.SynchronizedDataHolder
import io.github.ackeecz.guardian.core.keystore.android.AndroidKeyStoreSemaphore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.channels.FileChannel
import java.security.GeneralSecurityException
import java.security.KeyStore

/**
 * Class used to create and read encrypted files.
 *
 * All Android [KeyStore] operations are synchronized using a provided [Semaphore].
 * More info about this topic can be found in [AndroidKeyStoreSemaphore] documentation.
 *
 * **WARNING**: The encrypted file should not be backed up with Auto Backup. When restoring the
 * file it is likely the key used to encrypt it will no longer be present. You should exclude all
 * [EncryptedFile]s from backup using
 * [backup rules](https://developer.android.com/guide/topics/data/autobackup#IncludingFiles).
 *
 * Be aware that if you are not explicitly calling [EncryptedFile.Builder.setKeysetPrefName] there
 * is also a silently-created default preferences file created at
 * [Context].getFilesDir().getParent() + "/shared_prefs/__androidx_security_crypto_encrypted_file_pref__"
 *
 * This preferences file (or any others created with a custom specified location) also should be
 * excluded from backups.
 *
 * Basic use of the class:
 * ```
 * val getMasterKey = suspend { MasterKey.getOrCreate() }
 * val file = File(context.filesDir, "secret_data")
 * val encryptedFile = EncryptedFile.Builder(
 *     file = file,
 *     context = context,
 *     encryptionScheme = EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB,
 *     getMasterKey = getMasterKey,
 * ).build()
 * // Write to the encrypted file
 * val encryptedOutputStream = encryptedFile.openFileOutput()
 * // Read the encrypted file
 * val encryptedInputStream = encryptedFile.openFileInput()
 * ```
 */
public class EncryptedFile private constructor(private val fileBuilder: Builder) {

    private val file = fileBuilder.file
    private val associatedData = file.name.toByteArray(Charsets.UTF_8)
    private val streamingAeadHolder = StreamingAeadHolder()

    /**
     * Opens a [FileOutputStream] for writing that automatically encrypts the data based on the
     * provided settings.
     *
     * Please ensure that the same [MasterKey] and keyset are used to decrypt or it will cause
     * failures.
     *
     * @return The [FileOutputStream] that encrypts all data
     * @throws GeneralSecurityException when a bad [MasterKey] or keyset has been used
     * @throws IOException when the file already exists or is not available for writing
     */
    @Throws(GeneralSecurityException::class, IOException::class)
    public suspend fun openFileOutput(): FileOutputStream {
        if (file.exists()) {
            throw IOException("Output file already exists, please use a new file: ${file.name}")
        }
        val fileOutputStream = FileOutputStream(file)
        val streamingAead = streamingAeadHolder.getOrCreate()
        val encryptingStream = streamingAead.newEncryptingStream(fileOutputStream, associatedData)
        return EncryptedFileOutputStream(fileOutputStream.fd, encryptingStream)
    }

    /**
     * Opens a [FileInputStream] that reads encrypted files based on the previous settings.
     *
     * Please ensure that the same [MasterKey] and keyset are used to decrypt or it will cause
     * failures.
     *
     * @return The [FileInputStream] to read previously encrypted data.
     * @throws GeneralSecurityException when a bad [MasterKey] or keyset has been used
     * @throws FileNotFoundException when the file was not found
     * @throws IOException when other I/O errors occur
     */
    @Throws(GeneralSecurityException::class, IOException::class, FileNotFoundException::class)
    public suspend fun openFileInput(): FileInputStream {
        if (!file.exists()) {
            throw FileNotFoundException("File doesn't exist: ${file.name}")
        }
        val fileInputStream = FileInputStream(file)
        val streamingAead = streamingAeadHolder.getOrCreate()
        val decryptingStream = streamingAead.newDecryptingStream(fileInputStream, associatedData)
        return EncryptedFileInputStream(fileInputStream.fd, decryptingStream)
    }

    private inner class StreamingAeadHolder : SynchronizedDataHolder<StreamingAead>() {

        override suspend fun createSynchronizedData(): StreamingAead = withContext(fileBuilder.backgroundDispatcher) {
            StreamingAeadConfig.register()
            return@withContext AndroidKeysetManagerSynchronizedBuilder(fileBuilder.keyStoreSemaphore)
                .withKeyTemplate(fileBuilder.encryptionScheme.keyTemplate)
                .withSharedPref(
                    fileBuilder.context,
                    fileBuilder.keysetAlias,
                    fileBuilder.keysetPrefsFileName,
                )
                .withMasterKeyUri(fileBuilder.getMasterKey().keyStoreUri)
                .build()
                .keysetHandle
                .getPrimitive(StreamingAead::class.java)
        }
    }

    private class EncryptedFileOutputStream(
        descriptor: FileDescriptor?,
        private val encryptedOutputStream: OutputStream,
    ) : FileOutputStream(descriptor) {

        override fun write(b: ByteArray) {
            encryptedOutputStream.write(b)
        }

        override fun write(b: Int) {
            encryptedOutputStream.write(b)
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            encryptedOutputStream.write(b, off, len)
        }

        override fun close() {
            encryptedOutputStream.close()
        }

        override fun getChannel(): FileChannel {
            throw UnsupportedOperationException(
                "For encrypted files, please open the relevant FileInput/FileOutputStream."
            )
        }

        override fun flush() {
            encryptedOutputStream.flush()
        }
    }

    private class EncryptedFileInputStream(
        descriptor: FileDescriptor?,
        private val encryptedInputStream: InputStream,
    ) : FileInputStream(descriptor) {

        override fun read() = encryptedInputStream.read()

        override fun read(b: ByteArray) = encryptedInputStream.read(b)

        override fun read(b: ByteArray, off: Int, len: Int) = encryptedInputStream.read(b, off, len)

        override fun skip(n: Long) = encryptedInputStream.skip(n)

        override fun available() = encryptedInputStream.available()

        override fun close() {
            encryptedInputStream.close()
        }

        override fun getChannel(): FileChannel {
            throw UnsupportedOperationException(
                "For encrypted files, please open the relevant FileInput/FileOutputStream."
            )
        }

        @Synchronized
        override fun mark(readLimit: Int) {
            encryptedInputStream.mark(readLimit)
        }

        @Synchronized
        override fun reset() {
            encryptedInputStream.reset()
        }

        override fun markSupported() = encryptedInputStream.markSupported()
    }

    private companion object {

        private const val KEYSET_PREFS_FILE_NAME = "__androidx_security_crypto_encrypted_file_pref__"
        private const val KEYSET_ALIAS = "__androidx_security_crypto_encrypted_file_keyset__"
    }

    public class Builder public constructor(
        internal val file: File,
        context: Context,
        internal val encryptionScheme: FileEncryptionScheme,
        internal val getMasterKey: suspend () -> MasterKey,
    ) {

        internal val context = context.applicationContext
        internal var keysetPrefsFileName = KEYSET_PREFS_FILE_NAME
        internal var keysetAlias = KEYSET_ALIAS
        internal var backgroundDispatcher = Dispatchers.IO
        internal var keyStoreSemaphore: Semaphore = AndroidKeyStoreSemaphore

        /**
         * @param keysetPrefName The SharedPreferences file to store the keyset.
         * @return This Builder
         */
        public fun setKeysetPrefName(keysetPrefName: String): Builder {
            this.keysetPrefsFileName = keysetPrefName
            return this
        }

        /**
         * @param keysetAlias The alias in the SharedPreferences file to store the keyset. The
         * keyset holds the data encryption key that is used to encrypt/decrypt the file.
         *
         * @return This Builder
         */
        public fun setKeysetAlias(keysetAlias: String): Builder {
            this.keysetAlias = keysetAlias
            return this
        }

        /**
         * Sets [dispatcher] to be used for heavy operations that should be run on "background"
         * dispatcher.
         */
        public fun setBackgroundDispatcher(dispatcher: CoroutineDispatcher): Builder {
            backgroundDispatcher = dispatcher
            return this
        }

        /**
         * Sets [semaphore] used to synchronize Android [KeyStore] operations. It is recommended to
         * use a default [AndroidKeyStoreSemaphore], if you really don't need to provide a custom
         * [Semaphore].
         */
        public fun setKeyStoreSemaphore(semaphore: Semaphore): Builder {
            keyStoreSemaphore = semaphore
            return this
        }

        /**
         * Builds [EncryptedFile] that will be encrypted/decrypted by the generated keyset stored
         * in the shared preferences. The keyset is encrypted by the [MasterKey] provided by
         * [getMasterKey]. However, the underlying Tink library performs an Android KeyStore check
         * and if it shows as unreliable on the particular device, Tink does not use the
         * [MasterKey] and saves generated keyset in plain text instead of failing the operation.
         */
        public fun build(): EncryptedFile = EncryptedFile(this)
    }

    /**
     * The encryption scheme to encrypt files. The file content is encrypted using [StreamingAead] with
     * AES-GCM, with the file name as associated data.
     */
    public enum class FileEncryptionScheme {

        /**
         * This encryption scheme is generally better suited for smaller files.
         * If you expect your file to be in a range of a few KBs up to 1 MB or a few MBs max, then
         * this is probably the best option.
         */
        AES256_GCM_HKDF_4KB {

            override val keyTemplate: KeyTemplate = AesGcmHkdfStreamingKeyManager.aes256GcmHkdf4KBTemplate()
        },

        /**
         * This encryption scheme is generally better suited for bigger files.
         * If you expect your file to have a size of a few or a lot of MBs, then this is probably
         * the best option.
         */
        AES256_GCM_HKDF_1MB {

            override val keyTemplate: KeyTemplate = AesGcmHkdfStreamingKeyManager.aes256GcmHkdf1MBTemplate()
        };

        // Property declared outside of the primary constructor like this intentionally to keep
        // the property internal and not public
        internal abstract val keyTemplate: KeyTemplate
    }
}
