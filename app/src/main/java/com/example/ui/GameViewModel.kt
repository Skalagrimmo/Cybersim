package com.example.ui

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.*

// 3D Point for Cyberspace Wireframe Projection
data class Vector3D(val x: Float, val y: Float, val z: Float) {
    fun minus(other: Vector3D) = Vector3D(x - other.x, y - other.y, z - other.z)
}

data class CyberObstacle(
    val id: String,
    var pos: Vector3D,
    val type: String, // "CUBE" (data cube), "SHIELD" (recharge), "MINE" (explodes), "NODE" (security turret)
    var health: Int = 20,
    val color: Long
)

data class CyberLaser(
    var pos: Vector3D,
    val dir: Vector3D,
    val isPlayerShot: Boolean
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GameRepository(application)

    // Flow states
    val playerStats: StateFlow<PlayerStats?> = repository.playerStats.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val inventoryItems: StateFlow<List<InventoryItem>> = repository.inventoryItems.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val mapTiles: StateFlow<List<MapTile>> = repository.mapTiles.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Dijkstra variables for local tracking
    private val _selectedMapTile = MutableStateFlow<Pair<Int, Int>?>(null)
    val selectedMapTile = _selectedMapTile.asStateFlow()

    private val _currentPath = MutableStateFlow<List<Pair<Int, Int>>>(emptyList())
    val currentPath = _currentPath.asStateFlow()

    // Interactive inventory dragging/moving state
    private val _heldItem = MutableStateFlow<InventoryItem?>(null)
    val heldItem = _heldItem.asStateFlow()

    // Rotation flag for the held item (false = default, true = rotated 90 degrees)
    private val _heldItemRotated = MutableStateFlow(false)
    val heldItemRotated = _heldItemRotated.asStateFlow()

    // Game screen state overlay to handle transitions beautifully
    private val _currentScreen = MutableStateFlow("MAIN_MENU")
    val currentScreen = _currentScreen.asStateFlow()

    // --- CYBERSPACE WIREFRAME 3D STATE ENGINE ---
    private val _cyberHealth = MutableStateFlow(100)
    val cyberHealth = _cyberHealth.asStateFlow()

    private val _cyberShield = MutableStateFlow(100)
    val cyberShield = _cyberShield.asStateFlow()

    private val _cyberDataCubesRemaining = MutableStateFlow(5)
    val cyberDataCubesRemaining = _cyberDataCubesRemaining.asStateFlow()

    // 3D Camera coordinates
    private val _cyberCamPos = MutableStateFlow(Vector3D(0f, 0f, 0f))
    val cyberCamPos = _cyberCamPos.asStateFlow()

    // Angles (in radians)
    private val _cyberYaw = MutableStateFlow(0f) // Left-Right rotation
    val cyberYaw = _cyberYaw.asStateFlow()

    private val _cyberPitch = MutableStateFlow(0f) // Up-Down angle
    val cyberPitch = _cyberPitch.asStateFlow()

    private val _cyberSpeed = MutableStateFlow(0.12f)
    val cyberSpeed = _cyberSpeed.asStateFlow()

    // Interactive items in cyberspace
    val cyberObstacles = mutableStateListOf<CyberObstacle>()
    val cyberLasers = mutableStateListOf<CyberLaser>()

    // Hacked terminal tracking
    private var activeHackingTerminalX: Int = -1
    private var activeHackingTerminalY: Int = -1

    // Game loops
    private var cyberGameJob: Job? = null

    init {
        // Sync database active screen on launch
        viewModelScope.launch {
            repository.playerStats.collect { stats ->
                if (stats != null) {
                    _currentScreen.value = stats.activeScreen
                }
            }
        }
    }

    fun setScreen(screen: String) {
        _currentScreen.value = screen
        viewModelScope.launch {
            val stats = repository.getPlayerStatsSync() ?: return@launch
            repository.updatePlayerStats(stats.copy(activeScreen = screen))
        }
    }

    fun startNewGame(playerClass: String) {
        viewModelScope.launch {
            repository.startNewGame(playerClass)
            _selectedMapTile.value = null
            _currentPath.value = emptyList()
            _heldItem.value = null
            _heldItemRotated.value = false
            setScreen("GAME")
        }
    }

    // --- IMMERSIVE DIJKSTRA PATHFINDING AND MOVEMENT ---
    fun selectTile(x: Int, y: Int) {
        _selectedMapTile.value = x to y
        viewModelScope.launch {
            val stats = repository.getPlayerStatsSync() ?: return@launch
            val tiles = repository.getMapTilesSync()
            val path = repository.findShortestPath(tiles, stats.currentX, stats.currentY, x, y)
            _currentPath.value = path
        }
    }

    fun moveAlongPath() {
        val path = _currentPath.value
        if (path.isEmpty()) return

        viewModelScope.launch {
            val stats = repository.getPlayerStatsSync() ?: return@launch
            val tiles = repository.getMapTilesSync()
            val tileMap = tiles.associateBy { it.x to it.y }

            var currentStats = stats
            var energyCost = 0
            var movesMade = 0

            for (step in path) {
                val tile = tileMap[step] ?: continue
                if (tile.tileType == "WALL") break
                if (tile.tileType == "LOCKED_DOOR" && !tile.isHacked) {
                    currentStats = currentStats.copy(statusText = "Path blocked by locked Blast Door. Locate Hacking Terminal.")
                    break
                }
                if (tile.tileType == "MUTANT" && tile.enemyHealth > 0) {
                    currentStats = currentStats.copy(statusText = "Engaged by Mutant Guard! Defensive measures active.")
                    break
                }

                // Energy cost formula based on tile relief elevation
                val baseStepCost = 2
                val elevationDifference = abs(tile.elevation - (tileMap[currentStats.currentX to currentStats.currentY]?.elevation ?: tile.elevation))
                val totalStepCost = baseStepCost + elevationDifference

                if (currentStats.energy < totalStepCost) {
                    currentStats = currentStats.copy(statusText = "Low Energy reserves. Re-routing or consume battery cell.")
                    break
                }

                energyCost += totalStepCost
                movesMade++
                currentStats = currentStats.copy(
                    currentX = step.first,
                    currentY = step.second,
                    energy = currentStats.energy - totalStepCost
                )
                repository.updateTileExplored(step.first, step.second, true)
            }

            if (movesMade > 0) {
                val statusMessage = "Moved $movesMade sectors along relief path. Energy expended: $energyCost units."
                currentStats = currentStats.copy(statusText = statusMessage)
                repository.updatePlayerStats(currentStats)
                _currentPath.value = _currentPath.value.drop(movesMade)
                if (_currentPath.value.isEmpty()) {
                    _selectedMapTile.value = null
                }
            } else {
                repository.updatePlayerStats(currentStats.copy(statusText = "Awaiting movement commands..."))
            }
        }
    }

    // --- CYBER CHARACTER STAT UPGRADE TERMINAL ---
    fun upgradeStat(statName: String) {
        viewModelScope.launch {
            val stats = repository.getPlayerStatsSync() ?: return@launch
            val currentLevel = when (statName) {
                "Hacking" -> stats.hackingLevel
                "Repair" -> stats.repairLevel
                "Modify" -> stats.modifyLevel
                "Maintenance" -> stats.maintenanceLevel
                "Research" -> stats.researchLevel
                "Combat" -> stats.combatLevel
                else -> 1
            }

            if (currentLevel >= 6) {
                repository.updatePlayerStats(stats.copy(statusText = "$statName is already maximized (Lvl 6)."))
                return@launch
            }

            // Cyber Modules scaling progression formula: lvl * 8 modules
            val cost = currentLevel * 8
            if (stats.cyberModules < cost) {
                repository.updatePlayerStats(stats.copy(statusText = "Insufficient Cyber Modules. Needed: $cost. Have: ${stats.cyberModules}."))
                return@launch
            }

            val newStats = when (statName) {
                "Hacking" -> stats.copy(hackingLevel = stats.hackingLevel + 1, cyberModules = stats.cyberModules - cost)
                "Repair" -> stats.copy(repairLevel = stats.repairLevel + 1, cyberModules = stats.cyberModules - cost, maxEnergy = stats.maxEnergy + 10, energy = stats.energy + 10)
                "Modify" -> stats.copy(modifyLevel = stats.modifyLevel + 1, cyberModules = stats.cyberModules - cost)
                "Maintenance" -> stats.copy(maintenanceLevel = stats.maintenanceLevel + 1, cyberModules = stats.cyberModules - cost)
                "Research" -> stats.copy(researchLevel = stats.researchLevel + 1, cyberModules = stats.cyberModules - cost)
                "Combat" -> stats.copy(combatLevel = stats.combatLevel + 1, cyberModules = stats.cyberModules - cost, maxHealth = stats.maxHealth + 15, health = stats.health + 15)
                else -> stats
            }

            repository.updatePlayerStats(newStats.copy(statusText = "$statName successfully upgraded to Level ${currentLevel + 1}!"))
        }
    }

    // --- IMMERSIVE COMBAT ENCOUNTERS ---
    fun attackMutant(x: Int, y: Int) {
        viewModelScope.launch {
            val stats = repository.getPlayerStatsSync() ?: return@launch
            val tiles = repository.getMapTilesSync()
            val targetTile = tiles.find { it.x == x && it.y == y && it.tileType == "MUTANT" } ?: return@launch

            if (abs(stats.currentX - x) > 1 || abs(stats.currentY - y) > 1) {
                repository.updatePlayerStats(stats.copy(statusText = "Target out of effective weapon range. Move closer."))
                return@launch
            }

            val inventory = repository.getInventorySync()
            val primaryWeapon = inventory.find { it.isWeapon }

            // Combat math incorporating character stats & equipped weapons
            val baseDamage = when (primaryWeapon?.type) {
                "RIFLE" -> 25
                "PISTOL" -> 15
                "PSI_AMP" -> 20 + (stats.researchLevel * 5)
                else -> 10 // Bare fists
            }

            val criticalMultiplier = 1.0 + (stats.combatLevel * 0.15)
            val dealtDamage = (baseDamage * criticalMultiplier).toInt()

            val newEnemyHealth = (targetTile.enemyHealth - dealtDamage).coerceAtLeast(0)

            // Reciprocal mutant retaliation damage
            val incomingBase = if (targetTile.enemyHealth > 80) 25 else 15
            val incomingMitigated = (incomingBase - (stats.combatLevel * 2)).coerceAtLeast(5)
            val newPlayerHealth = (stats.health - incomingMitigated).coerceAtLeast(0)

            if (newPlayerHealth <= 0) {
                // Defeat State
                repository.updatePlayerStats(stats.copy(health = 0, statusText = "CRITICAL FAILURE. Vitals flatlining. Biological reconstruction needed.", activeScreen = "GAME_OVER"))
                setScreen("GAME_OVER")
                return@launch
            }

            if (newEnemyHealth <= 0) {
                // Defeated Mutant: Rewards Cyber Modules and clears sector
                val modulesAwarded = if (targetTile.enemyHealth > 50) 12 else 6
                repository.updateTileState(x, y, "EMPTY", hacked = false, enemyHealth = 0)
                repository.updatePlayerStats(stats.copy(
                    health = newPlayerHealth,
                    cyberModules = stats.cyberModules + modulesAwarded,
                    statusText = "Mutant terminated! Dealt $dealtDamage DMG. Gained $modulesAwarded Cyber Modules. Took $incomingMitigated DMG."
                ))
            } else {
                repository.updateTileState(x, y, "MUTANT", hacked = false, enemyHealth = newEnemyHealth)
                repository.updatePlayerStats(stats.copy(
                    health = newPlayerHealth,
                    statusText = "Shot Mutant! Dealt $dealtDamage DMG. Enemy HP: $newEnemyHealth/100. Took $incomingMitigated counter DMG."
                ))
            }
        }
    }

    // --- CONTAINER SEARCH AND LOOTING ---
    fun lootChest(x: Int, y: Int) {
        viewModelScope.launch {
            val stats = repository.getPlayerStatsSync() ?: return@launch
            val tiles = repository.getMapTilesSync()
            val tile = tiles.find { it.x == x && it.y == y && it.tileType == "CHEST" } ?: return@launch

            if (abs(stats.currentX - x) > 1 || abs(stats.currentY - y) > 1) {
                repository.updatePlayerStats(stats.copy(statusText = "Supply crate too far. Advance adjacent to loot."))
                return@launch
            }

            // Reward based on chest content type
            val message = when (tile.itemType) {
                "HEAVY_LASER" -> {
                    // Huge laser weapon 2x3
                    val added = attemptAutomaticPlace(InventoryItem(UUID.randomUUID().toString(), "Heavy Laser", 2, 3, 0, 0, true, "RIFLE", 0xFFFF0055))
                    if (added) "Acquired prototype Heavy Laser (2x3 slots) from crate!" else "No storage space for prototype Laser rifle."
                }
                "MEDKIT" -> {
                    val added = attemptAutomaticPlace(InventoryItem(UUID.randomUUID().toString(), "Nanite Patch", 1, 1, 0, 0, false, "CELL", 0xFF2ECC71))
                    if (added) "Recovered Nanite Medical Cell!" else "Inventory full. Clear slot to retrieve Medical Cell."
                }
                "MODULES" -> {
                    val count = 8 + stats.researchLevel * 2
                    repository.updatePlayerStats(stats.copy(cyberModules = stats.cyberModules + count, statusText = "Harvested $count Cyber Modules from mainframe module pile!"))
                    repository.updateTileState(x, y, "EMPTY", hacked = true, enemyHealth = 0)
                    return@launch
                }
                else -> {
                    val count = 5
                    repository.updatePlayerStats(stats.copy(cyberModules = stats.cyberModules + count, statusText = "Retrieved discarded parts. Cyber Modules +$count."))
                    repository.updateTileState(x, y, "EMPTY", hacked = true, enemyHealth = 0)
                    return@launch
                }
            }

            if (message.contains("Acquired") || message.contains("Recovered")) {
                repository.updateTileState(x, y, "EMPTY", hacked = true, enemyHealth = 0)
                repository.updatePlayerStats(stats.copy(statusText = message))
            } else {
                repository.updatePlayerStats(stats.copy(statusText = message))
            }
        }
    }

    // --- GRID PERSISTENT INVENTORY ENGINE ---
    fun selectHeldItem(item: InventoryItem) {
        _heldItem.value = item
        _heldItemRotated.value = false
    }

    fun rotateHeldItem() {
        val currentItem = _heldItem.value ?: return
        _heldItemRotated.value = !_heldItemRotated.value
    }

    fun dropHeldItem() {
        val item = _heldItem.value ?: return
        viewModelScope.launch {
            repository.removeInventoryItem(item)
            _heldItem.value = null
            _heldItemRotated.value = false
            val stats = repository.getPlayerStatsSync() ?: return@launch
            repository.updatePlayerStats(stats.copy(statusText = "Discarded ${item.name} to free grid storage space."))
        }
    }

    fun useItem(item: InventoryItem) {
        viewModelScope.launch {
            val stats = repository.getPlayerStatsSync() ?: return@launch
            val updatedStats = when (item.type) {
                "CELL" -> {
                    // Restore health
                    val restoreAmount = 40 + (stats.repairLevel * 10)
                    stats.copy(
                        health = (stats.health + restoreAmount).coerceAtMost(stats.maxHealth),
                        statusText = "Consumed Nanite Medkit. Healed $restoreAmount HP."
                    )
                }
                "NANITES" -> {
                    // Restore energy
                    val restoreAmount = 25 + (stats.modifyLevel * 10)
                    stats.copy(
                        energy = (stats.energy + restoreAmount).coerceAtMost(stats.maxEnergy),
                        statusText = "Consumed Battery Cell. Energy recharged: +$restoreAmount."
                    )
                }
                "MODULE" -> {
                    // Consume database item to inject Cyber Modules directly
                    stats.copy(
                        cyberModules = stats.cyberModules + 6,
                        statusText = "Decrypted Datapad Cyber-Cube! Cyber Modules +6."
                    )
                }
                else -> {
                    stats.copy(statusText = "Equipped weapon: ${item.name}. Combat bonus active.")
                }
            }

            // Remove consumables
            if (item.type == "CELL" || item.type == "NANITES" || item.type == "MODULE") {
                repository.removeInventoryItem(item)
            }
            repository.updatePlayerStats(updatedStats)
            _heldItem.value = null
        }
    }

    fun placeHeldItem(row: Int, col: Int) {
        val item = _heldItem.value ?: return
        viewModelScope.launch {
            val rotated = _heldItemRotated.value
            val itemW = if (rotated) item.height else item.width
            val itemH = if (rotated) item.width else item.height

            // Constraints validation
            if (col + itemW > 6 || row + itemH > 6) {
                val stats = repository.getPlayerStatsSync()
                stats?.let { repository.updatePlayerStats(it.copy(statusText = "Item exceeds inventory grid perimeter!")) }
                return@launch
            }

            val items = repository.getInventorySync().filter { it.id != item.id }
            if (isGridOccupied(row, col, itemW, itemH, items)) {
                val stats = repository.getPlayerStatsSync()
                stats?.let { repository.updatePlayerStats(it.copy(statusText = "Grid slots occupied by other bio-gear!")) }
                return@launch
            }

            // Enforce 5 weapon max capacity
            if (item.isWeapon) {
                val weaponsCount = items.count { it.isWeapon }
                if (weaponsCount >= 5) {
                    val stats = repository.getPlayerStatsSync()
                    stats?.let { repository.updatePlayerStats(it.copy(statusText = "Weapon weight limit exceeded. Max 5 weapons carried.")) }
                    return@launch
                }
            }

            // Update item grid position in database
            val updatedItem = item.copy(
                row = row,
                col = col,
                width = itemW,
                height = itemH
            )
            repository.addInventoryItem(updatedItem)
            _heldItem.value = null
            _heldItemRotated.value = false

            val stats = repository.getPlayerStatsSync()
            stats?.let { repository.updatePlayerStats(it.copy(statusText = "Stored ${item.name} in cyber-grid cluster ($col, $row).")) }
        }
    }

    private fun isGridOccupied(startRow: Int, startCol: Int, w: Int, h: Int, items: List<InventoryItem>): Boolean {
        val grid = Array(6) { BooleanArray(6) { false } }
        for (item in items) {
            for (r in item.row until (item.row + item.height)) {
                for (c in item.col until (item.col + item.width)) {
                    if (r in 0..5 && c in 0..5) {
                        grid[r][c] = true
                    }
                }
            }
        }

        for (r in startRow until (startRow + h)) {
            for (c in startCol until (startCol + w)) {
                if (r !in 0..5 || c !in 0..5 || grid[r][c]) {
                    return true
                }
            }
        }
        return false
    }

    private suspend fun attemptAutomaticPlace(newItem: InventoryItem): Boolean {
        val items = repository.getInventorySync()
        // Find first empty top-left spot fit
        for (row in 0..5) {
            for (col in 0..5) {
                val w = newItem.width
                val h = newItem.height
                if (col + w <= 6 && row + h <= 6) {
                    if (!isGridOccupied(row, col, w, h, items)) {
                        repository.addInventoryItem(newItem.copy(row = row, col = col))
                        return true
                    }
                }
            }
        }
        return false
    }

    // --- CYBERSPACE TERMINAL INITIATION ---
    fun startHackingTerminal(x: Int, y: Int) {
        viewModelScope.launch {
            val stats = repository.getPlayerStatsSync() ?: return@launch
            val tiles = repository.getMapTilesSync()
            val tile = tiles.find { it.x == x && it.y == y && it.tileType == "TERMINAL" } ?: return@launch

            if (abs(stats.currentX - x) > 1 || abs(stats.currentY - y) > 1) {
                repository.updatePlayerStats(stats.copy(statusText = "Terminal out of contact. Move adjacent to hack."))
                return@launch
            }

            activeHackingTerminalX = x
            activeHackingTerminalY = y

            // Skill checks
            val baseTime = 120
            val modifier = stats.hackingLevel * 20
            val totalHackingSeconds = (baseTime + modifier).coerceAtLeast(30)

            _cyberHealth.value = 100
            _cyberShield.value = 50 + stats.hackingLevel * 10
            _cyberCamPos.value = Vector3D(0f, 0f, -10f)
            _cyberYaw.value = 0f
            _cyberPitch.value = 0f
            _cyberSpeed.value = 0.15f

            // Populate Cyberspace obstacles procedurally (3D coordinates)
            cyberObstacles.clear()
            cyberLasers.clear()

            // 5 Data Cubes
            cyberObstacles.add(CyberObstacle("C1", Vector3D(-5f, 2f, 15f), "CUBE", color = 0xFF00E5FF))
            cyberObstacles.add(CyberObstacle("C2", Vector3D(4f, -2f, 30f), "CUBE", color = 0xFF00E5FF))
            cyberObstacles.add(CyberObstacle("C3", Vector3D(-2f, -3f, 45f), "CUBE", color = 0xFF00E5FF))
            cyberObstacles.add(CyberObstacle("C4", Vector3D(6f, 3f, 60f), "CUBE", color = 0xFF00E5FF))
            cyberObstacles.add(CyberObstacle("C5", Vector3D(0f, 0f, 75f), "CUBE", color = 0xFF00E5FF))

            _cyberDataCubesRemaining.value = 5

            // Turret nodes (shooting laser pulses) and security shields
            cyberObstacles.add(CyberObstacle("T1", Vector3D(3f, 3f, 20f), "NODE", health = 20, color = 0xFFFF0033))
            cyberObstacles.add(CyberObstacle("T2", Vector3D(-3f, -4f, 38f), "NODE", health = 20, color = 0xFFFF0033))
            cyberObstacles.add(CyberObstacle("T3", Vector3D(0f, 4f, 52f), "NODE", health = 30, color = 0xFFFF0033))

            // Proximity mines
            cyberObstacles.add(CyberObstacle("M1", Vector3D(-2f, 0f, 10f), "MINE", color = 0xFFFF9900))
            cyberObstacles.add(CyberObstacle("M2", Vector3D(2f, 1f, 25f), "MINE", color = 0xFFFF9900))
            cyberObstacles.add(CyberObstacle("M3", Vector3D(-4f, 2f, 48f), "MINE", color = 0xFFFF9900))
            cyberObstacles.add(CyberObstacle("M4", Vector3D(3f, -1f, 68f), "MINE", color = 0xFFFF9900))

            setScreen("CYBERSPACE")
            startCyberspaceLoop()
        }
    }

    private fun startCyberspaceLoop() {
        cyberGameJob?.cancel()
        cyberGameJob = viewModelScope.launch {
            var counter = 0
            while (_currentScreen.value == "CYBERSPACE" && _cyberHealth.value > 0) {
                delay(33) // ~30 FPS physical rendering logic tick
                counter++

                // Retrieve and compute flying vectors from Yaw / Pitch angles
                val yawRad = _cyberYaw.value
                val pitchRad = _cyberPitch.value
                val dx = sin(yawRad) * cos(pitchRad)
                val dy = sin(pitchRad)
                val dz = cos(yawRad) * cos(pitchRad)

                val speed = _cyberSpeed.value
                val currentCam = _cyberCamPos.value
                _cyberCamPos.value = Vector3D(
                    currentCam.x + dx * speed,
                    currentCam.y + dy * speed,
                    currentCam.z + dz * speed
                )

                // Loop lasers
                val iterator = cyberLasers.iterator()
                while (iterator.hasNext()) {
                    val laser = iterator.next()
                    laser.pos = Vector3D(
                        laser.pos.x + laser.dir.x * 0.9f,
                        laser.pos.y + laser.dir.y * 0.9f,
                        laser.pos.z + laser.dir.z * 0.9f
                    )

                    // Collision bounds check between player lasers and cyber turret nodes
                    if (laser.isPlayerShot) {
                        for (obs in cyberObstacles) {
                            if (obs.type == "NODE" && obs.health > 0) {
                                val dist = calculateDistance(laser.pos, obs.pos)
                                if (dist < 2.5f) {
                                    obs.health -= 10
                                    laser.pos = Vector3D(999f, 999f, 999f) // Destroy laser
                                    break
                                }
                            }
                        }
                    } else {
                        // Enemy laser vs player camera hit checks
                        val distToPlayer = calculateDistance(laser.pos, _cyberCamPos.value)
                        if (distToPlayer < 1.8f) {
                            damageCyberPlayer(15)
                            laser.pos = Vector3D(999f, 999f, 999f) // Destroy laser
                        }
                    }
                }

                // Clean up distant or out-of-bounds lasers
                cyberLasers.removeAll { calculateDistance(it.pos, _cyberCamPos.value) > 120f || it.pos.x > 900f }

                // Enemy AI: Nodes periodically track player and fire lasers
                if (counter % 45 == 0) {
                    for (obs in cyberObstacles) {
                        if (obs.type == "NODE" && obs.health > 0) {
                            val playerPos = _cyberCamPos.value
                            val vectorToPlayer = playerPos.minus(obs.pos)
                            val distance = sqrt(vectorToPlayer.x * vectorToPlayer.x + vectorToPlayer.y * vectorToPlayer.y + vectorToPlayer.z * vectorToPlayer.z)
                            if (distance < 50f && distance > 2f) {
                                val dir = Vector3D(
                                    vectorToPlayer.x / distance,
                                    vectorToPlayer.y / distance,
                                    vectorToPlayer.z / distance
                                )
                                cyberLasers.add(CyberLaser(obs.pos, dir, isPlayerShot = false))
                            }
                        }
                    }
                }

                // Collision bounds check between player and data cubes, mines
                val activeCam = _cyberCamPos.value
                val remainingObstacles = mutableListOf<CyberObstacle>()
                for (obs in cyberObstacles) {
                    val dist = calculateDistance(activeCam, obs.pos)
                    if (dist < 2.2f) {
                        when (obs.type) {
                            "CUBE" -> {
                                // Collected Data Cube
                                _cyberDataCubesRemaining.value -= 1
                            }
                            "MINE" -> {
                                // Exploded
                                damageCyberPlayer(30)
                            }
                            "NODE" -> {
                                if (obs.health > 0) {
                                    damageCyberPlayer(20)
                                } else {
                                    remainingObstacles.add(obs)
                                }
                            }
                        }
                    } else {
                        remainingObstacles.add(obs)
                    }
                }

                if (cyberObstacles.size != remainingObstacles.size) {
                    cyberObstacles.clear()
                    cyberObstacles.addAll(remainingObstacles)
                }

                // Check Hack Win condition: All data cubes harvested
                if (_cyberDataCubesRemaining.value <= 0) {
                    executeSuccessfulHack()
                    break
                }
            }

            if (_cyberHealth.value <= 0) {
                executeFailedHack()
            }
        }
    }

    private fun calculateDistance(p1: Vector3D, p2: Vector3D): Float {
        val dx = p1.x - p2.x
        val dy = p1.y - p2.y
        val dz = p1.z - p2.z
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    private fun damageCyberPlayer(amount: Int) {
        val currentShield = _cyberShield.value
        if (currentShield >= amount) {
            _cyberShield.value -= amount
        } else {
            val bleedDamage = amount - currentShield
            _cyberShield.value = 0
            _cyberHealth.value = (_cyberHealth.value - bleedDamage).coerceAtLeast(0)
        }
    }

    // Interactive flying controls
    fun steerCyberYaw(delta: Float) {
        _cyberYaw.value += delta
    }

    fun steerCyberPitch(delta: Float) {
        _cyberPitch.value = (_cyberPitch.value + delta).coerceIn(-1.4f, 1.4f)
    }

    fun adjustCyberSpeed(delta: Float) {
        _cyberSpeed.value = (_cyberSpeed.value + delta).coerceIn(0.04f, 0.4f)
    }

    fun fireCyberLaser() {
        if (_currentScreen.value != "CYBERSPACE") return
        val yawRad = _cyberYaw.value
        val pitchRad = _cyberPitch.value
        val dx = sin(yawRad) * cos(pitchRad)
        val dy = sin(pitchRad)
        val dz = cos(yawRad) * cos(pitchRad)

        // Spawn player projectile laser right in front of the camera
        val spawnPos = Vector3D(
            _cyberCamPos.value.x + dx * 1.5f,
            _cyberCamPos.value.y + dy * 1.5f,
            _cyberCamPos.value.z + dz * 1.5f
        )
        cyberLasers.add(CyberLaser(spawnPos, Vector3D(dx, dy, dz), isPlayerShot = true))
    }

    private fun executeSuccessfulHack() {
        viewModelScope.launch {
            val stats = repository.getPlayerStatsSync() ?: return@launch
            val rewardModules = 12 + stats.hackingLevel * 4

            // Shut down cameras and unlock security doors in the world
            val tiles = repository.getMapTilesSync()
            val updatedTiles = tiles.map { tile ->
                if (tile.tileType == "LOCKED_DOOR") {
                    tile.copy(tileType = "DOOR_UNLOCKED", isHacked = true)
                } else if (tile.tileType == "CAMERA") {
                    tile.copy(tileType = "EMPTY", isHacked = true)
                } else if (tile.x == activeHackingTerminalX && tile.y == activeHackingTerminalY) {
                    tile.copy(isHacked = true)
                } else {
                    tile
                }
            }
            repository.insertMapTiles(updatedTiles)

            repository.updatePlayerStats(stats.copy(
                cyberModules = stats.cyberModules + rewardModules,
                activeScreen = "GAME",
                statusText = "CYBERSPACE HACK SUCCESSFUL! Node decrypted. Security systems offline. Gained $rewardModules Cyber Modules."
            ))
            setScreen("GAME")
        }
    }

    private fun executeFailedHack() {
        viewModelScope.launch {
            val stats = repository.getPlayerStatsSync() ?: return@launch
            // Hardware surge feedback causes real physical damage to the player!
            val physicalDamageFeedback = (35 - stats.hackingLevel * 5).coerceAtLeast(10)
            val updatedHealth = (stats.health - physicalDamageFeedback).coerceAtLeast(0)

            val nextScreen = if (updatedHealth <= 0) "GAME_OVER" else "GAME"
            val text = if (updatedHealth <= 0) {
                "CRITICAL ERROR. Cyberspace ejection feedback surge fried player synapsis."
            } else {
                "CYBERSPACE SURGE DETECTED! Feedback ejection caused $physicalDamageFeedback damage to physical health neural-link."
            }

            repository.updatePlayerStats(stats.copy(
                health = updatedHealth,
                activeScreen = nextScreen,
                statusText = text
            ))
            setScreen(nextScreen)
        }
    }

    override fun onCleared() {
        super.onCleared()
        cyberGameJob?.cancel()
    }
}
