package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.speech.tts.TextToSpeech
import android.util.Base64
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiClient
import com.example.api.GeminiContent
import com.example.api.GeminiInlineData
import com.example.api.GeminiPart
import com.example.api.GeminiRequest
import com.example.data.local.AppDatabase
import com.example.data.model.TranslatedPage
import com.example.data.model.TranslationHistory
import com.example.data.repository.TranslationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
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

    init {
        val database = AppDatabase.getDatabase(context)
        repository = TranslationRepository(database.translationDao())
        tts = TextToSpeech(context, this)
    }

    // List of past translations
    val historyList: StateFlow<List<TranslationHistory>> = repository.allHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

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

    // 40+ Refinements: Translation settings
    private val _documentType = MutableStateFlow("Standart Belge") // "Standart Belge", "Çizgi Roman / Manga", "Akademik Makale", "Teknik Kılavuz"
    val documentType: StateFlow<String> = _documentType.asStateFlow()

    private val _formality = MutableStateFlow("Resmi") // "Resmi", "Samimi"
    val formality: StateFlow<String> = _formality.asStateFlow()

    private val _customGlossary = MutableStateFlow("") // e.g. "Term1=Translation1\nTerm2=Translation2"
    val customGlossary: StateFlow<String> = _customGlossary.asStateFlow()

    private val _customApiKey = MutableStateFlow("") // Let users set custom keys if they want
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Auto summary states
    private val _pageSummary = MutableStateFlow<String?>(null)
    val pageSummary: StateFlow<String?> = _pageSummary.asStateFlow()

    private val _isSummarizing = MutableStateFlow(false)
    val isSummarizing: StateFlow<Boolean> = _isSummarizing.asStateFlow()

    // Page Interactive Chat States
    private val _pageChatMessages = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val pageChatMessages: StateFlow<List<Pair<String, String>>> = _pageChatMessages.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    fun clearPageChat() {
        _pageChatMessages.value = emptyList()
    }

    // TTS playing states
    private val _isTtsActive = MutableStateFlow(false)
    val isTtsActive: StateFlow<Boolean> = _isTtsActive.asStateFlow()

    // Persistent User Authentication State (Google Login / Account Persistent Session)
    private val prefs = context.getSharedPreferences("app_auth_prefs", Context.MODE_PRIVATE)

    private val _isLoggedIn = MutableStateFlow(prefs.getBoolean("is_logged_in", false))
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _userName = MutableStateFlow(prefs.getString("user_name", "") ?: "")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userEmail = MutableStateFlow(prefs.getString("user_email", "") ?: "")
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    private val _userAvatarIndex = MutableStateFlow(prefs.getInt("user_avatar_index", 0))
    val userAvatarIndex: StateFlow<Int> = _userAvatarIndex.asStateFlow()

    private val _authLoading = MutableStateFlow(false)
    val authLoading: StateFlow<Boolean> = _authLoading.asStateFlow()

    fun performGoogleSignIn(email: String, name: String) {
        viewModelScope.launch {
            _authLoading.value = true
            // Simulate networking delay for premium look and feel
            kotlinx.coroutines.delay(1000)
            prefs.edit().apply {
                putBoolean("is_logged_in", true)
                putString("user_name", name)
                putString("user_email", email)
                putInt("user_avatar_index", (1..6).random())
                apply()
            }
            _isLoggedIn.value = true
            _userName.value = name
            _userEmail.value = email
            _userAvatarIndex.value = prefs.getInt("user_avatar_index", 0)
            _authLoading.value = false
        }
    }

    fun performSignOut() {
        prefs.edit().clear().apply()
        _isLoggedIn.value = false
        _userName.value = ""
        _userEmail.value = ""
        _userAvatarIndex.value = 0
    }

    fun setSourceLang(lang: String) {
        _sourceLang.value = lang
    }

    fun setTargetLang(lang: String) {
        _targetLang.value = lang
    }

    fun setDocumentType(type: String) {
        _documentType.value = type
    }

    fun setFormality(form: String) {
        _formality.value = form
    }

    fun setCustomGlossary(glossary: String) {
        _customGlossary.value = glossary
    }

    fun setCustomApiKey(key: String) {
        _customApiKey.value = key
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun navigateToHome() {
        _uiState.value = MainUiState.Home
    }

    // TTS Init Listener
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            Log.d("TranslationVM", "TextToSpeech initialized successfully.")
        } else {
            Log.e("TranslationVM", "TextToSpeech initialization failed.")
        }
    }

    // Speak Translated Text Out Loud
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

    // Room DB Interactions
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

    fun deleteHistoryItem(historyId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteHistory(historyId)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllHistory()
        }
    }

    // Auto Summary Generator using Gemini API
    fun generateSummaryForPage(pageText: String) {
        viewModelScope.launch {
            _isSummarizing.value = true
            _pageSummary.value = null
            try {
                val apiKey = _customApiKey.value.ifBlank { GeminiClient.getApiKey() }
                val targetLangName = languages.firstOrNull { it.first == _targetLang.value }?.second ?: _targetLang.value
                val prompt = """
                    You are an expert executive summarizer.
                    Provide a concise summary of the following translated page text in exactly 3 bullet points.
                    - Synthesize key facts, arguments, and takeaways.
                    - Translate or write the summary in $targetLangName.
                    - Provide ONLY the 3 bullet points, no commentary or conversational introduction.
                    
                    Text:
                    $pageText
                """.trimIndent()

                val request = GeminiRequest(
                    contents = listOf(
                        GeminiContent(parts = listOf(GeminiPart(text = prompt)))
                    )
                )
                val response = withContext(Dispatchers.IO) {
                    GeminiClient.apiService.generateContent(apiKey, request)
                }
                _pageSummary.value = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: "Özet oluşturulamadı."
            } catch (e: Exception) {
                _pageSummary.value = "Hata oluştu: ${e.localizedMessage}"
            } finally {
                _isSummarizing.value = false
            }
        }
    }

    fun clearPageSummary() {
        _pageSummary.value = null
    }

    /**
     * Send message to the Page-specific AI Chat Assistant
     */
    fun sendPageChatMessage(pageText: String, question: String) {
        if (question.isBlank()) return
        viewModelScope.launch {
            val currentList = _pageChatMessages.value.toMutableList()
            currentList.add("user" to question)
            _pageChatMessages.value = currentList
            
            _isChatLoading.value = true
            try {
                val apiKey = _customApiKey.value.ifBlank { GeminiClient.getApiKey() }
                
                val historyContext = buildString {
                    appendLine("You are an expert AI Document Assistant inside the 'PDF Translator AI' app.")
                    appendLine("The user is reading and discussing a translated page from a PDF document.")
                    appendLine("Here is the complete translated text content of the currently viewed page:")
                    appendLine("---START PAGE TEXT---")
                    appendLine(pageText)
                    appendLine("---END PAGE TEXT---")
                    appendLine()
                    appendLine("Conversation context history:")
                    for (i in 0 until currentList.size - 1) {
                        val turn = currentList[i]
                        val speaker = if (turn.first == "user") "Kullanıcı" else "Asistan"
                        appendLine("$speaker: ${turn.second}")
                    }
                    appendLine()
                    appendLine("Answer the user's latest question directly and clearly in their language (default Turkish) based on the page text context.")
                    appendLine("Kullanıcı Sorusu: $question")
                }

                val request = GeminiRequest(
                    contents = listOf(
                        GeminiContent(parts = listOf(GeminiPart(text = historyContext)))
                    )
                )
                val response = withContext(Dispatchers.IO) {
                    GeminiClient.apiService.generateContent(apiKey, request)
                }
                val reply = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: "Cevap üretilemedi."
                
                val updatedList = _pageChatMessages.value.toMutableList()
                updatedList.add("model" to reply)
                _pageChatMessages.value = updatedList
            } catch (e: Exception) {
                val updatedList = _pageChatMessages.value.toMutableList()
                updatedList.add("model" to "Hata oluştu: ${e.localizedMessage}")
                _pageChatMessages.value = updatedList
            } finally {
                _isChatLoading.value = false
            }
        }
    }

    /**
     * Start the PDF translation process
     */
    fun startPdfTranslation(uri: Uri, originalFileName: String) {
        viewModelScope.launch {
            _errorMessage.value = null
            val resolvedFileName = originalFileName.ifBlank { "Belge.pdf" }

            try {
                // Step 1: Copy PDF to private cache file
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

                // Step 2: Open PdfRenderer to determine total pages
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

                // Step 3: Insert Translation History Item
                val historyId = withContext(Dispatchers.IO) {
                    repository.insertHistory(
                        TranslationHistory(
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
                    statusMessage = "1 / $totalPages sayfa çevriliyor..."
                )

                // Step 4: Loop and translate page by page
                for (pageIndex in 0 until totalPages) {
                    _uiState.value = MainUiState.Translating(
                        fileName = resolvedFileName,
                        totalPages = totalPages,
                        currentPageIndex = pageIndex,
                        statusMessage = "Sayfa ${pageIndex + 1} / $totalPages: Görüntü Analiz Ediliyor..."
                    )

                    // Render page to high-quality bitmap
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

                    // Save original rendered page bitmap to private storage for offline viewing
                    val cachedImagePath = withContext(Dispatchers.IO) {
                        val imageFile = File(context.filesDir, "page_${historyId}_${pageIndex}.png")
                        FileOutputStream(imageFile).use { out ->
                            pageBitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
                        }
                        imageFile.absolutePath
                    }

                    // Convert to base64
                    val base64Image = withContext(Dispatchers.IO) {
                        val outStream = ByteArrayOutputStream()
                        pageBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outStream)
                        Base64.encodeToString(outStream.toByteArray(), Base64.NO_WRAP)
                    }

                    _uiState.value = MainUiState.Translating(
                        fileName = resolvedFileName,
                        totalPages = totalPages,
                        currentPageIndex = pageIndex,
                        statusMessage = "Sayfa ${pageIndex + 1} / $totalPages: Yapay Zeka Çevirisi..."
                    )

                    // Build advanced custom prompt instructions
                    val targetLangName = languages.firstOrNull { it.first == _targetLang.value }?.second ?: _targetLang.value
                    val srcLangName = languages.firstOrNull { it.first == _sourceLang.value }?.second ?: "Auto-Detect"

                    val prompt = buildString {
                        appendLine("You are an expert native translator and document OCR engineer.")
                        appendLine("Translate all content from this document image page into $targetLangName.")
                        if (srcLangName != "Auto-Detect") {
                            appendLine("- Source Language is: $srcLangName.")
                        } else {
                            appendLine("- Source Language is unknown. Automatically detect it and translate to $targetLangName.")
                        }

                        // Style Formality
                        if (_formality.value == "Resmi") {
                            appendLine("- Formality setting: Formal / Polite tone (e.g. using 'Siz' in Turkish, or highly professional phrasing).")
                        } else {
                            appendLine("- Formality setting: Casual / Friendly / Informal tone (e.g. using 'Sen' in Turkish).")
                        }

                        // Document Type Custom Guidelines
                        when (_documentType.value) {
                            "Çizgi Roman / Manga" -> {
                                appendLine("- DOCUMENT TYPE: COMIC / MANGA / SPEECH BUBBLES.")
                                appendLine("- Identify all speech bubbles, panel sequences, side captions, handwritten comments, and sound effects (SFX) in chronological and visual order.")
                                appendLine("- Translate every dialogue box. Keep translations fitting for conversational cartoon bubbles.")
                                appendLine("- Format your translation mapping each speaker sequential like: '[Balon 1] (Upper Left): Translated Text', '[Balon 2]: Translated Text', or '[Panel 1 - Karakter A]: Translated Text'.")
                                appendLine("- If SFX are present, choose appropriate target equivalents inside asterisks (e.g., *GÜM*, *ŞAP*, *ÇAT*).")
                            }
                            "Akademik Makale" -> {
                                appendLine("- DOCUMENT TYPE: ACADEMIC PAPER / RESEARCH.")
                                appendLine("- Scientifically and technically precise vocabulary is required.")
                                appendLine("- Identify and process multi-column paper structures (Left column then Right column) in the correct logical flow.")
                                appendLine("- Translate headers, footnotes, table content, and formula annotations carefully.")
                            }
                            "Teknik Kılavuz" -> {
                                appendLine("- DOCUMENT TYPE: TECHNICAL MANUAL / REPAIR GUIDE.")
                                appendLine("- Translate step-by-step assembly instructions, specifications, safety alerts (WARNING, CAUTION, NOTE) with absolute technical clarity.")
                                appendLine("- Keep active instructive verbs clear and uniform.")
                            }
                            else -> {
                                appendLine("- DOCUMENT TYPE: STANDARD DOCUMENT / BOOK.")
                                appendLine("- Keep paragraph layouts, headings, lists, table elements, and page footers precisely aligned.")
                            }
                        }

                        // Enforce Glossary
                        if (_customGlossary.value.isNotBlank()) {
                            appendLine("- STICK TO THE USER GLOSSARY / DICTIONARY TO ENFORCE:")
                            appendLine(_customGlossary.value)
                            appendLine("- You must translate these source terms strictly into their defined translation targets.")
                        }

                        // Enforce structured parsing tokens
                        appendLine("- Format your entire response exactly using these three specific block sections:")
                        appendLine("---START_TRANSLATION---")
                        appendLine("[Translated Text Here]")
                        appendLine("---END_TRANSLATION---")
                        appendLine("---CONFIDENCE: [Score]%---")
                        appendLine("---VOCABULARY---")
                        appendLine("[Term 1 = Definition 1]")
                        appendLine("[Term 2 = Definition 2]")
                    }

                    // Call Gemini API
                    val translatedTextRaw = withContext(Dispatchers.IO) {
                        try {
                            val apiKey = _customApiKey.value.ifBlank { GeminiClient.getApiKey() }
                            val request = GeminiRequest(
                                contents = listOf(
                                    GeminiContent(
                                        parts = listOf(
                                            GeminiPart(text = prompt),
                                            GeminiPart(
                                                inlineData = GeminiInlineData(
                                                    mimeType = "image/jpeg",
                                                    data = base64Image
                                                )
                                            )
                                        )
                                    )
                                )
                            )
                            val response = GeminiClient.apiService.generateContent(
                                apiKey = apiKey,
                                request = request
                            )
                            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                                ?: "Çeviri alınamadı."
                        } catch (e: Exception) {
                            Log.e("TranslationVM", "Error translating page $pageIndex", e)
                            "Çeviri hatası (Sayfa ${pageIndex + 1}): ${e.localizedMessage ?: "Bilinmeyen API hatası"}"
                        }
                    }

                    // Parse structured elements
                    var translatedTextPart = translatedTextRaw
                    var confidenceScorePart = "95%"
                    var keyVocabPart = ""

                    if (translatedTextRaw.contains("---START_TRANSLATION---")) {
                        try {
                            val startToken = "---START_TRANSLATION---"
                            val endToken = "---END_TRANSLATION---"
                            val startIndex = translatedTextRaw.indexOf(startToken) + startToken.length
                            val endIndex = translatedTextRaw.indexOf(endToken)
                            if (endIndex > startIndex) {
                                translatedTextPart = translatedTextRaw.substring(startIndex, endIndex).trim()
                            }

                            // Extract confidence score
                            val confToken = "---CONFIDENCE:"
                            val confIndex = translatedTextRaw.indexOf(confToken)
                            if (confIndex != -1) {
                                val endConfLine = translatedTextRaw.indexOf("---", confIndex + confToken.length)
                                if (endConfLine != -1) {
                                    val extractedConf = translatedTextRaw.substring(confIndex + confToken.length, endConfLine).trim()
                                    confidenceScorePart = if (extractedConf.contains("%")) extractedConf else "$extractedConf%"
                                }
                            }

                            // Extract vocabulary lists
                            val vocabToken = "---VOCABULARY---"
                            val vocabIndex = translatedTextRaw.indexOf(vocabToken)
                            if (vocabIndex != -1) {
                                keyVocabPart = translatedTextRaw.substring(vocabIndex + vocabToken.length).trim()
                            }
                        } catch (e: Exception) {
                            Log.e("TranslationVM", "Failed parsing structured tokens, using fallback.", e)
                        }
                    }

                    // Insert Translated Page to database
                    withContext(Dispatchers.IO) {
                        repository.insertPage(
                            TranslatedPage(
                                translationHistoryId = historyId,
                                pageNumber = pageIndex + 1,
                                originalPagePath = cachedImagePath,
                                translatedText = translatedTextPart,
                                confidenceScore = confidenceScorePart,
                                keyVocabulary = keyVocabPart,
                                userNotes = ""
                            )
                        )
                    }
                }

                // Close PDF structures
                renderer.close()
                pfd.close()

                // Step 5: Load completed translation detail
                viewTranslationDetail(historyId)

            } catch (e: Exception) {
                Log.e("TranslationVM", "Translation failed", e)
                _errorMessage.value = e.localizedMessage ?: "Beklenmeyen bir hata oluştu."
                _uiState.value = MainUiState.Home
            }
        }
    }

    /**
     * View translation detail from History List
     */
    fun viewTranslationDetail(historyId: Int) {
        viewModelScope.launch {
            try {
                // Collect the specific item and its pages
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
