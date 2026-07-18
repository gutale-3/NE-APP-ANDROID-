package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// ==========================================
// MODELS
// ==========================================

@Serializable
data class Product(
    val category: String,
    val name: String,
    val model: String,
    val features: String,
    val pcsCtn: String = "",
    val retail: Double = 0.0,
    val price: Double = 0.0,
    val image: String = ""
)

data class JourneyStep(
    val id: String,
    val label: String,
    val eyebrow: String,
    val title: String,
    val body: String,
    val tags: List<String>,
    val icon: ImageVector,
    val highlightColor: Color
)

data class Solution(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val tag: String,
    val subtitle: String,
    val description: String,
    val architecture: List<String>,
    val recommendedProducts: List<String>
)

// ==========================================
// MAIN ACTIVITY
// ==========================================

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            NashnaalTheme {
                MainAppScreen()
            }
        }
    }
}

// ==========================================
// THEME
// ==========================================

val NashnaalBlue = Color(0xFF0B8FCB)
val NashnaalNavy = Color(0xFF063A54)
val NashnaalLightBlue = Color(0xFFE0F2FE)
val BackgroundColor = Color(0xFFF8FAFC)
val CardBorderColor = Color(0xFFE2E8F0)

@Composable
fun NashnaalTheme(content: @Composable () -> Unit) {
    val colorScheme = lightColorScheme(
        primary = NashnaalBlue,
        onPrimary = Color.White,
        primaryContainer = NashnaalLightBlue,
        secondary = NashnaalNavy,
        background = BackgroundColor,
        surface = Color.White,
        onSurface = Color(0xFF0F172A),
        onSurfaceVariant = Color(0xFF475569)
    )

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

// ==========================================
// VIEW MODEL
// ==========================================

class MainViewModel(context: Context) : ViewModel() {
    var products by mutableStateOf<List<Product>>(emptyList())
        private set

    var categories by mutableStateOf<List<String>>(emptyList())
        private set

    init {
        products = loadProductsFromAssets(context)
        categories = listOf("All") + products.map { it.category }.distinct().sorted()
    }

    private fun loadProductsFromAssets(context: Context): List<Product> {
        return try {
            val jsonString = context.assets.open("products.json").bufferedReader().use { it.readText() }
            Json { ignoreUnknownKeys = true }.decodeFromString<List<Product>>(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}

// ==========================================
// OUTBOUND COMMUNICATIONS INTENTS
// ==========================================

fun launchWhatsApp(context: Context, text: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://api.whatsapp.com/send?phone=254798131085&text=${Uri.encode(text)}")
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://wa.me/254798131085?text=${Uri.encode(text)}")
        }
        context.startActivity(intent)
    }
}

fun launchDialer(context: Context) {
    val intent = Intent(Intent.ACTION_DIAL).apply {
        data = Uri.parse("tel:+254798131085")
    }
    context.startActivity(intent)
}

fun launchEmail(context: Context) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:sales@nashnaal.com")
        putExtra(Intent.EXTRA_SUBJECT, "Inquiry about Hikvision Systems")
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        // Fallback
    }
}

fun launchMap(context: Context) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse("geo:-1.2790893,36.8474745?q=Business+Bay+Square,+Nairobi,+Kenya")
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        // Fallback
    }
}

// ==========================================
// CORE LAYOUT / NAVIGATION SHELL
// ==========================================

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Explore : Screen("explore", "Explore", Icons.Default.Star)
    object Products : Screen("products", "Products", Icons.Default.ShoppingCart)
    object Solutions : Screen("solutions", "Solutions", Icons.Default.Info)
    object Services : Screen("services", "Services", Icons.Default.Build)
}

@Composable
fun MainAppScreen() {
    val context = LocalContext.current
    val viewModel: MainViewModel = viewModel { MainViewModel(context) }

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    var selectedProductForDetail by remember { mutableStateOf<Product?>(null) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("bottom_nav_bar"),
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                val screens = listOf(
                    Screen.Home,
                    Screen.Explore,
                    Screen.Products,
                    Screen.Solutions,
                    Screen.Services
                )
                screens.forEach { screen ->
                    val isSelected = currentScreen == screen
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentScreen = screen },
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title,
                                tint = if (isSelected) NashnaalBlue else Color.Gray
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp,
                                color = if (isSelected) NashnaalBlue else Color.Gray
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = NashnaalLightBlue
                        ),
                        modifier = Modifier.testTag("nav_item_${screen.route}")
                    )
                }
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundColor)
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "screen_transition"
            ) { screen ->
                when (screen) {
                    is Screen.Home -> HomeScreen(
                        onNavigateToProducts = { currentScreen = Screen.Products },
                        onNavigateToExplore = { currentScreen = Screen.Explore },
                        onNavigateToSolutions = { currentScreen = Screen.Solutions },
                        onProductDetail = { selectedProductForDetail = it }
                    )
                    is Screen.Explore -> ExploreScreen()
                    is Screen.Products -> ProductsScreen(
                        products = viewModel.products,
                        categories = viewModel.categories,
                        onProductClick = { selectedProductForDetail = it }
                    )
                    is Screen.Solutions -> SolutionsScreen()
                    is Screen.Services -> ServicesScreen()
                }
            }

            // Product Detail Dialog
            selectedProductForDetail?.let { product ->
                ProductDetailDialog(
                    product = product,
                    onDismiss = { selectedProductForDetail = null }
                )
            }
        }
    }
}

// ==========================================
// HOME SCREEN
// ==========================================

@Composable
fun HomeScreen(
    onNavigateToProducts: () -> Unit,
    onNavigateToExplore: () -> Unit,
    onNavigateToSolutions: () -> Unit,
    onProductDetail: (Product) -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen"),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // --- HERO BANNER ---
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(NashnaalBlue, NashnaalNavy)
                        )
                    )
                    .padding(horizontal = 24.dp, vertical = 40.dp)
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(99.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF5C5C))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Authorized Hikvision Partner",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Genuine Hikvision Security Systems in Kenya",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 38.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Authorized distributor supplying CCTV, Access Control, Video Intercoms, and Structured Cabling with certified Nairobi installation.",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = onNavigateToProducts,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = NashnaalBlue),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("browse_products_button")
                        ) {
                            Text("Browse Products", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onNavigateToExplore,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Explore NE Journey")
                        }
                    }
                }
            }
        }

        // --- DISTRIBUTOR HIGHLIGHTS ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val highlights = listOf(
                    Triple(Icons.Default.Done, "Genuine", "100% Channel Stock"),
                    Triple(Icons.Default.Call, "Certified", "Pro Installers"),
                    Triple(Icons.Default.Send, "Nationwide", "Fast Dispatch")
                )
                highlights.forEach { (icon, title, desc) ->
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, CardBorderColor)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = title,
                                tint = NashnaalBlue,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = NashnaalNavy
                            )
                            Text(
                                text = desc,
                                fontSize = 10.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // --- FEATURED INNOVATIONS SECTION ---
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Featured innovations",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    color = NashnaalNavy
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Innovation Card 1: G4 Dash Cam
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, CardBorderColor)
                ) {
                    Column {
                        // Header color panel
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFF0F172A), NashnaalNavy)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Car recording",
                                    tint = Color.White,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("AE-DI5042-G4", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Smart G4 Dash Camera",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = NashnaalNavy
                                )
                                Box(
                                    modifier = Modifier
                                        .background(NashnaalLightBlue, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("ADAS System", color = NashnaalBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Active road safety recording with 1440p super-clear video resolution, built-in driver assistance warning, and collision sensor alert system.",
                                fontSize = 13.sp,
                                color = Color.Gray,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    launchWhatsApp(context, "Hi NE, I'm interested in the Hikvision G4 Dash Camera (AE-DI5042-G4)")
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Send, contentDescription = "WhatsApp")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Enquire on WhatsApp")
                            }
                        }
                    }
                }

                // Innovation Card 2: ColorVu Turbo HD
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, CardBorderColor)
                ) {
                    Column {
                        // Header color panel
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(NashnaalBlue, Color(0xFF0369A1))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "CCTV HD",
                                    tint = Color.White,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("KF0T Series & ColorVu", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Turbo HD Night Vision",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = NashnaalNavy
                                )
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFFEF08A), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("F1.0 Lens Color", color = Color(0xFF854D0E), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Next-generation night vision capturing full-color video feed even in total darkness. Uses Smart Hybrid IR + White LED light with 3K resolution.",
                                fontSize = 13.sp,
                                color = Color.Gray,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    launchWhatsApp(context, "Hi NE, I'm interested in the KF0T ColorVu Turbo HD Night Vision Cameras")
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Send, contentDescription = "WhatsApp")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Enquire on WhatsApp")
                            }
                        }
                    }
                }
            }
        }

        // --- SOLUTIONS DIRECTORY CHIPS ---
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Security Solutions by Sector",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = NashnaalNavy
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val sectors = listOf("Buildings", "Education", "Logistics", "Retail", "Small Biz", "Traffic")
                    items(sectors) { sector ->
                        Card(
                            onClick = onNavigateToSolutions,
                            colors = CardDefaults.cardColors(containerColor = NashnaalLightBlue.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(99.dp)
                        ) {
                            Text(
                                text = sector,
                                color = NashnaalNavy,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // --- STORE VISIT DETAILS & CONTACT INFO ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NashnaalNavy),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Visit NE Nairobi Office & Showroom",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "📍 Business Bay Square, GFE 61, General Waruingi Road, Eastleigh, Nairobi, Kenya.",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "⏰ Monday - Saturday: 8:00 AM - 8:00 PM\n      Sunday: 9:00 AM - 6:00 PM",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { launchDialer(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = NashnaalBlue, contentColor = Color.White),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = "Call", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Call Us", fontSize = 13.sp)
                        }
                        Button(
                            onClick = { launchMap(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f), contentColor = Color.White),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = "Map", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("View Map", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// EXPLORE SCREEN (The 13-Step NE World Journey)
// ==========================================

@Composable
fun ExploreScreen() {
    val journeySteps = remember {
        listOf(
            JourneyStep(
                "sourcing", "Sourcing", "Authorized Hikvision Channel",
                "Every Camera Starts with the Real Thing",
                "NE sources every unit directly through Hikvision’s official channels — guaranteed genuine stock, complete firmware upgrades, and valid warranty. No grey-market guesswork.",
                listOf("Genuine Stock", "Official Channel", "Full Warranty"),
                Icons.Default.Done, Color(0xFF0284C7)
            ),
            JourneyStep(
                "warehouse", "Warehouse Hub", "Secure Distribution Base",
                "Stocked, Scanned, & Dispatched Instantly",
                "Our central Nairobi warehouse holds extensive stock of Turbo HD, IP Cameras, NVRs, network hubs, and fiber accessories. Ready for rapid logistics delivery nationwide.",
                listOf("Bulk Availability", "Secure Scanned", "Fast dispatch"),
                Icons.Default.Home, Color(0xFF0B8FCB)
            ),
            JourneyStep(
                "install", "Installation", "Expert Field Technicians",
                "System Design & Mounting Done Properly",
                "NE's certified technicians handle end-to-end onsite assessment, custom system design, proper wall mounts, angle settings, and server cabinet organization.",
                listOf("Certified Setup", "Clean Cabling", "Angle Optima"),
                Icons.Default.Build, Color(0xFF0F766E)
            ),
            JourneyStep(
                "monitoring", "Support & Monitoring", "Pro After-Sales Care",
                "Live Monitoring & Always-On Assistance",
                "Every client system NE sets up is supported by local technical agents. Get remote system updates, camera alignment adjustments, and hardware troubleshooting.",
                listOf("Remote Dial-in", "Local Nairobi Support", "Uptime Care"),
                Icons.Default.Settings, Color(0xFF4F46E5)
            ),
            JourneyStep(
                "camera", "Flagship Hardware", "Hikvision CCTV Core",
                "High Definition Multi-Pixel Resolution",
                "Robust camera casings built for local Kenyan weather conditions. Features smart motion detection 2.0, perimeter filter, and vehicle/human tracking.",
                listOf("Smart Motion", "Weather-proof IP67", "Acusense Tech"),
                Icons.Default.PlayArrow, Color(0xFF0F172A)
            ),
            JourneyStep(
                "colorvu", "ColorVu", "Advanced Low Light",
                "True Color Video Feeds in Near Darkness",
                "Traditional IR produces grainy black-and-white night footage. Hikvision ColorVu captures vivid full-color videos at midnight using F1.0 super-apertures.",
                listOf("24/7 Color", "F1.0 Aperture", "Warm LED Fill"),
                Icons.Default.Star, Color(0xFFD97706)
            ),
            JourneyStep(
                "duallens", "Dual-Lens Panoramic", "Maximum Wide Angle",
                "Stitched Views with Zero Angle Blind Spots",
                "Cover sprawling storefronts, large schoolyards, or warehouse bays in one seamless 180° image, eliminating the need for two separate cameras.",
                listOf("180° Panoramic", "Dual Sensors", "Pixel Stitching"),
                Icons.Default.Refresh, Color(0xFF059669)
            ),
            JourneyStep(
                "access", "Access Control", "Biometric Building Safety",
                "Manage Biometrics & Cards Securely",
                "Protect entries with fingerprint scanners, card readers, or custom facial recognition panels fully integrated with magnetic doors and intercom feedback.",
                listOf("Face Recognition", "Fingerprint Auth", "Magnetic Lock"),
                Icons.Default.Lock, Color(0xFFDC2626)
            ),
            JourneyStep(
                "networking", "Data Comm & Wi-Fi", "Core Network System",
                "High Speed Mesh Wi-Fi in Every Corner",
                "NE installs premium ceiling access points, high-speed PoE network switches, and custom routers to ensure heavy camera video streams never lag.",
                listOf("Gigabit Ports", "PoE Switches", "Access Points"),
                Icons.Default.Settings, Color(0xFF2563EB)
            ),
            JourneyStep(
                "display", "Smart Interactive Tablets", "Boardroom Display Panels",
                "4K Displays for Collaboration & Sync",
                "Equip classrooms, command offices, or corporate boardrooms with state-of-the-art 4K responsive touch displays featuring screen-sharing and interactive whiteboards.",
                listOf("4K Touch Screen", "Multi-Device Share", "Android Hub"),
                Icons.Default.Info, Color(0xFF7C3AED)
            ),
            JourneyStep(
                "power", "UPS Power Systems", "Electric Uptime Insurance",
                "Uptime Maintained Even When the Grid Fails",
                "Kenya power blackouts shouldn't blind your security. NE supplies custom uninterruptible power supplies (UPS) to keep cameras and NVR records running 24/7.",
                listOf("UPS Backup", "Surge Protection", "Always Online"),
                Icons.Default.PlayArrow, Color(0xFF059669)
            ),
            JourneyStep(
                "cabling", "Structured Cabling", "High Performance Media",
                "Terminated Cat6 Connections Built to Last",
                "Clean wiring stays reliable. We deploy pure copper Cat6 cabling, professional patch panel connections, and heavy-duty conduit tubing.",
                listOf("Cat6 Pure Copper", "Patch Panels", "Conduit Protect"),
                Icons.Default.Menu, Color(0xFF0284C7)
            ),
            JourneyStep(
                "coverage", "Total Site Coverage", "Professional Final Handover",
                "Integrated Security Ecosystem handovers",
                "We walk you through Hik-Connect app configurations on your phones, explain the physical NVR operations, and hand over a pristine, fully documented layout.",
                listOf("Hik-Connect Mobile", "Full NVR Access", "Owner Handover"),
                Icons.Default.Lock, Color(0xFF063A54)
            )
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("explore_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                Text(
                    text = "Fly Through the NE World",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp,
                    color = NashnaalNavy
                )
                Text(
                    text = "Understand how NE sources, stocks, deploys, and secures premium Hikvision hardware across Kenya step-by-step.",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    lineHeight = 20.sp
                )
            }
        }

        items(journeySteps) { step ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, CardBorderColor),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(step.highlightColor.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = step.icon,
                                contentDescription = step.label,
                                tint = step.highlightColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = step.eyebrow,
                                color = step.highlightColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = step.label,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = NashnaalNavy
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = step.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = step.body,
                        fontSize = 13.sp,
                        color = Color.Gray,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Tags flow
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        step.tags.forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .background(BackgroundColor, RoundedCornerShape(99.dp))
                                    .border(1.dp, CardBorderColor, RoundedCornerShape(99.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(tag, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// PRODUCTS SCREEN (Catalog with Search & Filter)
// ==========================================

@Composable
fun ProductsScreen(
    products: List<Product>,
    categories: List<String>,
    onProductClick: (Product) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val filteredProducts = remember(products, searchQuery, selectedCategory) {
        products.filter { product ->
            val matchesCategory = selectedCategory == "All" || product.category == selectedCategory
            val matchesSearch = product.name.contains(searchQuery, ignoreCase = true) ||
                    product.model.contains(searchQuery, ignoreCase = true) ||
                    product.features.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("products_screen")
    ) {
        // --- SEARCH HEADER ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_input"),
                placeholder = { Text("Search by name, model or specs...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        }

        // --- HORIZONTAL CATEGORIES ROW ---
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { category ->
                val isSelected = selectedCategory == category
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategory = category },
                    label = { Text(category) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NashnaalBlue,
                        selectedLabelColor = Color.White,
                        containerColor = BackgroundColor,
                        labelColor = Color.Gray
                    ),
                    shape = RoundedCornerShape(99.dp)
                )
            }
        }

        Divider(color = CardBorderColor)

        // --- PRODUCTS LIST ---
        if (filteredProducts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "No products found",
                        tint = Color.Gray,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No products match your search", fontWeight = FontWeight.Bold, color = Color.Gray)
                    Text("Try looking in another category", fontSize = 12.sp, color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "Found ${filteredProducts.size} results",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                items(filteredProducts) { product ->
                    ProductItemRow(product = product, onClick = { onProductClick(product) })
                }
            }
        }
    }
}

@Composable
fun ProductItemRow(product: Product, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("product_item_card_${product.model}"),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, CardBorderColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Simulated Product Thumbnail
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(NashnaalLightBlue.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "CCTV camera",
                    tint = NashnaalBlue,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.category,
                    color = NashnaalBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
                Text(
                    text = product.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = NashnaalNavy,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Model: ${product.model}",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (product.price > 0.0) "KES %,.0f".format(product.price) else "Inquire Price",
                    color = Color(0xFF059669),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp
                )
            }

            IconButton(onClick = onClick) {
                Icon(Icons.Default.ArrowForward, contentDescription = "View details", tint = Color.Gray)
            }
        }
    }
}

// ==========================================
// SOLUTIONS SCREEN
// ==========================================

@Composable
fun SolutionsScreen() {
    val solutions = remember {
        listOf(
            Solution(
                "buildings", "Smart Commercial Buildings", Icons.Default.Home,
                "Buildings", "Office Towers & Malls",
                "Secure, automated pedestrian entries, digital visitor clearance, smart elevator control integration, and continuous parking bay camera loops.",
                listOf(
                    "Facial Recognition Access Terminal at turnstiles",
                    "Under-vehicle surveillance checkpoints at garages",
                    "Dual-lens panoramic cameras in corridors",
                    "Hik-Central multi-station control dashboard"
                ),
                listOf("DS-2CD1143G2-LIU (4MP Dome)", "iDS-7216HQHI-M1 (16CH Acusense DVR)")
            ),
            Solution(
                "education", "Educational Campus Hubs", Icons.Default.Info,
                "Education", "Schools & Universities",
                "Broad campus boundary visual monitoring, main reception security gates, classroom digital display displays, and real-time announcement systems.",
                listOf(
                    "Cat6 fiber boundary camera circuits",
                    "Biometric access control for student labs",
                    "4K Interactive Touch Panels in lecture halls",
                    "Video Intercom links to Principal's office"
                ),
                listOf("DS-2CD1063G2-LIU (6MP Outdoor Bullet)", "DS-3WRE3N (Wi-Fi Access Point)")
            ),
            Solution(
                "logistics", "Warehouses & Logistics Ports", Icons.Default.Send,
                "Logistics", "Distribution Centers",
                "Complete bay loading monitors, forklift route cameras, high-definition stock scans, secure back office keyless locking, and UPS blackout backup systems.",
                listOf(
                    "24/7 ColorVu cameras at shipping bays",
                    "Thermal heat sensors for fire detection",
                    "Structured fiber trunks spanning large roofs",
                    "UPS backup systems holding 8-hour logs"
                ),
                listOf("DS-2CD1643G2-LIZU (Varifocal)", "DS-3E0518P-E (PoE Gigabit Switch)")
            ),
            Solution(
                "retail", "Retail & Shopping Supermarkets", Icons.Default.ShoppingCart,
                "Retail", "Stores & Supermarkets",
                "Full coverage of checkout counters, detailed record storage, aisle flow analysis, customer foot traffic indices, and automated stock surveillance.",
                listOf(
                    "Ultra-high resolution dome cams at registers",
                    "Acusense human categorization search",
                    "Intelligent noise-reduction intercom audio",
                    "Mobile alert sync with security guard phones"
                ),
                listOf("DS-2CE70DF0T-LPFS (ColorVu Turret)", "DS-7104HGHI-M1/T (Smart Audio DVR)")
            ),
            Solution(
                "smb", "Small & Medium Business (SMB)", Icons.Default.ShoppingCart,
                "Small Biz", "Offices & Local Stores",
                "Affordable CCTV setups, reliable Wi-Fi mesh systems, smart lock gates, and rapid Hik-Connect setup for instant monitoring from home.",
                listOf(
                    "4-Camera Turbo HD hybrid starter kit",
                    "Smart lock fingerprint entries",
                    "Wi-Fi Router extending mesh across office",
                    "App setup on up to 5 user smartphones"
                ),
                listOf("DS-2CE16D0T-LPFS (Dual-Light Camera)", "DS-7104HGHI-M1 (Budget NVR)")
            ),
            Solution(
                "traffic", "Intelligent Highway Traffic", Icons.Default.Warning,
                "Traffic", "Roadways & Parking Sites",
                "Automated speed cameras, number plate scanner, high-altitude wide viewing angles, and rugged dustproof pole installations.",
                listOf(
                    "License Plate Recognition (ANPR) cameras",
                    "Full weatherproof casing with dust guards",
                    "Wireless microwave links back to command room",
                    "Rugged solar battery panels for continuous highway feed"
                ),
                listOf("DS-2CD1083G2-LIU (8MP 4K Bullet)", "DS-3WF02-5AC (Wireless Bridges)")
            )
        )
    }

    var selectedSolution by remember { mutableStateOf(solutions.first()) }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .testTag("solutions_screen")
    ) {
        // Left Column: Industry Selector
        Column(
            modifier = Modifier
                .width(110.dp)
                .fillMaxHeight()
                .background(Color.White)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            solutions.forEach { solution ->
                val isSelected = selectedSolution == solution
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedSolution = solution }
                        .background(if (isSelected) NashnaalLightBlue else Color.Transparent)
                        .padding(vertical = 16.dp, horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = solution.icon,
                            contentDescription = solution.title,
                            tint = if (isSelected) NashnaalBlue else Color.Gray,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = solution.tag,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) NashnaalBlue else Color.Gray,
                            textAlign = TextAlign.Center,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }

        Divider(modifier = Modifier.width(1.dp).fillMaxHeight(), color = CardBorderColor)

        // Right Column: Solution Details
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text(
                text = selectedSolution.subtitle.uppercase(),
                color = NashnaalBlue,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 0.5.sp
            )
            Text(
                text = selectedSolution.title,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                color = NashnaalNavy,
                lineHeight = 26.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = selectedSolution.description,
                fontSize = 14.sp,
                color = Color.Gray,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "System Architecture",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = NashnaalNavy
            )
            Spacer(modifier = Modifier.height(8.dp))
            selectedSolution.architecture.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Done,
                        contentDescription = "Check",
                        tint = Color(0xFF059669),
                        modifier = Modifier
                            .size(18.dp)
                            .padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = item, fontSize = 13.sp, color = Color(0xFF0F172A), lineHeight = 18.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Core Recommended Products",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = NashnaalNavy
            )
            Spacer(modifier = Modifier.height(8.dp))
            selectedSolution.recommendedProducts.forEach { prodName ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = BackgroundColor),
                    border = BorderStroke(1.dp, CardBorderColor)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Product icon",
                            tint = NashnaalBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = prodName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = NashnaalNavy
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// SERVICES & SUPPORT SCREEN (Interactive Contact Flow)
// ==========================================

@Composable
fun ServicesScreen() {
    val context = LocalContext.current

    // Contact Form States
    var clientName by remember { mutableStateOf("") }
    var clientPhone by remember { mutableStateOf("") }
    var clientService by remember { mutableStateOf("CCTV Installation") }
    var clientLocation by remember { mutableStateOf("") }
    var isSubmittedSuccess by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("services_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                Text(
                    text = "Professional Security Services",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp,
                    color = NashnaalNavy
                )
                Text(
                    text = "Beyond supplying hardware, NE designs, installs, and supports complex corporate security networks and custom residential setups.",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    lineHeight = 20.sp
                )
            }
        }

        // Services Cards
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, CardBorderColor),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("🛠️ Certified Installation", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = NashnaalNavy)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "NE's installers are certified system integrators. We map physical premises, establish optimal viewing angles, terminate fiber/Cat6 runs clean, and configure secure recording cabinets.",
                        fontSize = 13.sp, color = Color.Gray, lineHeight = 18.sp
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, CardBorderColor),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("💡 Security System Design", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = NashnaalNavy)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "We design custom blueprints for malls, hospitals, universities, and homes. This guarantees that you purchase exactly the cameras and switches needed, with zero waste.",
                        fontSize = 13.sp, color = Color.Gray, lineHeight = 18.sp
                    )
                }
            }
        }

        // Interactive Support Callback Form
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, CardBorderColor),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Book Site Survey or Get Quote",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = NashnaalNavy
                    )
                    Text(
                        text = "Fill in the details below. This will launch a direct WhatsApp dispatch message with your survey request.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    if (isSubmittedSuccess) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFDCFCE7), RoundedCornerShape(8.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Done, contentDescription = "Success", tint = Color(0xFF16A34A), modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Survey Requested!", fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                                Text("Check your WhatsApp to complete dispatch.", fontSize = 11.sp, color = Color(0xFF15803D))
                                Spacer(modifier = Modifier.height(12.dp))
                                TextButton(onClick = { isSubmittedSuccess = false }) {
                                    Text("Submit Another Request")
                                }
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = clientName,
                            onValueChange = { clientName = it },
                            label = { Text("Your Name") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .testTag("survey_name_input"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = clientPhone,
                            onValueChange = { clientPhone = it },
                            label = { Text("Phone Number") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .testTag("survey_phone_input"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = clientLocation,
                            onValueChange = { clientLocation = it },
                            label = { Text("Location (e.g., Nairobi, Mombasa)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            singleLine = true
                        )

                        Text("Select System Required", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NashnaalNavy)
                        val systems = listOf("CCTV Installation", "Access Control System", "Video Intercom", "Complete Network Setup")
                        systems.forEach { system ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { clientService = system }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = clientService == system,
                                    onClick = { clientService = system }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(system, fontSize = 13.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (clientName.isNotBlank() && clientPhone.isNotBlank()) {
                                    val message = """
                                        Hi NE support, I want to book a site survey:
                                        - Name: $clientName
                                        - Phone: $clientPhone
                                        - Service: $clientService
                                        - Location: $clientLocation
                                    """.trimIndent()
                                    launchWhatsApp(context, message)
                                    isSubmittedSuccess = true
                                }
                            },
                            enabled = clientName.isNotBlank() && clientPhone.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("submit_survey_button"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Done, contentDescription = "Submit")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Submit Survey via WhatsApp")
                        }
                    }
                }
            }
        }

        // Quick contacts card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = NashnaalLightBlue.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, CardBorderColor),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Direct Support Channels", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = NashnaalNavy)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { launchWhatsApp(context, "Hi NE, I have an after-sales support question.") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "WhatsApp")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Chat on WhatsApp")
                    }
                    Button(
                        onClick = { launchDialer(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = NashnaalBlue),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = "Phone")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Call Helpline (0798 131085)")
                    }
                    Button(
                        onClick = { launchEmail(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = NashnaalNavy),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Email, contentDescription = "Email")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Email Sales (sales@nashnaal.com)")
                    }
                }
            }
        }
    }
}

// ==========================================
// PRODUCT DETAIL SHEET DIALOG
// ==========================================

@Composable
fun ProductDetailDialog(product: Product, onDismiss: () -> Unit) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val msg = "Hi NE, I am interested in the Hikvision product: ${product.name} (Model: ${product.model}, Price: KES ${"%,.0f".format(product.price)})."
                    launchWhatsApp(context, msg)
                },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("inquire_product_dialog_button")
            ) {
                Icon(Icons.Default.Send, contentDescription = "WhatsApp")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Enquire via WhatsApp")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color.Gray)
            }
        },
        title = {
            Column {
                Text(
                    text = product.category.uppercase(),
                    color = NashnaalBlue,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = product.name,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = NashnaalNavy,
                    lineHeight = 22.sp
                )
                Text(
                    text = "Model: ${product.model}",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Divider(color = CardBorderColor, modifier = Modifier.padding(vertical = 8.dp))

                Text(
                    text = "Specifications & Features",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = NashnaalNavy
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = product.features,
                    fontSize = 13.sp,
                    color = Color.Gray,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Carton Capacity", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = NashnaalNavy)
                        Text("${product.pcsCtn} pcs/Ctn", fontSize = 12.sp, color = Color.Gray)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Retail Price", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = NashnaalNavy)
                        Text(if (product.retail > 0.0) "KES %,.0f".format(product.retail) else "Inquire", fontSize = 12.sp, color = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NashnaalLightBlue.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Done, contentDescription = "Genuine", tint = NashnaalBlue, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Genuine Hikvision Kenya channel stock.",
                            color = NashnaalNavy,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = Color.White
    )
}
