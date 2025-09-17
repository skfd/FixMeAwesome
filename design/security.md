# Survey Me - Security Design Documentation

## 1. Overview

This document outlines the comprehensive security strategy for the Survey Me Android application, addressing threat modeling, security controls, data protection, privacy measures, and compliance requirements. It establishes security principles and implementation guidelines to protect user data and ensure application integrity.

### 1.1 Security Objectives

1. **Data Confidentiality**: Protect user data from unauthorized access
2. **Data Integrity**: Ensure data remains accurate and unmodified
3. **Authentication**: Verify user identity securely
4. **Authorization**: Control access to resources and features
5. **Privacy Protection**: Minimize data collection and protect user privacy
6. **Secure Communication**: Encrypt all data in transit
7. **Resilience**: Resist attacks and recover gracefully from security incidents
8. **Compliance**: Meet regulatory and legal requirements

### 1.2 Security Principles

- **Defense in Depth**: Multiple layers of security controls
- **Least Privilege**: Minimal permissions required for functionality
- **Secure by Default**: Security enabled out of the box
- **Zero Trust**: Verify everything, trust nothing
- **Privacy by Design**: Privacy considered at every stage
- **Transparent Security**: Clear communication about security measures
- **Continuous Security**: Regular updates and security patches
- **Incident Response**: Prepared procedures for security incidents

## 2. Threat Model

### 2.1 Assets to Protect

| Asset | Classification | Description |
|-------|---------------|-------------|
| Location Data | High Sensitivity | User GPS tracks and location history |
| Personal Information | High Sensitivity | User account details, email, preferences |
| POI Data | Medium Sensitivity | Points of interest and custom markers |
| Application Credentials | Critical | API keys, tokens, certificates |
| Map Cache | Low Sensitivity | Downloaded map tiles |
| Application Code | Medium Sensitivity | Source code and algorithms |
| User Preferences | Low Sensitivity | App settings and configuration |

### 2.2 Threat Actors

1. **Malicious Apps**: Other apps attempting to access Survey Me data
2. **Network Attackers**: Man-in-the-middle attacks on network traffic
3. **Device Thieves**: Physical access to user's device
4. **Malicious Users**: Authorized users attempting privilege escalation
5. **State Actors**: Government surveillance or data requests
6. **Cybercriminals**: Organized attacks for data theft
7. **Script Kiddies**: Automated attacks using known vulnerabilities

### 2.3 Attack Vectors and Mitigations

#### STRIDE Analysis

| Threat | Description | Impact | Mitigation |
|--------|-------------|--------|------------|
| **Spoofing** | Fake GPS locations | Data integrity compromised | Location verification algorithms |
| **Tampering** | Modified app or data | Malicious behavior | App signing, integrity checks |
| **Repudiation** | Denying actions | Accountability lost | Audit logging, timestamps |
| **Information Disclosure** | Data leaks | Privacy breach | Encryption, access controls |
| **Denial of Service** | App unavailability | Service disruption | Rate limiting, resource management |
| **Elevation of Privilege** | Unauthorized access | Full compromise | Permission checks, sandboxing |

### 2.4 Attack Trees

```
Goal: Steal User Location Data
├── Physical Access
│   ├── Steal Device
│   │   └── Bypass Screen Lock → Access App Data
│   └── Evil Maid Attack
│       └── Install Malware → Exfiltrate Data
├── Network Attack
│   ├── MITM Attack
│   │   └── Intercept API Calls → Capture Location
│   └── Rogue WiFi AP
│       └── DNS Hijacking → Redirect Traffic
└── Application Attack
    ├── Exploit Vulnerability
    │   └── SQL Injection → Database Access
    └── Social Engineering
        └── Phishing → Credential Theft
```

## 3. Security Architecture

### 3.1 Security Layers

```kotlin
// Security layer implementation
object SecurityArchitecture {
    // Layer 1: Device Security
    val deviceSecurity = DeviceSecurity(
        requireScreenLock = true,
        requireBiometric = false,
        detectRooted = true,
        detectEmulator = true
    )

    // Layer 2: Application Security
    val appSecurity = ApplicationSecurity(
        enableProguard = true,
        enableCertificatePinning = true,
        enableAntiTampering = true,
        enableSecureFlag = true
    )

    // Layer 3: Data Security
    val dataSecurity = DataSecurity(
        encryptionAlgorithm = "AES-256-GCM",
        keyDerivation = "PBKDF2",
        secureStorage = true,
        secureDeletion = true
    )

    // Layer 4: Network Security
    val networkSecurity = NetworkSecurity(
        tlsVersion = "1.3",
        certificatePinning = true,
        publicKeyPinning = true,
        enforceHttps = true
    )

    // Layer 5: Privacy Controls
    val privacyControls = PrivacyControls(
        minimizeDataCollection = true,
        anonymizeData = true,
        localProcessing = true,
        userConsent = true
    )
}
```

## 4. Authentication and Authorization

### 4.1 Authentication Implementation

```kotlin
class AuthenticationManager(
    private val biometricManager: BiometricManager,
    private val credentialManager: CredentialManager
) {
    // Biometric authentication
    suspend fun authenticateWithBiometric(): Result<AuthToken> {
        val cryptoObject = CryptoObject(
            cipher = createCipher()
        )

        return suspendCancellableCoroutine { continuation ->
            val biometricPrompt = BiometricPrompt(
                activity,
                ContextCompat.getMainExecutor(activity),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(
                        result: BiometricPrompt.AuthenticationResult
                    ) {
                        val token = generateAuthToken(result.cryptoObject)
                        continuation.resume(Result.success(token))
                    }

                    override fun onAuthenticationFailed() {
                        continuation.resume(
                            Result.failure(AuthenticationException("Biometric authentication failed"))
                        )
                    }
                }
            )

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Authenticate to Survey Me")
                .setSubtitle("Use your biometric credential")
                .setNegativeButtonText("Use password")
                .build()

            biometricPrompt.authenticate(promptInfo, cryptoObject)
        }
    }

    // Multi-factor authentication
    suspend fun authenticateWithMFA(
        password: String,
        otpCode: String
    ): Result<AuthToken> {
        // Verify password
        val passwordHash = hashPassword(password)
        if (!verifyPasswordHash(passwordHash)) {
            return Result.failure(AuthenticationException("Invalid password"))
        }

        // Verify OTP
        if (!verifyOTP(otpCode)) {
            return Result.failure(AuthenticationException("Invalid OTP code"))
        }

        // Generate auth token
        val token = generateAuthToken(
            userId = getCurrentUserId(),
            expiresIn = 3600 // 1 hour
        )

        return Result.success(token)
    }

    private fun hashPassword(password: String): String {
        val salt = generateSalt()
        val spec = PBEKeySpec(
            password.toCharArray(),
            salt,
            10000, // iterations
            256 // key length
        )
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = factory.generateSecret(spec).encoded
        return Base64.encodeToString(salt + hash, Base64.NO_WRAP)
    }
}
```

### 4.2 Authorization Framework

```kotlin
// Role-based access control
enum class Role {
    GUEST,
    USER,
    PREMIUM_USER,
    ADMIN
}

enum class Permission {
    READ_TRACKS,
    WRITE_TRACKS,
    DELETE_TRACKS,
    READ_POIS,
    WRITE_POIS,
    DELETE_POIS,
    EXPORT_DATA,
    MANAGE_SETTINGS,
    ACCESS_PREMIUM_FEATURES
}

class AuthorizationManager {
    private val rolePermissions = mapOf(
        Role.GUEST to setOf(
            Permission.READ_TRACKS,
            Permission.READ_POIS
        ),
        Role.USER to setOf(
            Permission.READ_TRACKS,
            Permission.WRITE_TRACKS,
            Permission.READ_POIS,
            Permission.WRITE_POIS,
            Permission.EXPORT_DATA,
            Permission.MANAGE_SETTINGS
        ),
        Role.PREMIUM_USER to Role.USER.permissions() + setOf(
            Permission.DELETE_TRACKS,
            Permission.DELETE_POIS,
            Permission.ACCESS_PREMIUM_FEATURES
        ),
        Role.ADMIN to Permission.values().toSet()
    )

    fun hasPermission(
        user: User,
        permission: Permission
    ): Boolean {
        val userPermissions = rolePermissions[user.role] ?: emptySet()
        return permission in userPermissions
    }

    fun requirePermission(
        user: User,
        permission: Permission
    ) {
        if (!hasPermission(user, permission)) {
            throw SecurityException(
                "User ${user.id} lacks permission: $permission"
            )
        }
    }
}

// Attribute-based access control
class AttributeBasedAccessControl {
    fun evaluateAccess(
        subject: Subject,
        resource: Resource,
        action: Action,
        environment: Environment
    ): AccessDecision {
        val policies = loadPolicies()

        for (policy in policies) {
            val decision = policy.evaluate(
                subject,
                resource,
                action,
                environment
            )

            if (decision == AccessDecision.DENY) {
                return AccessDecision.DENY
            }
        }

        return AccessDecision.ALLOW
    }
}
```

## 5. Data Protection

### 5.1 Encryption Implementation

```kotlin
class EncryptionManager {
    companion object {
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "SurveyMeEncryptionKey"
    }

    init {
        generateKey()
    }

    private fun generateKey() {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )

        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(false)
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGenerator.init(keyGenParameterSpec)
        keyGenerator.generateKey()
    }

    fun encrypt(plainText: ByteArray): EncryptedData {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)

        val secretKey = keyStore.getKey(KEY_ALIAS, null) as SecretKey

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        val iv = cipher.iv
        val cipherText = cipher.doFinal(plainText)

        return EncryptedData(
            cipherText = cipherText,
            iv = iv
        )
    }

    fun decrypt(encryptedData: EncryptedData): ByteArray {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)

        val secretKey = keyStore.getKey(KEY_ALIAS, null) as SecretKey

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(128, encryptedData.iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

        return cipher.doFinal(encryptedData.cipherText)
    }
}

data class EncryptedData(
    val cipherText: ByteArray,
    val iv: ByteArray
) {
    fun toBase64(): String {
        val combined = iv + cipherText
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    companion object {
        fun fromBase64(base64: String): EncryptedData {
            val combined = Base64.decode(base64, Base64.NO_WRAP)
            val iv = combined.sliceArray(0..11)
            val cipherText = combined.sliceArray(12 until combined.size)
            return EncryptedData(cipherText, iv)
        }
    }
}
```

### 5.2 Secure Storage

```kotlin
class SecureStorage(private val context: Context) {
    private val encryptionManager = EncryptionManager()

    // Encrypted SharedPreferences (API 23+)
    private val encryptedPrefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // Encrypted file storage
    fun saveSecureFile(
        filename: String,
        content: ByteArray
    ) {
        val encryptedData = encryptionManager.encrypt(content)

        val file = File(context.filesDir, "$filename.encrypted")
        file.writeBytes(encryptedData.cipherText)

        // Store IV separately
        encryptedPrefs.edit()
            .putString("${filename}_iv",
                Base64.encodeToString(encryptedData.iv, Base64.NO_WRAP)
            )
            .apply()
    }

    fun readSecureFile(filename: String): ByteArray? {
        val file = File(context.filesDir, "$filename.encrypted")
        if (!file.exists()) return null

        val cipherText = file.readBytes()
        val ivString = encryptedPrefs.getString("${filename}_iv", null)
            ?: return null
        val iv = Base64.decode(ivString, Base64.NO_WRAP)

        val encryptedData = EncryptedData(cipherText, iv)
        return encryptionManager.decrypt(encryptedData)
    }

    // Secure deletion
    fun secureDelete(filename: String) {
        val file = File(context.filesDir, "$filename.encrypted")
        if (file.exists()) {
            // Overwrite with random data before deletion
            val randomData = ByteArray(file.length().toInt())
            SecureRandom().nextBytes(randomData)
            file.writeBytes(randomData)
            file.delete()
        }

        // Remove IV from preferences
        encryptedPrefs.edit()
            .remove("${filename}_iv")
            .apply()
    }
}
```

## 6. Network Security

### 6.1 Certificate Pinning

```kotlin
class NetworkSecurityConfig {
    fun createSecureOkHttpClient(): OkHttpClient {
        // Certificate pinning
        val certificatePinner = CertificatePinner.Builder()
            .add("api.surveyme.app",
                "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
            )
            .add("*.surveyme.app",
                "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB="
            )
            .add("tile.openstreetmap.org",
                "sha256/CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC="
            )
            .build()

        // TLS configuration
        val spec = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
            .tlsVersions(TlsVersion.TLS_1_3, TlsVersion.TLS_1_2)
            .cipherSuites(
                CipherSuite.TLS_AES_128_GCM_SHA256,
                CipherSuite.TLS_AES_256_GCM_SHA384,
                CipherSuite.TLS_CHACHA20_POLY1305_SHA256
            )
            .build()

        // Custom trust manager for additional validation
        val trustManager = object : X509TrustManager {
            override fun checkClientTrusted(
                chain: Array<X509Certificate>,
                authType: String
            ) {
                // Not used for client
            }

            override fun checkServerTrusted(
                chain: Array<X509Certificate>,
                authType: String
            ) {
                // Additional certificate validation
                validateCertificateChain(chain)
                validateCertificateExpiry(chain[0])
                validateHostname(chain[0])
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> {
                return arrayOf()
            }
        }

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf(trustManager), SecureRandom())

        return OkHttpClient.Builder()
            .certificatePinner(certificatePinner)
            .connectionSpecs(listOf(spec))
            .sslSocketFactory(
                sslContext.socketFactory,
                trustManager
            )
            .hostnameVerifier { hostname, session ->
                // Custom hostname verification
                verifyHostname(hostname, session)
            }
            .build()
    }

    private fun validateCertificateChain(chain: Array<X509Certificate>) {
        // Verify certificate chain
        for (i in 0 until chain.size - 1) {
            val cert = chain[i]
            val issuer = chain[i + 1]

            try {
                cert.verify(issuer.publicKey)
            } catch (e: Exception) {
                throw CertificateException("Invalid certificate chain")
            }
        }
    }

    private fun validateCertificateExpiry(cert: X509Certificate) {
        try {
            cert.checkValidity()
        } catch (e: Exception) {
            throw CertificateException("Certificate expired or not yet valid")
        }
    }
}
```

### 6.2 API Security

```kotlin
class ApiSecurityInterceptor(
    private val authManager: AuthManager,
    private val integrityChecker: IntegrityChecker
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()

        // Add authentication header
        val token = authManager.getAuthToken()
        request = request.newBuilder()
            .addHeader("Authorization", "Bearer $token")
            .build()

        // Add request signing
        val signature = signRequest(request)
        request = request.newBuilder()
            .addHeader("X-Signature", signature)
            .build()

        // Add nonce for replay protection
        val nonce = generateNonce()
        request = request.newBuilder()
            .addHeader("X-Nonce", nonce)
            .build()

        // Add timestamp
        request = request.newBuilder()
            .addHeader("X-Timestamp", System.currentTimeMillis().toString())
            .build()

        // Send request
        val response = chain.proceed(request)

        // Verify response integrity
        if (!verifyResponseIntegrity(response)) {
            throw SecurityException("Response integrity check failed")
        }

        return response
    }

    private fun signRequest(request: Request): String {
        val data = buildString {
            append(request.method)
            append(request.url.encodedPath)
            append(request.body?.contentLength() ?: 0)
        }

        val mac = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(
            getSigningKey(),
            "HmacSHA256"
        )
        mac.init(secretKey)

        val signature = mac.doFinal(data.toByteArray())
        return Base64.encodeToString(signature, Base64.NO_WRAP)
    }

    private fun generateNonce(): String {
        val random = SecureRandom()
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}
```

## 7. Application Security

### 7.1 Anti-Tampering

```kotlin
class AntiTamperingManager(private val context: Context) {

    fun verifyAppIntegrity(): Boolean {
        return checkSignature() &&
               checkPackageName() &&
               checkInstallerPackage() &&
               checkDebuggable() &&
               checkResources()
    }

    private fun checkSignature(): Boolean {
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_SIGNATURES
        )

        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.signingInfo.apkContentsSigners
        } else {
            @Suppress("DEPRECATION")
            packageInfo.signatures
        }

        val expectedSignature = "308203..." // Your app's signature

        for (signature in signatures) {
            val hash = MessageDigest.getInstance("SHA-256")
                .digest(signature.toByteArray())
            val actualSignature = hash.toHexString()

            if (actualSignature != expectedSignature) {
                return false
            }
        }

        return true
    }

    private fun checkPackageName(): Boolean {
        return context.packageName == "com.surveyme"
    }

    private fun checkInstallerPackage(): Boolean {
        val installer = context.packageManager
            .getInstallerPackageName(context.packageName)

        return installer in listOf(
            "com.android.vending", // Google Play
            "com.amazon.venezia", // Amazon Appstore
            null // Direct install for testing
        )
    }

    private fun checkDebuggable(): Boolean {
        return (context.applicationInfo.flags and
                ApplicationInfo.FLAG_DEBUGGABLE) == 0
    }

    private fun checkResources(): Boolean {
        // Check for resource modifications
        val resources = context.resources
        val appName = resources.getString(R.string.app_name)
        return appName == "Survey Me"
    }
}

// Root detection
class RootDetection {
    fun isDeviceRooted(): Boolean {
        return checkRootManagementApps() ||
               checkPotentiallyDangerousApps() ||
               checkRootCloakingApps() ||
               checkSuBinary() ||
               checkBusyBoxBinary() ||
               checkForDangerousProps() ||
               checkForRWPaths() ||
               checkSuExists() ||
               checkForRootNative()
    }

    private fun checkRootManagementApps(): Boolean {
        val packages = listOf(
            "com.topjohnwu.magisk",
            "eu.chainfire.supersu",
            "com.koushikdutta.superuser",
            "com.thirdparty.superuser",
            "com.noshufou.android.su"
        )

        return packages.any { isPackageInstalled(it) }
    }

    private fun checkSuBinary(): Boolean {
        val paths = listOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/system/su",
            "/system/bin/.ext/.su"
        )

        return paths.any { File(it).exists() }
    }

    private fun checkForDangerousProps(): Boolean {
        val dangerous = mapOf(
            "ro.debuggable" to "1",
            "ro.secure" to "0"
        )

        return try {
            dangerous.any { (key, badValue) ->
                getSystemProperty(key) == badValue
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun getSystemProperty(key: String): String? {
        return try {
            val process = Runtime.getRuntime()
                .exec("getprop $key")
            val reader = BufferedReader(
                InputStreamReader(process.inputStream)
            )
            reader.readLine()
        } catch (e: Exception) {
            null
        }
    }
}
```

### 7.2 Code Obfuscation

```proguard
# ProGuard rules for security

# Obfuscate security-critical classes
-keep class !com.surveyme.security.**,** { *; }
-repackageclasses 'o'

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

# Encrypt strings
-adaptclassstrings
-adaptresourcefilenames
-adaptresourcefilecontents

# Additional obfuscation
-obfuscationdictionary proguard-dict.txt
-classobfuscationdictionary proguard-dict.txt
-packageobfuscationdictionary proguard-dict.txt

# Optimize
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification

# Remove debug information
-renamesourcefileattribute SourceFile
-keepattributes !LocalVariable*,!LineNumberTable
```

## 8. Privacy Protection

### 8.1 Data Minimization

```kotlin
class PrivacyManager {
    // Location anonymization
    fun anonymizeLocation(
        location: Location,
        precision: LocationPrecision = LocationPrecision.MEDIUM
    ): Location {
        return when (precision) {
            LocationPrecision.EXACT -> location
            LocationPrecision.HIGH -> roundLocation(location, 3) // ~100m
            LocationPrecision.MEDIUM -> roundLocation(location, 2) // ~1km
            LocationPrecision.LOW -> roundLocation(location, 1) // ~10km
            LocationPrecision.VERY_LOW -> roundLocation(location, 0) // ~100km
        }
    }

    private fun roundLocation(
        location: Location,
        decimals: Int
    ): Location {
        val factor = 10.0.pow(decimals)
        return Location(
            latitude = (location.latitude * factor).roundToInt() / factor,
            longitude = (location.longitude * factor).roundToInt() / factor
        )
    }

    // Data retention
    fun enforceDataRetention() {
        val retentionPeriod = 90 * 24 * 60 * 60 * 1000L // 90 days
        val cutoffTime = System.currentTimeMillis() - retentionPeriod

        // Delete old tracks
        trackRepository.deleteTracksOlderThan(cutoffTime)

        // Delete old notifications
        notificationRepository.deleteNotificationsOlderThan(cutoffTime)

        // Clear old cache
        cacheManager.clearOldCache(cutoffTime)
    }

    // Consent management
    fun requestConsent(
        dataType: DataType,
        purpose: Purpose
    ): ConsentResult {
        val dialog = ConsentDialog(
            dataType = dataType,
            purpose = purpose,
            retention = getRetentionPeriod(dataType),
            sharing = getSharingPolicy(dataType)
        )

        return dialog.show()
    }
}

enum class LocationPrecision {
    EXACT,     // No rounding
    HIGH,      // 3 decimal places (~100m)
    MEDIUM,    // 2 decimal places (~1km)
    LOW,       // 1 decimal place (~10km)
    VERY_LOW   // 0 decimal places (~100km)
}
```

### 8.2 GDPR Compliance

```kotlin
class GdprCompliance {
    // Right to access
    suspend fun exportUserData(userId: String): UserDataExport {
        return UserDataExport(
            profile = userRepository.getUser(userId),
            tracks = trackRepository.getUserTracks(userId),
            pois = poiRepository.getUserPois(userId),
            preferences = preferencesRepository.getUserPreferences(userId),
            exportDate = System.currentTimeMillis()
        )
    }

    // Right to rectification
    suspend fun updateUserData(
        userId: String,
        updates: Map<String, Any>
    ) {
        userRepository.updateUser(userId, updates)
        auditLog.log(
            "User data updated",
            userId,
            updates.keys
        )
    }

    // Right to erasure (Right to be forgotten)
    suspend fun deleteAllUserData(userId: String) {
        // Delete in correct order to maintain referential integrity
        notificationRepository.deleteUserNotifications(userId)
        trackPointRepository.deleteUserTrackPoints(userId)
        trackRepository.deleteUserTracks(userId)
        poiRepository.deleteUserPois(userId)
        preferencesRepository.deleteUserPreferences(userId)
        userRepository.deleteUser(userId)

        // Clear from cache
        cacheManager.clearUserCache(userId)

        // Log deletion
        auditLog.log(
            "User data deleted",
            userId,
            listOf("all_data")
        )
    }

    // Right to data portability
    fun exportDataInMachineReadableFormat(
        userData: UserDataExport
    ): String {
        return Json.encodeToString(userData)
    }

    // Cookie consent
    fun manageCookieConsent(): CookieConsent {
        return CookieConsent(
            necessary = true, // Always enabled
            functional = getConsentStatus("functional_cookies"),
            analytics = getConsentStatus("analytics_cookies"),
            advertising = false // We don't use advertising cookies
        )
    }
}
```

## 9. Security Monitoring and Incident Response

### 9.1 Security Monitoring

```kotlin
class SecurityMonitor {
    private val securityEvents = mutableListOf<SecurityEvent>()

    fun monitorSecurity() {
        // Monitor for suspicious activities
        detectJailbreak()
        detectDebugging()
        detectEmulator()
        detectHooking()
        detectScreenRecording()
        detectAccessibilityRisk()
    }

    private fun detectDebugging() {
        if (Debug.isDebuggerConnected()) {
            logSecurityEvent(
                SecurityEvent(
                    type = SecurityEventType.DEBUGGER_DETECTED,
                    severity = Severity.HIGH,
                    timestamp = System.currentTimeMillis()
                )
            )

            // Take action
            if (!BuildConfig.DEBUG) {
                terminateApp("Debugger detected")
            }
        }
    }

    private fun detectEmulator() {
        val isEmulator = Build.FINGERPRINT.contains("generic") ||
                        Build.MODEL.contains("google_sdk") ||
                        Build.MODEL.contains("Emulator") ||
                        Build.HARDWARE.contains("goldfish") ||
                        Build.PRODUCT.contains("sdk")

        if (isEmulator) {
            logSecurityEvent(
                SecurityEvent(
                    type = SecurityEventType.EMULATOR_DETECTED,
                    severity = Severity.MEDIUM,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    private fun detectHooking() {
        // Check for Xposed Framework
        val xposedClasses = listOf(
            "de.robv.android.xposed.XposedBridge",
            "de.robv.android.xposed.XC_MethodHook"
        )

        for (className in xposedClasses) {
            try {
                Class.forName(className)
                logSecurityEvent(
                    SecurityEvent(
                        type = SecurityEventType.HOOKING_FRAMEWORK_DETECTED,
                        severity = Severity.CRITICAL,
                        timestamp = System.currentTimeMillis()
                    )
                )
                terminateApp("Hooking framework detected")
            } catch (e: ClassNotFoundException) {
                // Class not found, continue checking
            }
        }
    }

    private fun logSecurityEvent(event: SecurityEvent) {
        securityEvents.add(event)

        // Send to remote logging if critical
        if (event.severity == Severity.CRITICAL) {
            sendToRemoteLogging(event)
        }
    }
}

data class SecurityEvent(
    val type: SecurityEventType,
    val severity: Severity,
    val timestamp: Long,
    val details: Map<String, Any> = emptyMap()
)

enum class SecurityEventType {
    DEBUGGER_DETECTED,
    EMULATOR_DETECTED,
    ROOT_DETECTED,
    HOOKING_FRAMEWORK_DETECTED,
    TAMPERING_DETECTED,
    SUSPICIOUS_ACTIVITY,
    AUTHENTICATION_FAILURE,
    AUTHORIZATION_FAILURE
}

enum class Severity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}
```

### 9.2 Incident Response Plan

```kotlin
class IncidentResponseManager {
    fun handleSecurityIncident(incident: SecurityIncident) {
        when (incident.severity) {
            Severity.CRITICAL -> handleCriticalIncident(incident)
            Severity.HIGH -> handleHighSeverityIncident(incident)
            Severity.MEDIUM -> handleMediumSeverityIncident(incident)
            Severity.LOW -> handleLowSeverityIncident(incident)
        }
    }

    private fun handleCriticalIncident(incident: SecurityIncident) {
        // 1. Contain the threat
        isolateAffectedComponents(incident)

        // 2. Assess damage
        val impact = assessImpact(incident)

        // 3. Notify stakeholders
        notifySecurityTeam(incident)
        if (impact.affectsUsers) {
            notifyAffectedUsers(impact.affectedUserIds)
        }

        // 4. Collect evidence
        val evidence = collectForensicData(incident)

        // 5. Remediate
        applySecurityPatch(incident)

        // 6. Recovery
        restoreServices(incident)

        // 7. Post-incident review
        schedulePostIncidentReview(incident)
    }

    private fun collectForensicData(incident: SecurityIncident): ForensicData {
        return ForensicData(
            logs = collectLogs(incident.timeframe),
            memoryDump = captureMemoryDump(),
            networkTraffic = captureNetworkTraffic(incident.timeframe),
            systemState = captureSystemState(),
            userActions = getUserActions(incident.timeframe)
        )
    }
}
```

## 10. Security Best Practices Checklist

### Development Phase
- [ ] Implement secure coding practices
- [ ] Use static analysis tools (e.g., SpotBugs, PMD)
- [ ] Perform dependency vulnerability scanning
- [ ] Conduct code reviews with security focus
- [ ] Implement unit tests for security features
- [ ] Use secure communication protocols
- [ ] Implement proper error handling
- [ ] Avoid hardcoding sensitive data

### Testing Phase
- [ ] Perform penetration testing
- [ ] Conduct security audits
- [ ] Test encryption implementation
- [ ] Verify certificate pinning
- [ ] Test authentication flows
- [ ] Validate input sanitization
- [ ] Test for injection vulnerabilities
- [ ] Verify anti-tampering measures

### Deployment Phase
- [ ] Enable ProGuard/R8 obfuscation
- [ ] Sign APK with release key
- [ ] Verify no debug information in release
- [ ] Implement app integrity checks
- [ ] Configure security headers
- [ ] Set up monitoring and alerting
- [ ] Document security procedures
- [ ] Train support staff on security

### Maintenance Phase
- [ ] Regular security updates
- [ ] Monitor for new vulnerabilities
- [ ] Update dependencies regularly
- [ ] Review security logs
- [ ] Conduct periodic security assessments
- [ ] Update threat model
- [ ] Maintain incident response plan
- [ ] User security education

---

*This security design document provides comprehensive security measures for the Survey Me application, ensuring data protection, user privacy, and application integrity throughout its lifecycle.*