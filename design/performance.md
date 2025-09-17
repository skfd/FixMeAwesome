# Survey Me - Performance Requirements and Optimization Documentation

## 1. Overview

This document defines comprehensive performance requirements, optimization strategies, and monitoring approaches for the Survey Me Android application. It establishes performance baselines, identifies bottlenecks, and provides implementation guidelines for achieving optimal application performance across diverse devices and network conditions.

### 1.1 Performance Objectives

1. **Responsiveness**: Instant UI feedback for all user interactions
2. **Efficiency**: Minimal battery and resource consumption
3. **Scalability**: Handle large datasets without degradation
4. **Reliability**: Consistent performance across device types
5. **Smoothness**: 60 FPS animations and transitions
6. **Quick Startup**: Fast application launch times
7. **Network Efficiency**: Minimal data usage and optimized caching
8. **Memory Optimization**: Efficient memory usage without leaks

### 1.2 Performance Principles

- **Measure First**: Profile before optimizing
- **User-Centric**: Focus on perceived performance
- **Progressive Enhancement**: Basic functionality on all devices
- **Lazy Loading**: Load resources only when needed
- **Caching Strategy**: Cache aggressively, invalidate smartly
- **Async Operations**: Never block the main thread
- **Resource Management**: Release resources promptly
- **Continuous Monitoring**: Track performance metrics in production

## 2. Performance Requirements

### 2.1 Application Startup Performance

| Metric | Target | Maximum | Measurement Point |
|--------|--------|---------|-------------------|
| Cold Start | < 2.0s | 3.0s | Launch to interactive map |
| Warm Start | < 1.0s | 1.5s | Background to foreground |
| Hot Start | < 0.5s | 0.7s | Activity recreation |
| First Frame | < 500ms | 750ms | First visible content |
| Time to Interactive | < 2.5s | 3.5s | All UI responsive |

### 2.2 Runtime Performance Metrics

| Operation | Target Latency | Maximum | 95th Percentile |
|-----------|---------------|---------|-----------------|
| Map Pan/Zoom | 16ms | 33ms | 25ms |
| POI Loading (100 items) | 200ms | 500ms | 350ms |
| POI Loading (1000 items) | 500ms | 1000ms | 750ms |
| Track Point Recording | 50ms | 100ms | 75ms |
| Location Update Processing | 100ms | 200ms | 150ms |
| Database Query (simple) | 10ms | 50ms | 25ms |
| Database Query (complex) | 50ms | 200ms | 100ms |
| Network Request | 500ms | 2000ms | 1000ms |
| Image Loading | 100ms | 300ms | 200ms |
| Screen Transition | 200ms | 400ms | 300ms |

### 2.3 Resource Consumption Targets

| Resource | Idle | Active Tracking | Heavy Usage | Maximum |
|----------|------|----------------|-------------|---------|
| CPU Usage | < 1% | < 10% | < 30% | 50% |
| Memory (RAM) | 50MB | 100MB | 150MB | 200MB |
| Battery Drain | 0.5%/hr | 5%/hr | 10%/hr | 15%/hr |
| Network Bandwidth | 0 | 10KB/min | 100KB/min | 1MB/min |
| Storage Growth | 0 | 1MB/hr | 5MB/hr | 10MB/hr |
| Wake Locks | None | Partial | Partial | Partial |

## 3. Performance Optimization Strategies

### 3.1 Startup Optimization

```kotlin
class StartupOptimizer {
    // Lazy initialization
    class AppInitializer {
        fun initializeApp(application: Application) {
            // Critical path - synchronous initialization
            initializeCriticalComponents()

            // Non-critical - asynchronous initialization
            GlobalScope.launch(Dispatchers.IO) {
                initializeAnalytics()
                initializeCrashReporting()
                preloadResources()
                warmupCaches()
            }
        }

        private fun initializeCriticalComponents() {
            // Only initialize what's needed for first screen
            DaggerAppComponent.builder()
                .application(application)
                .build()
                .inject(application)

            // Initialize database lazily
            DatabaseInitializer.initAsync(application)
        }

        private suspend fun preloadResources() {
            // Preload commonly used resources
            withContext(Dispatchers.IO) {
                // Preload map tiles for current location
                val location = LocationManager.getLastKnownLocation()
                location?.let {
                    MapTilePreloader.preloadArea(
                        center = LatLng(it.latitude, it.longitude),
                        radius = 1000, // meters
                        zoomLevels = 14..17
                    )
                }

                // Preload common images
                ImagePreloader.preload(
                    R.drawable.marker_default,
                    R.drawable.marker_selected,
                    R.drawable.user_location
                )
            }
        }
    }

    // Startup time tracking
    class StartupTimeTracker {
        private val startTime = SystemClock.elapsedRealtime()
        private val checkpoints = mutableMapOf<String, Long>()

        fun checkpoint(name: String) {
            val elapsed = SystemClock.elapsedRealtime() - startTime
            checkpoints[name] = elapsed

            if (BuildConfig.DEBUG) {
                Log.d("Startup", "$name: ${elapsed}ms")
            }
        }

        fun reportMetrics() {
            val metrics = StartupMetrics(
                coldStartTime = checkpoints["cold_start"] ?: 0,
                firstFrameTime = checkpoints["first_frame"] ?: 0,
                timeToInteractive = checkpoints["interactive"] ?: 0,
                checkpoints = checkpoints
            )

            Analytics.logEvent("app_startup", metrics.toBundle())
        }
    }
}

// App class optimization
class SurveyMeApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Strict mode for development
        if (BuildConfig.DEBUG) {
            enableStrictMode()
        }

        // Start initialization
        StartupOptimizer.AppInitializer().initializeApp(this)
    }

    private fun enableStrictMode() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build()
        )

        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build()
        )
    }
}
```

### 3.2 Memory Optimization

```kotlin
class MemoryOptimizer {
    // Memory-efficient image loading
    class ImageLoader {
        fun loadBitmap(
            context: Context,
            resId: Int,
            reqWidth: Int,
            reqHeight: Int
        ): Bitmap? {
            // First decode with inJustDecodeBounds=true to check dimensions
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeResource(context.resources, resId, options)

            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.RGB_565 // Lower memory usage

            return BitmapFactory.decodeResource(context.resources, resId, options)
        }

        private fun calculateInSampleSize(
            options: BitmapFactory.Options,
            reqWidth: Int,
            reqHeight: Int
        ): Int {
            val height = options.outHeight
            val width = options.outWidth
            var inSampleSize = 1

            if (height > reqHeight || width > reqWidth) {
                val halfHeight = height / 2
                val halfWidth = width / 2

                while ((halfHeight / inSampleSize) >= reqHeight &&
                       (halfWidth / inSampleSize) >= reqWidth) {
                    inSampleSize *= 2
                }
            }

            return inSampleSize
        }
    }

    // Memory leak prevention
    class LeakCanaryConfig {
        fun configure(application: Application) {
            if (BuildConfig.DEBUG) {
                // LeakCanary automatically installs itself
                AppWatcher.config = AppWatcher.config.copy(
                    watchActivities = true,
                    watchFragments = true,
                    watchFragmentViews = true,
                    watchViewModels = true
                )
            }
        }
    }

    // Memory cache management
    class MemoryCache<K, V>(maxSize: Int) {
        private val cache = object : LruCache<K, V>(maxSize) {
            override fun sizeOf(key: K, value: V): Int {
                return when (value) {
                    is Bitmap -> value.byteCount / 1024 // Size in KB
                    is String -> value.length * 2 / 1024 // Approximate size in KB
                    is ByteArray -> value.size / 1024
                    else -> 1 // Default size
                }
            }

            override fun entryRemoved(
                evicted: Boolean,
                key: K,
                oldValue: V,
                newValue: V?
            ) {
                // Clean up resources if needed
                if (oldValue is Bitmap && evicted) {
                    oldValue.recycle()
                }
            }
        }

        fun get(key: K): V? = cache.get(key)
        fun put(key: K, value: V) = cache.put(key, value)
        fun evictAll() = cache.evictAll()
        fun trimToSize(maxSize: Int) = cache.trimToSize(maxSize)
    }

    // Component lifecycle management
    class ComponentLifecycleManager : DefaultLifecycleObserver {
        private val components = mutableListOf<LifecycleAwareComponent>()

        override fun onStart(owner: LifecycleOwner) {
            components.forEach { it.onStart() }
        }

        override fun onStop(owner: LifecycleOwner) {
            components.forEach { it.onStop() }
        }

        override fun onDestroy(owner: LifecycleOwner) {
            components.forEach { it.cleanup() }
            components.clear()
        }
    }
}

// Memory monitoring
class MemoryMonitor(private val context: Context) {
    private val runtime = Runtime.getRuntime()

    fun getMemoryInfo(): MemoryInfo {
        val maxMemory = runtime.maxMemory() / 1024 / 1024 // MB
        val totalMemory = runtime.totalMemory() / 1024 / 1024
        val freeMemory = runtime.freeMemory() / 1024 / 1024
        val usedMemory = totalMemory - freeMemory

        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        return MemoryInfo(
            appMaxMemory = maxMemory,
            appUsedMemory = usedMemory,
            appFreeMemory = freeMemory,
            systemAvailableMemory = memoryInfo.availMem / 1024 / 1024,
            systemTotalMemory = memoryInfo.totalMem / 1024 / 1024,
            isLowMemory = memoryInfo.lowMemory
        )
    }

    fun onTrimMemory(level: Int) {
        when (level) {
            ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> {
                // Release UI resources
                releaseUIResources()
            }
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE -> {
                // Reduce cache sizes
                reduceCacheSizes(0.75f)
            }
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> {
                // Aggressive cache reduction
                reduceCacheSizes(0.5f)
            }
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> {
                // Clear all caches
                clearAllCaches()
            }
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> {
                // App is being killed
                performEmergencyCleanup()
            }
        }
    }
}
```

### 3.3 UI Performance Optimization

```kotlin
class UIPerformanceOptimizer {
    // RecyclerView optimization
    class OptimizedRecyclerView : RecyclerView.Adapter<ViewHolder>() {
        init {
            setHasStableIds(true)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            // Use ViewBinding for better performance
            val binding = ItemLayoutBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: List<Any>) {
            if (payloads.isEmpty()) {
                super.onBindViewHolder(holder, position, payloads)
            } else {
                // Partial update for better performance
                val bundle = payloads[0] as Bundle
                holder.updatePartial(bundle)
            }
        }

        // ViewHolder with optimizations
        class ViewHolder(private val binding: ItemLayoutBinding) :
            RecyclerView.ViewHolder(binding.root) {

            fun bind(item: Item) {
                binding.apply {
                    // Avoid expensive operations
                    titleText.text = item.title

                    // Load images asynchronously
                    Glide.with(imageView)
                        .load(item.imageUrl)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.placeholder)
                        .error(R.drawable.error)
                        .into(imageView)
                }
            }

            fun updatePartial(updates: Bundle) {
                // Only update changed fields
                if (updates.containsKey("title")) {
                    binding.titleText.text = updates.getString("title")
                }
            }
        }
    }

    // RecyclerView configuration
    fun configureRecyclerView(recyclerView: RecyclerView) {
        recyclerView.apply {
            // Enable optimizations
            setHasFixedSize(true)
            setItemViewCacheSize(20)
            isDrawingCacheEnabled = true
            drawingCacheQuality = View.DRAWING_CACHE_QUALITY_HIGH

            // Add item animator with shorter durations
            itemAnimator = DefaultItemAnimator().apply {
                addDuration = 120
                removeDuration = 120
                moveDuration = 120
                changeDuration = 120
            }

            // Use RecycledViewPool for multiple RecyclerViews
            setRecycledViewPool(sharedViewPool)

            // Add scroll listener for image loading
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    when (newState) {
                        RecyclerView.SCROLL_STATE_IDLE -> {
                            Glide.with(context).resumeRequests()
                        }
                        RecyclerView.SCROLL_STATE_DRAGGING,
                        RecyclerView.SCROLL_STATE_SETTLING -> {
                            Glide.with(context).pauseRequests()
                        }
                    }
                }
            })
        }
    }

    // Layout optimization
    class OptimizedLayout {
        // Use merge tags to reduce view hierarchy
        fun optimizeLayout() {
            // Use ViewStub for rarely used views
            val viewStub = findViewById<ViewStub>(R.id.stub)
            viewStub.setOnInflateListener { stub, inflated ->
                // Initialize view when inflated
            }

            // Inflate only when needed
            if (shouldShowRareView()) {
                viewStub.inflate()
            }
        }

        // Use ConstraintLayout for flat hierarchy
        fun createFlatLayout(): ConstraintLayout {
            return ConstraintLayout(context).apply {
                // Define constraints programmatically for dynamic layouts
                val set = ConstraintSet()
                set.clone(this)

                // Add optimized constraints
                set.applyTo(this)
            }
        }
    }

    // Rendering optimization
    fun enableHardwareAcceleration(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            view.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        }
    }

    // Animation optimization
    class AnimationOptimizer {
        fun createOptimizedAnimation(view: View): ViewPropertyAnimator {
            return view.animate()
                .setInterpolator(FastOutSlowInInterpolator())
                .setDuration(200) // Short duration
                .withLayer() // Hardware acceleration
        }

        fun disableAnimationsInPowerSave(context: Context) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            if (powerManager.isPowerSaveMode) {
                // Disable animations
                ValueAnimator.setDurationScale(0f)
            }
        }
    }
}
```

### 3.4 Database Performance Optimization

```kotlin
class DatabaseOptimizer {
    // Optimized database configuration
    fun createOptimizedDatabase(context: Context): SurveyMeDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            SurveyMeDatabase::class.java,
            "surveyme.db"
        )
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING) // WAL mode
            .setQueryExecutor(Executors.newFixedThreadPool(4)) // Thread pool
            .setTransactionExecutor(Executors.newSingleThreadExecutor())
            .addCallback(object : RoomDatabase.Callback() {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    // Enable foreign keys
                    db.execSQL("PRAGMA foreign_keys = ON")

                    // Optimize database
                    db.execSQL("PRAGMA optimize")

                    // Set cache size (in pages, default page size is 4096 bytes)
                    db.execSQL("PRAGMA cache_size = 10000") // ~40MB cache

                    // Enable memory-mapped I/O for better performance
                    db.execSQL("PRAGMA mmap_size = 268435456") // 256MB
                }
            })
            .build()
    }

    // Query optimization
    @Dao
    interface OptimizedTrackDao {
        // Use projection to select only needed columns
        @Query("""
            SELECT id, name, start_time, total_distance, status
            FROM tracks
            WHERE status = :status
            ORDER BY start_time DESC
            LIMIT :limit
        """)
        fun getRecentTracks(status: String, limit: Int = 20): Flow<List<TrackSummary>>

        // Use indices for faster queries
        @Query("""
            SELECT * FROM track_points
            WHERE track_id = :trackId
            AND timestamp BETWEEN :startTime AND :endTime
            ORDER BY sequence_number
        """)
        fun getTrackPointsInTimeRange(
            trackId: Long,
            startTime: Long,
            endTime: Long
        ): List<TrackPointEntity>

        // Batch operations for better performance
        @Insert
        suspend fun insertTrackPoints(points: List<TrackPointEntity>)

        @Transaction
        suspend fun insertTrackWithPoints(track: TrackEntity, points: List<TrackPointEntity>) {
            val trackId = insertTrack(track)
            val pointsWithTrackId = points.map { it.copy(trackId = trackId) }
            insertTrackPoints(pointsWithTrackId)
        }

        // Use raw queries for complex operations
        @RawQuery
        fun getTrackStatistics(query: SupportSQLiteQuery): TrackStatistics
    }

    // Database indices
    @Entity(
        tableName = "track_points",
        indices = [
            Index(value = ["track_id", "sequence_number"], unique = true),
            Index(value = ["track_id", "timestamp"]),
            Index(value = ["latitude", "longitude"])
        ]
    )
    data class TrackPointEntity(
        @PrimaryKey(autoGenerate = true) val id: Long,
        val trackId: Long,
        val sequenceNumber: Int,
        val latitude: Double,
        val longitude: Double,
        val timestamp: Long
    )

    // Query batching
    class BatchedQueryExecutor {
        private val batchSize = 100
        private val pendingQueries = mutableListOf<suspend () -> Unit>()

        fun addQuery(query: suspend () -> Unit) {
            pendingQueries.add(query)

            if (pendingQueries.size >= batchSize) {
                executeBatch()
            }
        }

        private fun executeBatch() {
            val batch = pendingQueries.toList()
            pendingQueries.clear()

            GlobalScope.launch(Dispatchers.IO) {
                database.runInTransaction {
                    batch.forEach { it() }
                }
            }
        }
    }
}
```

### 3.5 Network Performance Optimization

```kotlin
class NetworkOptimizer {
    // Optimized OkHttp client
    fun createOptimizedHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            // Connection pool
            .connectionPool(ConnectionPool(
                maxIdleConnections = 5,
                keepAliveDuration = 5,
                timeUnit = TimeUnit.MINUTES
            ))

            // Timeouts
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)

            // Cache
            .cache(Cache(
                directory = File(context.cacheDir, "http_cache"),
                maxSize = 50 * 1024 * 1024 // 50MB
            ))

            // Interceptors
            .addInterceptor(CacheInterceptor())
            .addInterceptor(CompressionInterceptor())
            .addNetworkInterceptor(NetworkMonitorInterceptor())

            // HTTP/2 and connection coalescing
            .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))

            // Retry and follow redirects
            .retryOnConnectionFailure(true)
            .followRedirects(true)
            .followSslRedirects(true)

            .build()
    }

    // Cache interceptor
    class CacheInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val originalResponse = chain.proceed(request)

            return if (isNetworkAvailable()) {
                // If network is available, cache for 1 minute
                originalResponse.newBuilder()
                    .removeHeader("Pragma")
                    .removeHeader("Cache-Control")
                    .header("Cache-Control", "public, max-age=60")
                    .build()
            } else {
                // If offline, use cache for up to 7 days
                originalResponse.newBuilder()
                    .removeHeader("Pragma")
                    .removeHeader("Cache-Control")
                    .header("Cache-Control", "public, only-if-cached, max-stale=604800")
                    .build()
            }
        }
    }

    // Request batching
    class RequestBatcher<T>(
        private val maxBatchSize: Int = 10,
        private val maxWaitTime: Long = 1000, // ms
        private val executor: (List<T>) -> Unit
    ) {
        private val pending = mutableListOf<T>()
        private var timer: CountDownTimer? = null

        @Synchronized
        fun add(request: T) {
            pending.add(request)

            if (pending.size >= maxBatchSize) {
                flush()
            } else if (timer == null) {
                startTimer()
            }
        }

        private fun startTimer() {
            timer = object : CountDownTimer(maxWaitTime, maxWaitTime) {
                override fun onTick(millisUntilFinished: Long) {}
                override fun onFinish() {
                    flush()
                }
            }.start()
        }

        @Synchronized
        private fun flush() {
            if (pending.isEmpty()) return

            val batch = pending.toList()
            pending.clear()
            timer?.cancel()
            timer = null

            GlobalScope.launch(Dispatchers.IO) {
                executor(batch)
            }
        }
    }

    // Adaptive quality based on network
    class AdaptiveQualityManager {
        fun getImageQuality(): ImageQuality {
            return when (getNetworkType()) {
                NetworkType.WIFI -> ImageQuality.HIGH
                NetworkType.MOBILE_4G -> ImageQuality.MEDIUM
                NetworkType.MOBILE_3G -> ImageQuality.LOW
                NetworkType.MOBILE_2G -> ImageQuality.VERY_LOW
                NetworkType.NONE -> ImageQuality.CACHED_ONLY
            }
        }

        fun getTileQuality(): TileQuality {
            return when (getNetworkType()) {
                NetworkType.WIFI -> TileQuality.VECTOR_HIGH_DPI
                NetworkType.MOBILE_4G -> TileQuality.VECTOR_STANDARD
                NetworkType.MOBILE_3G -> TileQuality.RASTER_256
                NetworkType.MOBILE_2G -> TileQuality.RASTER_128
                NetworkType.NONE -> TileQuality.CACHED_ONLY
            }
        }
    }
}
```

### 3.6 Battery Optimization

```kotlin
class BatteryOptimizer {
    // Adaptive location updates
    class AdaptiveLocationManager(private val context: Context) {
        private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        private val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

        fun getLocationRequest(): LocationRequest {
            val batteryLevel = getBatteryLevel()
            val isPowerSaveMode = powerManager.isPowerSaveMode

            return when {
                isPowerSaveMode -> createPowerSaveLocationRequest()
                batteryLevel < 20 -> createLowBatteryLocationRequest()
                batteryLevel < 50 -> createModerateBatteryLocationRequest()
                else -> createNormalLocationRequest()
            }
        }

        private fun createPowerSaveLocationRequest() = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
            interval = 30000 // 30 seconds
            fastestInterval = 15000
            smallestDisplacement = 50f // 50 meters
        }

        private fun createLowBatteryLocationRequest() = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
            interval = 20000 // 20 seconds
            fastestInterval = 10000
            smallestDisplacement = 20f
        }

        private fun createModerateBatteryLocationRequest() = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
            interval = 10000 // 10 seconds
            fastestInterval = 5000
            smallestDisplacement = 10f
        }

        private fun createNormalLocationRequest() = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 5000 // 5 seconds
            fastestInterval = 2000
            smallestDisplacement = 5f
        }

        private fun getBatteryLevel(): Int {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            } else {
                val batteryIntent = context.registerReceiver(
                    null,
                    IntentFilter(Intent.ACTION_BATTERY_CHANGED)
                )
                val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                if (level >= 0 && scale > 0) {
                    (level * 100 / scale)
                } else {
                    50 // Default to moderate battery
                }
            }
        }
    }

    // JobScheduler for background work
    class BackgroundWorkOptimizer {
        fun scheduleBackgroundWork(context: Context) {
            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler

            // Sync job - only on WiFi and charging
            val syncJob = JobInfo.Builder(JOB_ID_SYNC, ComponentName(context, SyncJobService::class.java))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
                .setRequiresCharging(true)
                .setRequiresDeviceIdle(true)
                .setPeriodic(24 * 60 * 60 * 1000) // Daily
                .build()

            // Cleanup job - flexible timing
            val cleanupJob = JobInfo.Builder(JOB_ID_CLEANUP, ComponentName(context, CleanupJobService::class.java))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setRequiresCharging(false)
                .setRequiresDeviceIdle(false)
                .setPeriodic(7 * 24 * 60 * 60 * 1000) // Weekly
                .setBackoffCriteria(30000, JobInfo.BACKOFF_POLICY_EXPONENTIAL)
                .build()

            jobScheduler.schedule(syncJob)
            jobScheduler.schedule(cleanupJob)
        }
    }

    // Doze mode compatibility
    class DozeOptimizer {
        fun configureForDoze(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

                // Check if app is whitelisted
                if (!powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
                    // Request whitelist (requires permission)
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    intent.data = Uri.parse("package:${context.packageName}")
                    context.startActivity(intent)
                }

                // Use WorkManager for Doze-compatible background work
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val workRequest = PeriodicWorkRequestBuilder<SyncWorker>(
                    15, TimeUnit.MINUTES // Minimum for Doze
                )
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(context)
                    .enqueueUniquePeriodicWork(
                        "sync",
                        ExistingPeriodicWorkPolicy.KEEP,
                        workRequest
                    )
            }
        }
    }
}
```

## 4. Performance Monitoring

### 4.1 Performance Metrics Collection

```kotlin
class PerformanceMonitor {
    private val metrics = mutableMapOf<String, PerformanceMetric>()

    // Frame rate monitoring
    class FrameRateMonitor : Choreographer.FrameCallback {
        private var frameCount = 0
        private var lastFrameTime = System.nanoTime()
        private val frameTimeBuffer = CircularFifoQueue<Long>(100)

        override fun doFrame(frameTimeNanos: Long) {
            val frameDuration = frameTimeNanos - lastFrameTime
            lastFrameTime = frameTimeNanos
            frameCount++

            frameTimeBuffer.add(frameDuration)

            if (frameCount % 100 == 0) {
                reportFrameMetrics()
            }

            Choreographer.getInstance().postFrameCallback(this)
        }

        private fun reportFrameMetrics() {
            val averageFrameTime = frameTimeBuffer.average()
            val fps = 1_000_000_000.0 / averageFrameTime
            val droppedFrames = frameTimeBuffer.count { it > 16_666_667 } // > 16.67ms

            PerformanceLogger.log(
                "Frame Metrics",
                mapOf(
                    "fps" to fps,
                    "average_frame_time_ms" to averageFrameTime / 1_000_000,
                    "dropped_frames" to droppedFrames
                )
            )
        }
    }

    // Network monitoring
    class NetworkMonitor {
        private var totalBytesSent = 0L
        private var totalBytesReceived = 0L
        private var requestCount = 0
        private var totalLatency = 0L

        fun recordRequest(
            bytesSent: Long,
            bytesReceived: Long,
            latency: Long
        ) {
            totalBytesSent += bytesSent
            totalBytesReceived += bytesReceived
            totalLatency += latency
            requestCount++

            if (requestCount % 100 == 0) {
                reportNetworkMetrics()
            }
        }

        private fun reportNetworkMetrics() {
            PerformanceLogger.log(
                "Network Metrics",
                mapOf(
                    "total_bytes_sent" to totalBytesSent,
                    "total_bytes_received" to totalBytesReceived,
                    "average_latency_ms" to totalLatency / requestCount,
                    "request_count" to requestCount
                )
            )
        }
    }

    // Database monitoring
    class DatabaseMonitor {
        fun monitorQuery(dao: Any, method: Method, args: Array<Any>?, result: Any?): Any? {
            val startTime = System.nanoTime()

            return try {
                result
            } finally {
                val duration = System.nanoTime() - startTime
                recordQueryMetrics(dao.javaClass.simpleName, method.name, duration)
            }
        }

        private fun recordQueryMetrics(dao: String, method: String, duration: Long) {
            PerformanceLogger.log(
                "Database Query",
                mapOf(
                    "dao" to dao,
                    "method" to method,
                    "duration_ms" to duration / 1_000_000
                )
            )
        }
    }
}
```

## 5. Performance Testing

### 5.1 Performance Test Suite

```kotlin
@RunWith(AndroidJUnit4::class)
class PerformanceTests {
    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @Test
    fun measureStartupTime() {
        val scenario = launchActivity<MainActivity>()

        benchmarkRule.measureRepeated {
            runWithTimingDisabled {
                scenario.close()
            }

            // Measure cold start
            val startTime = System.nanoTime()
            launchActivity<MainActivity>()
            val endTime = System.nanoTime()

            val startupTime = (endTime - startTime) / 1_000_000 // ms
            assertTrue("Startup time: ${startupTime}ms", startupTime < 3000)
        }
    }

    @Test
    fun measureMapPerformance() {
        benchmarkRule.measureRepeated {
            // Measure map rendering
            val mapView = MapView(context)
            mapView.measure(
                View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
            )
            mapView.layout(0, 0, 1080, 1920)
            mapView.draw(Canvas())
        }

        val metrics = benchmarkRule.getMetrics()
        assertTrue("Map render time", metrics.median < 33.0) // 30 FPS
    }

    @Test
    fun measureDatabasePerformance() = runTest {
        val database = createTestDatabase()
        val dao = database.trackDao()

        // Insert performance
        benchmarkRule.measureRepeated {
            val track = createTestTrack()
            dao.insertTrack(track)
        }

        // Query performance
        benchmarkRule.measureRepeated {
            dao.getAllTracks()
        }

        // Complex query performance
        benchmarkRule.measureRepeated {
            dao.getTracksInAreaWithPois(
                minLat = 52.0,
                maxLat = 53.0,
                minLon = 13.0,
                maxLon = 14.0
            )
        }
    }
}
```

## 6. Performance Best Practices

### 6.1 Development Guidelines

#### DO's:
- ✅ Profile before optimizing
- ✅ Use appropriate data structures
- ✅ Cache computed values
- ✅ Reuse objects when possible
- ✅ Use lazy initialization
- ✅ Batch database operations
- ✅ Compress network payloads
- ✅ Optimize images (size, format, caching)
- ✅ Use efficient layouts (ConstraintLayout)
- ✅ Implement view recycling

#### DON'Ts:
- ❌ Block the main thread
- ❌ Create objects in loops
- ❌ Use nested layouts excessively
- ❌ Load large bitmaps directly
- ❌ Perform I/O on main thread
- ❌ Keep references to contexts
- ❌ Use synchronous network calls
- ❌ Ignore memory leaks
- ❌ Parse JSON on main thread
- ❌ Animate during scrolling

### 6.2 Performance Checklist

#### Pre-Release:
- [ ] Run performance profiling
- [ ] Check startup time
- [ ] Verify 60 FPS animations
- [ ] Test on low-end devices
- [ ] Monitor memory usage
- [ ] Check battery consumption
- [ ] Validate network usage
- [ ] Review database queries
- [ ] Optimize APK size
- [ ] Enable R8/ProGuard

#### Post-Release:
- [ ] Monitor crash reports
- [ ] Track ANR rates
- [ ] Analyze performance metrics
- [ ] Review user feedback
- [ ] A/B test optimizations
- [ ] Update performance baselines

---

*This performance requirements and optimization document provides comprehensive guidelines for achieving optimal performance in the Survey Me application across all aspects of the system.*