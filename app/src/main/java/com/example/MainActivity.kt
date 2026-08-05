package com.example

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Send
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.TranslatedPage
import com.example.data.model.TranslationHistory
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainUiState
import com.example.ui.viewmodel.TranslationViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            if (com.google.firebase.FirebaseApp.getApps(this).isEmpty()) {
                com.google.firebase.FirebaseApp.initializeApp(this)
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "FirebaseApp init error: ${e.message}")
        }
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: TranslationViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val context = LocalContext.current
    var showHelpDialog by remember { mutableStateOf(false) }

    remember(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearErrorMessage()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Google PDF Çevirici",
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    }
                },
                navigationIcon = {
                    if (uiState !is MainUiState.Home) {
                        IconButton(onClick = { 
                            viewModel.stopSpeaking()
                            viewModel.navigateToHome() 
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Default.ArrowBack,
                                contentDescription = "Geri Dön"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Hakkında ve Yardım"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (val state = uiState) {
                is MainUiState.Home -> {
                    HomeScreen(viewModel = viewModel)
                }
                is MainUiState.Translating -> {
                    TranslatingScreen(state = state, onCancelClick = { viewModel.cancelTranslation() })
                }
                is MainUiState.ViewTranslation -> {
                    ViewTranslationScreen(state = state, viewModel = viewModel)
                }
            }

            // Help & Guide Dialog
            if (showHelpDialog) {
                HelpGuideDialog(onDismiss = { showHelpDialog = false })
            }
        }
    }
}

@Composable
fun HomeScreen(viewModel: TranslationViewModel) {
    val historyList by viewModel.historyList.collectAsState()
    val sourceLang by viewModel.sourceLang.collectAsState()
    val targetLang by viewModel.targetLang.collectAsState()

    // Configuration States
    val documentType by viewModel.documentType.collectAsState()
    val formality by viewModel.formality.collectAsState()
    val customGlossary by viewModel.customGlossary.collectAsState()
    val customApiKey by viewModel.customApiKey.collectAsState()

    var searchHistoryQuery by remember { mutableStateOf("") }
    var expandSettingsCard by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // SAF PDF Picker Launcher
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = getFileName(context, it) ?: "Doküman.pdf"
            viewModel.startPdfTranslation(it, fileName)
        }
    }

    // Filter history based on search query
    val filteredHistory = remember(historyList, searchHistoryQuery) {
        if (searchHistoryQuery.isBlank()) {
            historyList
        } else {
            historyList.filter {
                it.fileName.contains(searchHistoryQuery, ignoreCase = true) ||
                it.sourceLang.contains(searchHistoryQuery, ignoreCase = true) ||
                it.targetLang.contains(searchHistoryQuery, ignoreCase = true)
            }
        }
    }

    // Advanced statistics calculation (40+ refinements item)
    val totalDocuments = historyList.size
    val totalBookmarked = historyList.count { it.isBookmarked }
    val uniqueTargetLangs = historyList.map { it.targetLang }.distinct().size

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Image Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.translation_hero_banner),
                        contentDescription = "PDF Çeviri Görseli",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)),
                                    startY = 50f
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Text(
                            text = "MULTİMODAL YAPAY ZEKA GÜCÜYLE",
                            color = MaterialTheme.colorScheme.primaryContainer,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Görsel ve Metin PDF Çevirici",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Stats Overview Row (Refinement: Dashboard metrics)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatsCard(
                    title = "Çevrilen",
                    value = "$totalDocuments Belge",
                    icon = Icons.Default.Book,
                    modifier = Modifier.weight(1f)
                )
                StatsCard(
                    title = "Favori",
                    value = "$totalBookmarked Belge",
                    icon = Icons.Default.Bookmark,
                    modifier = Modifier.weight(1f)
                )
                StatsCard(
                    title = "Diller",
                    value = "$uniqueTargetLangs Hedef",
                    icon = Icons.Default.Language,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Configuration Toggle Button
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (expandSettingsCard) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) 
                                    else MaterialTheme.colorScheme.surfaceVariant
                ),
                onClick = { expandSettingsCard = !expandSettingsCard }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Gelişmiş Çeviri Ayarları",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Manga, Akademik Makale, Terim Sözlüğü vb.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        imageVector = if (expandSettingsCard) Icons.Default.Close else Icons.Default.Translate,
                        contentDescription = "Genişlet",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Expanded Settings Panel
        if (expandSettingsCard) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Çeviri Parametreleri",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )

                        // Document Type Selector
                        Column {
                            Text(text = "Belge Türü", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            val docTypes = listOf("Standart Belge", "Çizgi Roman / Manga", "Akademik Makale", "Teknik Kılavuz")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                docTypes.forEach { type ->
                                    val isSelected = documentType == type
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primary 
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            )
                                            .clickable { viewModel.setDocumentType(type) }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = type.replace(" / ", "/"),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary 
                                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }

                        // Formality Selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "Çeviri Tonu", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text(text = "Samimi veya resmi hitap dili", fontSize = 10.sp, color = Color.Gray)
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("Resmi", "Samimi").forEach { form ->
                                    val isSelected = formality == form
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.secondary 
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            )
                                            .clickable { viewModel.setFormality(form) }
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = form,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onSecondary 
                                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // Custom Glossary Input
                        Column {
                            Text(text = "Özel Terim Sözlüğü (Glossary)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text(text = "Her satıra 'aranan=çevirisi' yazın (Örn: AI=Yapay Zeka)", fontSize = 10.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = customGlossary,
                                onValueChange = { viewModel.setCustomGlossary(it) },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("E.g.\nPDF=Belge\nAgent=Temsilci", fontSize = 12.sp) },
                                maxLines = 3,
                                textStyle = MaterialTheme.typography.bodyMedium,
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                )
                            )
                        }


                    }
                }
            }
        }

        // Language Selectors
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    LanguageSelectionMenu(
                        selectedLang = sourceLang,
                        languages = viewModel.languages.map { it.first },
                        onLangSelected = { viewModel.setSourceLang(it) },
                        title = "Kaynak Dil",
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = {
                            if (sourceLang != "Otomatik Algıla") {
                                val temp = sourceLang
                                viewModel.setSourceLang(targetLang)
                                viewModel.setTargetLang(temp)
                            }
                        },
                        enabled = sourceLang != "Otomatik Algıla",
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (sourceLang != "Otomatik Algıla") MaterialTheme.colorScheme.primary 
                                else MaterialTheme.colorScheme.outlineVariant, 
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Dilleri Değiştir",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    LanguageSelectionMenu(
                        selectedLang = targetLang,
                        languages = viewModel.languages.filter { it.first != "Otomatik Algıla" }.map { it.first },
                        onLangSelected = { viewModel.setTargetLang(it) },
                        title = "Hedef Dil",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Action Trigger Button
        item {
            Button(
                onClick = { pdfPickerLauncher.launch("application/pdf") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "PDF SEÇ VE ANINDA ÇEVİR",
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        // Search Bar (Refinement: Search history filter)
        item {
            OutlinedTextField(
                value = searchHistoryQuery,
                onValueChange = { searchHistoryQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Geçmişte dosya veya dil ara...", fontSize = 13.sp) },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchHistoryQuery.isNotEmpty()) {
                        IconButton(onClick = { searchHistoryQuery = "" }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Temizle", modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )
        }

        // History Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Çeviri Geçmişi",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                if (historyList.isNotEmpty()) {
                    Text(
                        text = "Tümünü Temizle",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { viewModel.clearAllHistory() }
                    )
                }
            }
        }

        // History list items
        if (filteredHistory.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceContainerLowest,
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Book,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchHistoryQuery.isNotEmpty()) "Arama kriterine uygun sonuç bulunamadı" else "Henüz çeviri yapılmadı",
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        } else {
            items(filteredHistory) { item ->
                AdvancedHistoryRow(
                    item = item,
                    onItemClick = { viewModel.viewTranslationDetail(item.id) },
                    onDeleteClick = { viewModel.deleteHistoryItem(item.id) },
                    onBookmarkToggle = { viewModel.toggleBookmark(item.id, !item.isBookmarked) }
                )
            }
        }
    }
}

@Composable
fun StatsCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = title, fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
            Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun LanguageSelectionMenu(
    selectedLang: String,
    languages: List<String>,
    onLangSelected: (String) -> Unit,
    title: String,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = title, fontSize = 10.sp, color = MaterialTheme.colorScheme.outline, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable { expanded = true }
                .padding(vertical = 10.dp, horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = selectedLang,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.heightIn(max = 240.dp)
            ) {
                languages.forEach { lang ->
                    DropdownMenuItem(
                        text = { Text(text = lang, fontSize = 13.sp) },
                        onClick = {
                            onLangSelected(lang)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AdvancedHistoryRow(
    item: TranslationHistory,
    onItemClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onBookmarkToggle: () -> Unit
) {
    val dateString = remember(item.timestamp) {
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        sdf.format(Date(item.timestamp))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.PictureAsPdf,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.fileName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    // Badge for Document Type
                    Box(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.secondaryContainer,
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.documentType,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${item.sourceLang} → ${item.targetLang}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(text = "•", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    Text(text = dateString, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                }
            }

            Row {
                IconButton(onClick = onBookmarkToggle) {
                    Icon(
                        imageVector = if (item.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Favoriye Ekle",
                        tint = if (item.isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Sil",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun TranslatingScreen(state: MainUiState.Translating, onCancelClick: () -> Unit) {
    val rotation by animateFloatAsState(
        targetValue = 360f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = tween(2000, easing = androidx.compose.animation.core.LinearEasing)
        ),
        label = "rotation"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(110.dp)
                .shadow(elevation = 16.dp, shape = CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(54.dp)
                    .rotate(rotation)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Yapay Zekalı PDF Çevirisi",
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = state.fileName,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Progress Calculations
        val progress = if (state.totalPages > 0) {
            (state.currentPageIndex + 1).toFloat() / state.totalPages
        } else {
            0f
        }

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primaryContainer
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = state.statusMessage,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Aşamalar:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                StepRow(text = "1. PDF Sayfa Çözünürlüğü Optimize Ediliyor", done = state.currentPageIndex > 0)
                StepRow(text = "2. Comic Balonu & Metin Hücreleri Çıkarılıyor", done = state.currentPageIndex > 0 || state.statusMessage.contains("Çeviriyor"))
                StepRow(text = "3. Google Çeviri ile Sayfa Sayfa Çeviriliyor", done = state.statusMessage.contains("Veritabanı") || state.currentPageIndex > 0)
                StepRow(text = "4. Çeviri, Sözlük & Analizler Kaydediliyor", done = false)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        androidx.compose.material3.OutlinedButton(
            onClick = onCancelClick,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
        ) {
            Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Çeviriyi İptal Et", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun StepRow(text: String, done: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(if (done) MaterialTheme.colorScheme.primary else Color.Gray, CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 11.sp,
            color = if (done) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
fun ViewTranslationScreen(
    state: MainUiState.ViewTranslation,
    viewModel: TranslationViewModel
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabTitles = listOf("Çift Panel (Split)", "Sadece Çeviri", "AI Analizleri")

    var currentPageIndex by remember { mutableStateOf(0) }
    val totalPages = state.pages.size
    val activePage = state.pages.getOrNull(currentPageIndex)

    var textSizeMultiplier by remember { mutableFloatStateOf(1.0f) }

    // TTS state observation
    val isTtsActive by viewModel.isTtsActive.collectAsState()

    // Page Notes modification state
    var pageNotesText by remember(activePage) { mutableStateOf(activePage?.userNotes ?: "") }

    // On-demand summary observer
    val pageSummary by viewModel.pageSummary.collectAsState()
    val isSummarizing by viewModel.isSummarizing.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Document Info bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.historyItem.fileName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${state.historyItem.sourceLang} → ${state.historyItem.targetLang} • Sayfa ${currentPageIndex + 1} / $totalPages [Tür: ${state.historyItem.documentType}]",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Tabs
        TabRow(selectedTabIndex = selectedTabIndex) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        // Active page content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            if (activePage != null) {
                when (selectedTabIndex) {
                    0 -> {
                        // Tab 1: Split View (Orijinal görsel ve Çevrilen metin alt alta)
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Original page image box
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White)
                                    .shadow(1.dp, RoundedCornerShape(12.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (activePage.originalPagePath != null && File(activePage.originalPagePath).exists()) {
                                    val bitmap = remember(activePage.originalPagePath) {
                                        BitmapFactory.decodeFile(activePage.originalPagePath)
                                    }
                                    if (bitmap != null) {
                                        Image(
                                            bitmap = bitmap.asImageBitmap(),
                                            contentDescription = "Orijinal Sayfa Görseli",
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Text("Görsel yüklenemedi", color = Color.Gray)
                                    }
                                } else {
                                    Text("Orijinal sayfa görseli bulunamadı.", color = Color.Gray)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Translated text container
                            Card(
                                modifier = Modifier
                                    .weight(1.2f)
                                    .fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    // Controls Header
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "ÇEVRİLEN SAYFA METNİ",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(onClick = { if (textSizeMultiplier > 0.7f) textSizeMultiplier -= 0.1f }) {
                                                Icon(imageVector = Icons.Default.ZoomOut, contentDescription = "Yazıyı Küçült", modifier = Modifier.size(16.dp))
                                            }
                                            IconButton(onClick = { if (textSizeMultiplier < 2.0f) textSizeMultiplier += 0.1f }) {
                                                Icon(imageVector = Icons.Default.ZoomIn, contentDescription = "Yazıyı Büyüt", modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }

                                    LazyColumn(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 4.dp)
                                    ) {
                                        item {
                                            Text(
                                                text = activePage.translatedText,
                                                fontSize = (13 * textSizeMultiplier).sp,
                                                lineHeight = (18 * textSizeMultiplier).sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(bottom = 12.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        // Tab 2: Sadece Çeviri (Görsel yok, tam ekran okuma modu)
                        Card(
                            modifier = Modifier.fillMaxSize(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                                // Toolbar controls
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Tam Ekran Okuma Modu",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        // Text To Speech play/pause
                                        IconButton(onClick = {
                                            if (isTtsActive) {
                                                viewModel.stopSpeaking()
                                            } else {
                                                viewModel.speakText(activePage.translatedText, state.historyItem.targetLang)
                                            }
                                        }) {
                                            Icon(
                                                imageVector = if (isTtsActive) Icons.Default.Stop else Icons.Default.VolumeUp,
                                                contentDescription = "Seslendir",
                                                tint = if (isTtsActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        // Copy Button
                                        IconButton(onClick = {
                                            clipboardManager.setText(AnnotatedString(activePage.translatedText))
                                            Toast.makeText(context, "Metin panoya kopyalandı!", Toast.LENGTH_SHORT).show()
                                        }) {
                                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Kopyala")
                                        }

                                        // Share Button
                                        IconButton(onClick = {
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_SUBJECT, state.historyItem.fileName)
                                                putExtra(Intent.EXTRA_TEXT, activePage.translatedText)
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Çeviriyi Paylaş"))
                                        }) {
                                            Icon(imageVector = Icons.Default.Share, contentDescription = "Paylaş")
                                        }
                                    }
                                }

                                // Interactive Text Size Slider (Refinement: slider font-sizing)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.Default.ZoomOut, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                                    Slider(
                                        value = textSizeMultiplier,
                                        onValueChange = { textSizeMultiplier = it },
                                        valueRange = 0.7f..2.0f,
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 12.dp)
                                    )
                                    Icon(imageVector = Icons.Default.ZoomIn, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                    item {
                                        Text(
                                            text = activePage.translatedText,
                                            fontSize = (15 * textSizeMultiplier).sp,
                                            lineHeight = (22 * textSizeMultiplier).sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        // Tab 3: AI Analizleri (Confidence, Summary, Vocabulary & Notebook!)
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Section: Confidence Level
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                text = "Yapay Zeka Çeviri Güven Skoru",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = "Metin netliği ve OCR eşleştirme kalitesi",
                                                fontSize = 10.sp,
                                                color = Color.Gray
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = activePage.confidenceScore,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Black,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }

                            // Section: AI Summarizer On-Demand (Refinement: dynamic summary)
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Summarize,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.secondary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "AI Sayfa Özeti",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                )
                                            }

                                            Button(
                                                onClick = { viewModel.generateSummaryForPage(activePage.translatedText) },
                                                enabled = !isSummarizing,
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.secondary
                                                ),
                                                contentPadding = ButtonDefaults.ContentPadding
                                            ) {
                                                if (isSummarizing) {
                                                    CircularProgressIndicator(modifier = Modifier.size(12.dp), color = Color.White, strokeWidth = 2.dp)
                                                } else {
                                                    Text("Özet Oluştur", fontSize = 10.sp)
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        pageSummary?.let { summary ->
                                            Text(
                                                text = summary,
                                                fontSize = 12.sp,
                                                lineHeight = 18.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier
                                                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                                    .padding(12.dp)
                                                    .fillMaxWidth()
                                            )
                                        } ?: Text(
                                            text = "Bu sayfanın özetini çıkartmak için 'Özet Oluştur' butonuna basın.",
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }

                            // Section: Key Vocabulary (Refinement: interactive flashcards)
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Speed,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Etkileşimli Sözlük & Terimler",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        val parsedVocab = remember(activePage.keyVocabulary) {
                                            val list = mutableListOf<Pair<String, String>>()
                                            val rawText = activePage.keyVocabulary
                                            if (rawText.isNotBlank()) {
                                                val lines = rawText.lines()
                                                for (line in lines) {
                                                    val cleanLine = line.trim().removePrefix("[").removeSuffix("]").trim()
                                                    if (cleanLine.isNotBlank()) {
                                                        val parts = if (cleanLine.contains("=")) {
                                                            cleanLine.split("=", limit = 2)
                                                        } else if (cleanLine.contains(":")) {
                                                            cleanLine.split(":", limit = 2)
                                                        } else {
                                                            null
                                                        }
                                                        if (parts != null && parts.size == 2) {
                                                            list.add(parts[0].trim() to parts[1].trim())
                                                        } else {
                                                            list.add(cleanLine to "")
                                                        }
                                                    }
                                                }
                                            }
                                            list
                                        }

                                        if (parsedVocab.isNotEmpty()) {
                                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                parsedVocab.forEach { (term, definition) ->
                                                    Card(
                                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                                        shape = RoundedCornerShape(8.dp),
                                                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Column(modifier = Modifier.padding(12.dp)) {
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Text(
                                                                    text = term,
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontSize = 13.sp,
                                                                    color = MaterialTheme.colorScheme.primary,
                                                                    modifier = Modifier.weight(1f)
                                                                )
                                                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                                    IconButton(
                                                                        onClick = { viewModel.speakText(term, state.historyItem.targetLang) },
                                                                        modifier = Modifier.size(24.dp)
                                                                    ) {
                                                                        Icon(
                                                                            imageVector = Icons.Default.VolumeUp,
                                                                            contentDescription = "Seslendir",
                                                                            modifier = Modifier.size(16.dp),
                                                                            tint = MaterialTheme.colorScheme.secondary
                                                                        )
                                                                    }
                                                                    IconButton(
                                                                        onClick = {
                                                                            clipboardManager.setText(AnnotatedString("$term = $definition"))
                                                                            Toast.makeText(context, "Kopyalandı!", Toast.LENGTH_SHORT).show()
                                                                        },
                                                                        modifier = Modifier.size(24.dp)
                                                                    ) {
                                                                        Icon(
                                                                            imageVector = Icons.Default.ContentCopy,
                                                                            contentDescription = "Kopyala",
                                                                            modifier = Modifier.size(14.dp),
                                                                            tint = Color.Gray
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                            if (definition.isNotEmpty()) {
                                                                Spacer(modifier = Modifier.height(4.dp))
                                                                Text(
                                                                    text = definition,
                                                                    fontSize = 11.sp,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                    lineHeight = 16.sp
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            Text(
                                                text = "Bu sayfada çıkartılmış özel terim bulunamadı.",
                                                fontSize = 11.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                }
                            }

                            // Section: Interactive Page AI Chat (PDF Copilot)
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.QuestionAnswer,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Yapay Zeka Sayfa Asistanı (Soru Sor)",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        val chatMessages by viewModel.pageChatMessages.collectAsState()
                                        val isChatLoading by viewModel.isChatLoading.collectAsState()
                                        var chatInputText by remember { mutableStateOf("") }

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 240.dp)
                                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                                .padding(8.dp)
                                        ) {
                                            if (chatMessages.isEmpty()) {
                                                Column(
                                                    modifier = Modifier.fillMaxSize().padding(12.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.QuestionAnswer,
                                                        contentDescription = null,
                                                        tint = Color.Gray.copy(alpha = 0.5f),
                                                        modifier = Modifier.size(32.dp)
                                                    )
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Text(
                                                        text = "Çevrilen bu sayfa ile ilgili asistanla sohbet edin. Örn: 'Bu sayfadaki ana fikir nedir?' veya 'Zor terimleri bana açıklar mısın?'",
                                                        fontSize = 11.sp,
                                                        color = Color.Gray,
                                                        textAlign = TextAlign.Center,
                                                        lineHeight = 15.sp
                                                    )
                                                }
                                            } else {
                                                LazyColumn(
                                                    modifier = Modifier.fillMaxSize(),
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    items(chatMessages.size) { index ->
                                                        val (role, text) = chatMessages[index]
                                                        val isUser = role == "user"
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .background(
                                                                        if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                                        shape = RoundedCornerShape(
                                                                            topStart = 12.dp,
                                                                            topEnd = 12.dp,
                                                                            bottomStart = if (isUser) 12.dp else 0.dp,
                                                                            bottomEnd = if (isUser) 0.dp else 12.dp
                                                                        )
                                                                    )
                                                                    .padding(8.dp)
                                                                    .widthIn(max = 200.dp)
                                                            ) {
                                                                Text(
                                                                    text = text,
                                                                    fontSize = 11.sp,
                                                                    color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                                    lineHeight = 15.sp
                                                                )
                                                            }
                                                        }
                                                    }
                                                    if (isChatLoading) {
                                                        item {
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.Start
                                                            ) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp))
                                                                        .padding(8.dp)
                                                                ) {
                                                                    CircularProgressIndicator(
                                                                        modifier = Modifier.size(12.dp),
                                                                        strokeWidth = 2.dp,
                                                                        color = MaterialTheme.colorScheme.primary
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedTextField(
                                                value = chatInputText,
                                                onValueChange = { chatInputText = it },
                                                placeholder = { Text("Asistana sor...", fontSize = 11.sp) },
                                                modifier = Modifier.weight(1f),
                                                textStyle = MaterialTheme.typography.bodyMedium,
                                                singleLine = true,
                                                shape = RoundedCornerShape(8.dp),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                                )
                                            )

                                            IconButton(
                                                onClick = {
                                                    if (chatInputText.isNotBlank()) {
                                                        viewModel.sendPageChatMessage(activePage.translatedText, chatInputText)
                                                        chatInputText = ""
                                                    }
                                                },
                                                enabled = !isChatLoading && chatInputText.isNotBlank(),
                                                modifier = Modifier
                                                    .background(
                                                        if (chatInputText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .size(40.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Send,
                                                    contentDescription = "Gönder",
                                                    tint = if (chatInputText.isNotBlank()) MaterialTheme.colorScheme.onPrimary else Color.Gray,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Section: Page Specific Notes Notebook (Refinement: dynamic local notebook saves in ROOM)
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.EditNote,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Sayfa Notlarım",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                )
                                            }

                                            Button(
                                                onClick = {
                                                    viewModel.savePageNotes(activePage.id, pageNotesText)
                                                    Toast.makeText(context, "Notlar başarıyla kaydedildi!", Toast.LENGTH_SHORT).show()
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                            ) {
                                                Text("Kaydet", fontSize = 10.sp)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        OutlinedTextField(
                                            value = pageNotesText,
                                            onValueChange = { pageNotesText = it },
                                            placeholder = { Text("Buraya sayfa ile ilgili notlarınızı ekleyebilirsiniz...", fontSize = 12.sp) },
                                            modifier = Modifier.fillMaxWidth(),
                                            textStyle = MaterialTheme.typography.bodyMedium,
                                            maxLines = 4,
                                            shape = RoundedCornerShape(8.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Text("Sayfa yüklenemiyor.", modifier = Modifier.align(Alignment.Center))
            }
        }

        // Bottom Navigation Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { 
                    if (currentPageIndex > 0) {
                        viewModel.clearPageSummary()
                        viewModel.clearPageChat()
                        currentPageIndex--
                    }
                },
                enabled = currentPageIndex > 0,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Icon(imageVector = Icons.AutoMirrored.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Önceki", fontSize = 12.sp)
            }

            Text(
                text = "${currentPageIndex + 1} / $totalPages",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            Button(
                onClick = { 
                    if (currentPageIndex < totalPages - 1) {
                        viewModel.clearPageSummary()
                        viewModel.clearPageChat()
                        currentPageIndex++
                    }
                },
                enabled = currentPageIndex < totalPages - 1,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Sonraki", fontSize = 12.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(imageVector = Icons.AutoMirrored.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun HelpGuideDialog(onDismiss: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Anladım")
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Kullanım & Çeviri Kılavuzu")
            }
        },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                item {
                    Text(
                        text = "Google PDF Çevirici, Google Çeviri altyapısını kullanarak PDF sayfalarınızın metinlerini sayfa sayfa hızlı ve güvenilir biçimde çevirir.",
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = "Önemli Özellikler:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "• Hızlı Google Çeviri: Belgeler sayfa sayfa anında çevrilir.", fontSize = 11.sp)
                    Text(text = "• Seslendirme (TTS): Çevrilen metni sesli okutma imkanı.", fontSize = 11.sp)
                    Text(text = "• Sayfa Özeti Çıkarma: Çevrilen sayfaların özetini görüntüleme.", fontSize = 11.sp)
                    Text(text = "• Sayfa Not Defteri: Sayfa bazlı özel çalışma notlarınızı kaydetme.", fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Not: Herhangi bir giriş veya API anahtarı gerekmez. Keyifli çeviriler dileriz!",
                        fontSize = 10.sp,
                        color = Color.Gray,
                        lineHeight = 14.sp
                    )
                }
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

/**
 * Clean simple custom border stroke modifier
 */
@Composable
fun borderStroke() = Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))

/**
 * Extract filename from document URI
 */
private fun getFileName(context: Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    result = cursor.getString(index)
                }
            }
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/')
        if (cut != null && cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result
}

@Composable
fun AuthScreen(viewModel: TranslationViewModel) {
    // Auth screen removed - app opens directly
}

@Composable
fun ProfileDialog(viewModel: TranslationViewModel, onDismiss: () -> Unit) {
    // Profile dialog removed
}
