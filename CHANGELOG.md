# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
### core
### datastore
#### Added
- Possibility to cache keysets in memory to improve performance of cryptographic operations

### datastore-preferences
#### Added
- Possibility to cache keysets in memory to improve performance of cryptographic operations

### jetpack
#### Added
- Possibility to cache keysets in memory to improve performance of cryptographic operations



## BOM [1.1.0] - 2025-01-17

### core
#### Added
- `AndroidKeyStoreSemaphore` that is used to synchronize all Android KeyStore operations in Guardian
- Abstractions over a few JCA APIs that synchronize operations with Android KeyStore
- Possibility to provide a custom `Semaphore` to `MasterKey`

#### Fixed
- Android KeyStore synchronization in `MasterKey`

### datastore
#### Added
- Possibility to provide a custom `Semaphore` to `encryptedDataStore` and `DataStoreFactory.createEncrypted`

#### Fixed
- Android KeyStore synchronization in `encryptedDataStore` and `DataStoreFactory.createEncrypted`

### datastore-preferences
#### Added
- Possibility to provide a custom `Semaphore` to `encryptedPreferencesDataStore` and `PreferenceDataStoreFactory.createEncrypted`

#### Fixed
- Android KeyStore synchronization in `encryptedPreferencesDataStore` and `PreferenceDataStoreFactory.createEncrypted`

### jetpack
#### Added
- Possibility to provide a custom `Semaphore` to `EncryptedFile` and `EncryptedSharedPreferences`

#### Fixed
- Android KeyStore synchronization in `EncryptedFile` and `EncryptedSharedPreferences`



## BOM [1.0.0] - 2024-12-10

### core
#### Added
- First version of the artifact 🎉

### datastore
#### Added
- First version of the artifact 🎉

### datastore-preferences
#### Added
- First version of the artifact 🎉

### jetpack
#### Added
- First version of the artifact 🎉
