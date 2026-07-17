package com.example.data

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID
import kotlin.math.abs
import kotlin.math.sin

class GameRepository(private val context: Context) {

    private val database: AppDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "cybersim_game.db"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    private val gameDao = database.gameDao()

    val playerStats: Flow<PlayerStats?> = gameDao.getPlayerStats()
    val inventoryItems: Flow<List<InventoryItem>> = gameDao.getInventoryItems()
    val mapTiles: Flow<List<MapTile>> = gameDao.getMapTiles()

    suspend fun getPlayerStatsSync() = gameDao.getPlayerStatsSync()
    suspend fun getInventorySync() = gameDao.getInventoryItemsSync()
    suspend fun getMapTilesSync() = gameDao.getMapTilesSync()

    suspend fun updatePlayerStats(stats: PlayerStats) {
        gameDao.insertPlayerStats(stats)
    }

    suspend fun addInventoryItem(item: InventoryItem) {
        gameDao.insertInventoryItem(item)
    }

    suspend fun removeInventoryItem(item: InventoryItem) {
        gameDao.deleteInventoryItem(item)
    }

    suspend fun updateTileState(x: Int, y: Int, type: String, hacked: Boolean, enemyHealth: Int) {
        gameDao.updateTileState(x, y, type, hacked, enemyHealth)
    }

    suspend fun updateTileExplored(x: Int, y: Int, explored: Boolean) {
        gameDao.updateTileExplored(x, y, explored)
    }

    suspend fun insertMapTiles(tiles: List<MapTile>) {
        gameDao.insertMapTiles(tiles)
    }

    // Procedural grid relief generation and game state reset
    suspend fun startNewGame(playerClass: String) {
        gameDao.clearInventory()
        gameDao.clearMap()

        // Configure class-specific multipliers & defaults
        val classData = when (playerClass) {
            "Marine" -> Triple(120, 30, 1) to Pair(1, 3) // health, energy, hacking level, modify/repair, combat
            "Navy" -> Triple(90, 60, 3) to Pair(2, 1)
            "OSA" -> Triple(80, 80, 1) to Pair(1, 1)
            else -> Triple(100, 50, 1) to Pair(1, 1)
        }
        val hpStats = classData.first
        val skills = classData.second

        val playerStats = PlayerStats(
            selectedClass = playerClass,
            health = hpStats.first,
            maxHealth = hpStats.first,
            energy = hpStats.second,
            maxEnergy = hpStats.second,
            hackingLevel = hpStats.third,
            repairLevel = skills.first,
            combatLevel = skills.second,
            cyberModules = 10,
            currentX = 1,
            currentY = 1,
            activeScreen = "GAME",
            statusText = "Cybernetic systems online. Class initialized: $playerClass."
        )
        gameDao.insertPlayerStats(playerStats)

        // Seed initial items based on class
        val items = mutableListOf<InventoryItem>()
        when (playerClass) {
            "Marine" -> {
                // Assault Rifle takes 2x3 slots
                items.add(InventoryItem(UUID.randomUUID().toString(), "Heavy AR", 2, 3, 0, 0, true, "RIFLE", 0xFFFF4A4A))
                items.add(InventoryItem(UUID.randomUUID().toString(), "Ammo Pack", 1, 1, 0, 2, false, "NANITES", 0xFFE0E0E0))
                items.add(InventoryItem(UUID.randomUUID().toString(), "Medkit", 1, 1, 1, 2, false, "CELL", 0xFF2ECC71))
            }
            "Navy" -> {
                // Cyber Pistol takes 2x1 slots
                items.add(InventoryItem(UUID.randomUUID().toString(), "Cyber Pistol", 2, 1, 0, 0, true, "PISTOL", 0xFF00F0FF))
                // Hacking Device takes 1x2 slots
                items.add(InventoryItem(UUID.randomUUID().toString(), "Hack Module", 1, 2, 1, 0, false, "MODULE", 0xFFF1C40F))
                items.add(InventoryItem(UUID.randomUUID().toString(), "Nanite Injector", 1, 1, 0, 2, false, "NANITES", 0xFFE0E0E0))
            }
            "OSA" -> {
                // PSI Amp takes 1x2 slots
                items.add(InventoryItem(UUID.randomUUID().toString(), "PSI Amp", 1, 2, 0, 0, true, "PSI_AMP", 0xFF9B59B6))
                items.add(InventoryItem(UUID.randomUUID().toString(), "Psi Hypos", 1, 1, 0, 1, false, "CELL", 0xFF9B59B6))
                items.add(InventoryItem(UUID.randomUUID().toString(), "Cyber-Cube", 1, 1, 1, 1, false, "MODULE", 0xFFF1C40F))
            }
        }
        gameDao.insertInventoryItems(items)

        // Procedural Mosaic World map layout creation using mathematical waves
        val size = 12
        val mapTiles = mutableListOf<MapTile>()
        for (x in 0 until size) {
            for (y in 0 until size) {
                // Elevation simulated as a sinusoidal relief map (creates neat hills & valleys)
                val elevVal = (4 + sin(x.toDouble() * 0.8) * 2 + sin(y.toDouble() * 0.8) * 2).toInt()
                val elevation = elevVal.coerceIn(0, 8)

                var tileType = "EMPTY"
                var hasItem = false
                var itemType = ""
                var enemyHealth = 0

                // Scatter game objects across the mosaic world
                if (x == 1 && y == 1) {
                    tileType = "EMPTY" // Spawn point
                } else if ((x == 0 || y == 0 || x == size - 1 || y == size - 1) && (x + y) % 3 == 0) {
                    tileType = "WALL" // Boundary pillars
                } else if (x == 3 && y == 4) {
                    tileType = "TERMINAL" // Security hacking terminal 1
                } else if (x == 7 && y == 2) {
                    tileType = "TERMINAL" // Terminal 2 (Mainframe)
                } else if (x == 9 && y == 8) {
                    tileType = "TERMINAL" // Cyberspace gate
                } else if (x == 4 && y == 7) {
                    tileType = "LOCKED_DOOR" // Locked blast door
                } else if (x == 5 && y == 1) {
                    tileType = "CAMERA" // Security Camera (blocking paths)
                } else if (x == 8 && y == 10) {
                    tileType = "CAMERA" // Security Camera 2
                } else if (x == 2 && y == 6) {
                    tileType = "MUTANT" // Toxic mutant guard
                    enemyHealth = 40
                } else if (x == 8 && y == 5) {
                    tileType = "MUTANT" // High-security security cyborg
                    enemyHealth = 80
                } else if (x == 10 && y == 3) {
                    tileType = "CHEST" // High-tier supply crate
                    hasItem = true
                    itemType = "HEAVY_LASER" // Huge weapon (2x3) inside
                } else if ((x * y) % 17 == 7) {
                    tileType = "CHEST" // Medical locker
                    hasItem = true
                    itemType = "MEDKIT"
                } else if ((x + y) % 13 == 11) {
                    tileType = "CHEST" // Cyber Modules pile
                    hasItem = true
                    itemType = "MODULES"
                }

                mapTiles.add(
                    MapTile(
                        x = x,
                        y = y,
                        elevation = elevation,
                        tileType = tileType,
                        isExplored = (x <= 2 && y <= 2), // Spawn area starting revealed
                        isHacked = false,
                        hasItem = hasItem,
                        itemType = itemType,
                        enemyHealth = enemyHealth
                    )
                )
            }
        }

        // Apply Dijkstra-based procedural terrain carving to guarantee a fully navigable relief map
        val finalTiles = carveDijkstraPaths(mapTiles, size)
        gameDao.insertMapTiles(finalTiles)
    }

    // Dijkstra-based path carving to shape the procedural elevation and guarantee a fully navigable relief map
    private fun carveDijkstraPaths(tiles: List<MapTile>, gridSize: Int): List<MapTile> {
        val size = gridSize
        val tileMap = tiles.associateBy { it.x to it.y }.toMutableMap()

        // Key strategic coordinates we must guarantee are navigable from spawn (1, 1)
        val targets = listOf(
            3 to 4,   // Security Terminal 1
            7 to 2,   // Mainframe Terminal
            9 to 8,   // Cyberspace Gate Terminal
            4 to 7,   // Locked Blast Door
            5 to 1,   // Security Camera 1
            8 to 10,  // Security Camera 2
            2 to 6,   // Mutant Guard 1
            8 to 5,   // Cyborg Guard 2
            10 to 3,  // High-Tier Supply Chest
            10 to 10  // Exploration sector
        )

        val start = 1 to 1

        for (target in targets) {
            val (targetX, targetY) = target
            // Find a high-tolerance path from spawn (1, 1) that can step through wall/hills but is optimized
            val path = findHighTolerancePath(tileMap, start.first, start.second, targetX, targetY, size)
            if (path.isNotEmpty()) {
                var prevTile = tileMap[start]!!
                for (step in path) {
                    val currTile = tileMap[step] ?: continue
                    var updatedTile = currTile

                    // 1. Carve away WALLs along this path to keep path open
                    if (updatedTile.tileType == "WALL") {
                        updatedTile = updatedTile.copy(tileType = "EMPTY")
                    }

                    // 2. Smooth the relief elevation: ensure steepness (elevDiff) between steps is at most 1
                    val elevDiff = abs(updatedTile.elevation - prevTile.elevation)
                    if (elevDiff > 1) {
                        val newElev = if (updatedTile.elevation > prevTile.elevation) {
                            prevTile.elevation + 1
                        } else {
                            prevTile.elevation - 1
                        }
                        updatedTile = updatedTile.copy(elevation = newElev.coerceIn(0, 8))
                    }

                    tileMap[step] = updatedTile
                    prevTile = updatedTile
                }
            }
        }
        return tileMap.values.toList()
    }

    private fun findHighTolerancePath(
        tileMap: Map<Pair<Int, Int>, MapTile>,
        startX: Int,
        startY: Int,
        targetX: Int,
        targetY: Int,
        gridSize: Int
    ): List<Pair<Int, Int>> {
        val dist = mutableMapOf<Pair<Int, Int>, Double>()
        val parent = mutableMapOf<Pair<Int, Int>, Pair<Int, Int>>()
        val unvisited = mutableSetOf<Pair<Int, Int>>()

        for (x in 0 until gridSize) {
            for (y in 0 until gridSize) {
                val node = x to y
                dist[node] = Double.MAX_VALUE
                unvisited.add(node)
            }
        }

        val start = startX to startY
        dist[start] = 0.0

        val directions = listOf(
            0 to 1, 1 to 0, 0 to -1, -1 to 0
        )

        while (unvisited.isNotEmpty()) {
            val u = unvisited.minByOrNull { dist[it] ?: Double.MAX_VALUE } ?: break
            if (u == targetX to targetY) break
            if (dist[u] == Double.MAX_VALUE) break

            unvisited.remove(u)

            val uTile = tileMap[u] ?: continue
            val uElev = uTile.elevation

            for ((dx, dy) in directions) {
                val v = (u.first + dx) to (u.second + dy)
                if (v in unvisited) {
                    val vTile = tileMap[v] ?: continue
                    val vElev = vTile.elevation
                    val elevDiff = abs(vElev - uElev)

                    // Custom weight formula for carving:
                    // Base transition weight (1.0)
                    // Slope steepness penalty (elevDiff * 10.0)
                    // High but traversable wall crossing cost (50.0)
                    val baseCost = 1.0 + (elevDiff * 10.0)
                    val obstacleCost = if (vTile.tileType == "WALL") 50.0 else 0.0
                    val weight = baseCost + obstacleCost

                    val alt = (dist[u] ?: Double.MAX_VALUE) + weight
                    if (alt < (dist[v] ?: Double.MAX_VALUE)) {
                        dist[v] = alt
                        parent[v] = u
                    }
                }
            }
        }

        val path = mutableListOf<Pair<Int, Int>>()
        var curr = targetX to targetY
        if (parent[curr] == null && curr != start) return emptyList()

        while (curr != start) {
            path.add(0, curr)
            curr = parent[curr] ?: break
        }
        return path
    }

    // Dijkstra's Algorithm implementation on our procedurally-generated relief map.
    // Movement costs are proportional to the change in elevation (relief difficulty)
    // plus base tile difficulty.
    fun findShortestPath(
        tiles: List<MapTile>,
        startX: Int,
        startY: Int,
        targetX: Int,
        targetY: Int,
        gridSize: Int = 12
    ): List<Pair<Int, Int>> {
        val tileMap = tiles.associateBy { it.x to it.y }
        val dist = mutableMapOf<Pair<Int, Int>, Double>()
        val parent = mutableMapOf<Pair<Int, Int>, Pair<Int, Int>>()
        val unvisited = mutableSetOf<Pair<Int, Int>>()

        for (x in 0 until gridSize) {
            for (y in 0 until gridSize) {
                val node = x to y
                dist[node] = Double.MAX_VALUE
                unvisited.add(node)
            }
        }

        val start = startX to startY
        dist[start] = 0.0

        val directions = listOf(
            0 to 1, 1 to 0, 0 to -1, -1 to 0
        )

        while (unvisited.isNotEmpty()) {
            val u = unvisited.minByOrNull { dist[it] ?: Double.MAX_VALUE } ?: break
            if (u == targetX to targetY) break
            if (dist[u] == Double.MAX_VALUE) break

            unvisited.remove(u)

            val uTile = tileMap[u] ?: continue
            val uElev = uTile.elevation

            for ((dx, dy) in directions) {
                val v = (u.first + dx) to (u.second + dy)
                if (v in unvisited) {
                    val vTile = tileMap[v] ?: continue
                    if (vTile.tileType == "WALL") continue // Obstacle block

                    val vElev = vTile.elevation
                    val elevDiff = abs(vElev - uElev)
                    
                    // Dijkstra cost weight formula:
                    // Base transition (1.0) + elevation differences (relief steepness adds 0.5 cost per scale level)
                    // Mutants and locked doors add high crossing weight unless hacked/cleared
                    val baseCost = 1.0 + (elevDiff * 0.5)
                    val obstacleCost = when (vTile.tileType) {
                        "MUTANT" -> if (vTile.enemyHealth > 0) 4.0 else 0.0
                        "LOCKED_DOOR" -> if (!vTile.isHacked) 10.0 else 0.0
                        else -> 0.0
                    }
                    val weight = baseCost + obstacleCost

                    val alt = (dist[u] ?: Double.MAX_VALUE) + weight
                    if (alt < (dist[v] ?: Double.MAX_VALUE)) {
                        dist[v] = alt
                        parent[v] = u
                    }
                }
            }
        }

        // Reconstruct path
        val path = mutableListOf<Pair<Int, Int>>()
        var curr = targetX to targetY
        if (parent[curr] == null && curr != start) return emptyList()

        while (curr != start) {
            path.add(0, curr)
            curr = parent[curr] ?: break
        }
        return path
    }
}
