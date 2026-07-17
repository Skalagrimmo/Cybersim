package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "player_stats")
data class PlayerStats(
    @PrimaryKey val id: Int = 1,
    val selectedClass: String = "Marine",
    val cyberModules: Int = 15,
    val hackingLevel: Int = 1,
    val repairLevel: Int = 1,
    val modifyLevel: Int = 1,
    val maintenanceLevel: Int = 1,
    val researchLevel: Int = 1,
    val combatLevel: Int = 1,
    val health: Int = 100,
    val maxHealth: Int = 100,
    val energy: Int = 50,
    val maxEnergy: Int = 50,
    val currentX: Int = 2,
    val currentY: Int = 2,
    val activeScreen: String = "MAIN_MENU",
    val statusText: String = "Systems Online."
)

@Entity(tableName = "inventory_items")
data class InventoryItem(
    @PrimaryKey val id: String,
    val name: String,
    val width: Int, // Grid slot width
    val height: Int, // Grid slot height
    val row: Int, // Grid placement row (0..5)
    val col: Int, // Grid placement column (0..5)
    val isWeapon: Boolean,
    val type: String, // "RIFLE", "PISTOL", "SHOTGUN", "PSI_AMP", "NANITES", "CELL", "MODULE"
    val color: Long // Representing color hex (e.g. 0xFF4A90E2)
)

@Entity(tableName = "map_tiles", primaryKeys = ["x", "y"])
data class MapTile(
    val x: Int,
    val y: Int,
    val elevation: Int, // 0..9 representing relief height
    val tileType: String, // "EMPTY", "WALL", "TERMINAL", "CHEST", "LOCKED_DOOR", "CAMERA", "MUTANT"
    val isExplored: Boolean = false,
    val isHacked: Boolean = false,
    val hasItem: Boolean = false,
    val itemType: String = "",
    val enemyHealth: Int = 0
)

@Dao
interface GameDao {
    @Query("SELECT * FROM player_stats WHERE id = 1")
    fun getPlayerStats(): Flow<PlayerStats?>

    @Query("SELECT * FROM player_stats WHERE id = 1")
    suspend fun getPlayerStatsSync(): PlayerStats?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayerStats(stats: PlayerStats)

    @Query("SELECT * FROM inventory_items")
    fun getInventoryItems(): Flow<List<InventoryItem>>

    @Query("SELECT * FROM inventory_items")
    suspend fun getInventoryItemsSync(): List<InventoryItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventoryItem(item: InventoryItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventoryItems(items: List<InventoryItem>)

    @Delete
    suspend fun deleteInventoryItem(item: InventoryItem)

    @Query("DELETE FROM inventory_items")
    suspend fun clearInventory()

    @Query("SELECT * FROM map_tiles")
    fun getMapTiles(): Flow<List<MapTile>>

    @Query("SELECT * FROM map_tiles")
    suspend fun getMapTilesSync(): List<MapTile>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMapTiles(tiles: List<MapTile>)

    @Query("UPDATE map_tiles SET isExplored = :explored WHERE x = :x AND y = :y")
    suspend fun updateTileExplored(x: Int, y: Int, explored: Boolean)

    @Query("UPDATE map_tiles SET tileType = :type, isHacked = :hacked, enemyHealth = :enemyHealth WHERE x = :x AND y = :y")
    suspend fun updateTileState(x: Int, y: Int, type: String, hacked: Boolean, enemyHealth: Int)

    @Query("DELETE FROM map_tiles")
    suspend fun clearMap()
}

@Database(entities = [PlayerStats::class, InventoryItem::class, MapTile::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
}
