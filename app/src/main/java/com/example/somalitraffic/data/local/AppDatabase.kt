package com.example.somalitraffic.data.local

import android.content.Context
import androidx.room.*
import com.example.somalitraffic.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TrafficDao {
    @Query("SELECT * FROM intersections WHERE status = 'active' ORDER BY name ASC")
    fun getAllIntersections(): Flow<List<Intersection>>

    @Query("SELECT * FROM intersections WHERE status = 'active' ORDER BY name ASC")
    suspend fun getAllIntersectionsSync(): List<Intersection>

    @Query("SELECT * FROM intersections WHERE id = :id LIMIT 1")
    suspend fun getIntersectionById(id: String): Intersection?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIntersections(intersections: List<Intersection>)

    @Query("DELETE FROM intersections")
    suspend fun deleteAllIntersections()

    @Query("DELETE FROM intersections WHERE id LIKE 'int-%'")
    suspend fun deleteMockIntersections()

    @Update
    suspend fun updateIntersection(intersection: Intersection)

    @Query("SELECT * FROM traffic_reports ORDER BY createdAt DESC LIMIT 50")
    fun getRecentReports(): Flow<List<TrafficReport>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrafficReport(report: TrafficReport)

    @Query("SELECT * FROM traffic_incidents WHERE status = 'active' ORDER BY createdAt DESC")
    fun getActiveIncidents(): Flow<List<TrafficIncident>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncidents(incidents: List<TrafficIncident>)
}

@Dao
interface DeviceDao {
    @Query("SELECT * FROM devices WHERE userId = :userId LIMIT 1")
    fun getDeviceForUser(userId: String): Flow<Device?>

    @Query("SELECT * FROM devices WHERE userId = :userId LIMIT 1")
    suspend fun getDeviceForUserDirect(userId: String): Device?

    @Query("SELECT * FROM devices WHERE userId = :userId ORDER BY updatedAt DESC, createdAt DESC")
    fun getDevicesForUser(userId: String): Flow<List<Device>>

    @Query("SELECT * FROM devices WHERE id = :deviceId LIMIT 1")
    fun getDeviceById(deviceId: String): Flow<Device?>

    @Query("SELECT * FROM devices WHERE id = :deviceId LIMIT 1")
    suspend fun getDeviceByIdDirect(deviceId: String): Device?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: Device)

    @Update
    suspend fun updateDevice(device: Device)

    @Query("DELETE FROM devices WHERE id = :deviceId")
    suspend fun deleteDevice(deviceId: String)

    @Query("DELETE FROM devices WHERE userId = :userId AND id != :keepDeviceId")
    suspend fun deleteOtherDevicesForUser(userId: String, keepDeviceId: String)

    @Query("SELECT * FROM devices ORDER BY lastSeen DESC")
    fun getAllDevices(): Flow<List<Device>>

    @Query("SELECT * FROM devices ORDER BY lastSeen DESC LIMIT 1")
    suspend fun getLatestDeviceDirect(): Device?

    @Query("SELECT * FROM device_locations WHERE deviceId = :deviceId ORDER BY recordedAt DESC LIMIT 20")
    fun getDeviceLocations(deviceId: String): Flow<List<DeviceLocation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeviceLocation(location: DeviceLocation)

    @Query("SELECT * FROM device_recovery_events WHERE deviceId = :deviceId ORDER BY createdAt DESC LIMIT 30")
    fun getDeviceEvents(deviceId: String): Flow<List<DeviceRecoveryEvent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeviceEvent(event: DeviceRecoveryEvent)
}

@Dao
interface SupportDao {
    @Query("SELECT * FROM support_tickets ORDER BY createdAt DESC")
    fun getAllTickets(): Flow<List<SupportTicket>>

    @Query("SELECT * FROM support_tickets WHERE userId = :userId ORDER BY createdAt DESC")
    fun getTicketsForUser(userId: String): Flow<List<SupportTicket>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTicket(ticket: SupportTicket)

    @Query("SELECT * FROM subscriptions WHERE userId = :userId ORDER BY CASE WHEN status IN ('active', 'completed', 'approved') THEN 1 ELSE 2 END, startDate DESC LIMIT 1")
    fun getSubscriptionForUser(userId: String): Flow<Subscription?>

    @Query("SELECT * FROM subscriptions WHERE userId = :userId OR (senderPhone IS NOT NULL AND senderPhone != '' AND (senderPhone = :phone OR :phone LIKE '%' || senderPhone OR senderPhone LIKE '%' || :phone)) ORDER BY CASE WHEN status IN ('active', 'completed', 'approved') THEN 1 ELSE 2 END, startDate DESC LIMIT 1")
    fun getSubscriptionForUserOrPhone(userId: String, phone: String): Flow<Subscription?>

    @Query("SELECT * FROM subscriptions WHERE userId = :userId ORDER BY CASE WHEN status IN ('active', 'completed', 'approved') THEN 1 ELSE 2 END, startDate DESC LIMIT 1")
    suspend fun getSubscriptionForUserSync(userId: String): Subscription?

    @Query("SELECT * FROM subscriptions WHERE userId = :userId OR (senderPhone IS NOT NULL AND senderPhone != '' AND (senderPhone = :phone OR :phone LIKE '%' || senderPhone OR senderPhone LIKE '%' || :phone)) ORDER BY CASE WHEN status IN ('active', 'completed', 'approved') THEN 1 ELSE 2 END, startDate DESC LIMIT 1")
    suspend fun getSubscriptionForUserOrPhoneSync(userId: String, phone: String): Subscription?

    @Query("UPDATE subscriptions SET status = :status WHERE userId = :userId")
    suspend fun updateAllSubscriptionsStatusForUser(userId: String, status: String)

    @Query("DELETE FROM subscriptions WHERE userId = :userId AND id != :keepId")
    suspend fun deleteOtherSubscriptionsForUser(userId: String, keepId: String)

    @Query("SELECT * FROM subscriptions WHERE id = :subscriptionId LIMIT 1")
    suspend fun getSubscriptionById(subscriptionId: String): Subscription?

    @Query("SELECT * FROM subscriptions ORDER BY startDate DESC")
    fun getAllSubscriptions(): Flow<List<Subscription>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(subscription: Subscription)

    @Query("SELECT * FROM audit_logs ORDER BY createdAt DESC LIMIT 100")
    fun getAuditLogs(): Flow<List<AuditLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLog)
}

@Dao
interface RideDao {
    @Query("SELECT * FROM rides ORDER BY createdAt DESC")
    fun getAllRides(): Flow<List<RideEntity>>

    @Query("SELECT * FROM rides WHERE id = :id LIMIT 1")
    fun getRideById(id: String): Flow<RideEntity?>

    @Query("SELECT * FROM rides WHERE id = :id LIMIT 1")
    suspend fun getRideByIdDirect(id: String): RideEntity?

    @Query("SELECT * FROM rides WHERE status NOT IN ('completed', 'cancelled') ORDER BY createdAt DESC LIMIT 1")
    fun getActiveRide(): Flow<RideEntity?>

    @Query("SELECT * FROM rides WHERE status = 'pending' ORDER BY createdAt DESC")
    fun getPendingRides(): Flow<List<RideEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRide(ride: RideEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRides(rides: List<RideEntity>)

    @Update
    suspend fun updateRide(ride: RideEntity)

    @Query("DELETE FROM rides WHERE id = :id")
    suspend fun deleteRide(id: String)
}

@Database(
    entities = [
        Intersection::class,
        TrafficReport::class,
        TrafficIncident::class,
        Device::class,
        DeviceLocation::class,
        DeviceRecoveryEvent::class,
        Subscription::class,
        SupportTicket::class,
        AuditLog::class,
        RideEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trafficDao(): TrafficDao
    abstract fun deviceDao(): DeviceDao
    abstract fun supportDao(): SupportDao
    abstract fun rideDao(): RideDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gobajaaj_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
