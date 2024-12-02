package io.github.ackeecz.security.core.junit.rule

import android.security.keystore.KeyGenParameterSpec
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.junit.rules.ExternalResource
import java.io.InputStream
import java.io.OutputStream
import java.math.BigInteger
import java.security.Key
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyPairGeneratorSpi
import java.security.KeyStore
import java.security.KeyStoreSpi
import java.security.Provider
import java.security.SecureRandom
import java.security.Security
import java.security.cert.Certificate
import java.security.cert.X509Certificate
import java.security.spec.AlgorithmParameterSpec
import java.util.Collections
import java.util.Date
import java.util.Enumeration
import javax.crypto.KeyGenerator
import javax.crypto.KeyGeneratorSpi
import javax.crypto.SecretKey

class AndroidFakeKeyStoreRule : ExternalResource() {

    override fun before() {
        AndroidKeyStoreProvider.setUp()
    }

    override fun after() {
        AndroidKeyStoreProvider.tearDown()
    }

    /**
     * Returns all key generations for the [alias] during this test execution
     */
    fun getKeyHistory(alias: String): List<KeyStore.Entry> {
        return AndroidKeyStoreProvider.getKeyHistory(alias)
    }
}

private class AndroidKeyStoreProvider private constructor() : Provider(NAME, 1.0, "") {

    init {
        put("KeyStore.AndroidKeyStore", AndroidKeyStore::class.java.name)
        put("KeyGenerator.AES", AesKeyGenerator::class.java.name)
        put("KeyPairGenerator.EC", ECKeyPairGenerator::class.java.name)
    }

    class AndroidKeyStore : KeyStoreSpi() {

        override fun engineIsKeyEntry(alias: String?): Boolean = wrapped.isKeyEntry(alias)

        override fun engineIsCertificateEntry(alias: String?): Boolean = wrapped.isCertificateEntry(alias)

        override fun engineGetCertificate(alias: String?): Certificate = wrapped.getCertificate(alias)

        override fun engineGetCreationDate(alias: String?): Date = wrapped.getCreationDate(alias)

        override fun engineDeleteEntry(alias: String) {
            keyStorage.remove(alias)
        }

        override fun engineSetKeyEntry(
            alias: String?,
            key: Key?,
            password: CharArray?,
            chain: Array<out Certificate>?,
        ) {
            wrapped.setKeyEntry(alias, key, password, chain)
        }

        override fun engineSetKeyEntry(
            alias: String?,
            key: ByteArray?,
            chain: Array<out Certificate>?,
        ) {
            wrapped.setKeyEntry(alias, key, chain)
        }

        override fun engineStore(
            stream: OutputStream?,
            password: CharArray?,
        ) {
            wrapped.store(stream, password)
        }

        override fun engineSize(): Int = wrapped.size()

        override fun engineAliases(): Enumeration<String> = Collections.enumeration(keyStorage.keyAliases)

        override fun engineContainsAlias(alias: String?): Boolean = keyStorage.keyAliases.contains(alias)

        override fun engineLoad(
            stream: InputStream?,
            password: CharArray?,
        ) {
            wrapped.load(stream, password)
        }

        override fun engineGetCertificateChain(
            alias: String?,
        ): Array<Certificate>? = wrapped.getCertificateChain(alias)

        override fun engineSetCertificateEntry(
            alias: String?,
            cert: Certificate?,
        ) {
            wrapped.setCertificateEntry(alias, cert)
        }

        override fun engineGetCertificateAlias(
            cert: Certificate?,
        ): String? = wrapped.getCertificateAlias(cert)

        override fun engineGetKey(
            alias: String,
            password: CharArray?,
        ): Key? = (keyStorage.get(alias) as? KeyStore.SecretKeyEntry)?.secretKey

        override fun engineGetEntry(
            p0: String,
            p1: KeyStore.ProtectionParameter?,
        ): KeyStore.Entry? = keyStorage.get(p0)

        override fun engineSetEntry(
            p0: String,
            p1: KeyStore.Entry,
            p2: KeyStore.ProtectionParameter?,
        ) {
            keyStorage.set(p0, p1)
        }

        override fun engineLoad(p0: KeyStore.LoadStoreParameter?) {
            wrapped.load(p0)
        }

        override fun engineStore(p0: KeyStore.LoadStoreParameter?) {
            wrapped.store(p0)
        }

        override fun engineEntryInstanceOf(
            p0: String?,
            p1: Class<out KeyStore.Entry>?,
        ): Boolean = wrapped.entryInstanceOf(p0, p1)

        internal companion object {

            private val wrapped = KeyStore.getInstance("JKS")
        }
    }

    class AesKeyGenerator : KeyGeneratorSpi() {

        private val wrapped = KeyGenerator.getInstance("AES")
        private var lastSpec: KeyGenParameterSpec? = null

        override fun engineInit(random: SecureRandom?) {
            wrapped.init(random)
        }

        override fun engineInit(params: AlgorithmParameterSpec?, random: SecureRandom?) {
            wrapped.init(random).also {
                lastSpec = params as KeyGenParameterSpec
            }
        }

        override fun engineInit(keysize: Int, random: SecureRandom?) {
            wrapped.init(keysize, random)
        }

        override fun engineGenerateKey(): SecretKey = wrapped.generateKey().also {
            keyStorage.set(lastSpec!!.keystoreAlias, KeyStore.SecretKeyEntry(it))
        }
    }

    class ECKeyPairGenerator : KeyPairGeneratorSpi() {

        private val wrapped = KeyPairGenerator.getInstance("EC")

        private var lastSpec: KeyGenParameterSpec? = null

        override fun generateKeyPair(): KeyPair = wrapped.generateKeyPair().also { keyPair ->
            keyStorage.set(
                alias = lastSpec!!.keystoreAlias,
                entry = KeyStore.PrivateKeyEntry(
                    keyPair.private,
                    arrayOf(keyPair.toCertificate()),
                ),
            )
        }

        @Suppress("MagicNumber")
        private fun KeyPair.toCertificate(): X509Certificate? {
            val from = Date()
            val to = Date(from.time + 365L * 1000L * 24L * 60L * 60L)

            val serialNumber = BigInteger(64, SecureRandom())
            val owner = X500Name("cn=Unknown")

            val sigGen = JcaContentSignerBuilder("SHA256withECDSA").build(private)

            val holder = X509v3CertificateBuilder(
                owner,
                serialNumber,
                from,
                to,
                owner,
                SubjectPublicKeyInfo.getInstance(public.encoded)
            ).build(sigGen)

            return JcaX509CertificateConverter().getCertificate(holder)
        }

        override fun initialize(p0: Int, p1: SecureRandom?): Unit = Unit

        override fun initialize(p0: AlgorithmParameterSpec?, p1: SecureRandom?) {
            lastSpec = p0 as KeyGenParameterSpec
        }
    }

    companion object {

        private const val NAME = "AndroidKeyStore"
        private var keyStorage = KeyStorage()

        fun setUp() {
            Security.addProvider(AndroidKeyStoreProvider())
        }

        fun tearDown() {
            keyStorage = KeyStorage()
            Security.removeProvider(NAME)
        }

        fun getKeyHistory(alias: String) = keyStorage.getKeyHistory(alias)
    }
}

private class KeyStorage {

    private val aliasToKeyMap = mutableMapOf<String, KeyStore.Entry>()

    val keyAliases get() = aliasToKeyMap.keys

    /**
     * Tracks history of all key generations for a specific key alias
     */
    private val aliasToKeyHistoryMap = mutableMapOf<String, List<KeyStore.Entry>>()

    fun get(alias: String) = aliasToKeyMap[alias]

    fun set(alias: String, entry: KeyStore.Entry) {
        aliasToKeyMap[alias] = entry
        aliasToKeyHistoryMap[alias] = aliasToKeyHistoryMap[alias].orEmpty() + entry
    }

    fun remove(alias: String) {
        aliasToKeyMap.remove(alias)
    }

    fun getKeyHistory(alias: String) = aliasToKeyHistoryMap[alias].orEmpty()
}
