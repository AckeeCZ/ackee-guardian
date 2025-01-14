# Ackee Guardian

[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.ackeecz/guardian-bom)](https://central.sonatype.com/artifact/io.github.ackeecz/guardian-bom)

## Overview

The galaxy has the Guardians of the Galaxy to fend off galactic threats and save us from cosmic chaos. 
But who’s watching over your apps, shielding them from malicious attackers and software mischief?

Enter **Ackee Guardian** – your app's fearless defender, standing strong against the dark forces of bugs, 
vulnerabilities, and threats. Just as the Guardians of the Galaxy protect the universe, Ackee Guardian 
ensures your Android app stays secure and your users stay safe.

Ackee Guardian focuses on cryptography and in [Ackee](https://www.ackee.cz/) 
we use it to share some common cryptographic implementations across our projects, but it 
contains useful logic suited for anyone's needs. More specifically, you can use it as a 100% 
compatible replacement for [Jetpack Security Crypto](https://developer.android.com/reference/kotlin/androidx/security/crypto/package-summary) 
library and there is more!

## Architecture

Library consists of several modules:
- `core` contains some basic core logic like `MasterKey` class that is being used by other modules of the library
- `datastore` provides encrypted `DataStore` implementation
- `datastore-preferences` provides encrypted `PreferenceDataStore` implementation
- `jetpack` is a rewritten and improved Jetpack Security library

### Core

Contains basic core cryptographic logic like `MasterKey` class (rewritten from Jetpack Security)
that is used by other modules to encrypt the data. You don't have to depend on this module directly,
if you use `datastore` modules or `jetpack`.

#### Android KeyStore synchronization

Android KeyStore is not thread-safe and its operations must be synchronized to avoid errors on various
range of devices. Ackee Guardian synchronizes all Android KeyStore operations performed under the hood
using a single `AndroidKeyStoreSemaphore` object.

It is important to know that you need to synchronize all KeyStore operations, not only those using 
`KeyStore` class, but even all others using various classes from JCA that are backed-up by 
AndroidKeyStore provider implementation. This includes e.g. `KeyGenerator` for key generation in 
Android KeyStore or `Cipher` for encryption/decryption using keys stored in Android KeyStore. 
These operations have to be synchronized across your whole app, so even though Ackee Guardian 
synchronizes operations under the hood, you need to synchronize your custom operations involving 
Android KeyStore together with those in Ackee Guardian. Guardian already provides some abstractions 
over JCA APIs backed by AndroidKeyStore provider, that are properly synchronized like 
`SynchronizedAndroidKeyStore` or `SynchronizedAndroidKeyGenerator`, that you can use without any 
other synchronization code. However, not all JCA APIs are covered or maybe you can't use provided 
abstractions for some reason. In these cases, the simplest way to synchronize everything correctly 
is to wrap all your Android KeyStore operations in the `AndroidKeyStoreSemaphore.withPermit` calls.

There is more options how you can approach the synchronization using Ackee Guardian, which are
discussed in more detail in `AndroidKeyStoreSemaphore` documentation, that also provides more
information about this topic and implementation in Guardian.

### DataStore

DataStore modules provide an encrypted version of `DataStore`s. They use [Tink](https://github.com/tink-crypto/tink-java)
library for cryptographic algorithms under the hood the same as original Jetpack Security and 
`jetpack` module as well.

### Jetpack

This was the main reason why we decided to create this library. We have been using Jetpack Security
library on several projects, but it had some issues. First, it was 
[silently deprecated](https://developer.android.com/privacy-and-security/cryptography#jetpack_security_crypto_library)
without providing any alternative. The latest stable version was released in 2021, which is pretty old,
especially considering that it is a library focused on security. There were several issues, some of them
fixed in alpha releases, but they never made it to stable. Since we have been already using Jetpack
Security, needed some alternative and otherwise liked the abstractions it provided, we decided to
completely rewrite it and fix the known issues it had.

`jetpack` module contains `EncryptedSharedPreferences` and `EncryptedFile` implementations. `MasterKey`
is ported as well, but is part of `core` module, because it is reused by other modules as well. However,
if you need original Jetpack Security functionality, it is sufficient to depend on `jetpack` only and
`core` is included automatically.

#### Compatibility with Jetpack Security

`jetpack` is 100% compatible in terms of data compatibility with Jetpack Security. It means that if
you already use Jetpack Security on your project and want to switch to `jetpack`, you can just replace
the library, make necessary adjustments to source code and run the app. The already created encrypted data
will work fine with the `jetpack` implementation.

Regarding source code compatibility, we had to make some big necessary breaking changes to improve the
implementation and we also did some smaller not necessary breaking changes, which are easy to adapt to, but
we believe they improve the API. We tried to keep the API as consistent with Jetpack Security as possible
and only broke it when it provided some benefits.

The smaller changes mostly involve `MasteKey` class changes. When you use this class to get a master key,
it actually returns its instance that needs to be passed to encrypted implementations. This provides
a more type-safe API compared to a general `String` representation.

The biggest breaking changes involve `EncryptedSharedPreferences`. The original Jetpack Security's
implementation returned the instance of `SharedPreferences`, which was beneficial, because you could
have used this on all places where you needed a regular `SharedPreferences` types. However, there were
also some problems. Those problems might not be noticeable for a few key-value pairs stored to preferences,
but becomes visible for a lot of key-value pairs or data of a bigger size. All crypto operations of
original `EncryptedSharedPreferences` (and `EncryptedFile` as well) are executed on the caller's thread,
possibly blocking it for more intensive operations. This is especially problematic for methods, where
you do not expect this even for the regular `SharedPreferences` like `apply`, which is actually one of
those most problematic methods. Since we wanted to improve this and use coroutines for that, we had
to break this completely and we introduced a new `EncryptedSharedPreferences` interface that is basically
a 1:1 copy of the `SharedPreferences` interface, but have all relevant methods `suspend` to not block
caller's thread. We understand, that this big breaking change might be problematic for apps relying
heavily on `SharedPreferences` (e.g. passing it to a third-party library), so there is also an extension
`EncryptedSharedPreferences.adaptToSharedPreferences`, which adapts `EncryptedSharedPreferences` to
`SharedPreferences`. However, you should not use this, unless really necessary, and you should migrate 
to `EncryptedSharedPreferences` to get all benefits it offers, as the adapter just blocks while waiting
for the internal `EncryptedSharedPreferences` suspend functions to complete.

#### Improvements over Jetpack Security

During rewrite of Jetpack Security library we made following improvements:
- Rewritten from Java to 100% Kotlin.
- All logic is covered by tests. We followed a careful process of refactoring, when we first covered
all the existing functionality by tests and then started to rewrite the implementations, which gave
us a confidence to not break anything.
- Improve some APIs like `MasterKey`, which is now more type-safe and also offers `KeyGenParameterSpec.Builder`
configured with the same default values as the original implementation, but you can take this and apply
additional custom configurations before building the final spec and getting a key.
- Improve performance of the `EncryptedSharedPreferences` for various methods like getting all key-value
pairs that made unnecessary extra encryptions/decryptions under the hood.
- Remove all blocking calls and making heavy methods suspend instead.
- Fix synchronization issues during master key creation and increase Tink library version from the old one,
used in Jetpack Security, that also had some synchronization issues, that were fixed in later releases.
- Since one of the major issues of Jetpack Security was an outdated Tink library, which makes all the
crypto operations and Jetpack Security was basically just a thin abstraction over it, we wanted to 
try to prevent the same issues in the future and so we decided to force clients of Ackee Guardian library
to depend on Tink explicitly. This allows clients to have a better control over updates, independent
of Ackee Guardian updates.
- Fix several bugs discovered in `EncryptedSharedPreferences` during covering the logic by tests:
  - If you saved empty string `Set`, you didn't get it back by using `getStringSet`, but you got 
    default value passed in parameter instead.
  - Storing `Set` with null threw NPE.
  - `get*` methods didn't throw `ClassCastException` as specified in `SharedPreferences` contracts, 
    when you tried to access some key using an incorrect get method.
  - Contract of `SharedPreferences.registerOnSharedPreferenceChangeListener` specifies that it does not
    store strong references on the listener objects, but it actually incorrectly did.
  - Contract of `OnSharedPreferenceChangeListener.onSharedPreferenceChanged` specifies, that it has to be
    invoked from the main thread, but this was not ensured.
  - `OnSharedPreferenceChangeListener.onSharedPreferenceChanged` was being called multiple times per
    one key in one editor, if the editor did multiple changes on the same key.
  - `OnSharedPreferenceChangeListener.onSharedPreferenceChanged` was being called even when the key
    was added and then removed in the same editor.

## Setup

Add the following dependencies to your `libs.versions.toml`, depending on what you need. You should
always use BOM to be sure to get binary compatible dependencies. If you need only `jetpack` features,
just declare BOM and `io.github.ackeecz:guardian-jetpack`. If you need only particular DataStore, 
then declare BOM and particular DataStore dependency, e.g. `io.github.ackeecz:guardian-datastore`.
You don't need to declare `io.github.ackeecz:guardian-core` dependency, unless you depend only on
`core` without any DataStore or `jetpack` modules.

```toml
[versions]
ackee-guardian-bom = "SPECIFY_VERSION"
tink = "SPECIFY_VERSION"

[libraries]
ackee-guardian-bom = { module = "io.github.ackeecz:guardian-bom", version.ref = "ackee-guardian-bom" }
ackee-guardian-core = { module = "io.github.ackeecz:guardian-core" }
ackee-guardian-datastore = { module = "io.github.ackeecz:guardian-datastore" }
ackee-guardian-datastore-preferences = { module = "io.github.ackeecz:guardian-datastore-preferences" }
ackee-guardian-jetpack = { module = "io.github.ackeecz:guardian-jetpack" }

tink-android = { module = "com.google.crypto.tink:tink-android", version.ref = "tink" }
```

Then specify dependencies in your `build.gradle.kts`:

```kotlin
dependencies {

    // Always use BOM
    implementation(platform(libs.ackee.guardian.bom))
    // Optional core dependency. Needed to be specified only if you do not use any other artifact
    // and want to use core in your app.
    implementation(libs.ackee.guardian.core)
    // For encrypted DataStore
    implementation(libs.ackee.guardian.datastore)
    // For encrypted preferences DataStore
    implementation(libs.ackee.guardian.datastore.preferences)
     // For Jetpack Security port
    implementation(libs.ackee.guardian.jetpack)

    // Dependency on Tink must be included explicitly. This allows clients of Ackee Guardian library 
    // to control the version of Tink themselves, being able to keep it up-to-date as much as possible 
    // and not depend on Ackee Guardian releases.
    implementation(libs.tink.android)
}
```

## Usage

Basic usage of the main library functionality is described bellow. You can also take a look on tests
to get even more detailed picture.

### DataStore

The usages of encrypted DataStore implementations are almost the same as the classic DataStore. Both
classic and preferences encrypted DataStore implementations can be created using property delegates
or factories. The main difference is the `DataStoreCryptoParams` class that contains necessary
parameters specific to crypto operations over DataStore. Check the documentation of this class for
more details of what you can customize. Once you create an encrypted version of DataStore, you can
use it exactly the same as the classic unencrypted DataStore instance.

Encrypted DataStore delegate:

```kotlin
val Context.myDataStore by encryptedDataStore(
    cryptoParams = DataStoreCryptoParams(
        encryptionScheme = DataStoreEncryptionScheme.AES256_GCM_HKDF_4KB,
        getMasterKey = { MasterKey.getOrCreate() },
    ),
    fileName = "filename",
    serializer = serializer,
    // Other params as in dataStore delegate
)
```

Encrypted DataStore factory:

```kotlin
DataStoreFactory.createEncrypted(
    context = context,
    cryptoParams = DataStoreCryptoParams(
        encryptionScheme = DataStoreEncryptionScheme.AES256_GCM_HKDF_4KB,
        getMasterKey = { MasterKey.getOrCreate() },
    ),
    serializer = serializer,
    produceFile = { context.dataStoreFile("encrypted_data") },
    // Other params as in DataStoreFactory.create
)
```

Encrypted PreferenceDataStore delegate:

```kotlin
val Context.myDataStore by encryptedPreferencesDataStore(
    cryptoParams = DataStoreCryptoParams(
        encryptionScheme = DataStoreEncryptionScheme.AES256_GCM_HKDF_4KB,
        getMasterKey = { MasterKey.getOrCreate() },
    ),
    name = "preferences_name",
    // Other params as in preferencesDataStore delegate
)
```

Encrypted PreferenceDataStore factory:

```kotlin
PreferenceDataStoreFactory.createEncrypted(
    context = context,
    cryptoParams = DataStoreCryptoParams(
        encryptionScheme = DataStoreEncryptionScheme.AES256_GCM_HKDF_4KB,
        getMasterKey = { MasterKey.getOrCreate() },
    ),
    produceFile = { context.preferencesDataStoreFile("encrypted_data") },
    // Other params as in PreferenceDataStoreFactory.create
)
```

### Jetpack

Using classes from `jetpack` module is almost the same as using the Jetpack Security classes.

`EncryptedFile`:

```kotlin
val encryptedFile = EncryptedFile.Builder(
    file = File(context.filesDir, "secret_data"),
    context = context,
    encryptionScheme = EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB,
    getMasterKey = { MasterKey.getOrCreate() },
).build()
// Write to the encrypted file
val encryptedOutputStream = encryptedFile.openFileOutput()
// Read the encrypted file
val encryptedInputStream = encryptedFile.openFileInput()
```

`EncryptedSharedPreferences`:

```kotlin
val encryptedSharedPreferences: EncryptedSharedPreferences = EncryptedSharedPreferences.create(
    fileName = "secret_shared_prefs",
    getMasterKey = { MasterKey.getOrCreate() },
    context = context,
    prefKeyEncryptionScheme = EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    prefValueEncryptionScheme = EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
)
// Use EncryptedSharedPreferences and Editor as you would normally use SharedPreferences
encryptedSharedPreferences.edit {
    putString("secret_key", "secret_value")
}
```

As discussed above in Architecture section, `EncryptedSharedPreferences.create` no longer return
`SharedPreferences` type but a new `EncryptedSharedPreferences`. You are highly encouraged to
use this new type, but if you really **do** need `SharedPreferences`, there is an extension, that
can adapt `EncryptedSharedPreferences` to `SharedPreferences`.

```kotlin
val sharedPreferences: SharedPreferences = encryptedSharedPreferences.adaptToSharedPreferences()
```

## Credits

Developed by [Ackee](https://www.ackee.cz) team with 💙. 

`MasterKey` class from `core` and `EncryptedFile` and `EncryptedSharedPreferences` classes from 
`jetpack` are based on [Jetpack Security Crypto library](https://developer.android.com/reference/kotlin/androidx/security/crypto/package-summary)
published by Google LLC, under the Apache License 2.0.
