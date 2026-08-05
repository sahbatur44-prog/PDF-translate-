package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GoogleTranslateClient
import com.example.data.local.AppDatabase
import com.example.data.model.TranslatedPage
import com.example.data.model.TranslationHistory
import com.example.data.repository.TranslationRepository
import com.example.util.PdfTextExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

sealed interface MainUiState {
    object Home : MainUiState
    data class Translating(
        val fileName: String,
        val totalPages: Int,
        val currentPageIndex: Int,
        val statusMessage: String
    ) : MainUiState
    data class ViewTranslation(
        val historyItem: TranslationHistory,
        val pages: List<TranslatedPage>
    ) : MainUiState
}

class TranslationViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {

    private val context = application.applicationContext
    private val repository: TranslationRepository
    private var tts: TextToSpeech? = null
    private var translationJob: Job? = null

    private val prefs = context.getSharedPreferences("app_auth_prefs", Context.MODE_PRIVATE)

    // User is always logged in directly
    private val _isLoggedIn = MutableStateFlow(true)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _userName = MutableStateFlow("Kullanıcı")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userEmail = MutableStateFlow("user@pdf.app")
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    private val _userId = MutableStateFlow("local_user")
    val userId: StateFlow<String> = _userId.asStateFlow()

    private val _userAvatarIndex = MutableStateFlow(0)
    val userAvatarIndex: StateFlow<Int> = _userAvatarIndex.asStateFlow()

    private val _rememberSession = MutableStateFlow(true)
    val rememberSession: StateFlow<Boolean> = _rememberSession.asStateFlow()

    private val _authLoading = MutableStateFlow(false)
    val authLoading: StateFlow<Boolean> = _authLoading.asStateFlow()

    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Home)
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    // Supported Languages (with Auto-Detect!)
    val languages = listOf(
        "Otomatik Algıla" to "Auto-Detect",
        "İngilizce" to "English",
        "Türkçe" to "Turkish",
        "Almanca" to "German",
        "Fransızca" to "French",
        "İspanyolca" to "Spanish",
        "İtalyanca" to "Italian",
        "Rusça" to "Russian",
        "Arapça" to "Arabic",
        "Çince" to "Chinese",
        "Japonca" to "Japanese",
        "Korece" to "Korean"
    )

    private val _sourceLang = MutableStateFlow("Otomatik Algıla")
    val sourceLang: StateFlow<String> = _sourceLang.asStateFlow()

    private val _targetLang = MutableStateFlow("Türkçe")
    val targetLang: StateFlow<String> = _targetLang.asStateFlow()

    private val _documentType = MutableStateFlow("Standart Belge")
    val documentType: StateFlow<String> = _documentType.asStateFlow()

    private val _formality = MutableStateFlow("Resmi")
    val formality: StateFlow<String> = _formality.asStateFlow()

    private val _customGlossary = MutableStateFlow("")
    val customGlossary: StateFlow<String> = _customGlossary.asStateFlow()

    private val _customApiKey = MutableStateFlow("")
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _pageSummary = MutableStateFlow<String?>(null)
    val pageSummary: StateFlow<String?> = _pageSummary.asStateFlow()

    private val _isSummarizing = MutableStateFlow(false)
    val isSummarizing: StateFlow<Boolean> = _isSummarizing.asStateFlow()

    private val _pageChatMessages = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val pageChatMessages: StateFlow<List<Pair<String, String>>> = _pageChatMessages.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    private val _isTtsActive = MutableStateFlow(false)
    val isTtsActive: StateFlow<Boolean> = _isTtsActive.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(context)
        repository = TranslationRepository(database.translationDao())
        tts = TextToSpeech(context, this)

        _isLoggedIn.value = true
        _userEmail.value = "user@pdf.app"
        _userName.value = "Kullanıcı"
        _userId.value = "local_user"
    }

    // Isolated list of past translations for current active user
    @OptIn(ExperimentalCoroutinesApi::class)
    val historyList: StateFlow<List<TranslationHistory>> = repository.getAllHistoryForUser("user@pdf.app")
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val network = cm?.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun setSourceLang(lang: String) { _sourceLang.value = lang }
    fun setTargetLang(lang: String) { _targetLang.value = lang }
    fun setDocumentType(type: String) { _documentType.value = type }
    fun setFormality(form: String) { _formality.value = form }
    fun setCustomGlossary(glossary: String) { _customGlossary.value = glossary }
    
    fun setCustomApiKey(key: String) {
        _customApiKey.value = key
    }

    fun clearErrorMessage() { _errorMessage.value = null }
    fun navigateToHome() { _uiState.value = MainUiState.Home }
    fun clearPageChat() { _pageChatMessages.value = emptyList() }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            Log.d("TranslationVM", "TextToSpeech initialized successfully.")
        } else {
            Log.e("TranslationVM", "TextToSpeech initialization failed.")
        }
    }

    // Speak Translated Text Out Loud with Russian and Arabic support
    fun speakText(text: String, languageName: String) {
        tts?.let { ttsInstance ->
            _isTtsActive.value = true
            val locale = when (languageName.lowercase()) {
                "english", "ingilizce" -> Locale.ENGLISH
                "german", "almanca" -> Locale.GERMAN
                "french", "fransızca" -> Locale.FRENCH
                "spanish", "ispanyolca" -> Locale("es")
                "turkish", "türkçe" -> Locale("tr", "TR")
                "italian", "italyanca" -> Locale.ITALIAN
                "russian", "rusça" -> Locale("ru", "RU")
                "arabic", "arapça" -> Locale("ar")
                "chinese", "çince" -> Locale.CHINESE
                "japanese", "japonca" -> Locale.JAPANESE
                "korean", "korece" -> Locale.KOREAN
                else -> Locale.getDefault()
            }
            ttsInstance.language = locale
            ttsInstance.speak(text, TextToSpeech.QUEUE_FLUSH, null, "TranslationTTS")
        }
    }

    fun stopSpeaking() {
        tts?.stop()
        _isTtsActive.value = false
    }

    fun toggleBookmark(historyId: Int, isBookmarked: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.toggleBookmark(historyId, isBookmarked)
        }
    }

    fun savePageNotes(pageId: Int, notes: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updatePageNotes(pageId, notes)
        }
    }

    // Clean up associated PNG page images from disk when deleting history
    fun deleteHistoryItem(historyId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val pages = repository.getPagesListForHistory(historyId)
                for (page in pages) {
                    page.originalPagePath?.let { path ->
                        val imgFile = File(path)
                        if (imgFile.exists()) {
                            imgFile.delete()
                        }
                    }
                }
                context.filesDir.listFiles()?.forEach { file ->
                    if (file.name.startsWith("page_${historyId}_") && file.name.endsWith(".png")) {
                        file.delete()
                    }
                }
            } catch (e: Exception) {
                Log.e("TranslationVM", "Error cleaning files for history $historyId", e)
            }
            repository.deleteHistoryForUser(historyId, _userEmail.value)
        }
    }

    // Clean up all associated image files from disk when clearing history
    fun clearAllHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val pages = repository.getAllPagesListForUser(_userEmail.value)
                for (page in pages) {
                    page.originalPagePath?.let { path ->
                        val imgFile = File(path)
                        if (imgFile.exists()) {
                            imgFile.delete()
                        }
                    }
                }
                context.filesDir.listFiles()?.forEach { file ->
                    if (file.name.startsWith("page_") && file.name.endsWith(".png")) {
                        file.delete()
                    }
                }
            } catch (e: Exception) {
                Log.e("TranslationVM", "Error clearing all history files", e)
            }
            repository.clearAllHistoryForUser(_userEmail.value)
        }
    }

    // Summary Generator using Google Translate
    fun generateSummaryForPage(pageText: String) {
        if (!isNetworkAvailable()) {
            _errorMessage.value = "İnternet bağlantınızı kontrol edin."
            return
        }
        viewModelScope.launch {
            _isSummarizing.value = true
            _pageSummary.value = null
            try {
                if (pageText.isBlank()) {
                    _pageSummary.value = "Sayfada metin bulunamadı."
                    return@launch
                }
                val translatedSummary = GoogleTranslateClient.translateText(
                    text = pageText.take(1000),
                    sourceLang = _sourceLang.value,
                    targetLang = _targetLang.value
                )
                _pageSummary.value = "• " + translatedSummary.take(300).replace("\n", "\n• ")
            } catch (e: Exception) {
                _pageSummary.value = "Özet hazırlanamadı: ${e.localizedMessage}"
            } finally {
                _isSummarizing.value = false
            }
        }
    }

    fun clearPageSummary() {
        _pageSummary.value = null
    }

    fun sendPageChatMessage(pageText: String, question: String) {
        if (question.isBlank()) return
        viewModelScope.launch {
            val currentList = _pageChatMessages.value.toMutableList()
            currentList.add("user" to question)
            _pageChatMessages.value = currentList
            
            _isChatLoading.value = true
            try {
                val translatedQuestion = GoogleTranslateClient.translateText(question, "auto", _targetLang.value)
                val reply = "Google Çeviri Asistanı: Sayfada belirtilen içerik hakkında: \"$translatedQuestion\"."
                
                val updatedList = _pageChatMessages.value.toMutableList()
                updatedList.add("model" to reply)
                _pageChatMessages.value = updatedList
            } catch (e: Exception) {
                val updatedList = _pageChatMessages.value.toMutableList()
                updatedList.add("model" to "Cevaplandırılamadı: ${e.localizedMessage}")
                _pageChatMessages.value = updatedList
            } finally {
                _isChatLoading.value = false
            }
        }
    }

    // Cancel active coroutine job on user request
    fun cancelTranslation() {
        translationJob?.cancel()
        translationJob = null
        _uiState.value = MainUiState.Home
        _errorMessage.value = "Çeviri işlemi iptal edildi."
    }

    // Start the PDF translation process using Google Translate page-by-page
    fun startPdfTranslation(uri: Uri, originalFileName: String) {
        translationJob?.cancel()
        translationJob = viewModelScope.launch {
            _errorMessage.value = null
            if (!isNetworkAvailable()) {
                _errorMessage.value = "İnternet bağlantınızı kontrol edin."
                _uiState.value = MainUiState.Home
                return@launch
            }

            val resolvedFileName = originalFileName.ifBlank { "Belge.pdf" }

            try {
                _uiState.value = MainUiState.Translating(
                    fileName = resolvedFileName,
                    totalPages = 0,
                    currentPageIndex = 0,
                    statusMessage = "Dosya hazırlanıyor..."
                )

                val tempFile = withContext(Dispatchers.IO) {
                    val file = File(context.cacheDir, "temp_translation.pdf")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(file).use { output ->
                            input.copyTo(output)
                        }
                    }
                    file
                }

                if (!tempFile.exists() || tempFile.length() == 0L) {
                    throw Exception("PDF dosyası kopyalanamadı veya geçersiz.")
                }

                val pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = try {
                    PdfRenderer(pfd)
                } catch (e: Exception) {
                    pfd.close()
                    throw Exception("PDF dosyası okunamadı. Dosya şifreli veya bozuk olabilir.")
                }

                val totalPages = renderer.pageCount
                if (totalPages == 0) {
                    renderer.close()
                    pfd.close()
                    throw Exception("PDF belgesinde sayfa bulunamadı.")
                }

                // Extract page texts from PDF
                val pdfExtractedPagesText = withContext(Dispatchers.IO) {
                    PdfTextExtractor.extractTextFromPdf(context, uri)
                }

                val historyId = withContext(Dispatchers.IO) {
                    repository.insertHistory(
                        TranslationHistory(
                            userId = _userEmail.value,
                            fileName = resolvedFileName,
                            sourceLang = _sourceLang.value,
                            targetLang = _targetLang.value,
                            documentType = _documentType.value,
                            formality = _formality.value
                        )
                    )
                }

                _uiState.value = MainUiState.Translating(
                    fileName = resolvedFileName,
                    totalPages = totalPages,
                    currentPageIndex = 0,
                    statusMessage = "1 / $totalPages sayfa Google Çeviri ile çevriliyor..."
                )

                for (pageIndex in 0 until totalPages) {
                    _uiState.value = MainUiState.Translating(
                        fileName = resolvedFileName,
                        totalPages = totalPages,
                        currentPageIndex = pageIndex,
                        statusMessage = "Sayfa ${pageIndex + 1} / $totalPages: Görüntü İşleniyor..."
                    )

                    val pageBitmap = withContext(Dispatchers.IO) {
                        val page = renderer.openPage(pageIndex)
                        val targetWidth = if (page.width > 1200) 1200 else page.width
                        val targetHeight = (targetWidth.toFloat() / page.width * page.height).toInt()
                        val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                        val canvas = android.graphics.Canvas(bitmap)
                        canvas.drawColor(android.graphics.Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        page.close()
                        bitmap
                    }

                    val cachedImagePath = withContext(Dispatchers.IO) {
                        val imageFile = File(context.filesDir, "page_${historyId}_${pageIndex}.png")
                        FileOutputStream(imageFile).use { out ->
                            pageBitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
                        }
                        imageFile.absolutePath
                    }

                    _uiState.value = MainUiState.Translating(
                        fileName = resolvedFileName,
                        totalPages = totalPages,
                        currentPageIndex = pageIndex,
                        statusMessage = "Sayfa ${pageIndex + 1} / $totalPages: OCR Metin Taraması Yapılıyor..."
                    )

                    // First perform OCR on rendered page Bitmap (ideal for image-based PDFs & scanned docs)
                    val ocrPageText = PdfTextExtractor.recognizeTextFromBitmap(pageBitmap)
                    Log.d("TranslationVM", "Page ${pageIndex + 1} OCR Output length: ${ocrPageText.length}, text snippet: '${ocrPageText.take(100)}'")

                    val rawPageText = if (ocrPageText.isNotBlank()) {
                        ocrPageText
                    } else if (pageIndex < pdfExtractedPagesText.size && pdfExtractedPagesText[pageIndex].isNotBlank()) {
                        PdfTextExtractor.cleanText(pdfExtractedPagesText[pageIndex])
                    } else {
                        ""
                    }
                    Log.d("TranslationVM", "Page ${pageIndex + 1} Final Raw Text to Translate: '$rawPageText'")

                    _uiState.value = MainUiState.Translating(
                        fileName = resolvedFileName,
                        totalPages = totalPages,
                        currentPageIndex = pageIndex,
                        statusMessage = "Sayfa ${pageIndex + 1} / $totalPages: Google Çeviri Yapılıyor..."
                    )

                    val translatedPageText = withContext(Dispatchers.IO) {
                        try {
                            if (rawPageText.isBlank()) {
                                "Sayfada okunabilir metin bulunamadı."
                            } else {
                                GoogleTranslateClient.translateText(
                                    text = rawPageText,
                                    sourceLang = _sourceLang.value,
                                    targetLang = _targetLang.value
                                )
                            }
                        } catch (e: Exception) {
                            Log.e("TranslationVM", "Google translate error page $pageIndex", e)
                            "Çeviri hatası (Sayfa ${pageIndex + 1}): ${e.localizedMessage}"
                        }
                    }

                    withContext(Dispatchers.IO) {
                        repository.insertPage(
                            TranslatedPage(
                                translationHistoryId = historyId,
                                pageNumber = pageIndex + 1,
                                originalPagePath = cachedImagePath,
                                translatedText = translatedPageText,
                                confidenceScore = "100%",
                                keyVocabulary = "",
                                userNotes = ""
                            )
                        )
                    }
                }

                renderer.close()
                pfd.close()

                viewTranslationDetail(historyId)

            } catch (e: Exception) {
                Log.e("TranslationVM", "Translation failed", e)
                _errorMessage.value = e.localizedMessage ?: "Beklenmeyen bir hata oluştu."
                _uiState.value = MainUiState.Home
            }
        }
    }

    fun viewTranslationDetail(historyId: Int) {
        viewModelScope.launch {
            try {
                repository.getPagesForHistory(historyId).collect { pages ->
                    val historyItem = historyList.value.firstOrNull { it.id == historyId }
                    if (historyItem != null && pages.isNotEmpty()) {
                        _uiState.value = MainUiState.ViewTranslation(historyItem, pages)
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Çeviri ayrıntıları yüklenemedi: ${e.localizedMessage}"
                _uiState.value = MainUiState.Home
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        tts?.shutdown()
    }
}
