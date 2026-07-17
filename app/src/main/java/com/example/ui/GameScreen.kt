package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.data.InventoryItem
import com.example.data.MapTile
import com.example.data.PlayerStats
import kotlin.math.*

private val CyanAccent = Color(0xFF00F0FF)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GameScreen(
    viewModel: GameViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val stats by viewModel.playerStats.collectAsState()
    val inventory by viewModel.inventoryItems.collectAsState()
    val tiles by viewModel.mapTiles.collectAsState()
    val currentScreen by viewModel.currentScreen.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF040A0D))
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // Render current state
        when (currentScreen) {
            "MAIN_MENU" -> MainMenu(
                onStartClick = { viewModel.setScreen("CLASS_SELECT") },
                onRestoreClick = { viewModel.setScreen("GAME") },
                hasSavedGame = stats != null
            )
            "CLASS_SELECT" -> ClassSelectScreen(
                onSelectClass = { playerClass ->
                    viewModel.startNewGame(playerClass)
                }
            )
            "CYBERSPACE" -> CyberspaceScreen(viewModel)
            "GAME_OVER" -> GameOverScreen(
                isVictory = false,
                stats = stats,
                onRestart = { viewModel.setScreen("MAIN_MENU") }
            )
            else -> GameBoard(viewModel, stats, inventory, tiles)
        }
    }
}

// --- MAIN MENU VIEW ---
@Composable
fun MainMenu(
    onStartClick: () -> Unit,
    onRestoreClick: () -> Unit,
    hasSavedGame: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // High-fidelity background image banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .padding(bottom = 24.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.3f))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    painter = painterResource(id = R.drawable.img_cyber_splash),
                    contentDescription = "CyberSim Terminal Banner",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Glitch neon overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xFF05080A).copy(alpha = 0.9f))
                            )
                        )
                    )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "NEURAL SYNC V7.82",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanAccent,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "CITADEL CYBERSIM",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        Text(
            text = "SYSTEM STATUS: RETRO-IMMERSIVE SIMULATOR ONLINE",
            style = MaterialTheme.typography.bodyMedium,
            color = CyanAccent.copy(alpha = 0.8f),
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Button(
            onClick = onStartClick,
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(56.dp)
                .testTag("start_game_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = CyanAccent,
                contentColor = Color(0xFF05080A)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "INITIALIZE NEW NEURAL LINK",
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 15.sp
            )
        }

        if (hasSavedGame) {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onRestoreClick,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(56.dp)
                    .testTag("restore_game_button"),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent),
                border = BorderStroke(1.5.dp, CyanAccent.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "RESTORE SECURED SAVED STATE",
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
        Text(
            text = "INCLUDES PHYSICS Voxel Grid • DIJKSTRA PATHFINDING • 3D CYBERSPACE FLIGHT MODULE",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFC0E0EA).copy(alpha = 0.4f),
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(0.9f)
        )
    }
}

// --- CLASS SELECT SCREEN ---
@Composable
fun ClassSelectScreen(
    onSelectClass: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "LINK MATRIX: SELECT COGNITIVE CHASSIS",
            style = MaterialTheme.typography.titleLarge,
            color = CyanAccent,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(top = 16.dp, bottom = 24.dp)
        )

        // Class 1: Marine
        ClassCard(
            title = "UNEF MARINE (COMBAT DIVISION)",
            accentColor = Color(0xFFFF9900),
            description = "High defensive rating and weapon mastery. Starts with automatic assault rifle. Perfect for direct assault operations.",
            statsText = "Combat Lvl 3 • Max HP +20 • Hardened Endoskeleton • Starts with Heavy AR Weapon",
            onClick = { onSelectClass("Marine") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Class 2: Navy
        ClassCard(
            title = "NAVY ACQUISITIONS (TECH DIVISION)",
            accentColor = CyanAccent,
            description = "Advanced cybernetic specialist. High tech skills allow bypassing cameras and lock mechanisms instantly. Starts with electronic decryptors.",
            statsText = "Hacking Lvl 3 • Repair Lvl 2 • Tactical Pistol • Security Decryption Blueprints",
            onClick = { onSelectClass("Navy") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Class 3: OSA
        ClassCard(
            title = "OSA INTELLECT (PSYCHIC SECTOR)",
            accentColor = Color(0xFF9B59B6),
            description = "Harness psionic projection cells. Manipulate electromagnetic wave interference and project offensive mental blasts.",
            statsText = "Mental Energy Lvl 3 • PSI Amplifier • Med-Hypos • Amplified Cybernetic Node Shield",
            onClick = { onSelectClass("OSA") }
        )
    }
}

@Composable
fun ClassCard(
    title: String,
    accentColor: Color,
    description: String,
    statsText: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1A22)),
        border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = accentColor,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Select",
                    tint = accentColor
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFC0E0EA),
                fontFamily = FontFamily.Monospace,
                lineHeight = 16.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(accentColor.copy(alpha = 0.15f))
                    .padding(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            ) {
                Text(
                    text = statsText,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

// --- GAME OVER VIEW ---
@Composable
fun GameOverScreen(
    isVictory: Boolean,
    stats: PlayerStats?,
    onRestart: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (isVictory) Icons.Default.CheckCircle else Icons.Default.Cancel,
            contentDescription = "Status Icon",
            tint = if (isVictory) CyanAccent else Color(0xFFFF3333),
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = if (isVictory) "NEURAL SYNC COMPLETE" else "NEURAL FEEDBACK FATAL SURGE",
            style = MaterialTheme.typography.headlineMedium,
            color = if (isVictory) CyanAccent else Color(0xFFFF3333),
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Chassis link closed. Final logs saved to localized core.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFC0E0EA).copy(alpha = 0.7f),
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(0.9f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1A22)),
            border = BorderStroke(1.dp, Color(0xFFC0E0EA).copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "INTERFACE OVERVIEW:", style = MaterialTheme.typography.labelMedium, color = CyanAccent, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Chassis Model: ${stats?.selectedClass ?: "Unknown"}", color = Color.White, fontFamily = FontFamily.Monospace)
                Text(text = "Hacking Stat: Lvl ${stats?.hackingLevel ?: 1}", color = Color.White, fontFamily = FontFamily.Monospace)
                Text(text = "Combat Stat: Lvl ${stats?.combatLevel ?: 1}", color = Color.White, fontFamily = FontFamily.Monospace)
                Text(text = "Cyber Modules: ${stats?.cyberModules ?: 0}", color = Color.White, fontFamily = FontFamily.Monospace)
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onRestart,
            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth(0.8f).height(50.dp)
        ) {
            Text(text = "RETURN TO CORE MENU", color = Color(0xFF050B0D), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
    }
}

// --- MAIN GAMEBOARD BOARD AND CONTROLS ---
@Composable
fun GameBoard(
    viewModel: GameViewModel,
    stats: PlayerStats?,
    inventory: List<InventoryItem>,
    tiles: List<MapTile>
) {
    var activeTab by remember { mutableStateOf("MAP") } // "MAP", "INVENTORY", "UPGRADES"

    if (stats == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF00F0FF))
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // RETRO HEADER HUD
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0A1A1F),
                            Color(0xFF05080A)
                        )
                    )
                )
                .drawBehind {
                    drawLine(
                        color = Color(0xFF00F0FF).copy(alpha = 0.2f),
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                .padding(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    // LEFT: VITALITY (Health Monitor Progress)
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "VITALITY",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFF87171),
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "${((stats.health.toFloat() / stats.maxHealth) * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFF87171),
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .background(Color(0xFF05080A), RoundedCornerShape(3.dp))
                                .border(BorderStroke(0.5.dp, Color(0xFFF87171).copy(alpha = 0.3f)), RoundedCornerShape(3.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(fraction = (stats.health.toFloat() / stats.maxHealth).coerceIn(0f, 1f))
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(Color(0xFFDC2626), Color(0xFFF87171))
                                        ),
                                        shape = RoundedCornerShape(3.dp)
                                    )
                            )
                        }
                    }

                    // CENTER: CLASS / LOCATION
                    Column(
                        modifier = Modifier.weight(1.1f).padding(horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "SECTOR 0${stats.currentX}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF00F0FF).copy(alpha = 0.6f),
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.sp,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = "${stats.selectedClass.uppercase()} INFILTRATOR",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            letterSpacing = 0.5.sp
                        )
                    }

                    // RIGHT: PSI-ENERGY
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PSI-ENERGY",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF00F0FF),
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "${stats.energy}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF00F0FF),
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .background(Color(0xFF05080A), RoundedCornerShape(3.dp))
                                .border(BorderStroke(0.5.dp, Color(0xFF00F0FF).copy(alpha = 0.3f)), RoundedCornerShape(3.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(fraction = (stats.energy.toFloat() / stats.maxEnergy).coerceIn(0f, 1f))
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(Color(0xFF0891B2), Color(0xFF00F0FF))
                                        ),
                                        shape = RoundedCornerShape(3.dp)
                                    )
                            )
                        }
                    }
                }

                // LOG / DIAGNOSTIC STREAM
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                        .border(BorderStroke(1.dp, Color(0xFF00F0FF).copy(alpha = 0.15f)), RoundedCornerShape(4.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "SYNC_LOG: ${stats.statusText}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF00F0FF),
                        fontFamily = FontFamily.Monospace,
                        maxLines = 2,
                        minLines = 2
                    )
                }
            }
        }

        // CENTER SUB-SCREEN PANEL SWAP
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (activeTab) {
                "MAP" -> MapPanel(viewModel, stats, tiles)
                "INVENTORY" -> InventoryPanel(viewModel, inventory)
                "UPGRADES" -> UpgradesPanel(viewModel, stats)
            }
        }

        // FOOTER RETRO NAVIGATION BAR WITH TAB PILLS
        NavigationBar(
            containerColor = Color(0xFF0A1A1F),
            modifier = Modifier
                .height(72.dp)
                .drawBehind {
                    drawLine(
                        color = Color(0xFF00F0FF).copy(alpha = 0.2f),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = 1.dp.toPx()
                    )
                }
        ) {
            NavigationBarItem(
                selected = activeTab == "MAP",
                onClick = { activeTab = "MAP" },
                icon = { Icon(Icons.Default.Map, contentDescription = "Mosaic world Map") },
                label = { Text("MOSAIC WORLD", fontFamily = FontFamily.Monospace, fontSize = 9.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF05080A),
                    selectedTextColor = Color(0xFF00F0FF),
                    indicatorColor = Color(0xFF00F0FF),
                    unselectedIconColor = Color(0xFFCBD5E1).copy(alpha = 0.6f),
                    unselectedTextColor = Color(0xFFCBD5E1).copy(alpha = 0.6f)
                )
            )
            NavigationBarItem(
                selected = activeTab == "INVENTORY",
                onClick = { activeTab = "INVENTORY" },
                icon = { Icon(Icons.Default.GridOn, contentDescription = "Inventory grid") },
                label = { Text("CYBER-GRID", fontFamily = FontFamily.Monospace, fontSize = 9.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF05080A),
                    selectedTextColor = Color(0xFF00F0FF),
                    indicatorColor = Color(0xFF00F0FF),
                    unselectedIconColor = Color(0xFFCBD5E1).copy(alpha = 0.6f),
                    unselectedTextColor = Color(0xFFCBD5E1).copy(alpha = 0.6f)
                )
            )
            NavigationBarItem(
                selected = activeTab == "UPGRADES",
                onClick = { activeTab = "UPGRADES" },
                icon = { Icon(Icons.Default.Memory, contentDescription = "Upgrade modules") },
                label = { Text("CYBER_UPGRADES", fontFamily = FontFamily.Monospace, fontSize = 9.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF05080A),
                    selectedTextColor = Color(0xFF00F0FF),
                    indicatorColor = Color(0xFF00F0FF),
                    unselectedIconColor = Color(0xFFCBD5E1).copy(alpha = 0.6f),
                    unselectedTextColor = Color(0xFFCBD5E1).copy(alpha = 0.6f)
                )
            )
        }
    }
}

// --- SUB PANEL: GEOMETRIC VOXEL MAP & PATH FINDING ---
@Composable
fun MapPanel(
    viewModel: GameViewModel,
    stats: PlayerStats,
    tiles: List<MapTile>
) {
    val selectedTile by viewModel.selectedMapTile.collectAsState()
    val currentPath by viewModel.currentPath.collectAsState()

    // Screen height constraints mapping for safety
    val configuration = LocalConfiguration.current
    val isTallScreen = configuration.screenHeightDp > 600

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Text(
            text = "PROJECTION ENGINE: procedurally elevated voxel terrain relief system",
            style = MaterialTheme.typography.labelSmall,
            color = CyanAccent.copy(alpha = 0.6f),
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        // 3D Isometric Voxel drawing canvas container
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Black)
                .border(BorderStroke(1.5.dp, CyanAccent.copy(alpha = 0.3f)), RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
        ) {
            IsometricVoxelCanvas(
                tiles = tiles,
                playerX = stats.currentX,
                playerY = stats.currentY,
                selectedX = selectedTile?.first ?: -1,
                selectedY = selectedTile?.second ?: -1,
                currentPath = currentPath,
                onTileSelected = { x, y -> viewModel.selectTile(x, y) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // INTERACTION AND EXECUTION CONTROL DECK
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A131A)),
            border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (selectedTile != null) {
                    val targetX = selectedTile!!.first
                    val targetY = selectedTile!!.second
                    val tile = tiles.find { it.x == targetX && it.y == targetY }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "TARGET SECTOR: [$targetX, $targetY]",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "RELIEF ELEVATION: Lvl ${tile?.elevation ?: 0} • TYPE: ${tile?.tileType ?: "EMPTY"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFC0E0EA),
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        if (currentPath.isNotEmpty()) {
                            Text(
                                text = "STEPS: ${currentPath.size} | COST: ${currentPath.size * 2} NRG",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = CyanAccent,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Move vector path execution
                        if (currentPath.isNotEmpty()) {
                            Button(
                                onClick = { viewModel.moveAlongPath() },
                                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.weight(1f).testTag("move_button")
                            ) {
                                Icon(Icons.Default.DirectionsRun, contentDescription = "Move icon", tint = Color(0xFF050B0D))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("EXECUTE COGNITIVE PATH", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF050B0D), fontWeight = FontWeight.Bold)
                            }
                        }

                        // Specific actions based on target adjacent tile types
                        if (tile != null && isAdjacent(stats.currentX, stats.currentY, targetX, targetY)) {
                            when (tile.tileType) {
                                "MUTANT" -> {
                                    Button(
                                        onClick = { viewModel.attackMutant(targetX, targetY) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3333)),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.weight(1f).testTag("attack_button")
                                    ) {
                                        Icon(Icons.Default.Bolt, contentDescription = "Combat attack", tint = Color.White)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("ENGAGE BIO-MUTANT", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                    }
                                }
                                "TERMINAL" -> {
                                    Button(
                                        onClick = { viewModel.startHackingTerminal(targetX, targetY) },
                                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.weight(1f).testTag("hack_button")
                                    ) {
                                        Icon(Icons.Default.Terminal, contentDescription = "Cyberspace link", tint = Color(0xFF050B0D))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("CYBERSPACE NEURAL LINK", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF050B0D), fontWeight = FontWeight.Bold)
                                    }
                                }
                                "CHEST" -> {
                                    Button(
                                        onClick = { viewModel.lootChest(targetX, targetY) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9900)),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.weight(1f).testTag("loot_button")
                                    ) {
                                        Icon(Icons.Default.AllInbox, contentDescription = "Loot", tint = Color(0xFF050B0D))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("OPEN CARGO BLOCK", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF050B0D), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = "AWAITING TELEMETRY VECTOR INPUT • SELECT SECTOR TILE",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFC0E0EA).copy(alpha = 0.5f),
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

private fun isAdjacent(x1: Int, y1: Int, x2: Int, y2: Int): Boolean {
    return abs(x1 - x2) <= 1 && abs(y1 - y2) <= 1
}

// --- COMPLEX COMPOSE ISOMETRIC VOXEL GRID RENDERER ---
@Composable
fun IsometricVoxelCanvas(
    tiles: List<MapTile>,
    playerX: Int,
    playerY: Int,
    selectedX: Int,
    selectedY: Int,
    currentPath: List<Pair<Int, Int>>,
    onTileSelected: (Int, Int) -> Unit
) {
    val gridSize = 12
    val scrollState = rememberScrollState()

    // Draw the entire map using standard Canvas calculations
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                val pWidth = size.width.toFloat()
                val pHeight = size.height.toFloat()
                detectTapGestures { offset ->
                    // Center offsets
                    val centerX = pWidth / 2f
                    val centerY = pHeight / 3f

                    val tileW = pWidth / 26f
                    val tileH = tileW * 0.55f
                    val heightStep = tileW * 0.35f

                    var bestTile: Pair<Int, Int>? = null
                    var minDistance = Float.MAX_VALUE

                    for (x in 0 until gridSize) {
                        for (y in 0 until gridSize) {
                            val matchingTile = tiles.find { it.x == x && it.y == y }
                            val hVal = matchingTile?.elevation ?: 0

                            // Calculate expected projected coordinate
                            val screenX = centerX + (x - y) * tileW
                            val screenY = centerY + (x + y) * tileH - hVal * heightStep

                            val dx = offset.x - screenX
                            val dy = offset.y - screenY
                            val distance = sqrt(dx * dx + dy * dy)
                            if (distance < minDistance && distance < tileW * 1.5f) {
                                minDistance = distance
                                bestTile = x to y
                            }
                        }
                    }

                    bestTile?.let {
                        onTileSelected(it.first, it.second)
                    }
                }
            }
    ) {
        val width = size.width
        val height = size.height

        val centerX = width / 2f
        val centerY = height / 3.5f

        val tileW = width / 26f
        val tileH = tileW * 0.55f
        val heightStep = tileW * 0.35f

        // Grid boundaries drawn
        val tileMap = tiles.associateBy { it.x to it.y }

        // Render back-to-front (Painters Algorithm sorting: draw x+y from 0 to max)
        for (sum in 0..(gridSize * 2 - 2)) {
            for (x in 0 until gridSize) {
                val y = sum - x
                if (y in 0 until gridSize) {
                    val tile = tileMap[x to y] ?: continue
                    val isPlayer = (x == playerX && y == playerY)
                    val isSelected = (x == selectedX && y == selectedY)
                    val isInPath = currentPath.contains(x to y)

                    drawVoxelBlock(
                        cx = centerX + (x - y) * tileW,
                        cy = centerY + (x + y) * tileH,
                        tileW = tileW,
                        tileH = tileH,
                        elevation = tile.elevation,
                        heightStep = heightStep,
                        tile = tile,
                        isPlayer = isPlayer,
                        isSelected = isSelected,
                        isInPath = isInPath
                    )
                }
            }
        }
    }
}

fun DrawScope.drawVoxelBlock(
    cx: Float,
    cy: Float,
    tileW: Float,
    tileH: Float,
    elevation: Int,
    heightStep: Float,
    tile: MapTile,
    isPlayer: Boolean,
    isSelected: Boolean,
    isInPath: Boolean
) {
    val h = elevation * heightStep
    val topY = cy - h

    // Voxel top-rhombus vertices
    val topPt = Offset(cx, topY - tileH)
    val rightPt = Offset(cx + tileW, topY)
    val bottomPt = Offset(cx, topY + tileH)
    val leftPt = Offset(cx - tileW, topY)

    // Colors styled dynamically based on depth and terrain relief state
    val terrainColor = when {
        tile.tileType == "WALL" -> Color(0xFF1B2A35)
        else -> {
            // Generate a grid pattern wave color reflecting Dijkstra costs
            val intensity = (0.2f + (elevation / 10f)).coerceIn(0.1f, 0.9f)
            Color(0xFF0A3C36).copy(
                red = 0.05f + intensity * 0.05f,
                green = 0.25f + intensity * 0.45f,
                blue = 0.20f + intensity * 0.35f
            )
        }
    }

    val topFacePath = Path().apply {
        moveTo(topPt.x, topPt.y)
        lineTo(rightPt.x, rightPt.y)
        lineTo(bottomPt.x, bottomPt.y)
        lineTo(leftPt.x, leftPt.y)
        close()
    }

    // DRAW TOP RHOMBUS
    drawPath(
        path = topFacePath,
        color = when {
            isPlayer -> Color(0xFF00FF66)
            isSelected -> Color(0xFFFF9900)
            isInPath -> CyanAccent.copy(alpha = 0.8f)
            else -> terrainColor
        }
    )

    // DRAW LEFT AND RIGHT FACES (Extrusion sides for structural Voxel voxel styling)
    if (elevation > 0) {
        val bottomOffset = cy

        // Left front panel
        val leftFacePath = Path().apply {
            moveTo(leftPt.x, leftPt.y)
            lineTo(bottomPt.x, bottomPt.y)
            lineTo(bottomPt.x, bottomPt.y + h)
            lineTo(leftPt.x, leftPt.y + h)
            close()
        }

        // Right front panel
        val rightFacePath = Path().apply {
            moveTo(bottomPt.x, bottomPt.y)
            lineTo(rightPt.x, rightPt.y)
            lineTo(rightPt.x, rightPt.y + h)
            lineTo(bottomPt.x, bottomPt.y + h)
            close()
        }

        // Slightly shaded left/right sides to give high depth contrast
        drawPath(path = leftFacePath, color = terrainColor.copy(alpha = 0.65f))
        drawPath(path = rightFacePath, color = terrainColor.copy(alpha = 0.45f))

        // Draw side borders
        drawLine(CyanAccent.copy(alpha = 0.2f), leftPt, Offset(leftPt.x, leftPt.y + h), strokeWidth = 1f)
        drawLine(CyanAccent.copy(alpha = 0.2f), bottomPt, Offset(bottomPt.x, bottomPt.y + h), strokeWidth = 1f)
        drawLine(CyanAccent.copy(alpha = 0.2f), rightPt, Offset(rightPt.x, rightPt.y + h), strokeWidth = 1f)
    }

    // Draw top face borders
    drawPath(
        path = topFacePath,
        color = when {
            isPlayer -> Color(0xFFFFFFFF)
            isSelected -> Color(0xFFFF9900)
            else -> CyanAccent.copy(alpha = 0.35f)
        },
        style = Stroke(width = if (isPlayer || isSelected) 2f else 1f)
    )

    // DRAW DETAILED INTERACTION MARKERS ON TOP
    when (tile.tileType) {
        "WALL" -> {
            // Draw a elevated pillar
            val pillarHeight = tileW * 0.9f
            drawRect(
                color = Color(0xFF2C3E50),
                topLeft = Offset(cx - tileW * 0.3f, topY - pillarHeight),
                size = Size(tileW * 0.6f, pillarHeight)
            )
            drawRect(
                color = CyanAccent.copy(alpha = 0.5f),
                topLeft = Offset(cx - tileW * 0.3f, topY - pillarHeight),
                size = Size(tileW * 0.6f, pillarHeight),
                style = Stroke(width = 1f)
            )
        }
        "TERMINAL" -> {
            // Draw hacking glowing screen
            val centerOffset = Offset(cx, topY - tileH * 0.2f)
            drawCircle(color = CyanAccent, radius = tileW * 0.35f, center = centerOffset)
            drawCircle(color = Color(0xFF050B0D), radius = tileW * 0.2f, center = centerOffset)
            drawRect(
                color = CyanAccent,
                topLeft = Offset(cx - tileW * 0.15f, centerOffset.y - tileW * 0.15f),
                size = Size(tileW * 0.3f, tileW * 0.3f)
            )
        }
        "MUTANT" -> {
            if (tile.enemyHealth > 0) {
                // RED hazard diamond representing mutant enemy
                val centerOffset = Offset(cx, topY - tileH * 0.1f)
                val shapePath = Path().apply {
                    moveTo(centerOffset.x, centerOffset.y - tileW * 0.4f)
                    lineTo(centerOffset.x + tileW * 0.3f, centerOffset.y)
                    lineTo(centerOffset.x, centerOffset.y + tileW * 0.4f)
                    lineTo(centerOffset.x - tileW * 0.3f, centerOffset.y)
                    close()
                }
                drawPath(path = shapePath, color = Color(0xFFFF3333))
                drawPath(path = shapePath, color = Color.White, style = Stroke(width = 1.5f))
            }
        }
        "CHEST" -> {
            // Supply chest wireframe steel box
            val sizeC = tileW * 0.5f
            drawRect(
                color = Color(0xFFFF9900),
                topLeft = Offset(cx - sizeC / 2f, topY - sizeC / 2f),
                size = Size(sizeC, sizeC)
            )
            drawRect(
                color = Color.White,
                topLeft = Offset(cx - sizeC / 2f, topY - sizeC / 2f),
                size = Size(sizeC, sizeC),
                style = Stroke(width = 1f)
            )
        }
        "LOCKED_DOOR" -> {
            // Heavy locked security gate lines
            drawLine(
                color = Color(0xFFFF3333),
                start = Offset(cx - tileW * 0.6f, topY - tileH * 0.5f),
                end = Offset(cx + tileW * 0.6f, topY + tileH * 0.5f),
                strokeWidth = 3f
            )
            drawLine(
                color = Color(0xFFFF3333),
                start = Offset(cx - tileW * 0.6f, topY + tileH * 0.5f),
                end = Offset(cx + tileW * 0.6f, topY - tileH * 0.5f),
                strokeWidth = 3f
            )
        }
        "DOOR_UNLOCKED" -> {
            // Open bypass green gate
            drawLine(
                color = CyanAccent,
                start = Offset(cx - tileW * 0.4f, topY),
                end = Offset(cx + tileW * 0.4f, topY),
                strokeWidth = 2f
            )
        }
        "CAMERA" -> {
            // Camera sensor warning line
            drawCircle(Color(0xFFFF3333), radius = tileW * 0.2f, center = Offset(cx, topY - tileW * 0.4f))
            drawLine(
                color = Color(0xFFFF3333).copy(alpha = 0.3f),
                start = Offset(cx, topY - tileW * 0.4f),
                end = Offset(cx, topY + tileH),
                strokeWidth = 2f
            )
        }
    }

    // DRAW NEON COORDINATE DOT TO EMPHASIZE VECTOR LAYOUT
    if (tile.x % 4 == 0 && tile.y % 4 == 0) {
        drawCircle(CyanAccent.copy(alpha = 0.15f), radius = 3f, center = topPt)
    }
}

// --- SUB PANEL: GRID PERSISTENT INVENTORY SYSTEM ---
@Composable
fun InventoryPanel(
    viewModel: GameViewModel,
    inventory: List<InventoryItem>
) {
    val heldItem by viewModel.heldItem.collectAsState()
    val heldItemRotated by viewModel.heldItemRotated.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "INVENTORY GRID DRAWER (6x6 PERSISTENT RE-ASSEMBLER)",
            style = MaterialTheme.typography.titleSmall,
            color = CyanAccent,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left Column: The Interactive 6x6 Grid
            Box(
                modifier = Modifier
                    .weight(1.2f)
                    .aspectRatio(1f)
                    .background(Color.Black)
                    .border(BorderStroke(2.dp, CyanAccent.copy(alpha = 0.4f)), RoundedCornerShape(4.dp))
                    .padding(4.dp)
            ) {
                // Grid cell borders and item coordinate overlays
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cellSize = size.width / 6f
                    for (i in 0..6) {
                        // Horizontal divisions
                        drawLine(
                            color = CyanAccent.copy(alpha = 0.15f),
                            start = Offset(0f, i * cellSize),
                            end = Offset(size.width, i * cellSize),
                            strokeWidth = 1f
                        )
                        // Vertical divisions
                        drawLine(
                            color = CyanAccent.copy(alpha = 0.15f),
                            start = Offset(i * cellSize, 0f),
                            end = Offset(i * cellSize, size.height),
                            strokeWidth = 1f
                        )
                    }
                }

                // Render placed items overlay
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val cellW = maxWidth / 6f
                    val cellH = maxHeight / 6f

                    // Click layer for placing items
                    for (r in 0..5) {
                        for (c in 0..5) {
                            Box(
                                modifier = Modifier
                                    .absoluteOffset(x = cellW * c, y = cellH * r)
                                    .size(cellW, cellH)
                                    .clickable { viewModel.placeHeldItem(r, c) }
                            )
                        }
                    }

                    // Render items
                    for (item in inventory) {
                        val color = Color(item.color)
                        val isHeld = heldItem?.id == item.id

                        Box(
                            modifier = Modifier
                                .absoluteOffset(x = cellW * item.col, y = cellH * item.row)
                                .size(cellW * item.width - 2.dp, cellH * item.height - 2.dp)
                                .background(color.copy(alpha = if (isHeld) 0.3f else 0.75f))
                                .border(
                                    BorderStroke(
                                        if (isHeld) 2.dp else 1.dp,
                                        if (isHeld) Color.White else color
                                    ),
                                    RoundedCornerShape(2.dp)
                                )
                                .clickable { viewModel.selectHeldItem(item) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(2.dp)
                            ) {
                                Icon(
                                    imageVector = when (item.type) {
                                        "RIFLE" -> Icons.Default.Dangerous
                                        "PISTOL" -> Icons.Default.FlashOn
                                        "PSI_AMP" -> Icons.Default.OfflineBolt
                                        "CELL" -> Icons.Default.MedicalServices
                                        "NANITES" -> Icons.Default.BatteryChargingFull
                                        else -> Icons.Default.SmartButton
                                    },
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 7.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // Right Column: Held Gear diagnostics
            Column(
                modifier = Modifier
                    .weight(0.8f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (heldItem != null) {
                    val item = heldItem!!
                    val itemW = if (heldItemRotated) item.height else item.width
                    val itemH = if (heldItemRotated) item.width else item.height

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A131A)),
                        border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "DIAGNOSTIC:",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyanAccent,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.name.uppercase(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "SIZE: ${itemW}x${itemH}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFC0E0EA),
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "CLASSIF: ${item.type}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFC0E0EA),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // ROTATE ITEM
                    Button(
                        onClick = { viewModel.rotateHeldItem() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A131A)),
                        border = BorderStroke(1.dp, CyanAccent),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.fillMaxWidth().testTag("rotate_button")
                    ) {
                        Icon(Icons.Default.RotateRight, contentDescription = "Rotate icon", tint = CyanAccent)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ROTATE GEAR", color = CyanAccent, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }

                    // USE ITEM
                    Button(
                        onClick = { viewModel.useItem(item) },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.fillMaxWidth().testTag("use_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Use icon", tint = Color(0xFF050B0D))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("USE / EQUIP", color = Color(0xFF050B0D), fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }

                    // DISCARD ITEM
                    Button(
                        onClick = { viewModel.dropHeldItem() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3333).copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, Color(0xFFFF3333)),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.fillMaxWidth().testTag("discard_button")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Discard icon", tint = Color(0xFFFF3333))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("DISCARD UNIT", color = Color(0xFFFF3333), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0A131A))
                            .border(BorderStroke(1.dp, CyanAccent.copy(alpha = 0.1f)), RoundedCornerShape(4.dp))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "SELECT ANY EQUIPPED GEAR TO RE-ARRANGE OR ACTIVATE INJECTOR SYSTEM",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFC0E0EA).copy(alpha = 0.4f),
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// --- SUB PANEL: NEURAL STATS UPGRADES ---
@Composable
fun UpgradesPanel(
    viewModel: GameViewModel,
    stats: PlayerStats
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "NEURAL INJECTOR: PROGRESSION ARRAY",
                style = MaterialTheme.typography.titleSmall,
                color = CyanAccent,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Box(
                modifier = Modifier
                    .background(CyanAccent.copy(alpha = 0.15f))
                    .border(BorderStroke(1.dp, CyanAccent), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "CYBER MODULES: ${stats.cyberModules}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Render the 6 stats
        StatUpgradeRow("Hacking", stats.hackingLevel, "Enables accessing high-security cyberspace terminal nodes.", viewModel, stats.cyberModules)
        StatUpgradeRow("Repair", stats.repairLevel, "Increases maximum bio-electric energy capacities (+10 per level).", viewModel, stats.cyberModules)
        StatUpgradeRow("Modify", stats.modifyLevel, "Enables attaching thermal capacitors or sub-processors to weaponry.", viewModel, stats.cyberModules)
        StatUpgradeRow("Maintenance", stats.maintenanceLevel, "Reduces weapon friction wear and structural degradation.", viewModel, stats.cyberModules)
        StatUpgradeRow("Research", stats.researchLevel, "Boosts biological diagnostics, increasing combat DMG to target Mutants.", viewModel, stats.cyberModules)
        StatUpgradeRow("Combat", stats.combatLevel, "Increases general kinetic weapon output and maximum health (+15 per level).", viewModel, stats.cyberModules)
    }
}

@Composable
fun StatUpgradeRow(
    name: String,
    level: Int,
    description: String,
    viewModel: GameViewModel,
    currentModules: Int
) {
    val upgradeCost = level * 8
    val canAfford = currentModules >= upgradeCost && level < 6

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1B23)),
        border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = name.uppercase(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "LVL $level/6",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanAccent,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        text = description,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFC0E0EA).copy(alpha = 0.7f),
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.fillMaxWidth(0.68f)
                    )
                }

                if (level < 6) {
                    Button(
                        onClick = { viewModel.upgradeStat(name) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (canAfford) CyanAccent else Color(0xFFFF9900).copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.width(100.dp).height(38.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("UPGRADE", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = if (canAfford) Color(0xFF050B0D) else Color.White, fontWeight = FontWeight.Bold)
                            Text("$upgradeCost MODS", fontSize = 7.sp, fontFamily = FontFamily.Monospace, color = if (canAfford) Color(0xFF050B0D) else Color.White)
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .background(CyanAccent.copy(alpha = 0.15f))
                            .border(BorderStroke(1.dp, CyanAccent), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("MAXIMUM", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CyanAccent, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            // Render nice visual dots for status tracking level (1..6)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (i in 1..6) {
                    Box(
                        modifier = Modifier
                            .size(16.dp, 4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (i <= level) CyanAccent else CyanAccent.copy(alpha = 0.15f)
                            )
                    )
                }
            }
        }
    }
}

// --- DYNAMIC CYBERSPACE WIREFRAME 3D RENDER SCREEN ---
@Composable
fun CyberspaceScreen(
    viewModel: GameViewModel
) {
    val health by viewModel.cyberHealth.collectAsState()
    val shield by viewModel.cyberShield.collectAsState()
    val cubesRemaining by viewModel.cyberDataCubesRemaining.collectAsState()

    // Retrieve positions for camera simulation rendering
    val camPos by viewModel.cyberCamPos.collectAsState()
    val yaw by viewModel.cyberYaw.collectAsState()
    val pitch by viewModel.cyberPitch.collectAsState()
    val speed by viewModel.cyberSpeed.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF020608))
            .padding(12.dp)
    ) {
        // Cyberspace HUD Overlay Banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("NEURAL RE-LINK INTRUSION VECTOR", color = CyanAccent, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
                Text("COORDINATES: X:${"%.1f".format(camPos.x)} Y:${"%.1f".format(camPos.y)} Z:${"%.1f".format(camPos.z)}", color = Color.White.copy(alpha = 0.6f), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            }

            Box(
                modifier = Modifier
                    .background(CyanAccent.copy(alpha = 0.15f))
                    .border(BorderStroke(1.dp, CyanAccent), RoundedCornerShape(4.dp))
                    .padding(6.dp)
            ) {
                Text(
                    text = "DATA COBLES: $cubesRemaining LEFT",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Progression indicators
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "INTEGRITY: $health%", style = MaterialTheme.typography.labelSmall, color = Color(0xFF00E5FF), fontFamily = FontFamily.Monospace)
                LinearProgressIndicator(
                    progress = { health / 100f },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = Color(0xFF00E5FF),
                    trackColor = Color(0xFF00E5FF).copy(alpha = 0.15f)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "COGNITIVE SHIELD: $shield%", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFFCC00), fontFamily = FontFamily.Monospace)
                LinearProgressIndicator(
                    progress = { shield / 100f },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = Color(0xFFFFCC00),
                    trackColor = Color(0xFFFFCC00).copy(alpha = 0.15f)
                )
            }
        }

        // THE 3D WIREFRAME VECTOR CANVAS
        Box(
            modifier = Modifier
                .weight(1.3f)
                .fillMaxWidth()
                .background(Color.Black)
                .border(BorderStroke(2.dp, CyanAccent), RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
        ) {
            Cyberspace3DCanvas(
                camPos = camPos,
                yaw = yaw,
                pitch = pitch,
                obstacles = viewModel.cyberObstacles,
                lasers = viewModel.cyberLasers
            )

            // Scanning line screen effects
            Canvas(modifier = Modifier.fillMaxSize()) {
                val lineSpacing = 16f
                for (y in 0..size.height.toInt() step lineSpacing.toInt()) {
                    drawLine(
                        color = CyanAccent.copy(alpha = 0.05f),
                        start = Offset(0f, y.toFloat()),
                        end = Offset(size.width, y.toFloat()),
                        strokeWidth = 1f
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // INTERACTIVE RETRO JOYSTICK FLIGHT CONTROLLER PANEL
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF09141C)),
            border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(10.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Yaw Steering Pad (Left/Right)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("STEER VECTOR", style = MaterialTheme.typography.labelSmall, color = Color(0xFFC0E0EA), fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = { viewModel.steerCyberYaw(-0.15f) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0E1C24)),
                            border = BorderStroke(1.dp, CyanAccent),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.size(54.dp).testTag("yaw_left")
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Yaw Left", tint = CyanAccent)
                        }
                        Button(
                            onClick = { viewModel.steerCyberYaw(0.15f) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0E1C24)),
                            border = BorderStroke(1.dp, CyanAccent),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.size(54.dp).testTag("yaw_right")
                        ) {
                            Icon(Icons.Default.ArrowForward, contentDescription = "Yaw Right", tint = CyanAccent)
                        }
                    }
                }

                // Pitch/Climb Steering Pad (Up/Down)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("PITCH VECTOR", style = MaterialTheme.typography.labelSmall, color = Color(0xFFC0E0EA), fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = { viewModel.steerCyberPitch(0.12f) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0E1C24)),
                            border = BorderStroke(1.dp, CyanAccent),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.size(54.dp).testTag("pitch_up")
                        ) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = "Pitch Climb", tint = CyanAccent)
                        }
                        Button(
                            onClick = { viewModel.steerCyberPitch(-0.12f) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0E1C24)),
                            border = BorderStroke(1.dp, CyanAccent),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.size(54.dp).testTag("pitch_down")
                        ) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = "Pitch Dive", tint = CyanAccent)
                        }
                    }
                }

                // ACTION: DISCHARGE SYSTEM FIRE LASER
                Button(
                    onClick = { viewModel.fireCyberLaser() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3333)),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.size(80.dp, 60.dp).testTag("fire_laser_button")
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.FlashOn, contentDescription = "Discharge", tint = Color.White)
                        Text("FIRE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

// --- 3D PERSPECTIVE WIREFRAME MATRIX CALCULATOR CANVAS ---
@Composable
fun Cyberspace3DCanvas(
    camPos: Vector3D,
    yaw: Float,
    pitch: Float,
    obstacles: List<CyberObstacle>,
    lasers: List<CyberLaser>
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        val cx = w / 2f
        val cy = h / 2f

        val focalLength = w * 0.85f // View angle scale

        // Precompute matrix rotation trigonometric values
        val cosYaw = cos(-yaw)
        val sinYaw = sin(-yaw)
        val cosPitch = cos(-pitch)
        val sinPitch = sin(-pitch)

        // Helper to project 3D point to 2D view screen
        fun project(p: Vector3D): Offset? {
            // 1. Translate relative to camera
            val dx = p.x - camPos.x
            val dy = p.y - camPos.y
            val dz = p.z - camPos.z

            // 2. Rotate Yaw (Y-axis)
            val rx1 = dx * cosYaw - dz * sinYaw
            val rz1 = dx * sinYaw + dz * cosYaw

            // 3. Rotate Pitch (X-axis)
            val ry = dy * cosPitch - rz1 * sinPitch
            val rz = dy * sinPitch + rz1 * cosPitch

            // Perspective boundary checks (behind the screen = cull)
            if (rz <= 1f) return null

            // 4. Perspective division projection
            val sx = cx + (rx1 / rz) * focalLength
            val sy = cy + (ry / rz) * focalLength
            return Offset(sx, sy)
        }

        // DRAW CYBER-TUNNEL WIREFRAME LINES (Floor & Ceil grids)
        val tunnelSectors = 8
        val tunnelDepthStep = 15f
        for (i in 0..tunnelSectors) {
            val zOffset = (camPos.z / tunnelDepthStep).toInt() * tunnelDepthStep + (i * tunnelDepthStep)
            // Left-Top boundary line
            val lt1 = project(Vector3D(-8f, 6f, zOffset))
            val rt1 = project(Vector3D(8f, 6f, zOffset))
            val rb1 = project(Vector3D(8f, -6f, zOffset))
            val lb1 = project(Vector3D(-8f, -6f, zOffset))

            val lt2 = project(Vector3D(-8f, 6f, zOffset + tunnelDepthStep))
            val rt2 = project(Vector3D(8f, 6f, zOffset + tunnelDepthStep))
            val rb2 = project(Vector3D(8f, -6f, zOffset + tunnelDepthStep))
            val lb2 = project(Vector3D(-8f, -6f, zOffset + tunnelDepthStep))

            // Draw grids connecting segments
            if (lt1 != null && rt1 != null) drawLine(CyanAccent.copy(alpha = 0.15f), lt1, rt1, strokeWidth = 1.2f)
            if (rt1 != null && rb1 != null) drawLine(CyanAccent.copy(alpha = 0.15f), rt1, rb1, strokeWidth = 1.2f)
            if (rb1 != null && lb1 != null) drawLine(CyanAccent.copy(alpha = 0.15f), rb1, lb1, strokeWidth = 1.2f)
            if (lb1 != null && lt1 != null) drawLine(CyanAccent.copy(alpha = 0.15f), lb1, lt1, strokeWidth = 1.2f)

            if (lt1 != null && lt2 != null) drawLine(CyanAccent.copy(alpha = 0.1f), lt1, lt2, strokeWidth = 0.8f)
            if (rt1 != null && rt2 != null) drawLine(CyanAccent.copy(alpha = 0.1f), rt1, rt2, strokeWidth = 0.8f)
            if (rb1 != null && rb2 != null) drawLine(CyanAccent.copy(alpha = 0.1f), rb1, rb2, strokeWidth = 0.8f)
            if (lb1 != null && lb2 != null) drawLine(CyanAccent.copy(alpha = 0.1f), lb1, lb2, strokeWidth = 0.8f)
        }

        // DRAW PROJECTILES IN FLIGHT (glowing laser lines)
        for (laser in lasers) {
            val startPt = project(laser.pos)
            val endPt = project(Vector3D(
                laser.pos.x + laser.dir.x * 2.5f,
                laser.pos.y + laser.dir.y * 2.5f,
                laser.pos.z + laser.dir.z * 2.5f
            ))

            if (startPt != null && endPt != null) {
                drawLine(
                    color = if (laser.isPlayerShot) CyanAccent else Color(0xFFFF3333),
                    start = startPt,
                    end = endPt,
                    strokeWidth = 3f
                )
            }
        }

        // DRAW FLOATING COGNITIVE OBSTACLES (DATA CUBES, SECURITY TURRETS)
        for (obs in obstacles) {
            val centerPt = project(obs.pos) ?: continue
            val dist = obs.pos.minus(camPos)
            val zDist = dist.z
            if (zDist < 0.2f) continue

            // Radius scales relative to distance division
            val scaleRadius = (focalLength / zDist) * 0.45f
            val rad = scaleRadius.coerceIn(4f, 150f)

            when (obs.type) {
                "CUBE" -> {
                    // Draw a neat 3D perspective wireframe Cube
                    val p = obs.pos
                    val size = 0.8f
                    val pt1 = project(Vector3D(p.x - size, p.y - size, p.z - size))
                    val pt2 = project(Vector3D(p.x + size, p.y - size, p.z - size))
                    val pt3 = project(Vector3D(p.x + size, p.y + size, p.z - size))
                    val pt4 = project(Vector3D(p.x - size, p.y + size, p.z - size))

                    val pt5 = project(Vector3D(p.x - size, p.y - size, p.z + size))
                    val pt6 = project(Vector3D(p.x + size, p.y - size, p.z + size))
                    val pt7 = project(Vector3D(p.x + size, p.y + size, p.z + size))
                    val pt8 = project(Vector3D(p.x - size, p.y + size, p.z + size))

                    // Draw connecting edges
                    val edges = listOf(
                        pt1 to pt2, pt2 to pt3, pt3 to pt4, pt4 to pt1,
                        pt5 to pt6, pt6 to pt7, pt7 to pt8, pt8 to pt5,
                        pt1 to pt5, pt2 to pt6, pt3 to pt7, pt4 to pt8
                    )

                    for ((start, end) in edges) {
                        if (start != null && end != null) {
                            drawLine(Color(obs.color), start, end, strokeWidth = 2f)
                        }
                    }
                    // Draw a glowing center dot
                    drawCircle(Color(obs.color).copy(alpha = 0.5f), radius = rad * 0.3f, center = centerPt)
                }
                "MINE" -> {
                    // Draw orange warning core
                    drawCircle(Color(obs.color), radius = rad, center = centerPt, style = Stroke(width = 1.5f))
                    drawCircle(Color(obs.color).copy(alpha = 0.4f), radius = rad * 0.4f, center = centerPt)
                    // Draw mine spike vectors
                    val angles = listOf(0f, 60f, 120f, 180f, 240f, 300f)
                    for (ang in angles) {
                        val rads = ang * (PI / 180f)
                        val offsetSpike = Offset(
                            centerPt.x + cos(rads).toFloat() * rad * 1.5f,
                            centerPt.y + sin(rads).toFloat() * rad * 1.5f
                        )
                        drawLine(Color(obs.color), centerPt, offsetSpike, strokeWidth = 1f)
                    }
                }
                "NODE" -> {
                    if (obs.health > 0) {
                        // Threatening security program turret
                        drawCircle(Color(0xFFFF3333), radius = rad, center = centerPt)
                        drawCircle(Color.White, radius = rad, center = centerPt, style = Stroke(width = 2f))
                        // Draw health ratio
                        val hpRatio = obs.health / 20f
                        drawArc(
                            color = CyanAccent,
                            startAngle = 0f,
                            sweepAngle = 360f * hpRatio,
                            useCenter = false,
                            topLeft = Offset(centerPt.x - rad * 1.2f, centerPt.y - rad * 1.2f),
                            size = Size(rad * 2.4f, rad * 2.4f),
                            style = Stroke(width = 3f)
                        )
                    }
                }
            }
        }
    }
}
