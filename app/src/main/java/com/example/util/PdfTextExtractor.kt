package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PdfTextExtractor {

    /**
     * Performs OCR on a given page Bitmap using Google ML Kit Text Recognition.
     * Perfect for Image-to-PDF, scanned documents, and comics.
     */
    suspend fun recognizeTextFromBitmap(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val task = recognizer.process(image)
            val result = Tasks.await(task)
            val text = result.text.trim()
            cleanText(text)
        } catch (e: Exception) {
            Log.e("PdfTextExtractor", "MLKit OCR Error: ${e.message}", e)
            ""
        }
    }

    /**
     * Extracts text page by page from a local PDF Uri as fallback.
     */
    fun extractTextFromPdf(context: Context, uri: Uri): List<String> {
        val pagesText = mutableListOf<String>()
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val bytes = inputStream.readBytes()
                val fullText = String(bytes, Charsets.ISO_8859_1)

                val streamBlocks = extractStreamBlocks(fullText)
                if (streamBlocks.isNotEmpty()) {
                    for (block in streamBlocks) {
                        val extracted = parsePdfTextStream(block)
                        val cleaned = cleanText(extracted)
                        if (cleaned.isNotBlank()) {
                            pagesText.add(cleaned)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("PdfTextExtractor", "Error extracting text from PDF", e)
        }
        return pagesText
    }

    private fun extractStreamBlocks(fullText: String): List<String> {
        val blocks = mutableListOf<String>()
        var searchIndex = 0
        while (searchIndex < fullText.length) {
            val streamStart = fullText.indexOf("stream", searchIndex)
            if (streamStart == -1) break
            val streamEnd = fullText.indexOf("endstream", streamStart)
            if (streamEnd == -1) break

            val streamContent = fullText.substring(streamStart + 6, streamEnd)
            if (streamContent.contains("BT") && streamContent.contains("ET")) {
                blocks.add(streamContent)
            }
            searchIndex = streamEnd + 9
        }
        return blocks
    }

    private fun parsePdfTextStream(streamContent: String): String {
        val sb = StringBuilder()
        var index = 0
        while (index < streamContent.length) {
            val btPos = streamContent.indexOf("BT", index)
            if (btPos == -1) break
            val etPos = streamContent.indexOf("ET", btPos)
            if (etPos == -1) break

            val textBlock = streamContent.substring(btPos + 2, etPos)
            var i = 0
            while (i < textBlock.length) {
                if (textBlock[i] == '(') {
                    val closeParen = findClosingParen(textBlock, i)
                    if (closeParen != -1) {
                        val rawText = textBlock.substring(i + 1, closeParen)
                        val decoded = decodePdfString(rawText)
                        if (decoded.isNotBlank()) {
                            sb.append(decoded).append(" ")
                        }
                        i = closeParen + 1
                        continue
                    }
                } else if (textBlock[i] == '[') {
                    val closeBracket = textBlock.indexOf(']', i)
                    if (closeBracket != -1) {
                        val bracketContent = textBlock.substring(i + 1, closeBracket)
                        var j = 0
                        while (j < bracketContent.length) {
                            if (bracketContent[j] == '(') {
                                val cParen = findClosingParen(bracketContent, j)
                                if (cParen != -1) {
                                    val rawText = bracketContent.substring(j + 1, cParen)
                                    val decoded = decodePdfString(rawText)
                                    if (decoded.isNotBlank()) {
                                        sb.append(decoded)
                                    }
                                    j = cParen + 1
                                    continue
                                }
                            }
                            j++
                        }
                        sb.append(" ")
                        i = closeBracket + 1
                        continue
                    }
                }
                i++
            }
            sb.append("\n")
            index = etPos + 2
        }
        return sb.toString().trim()
    }

    private fun findClosingParen(str: String, startIdx: Int): Int {
        var depth = 0
        for (i in startIdx until str.length) {
            val c = str[i]
            if (c == '\\') {
                continue
            }
            if (c == '(') depth++
            if (c == ')') {
                depth--
                if (depth == 0) return i
            }
        }
        return -1
    }

    private fun decodePdfString(input: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < input.length) {
            val c = input[i]
            if (c == '\\' && i + 1 < input.length) {
                val next = input[i + 1]
                when (next) {
                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    't' -> sb.append('\t')
                    '(' -> sb.append('(')
                    ')' -> sb.append(')')
                    '\\' -> sb.append('\\')
                    else -> sb.append(next)
                }
                i += 2
            } else {
                sb.append(c)
                i++
            }
        }
        return sb.toString()
    }

    /**
     * Cleans control characters and verifies text sanity.
     * Prevents garbled binary stream data from being treated as text.
     */
    fun cleanText(input: String): String {
        if (input.isBlank()) return ""
        // Filter out non-printable ASCII / control characters
        val filtered = input.replace(Regex("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F-\\x9F]"), "")
        var printableCount = 0
        for (c in filtered) {
            if (c.isLetterOrDigit() || c.isWhitespace() || c in ".,!?:;\"'()-+/*=@#$%&[]{}|/\\çğıöşüÇĞİÖŞÜ") {
                printableCount++
            }
        }
        val ratio = if (filtered.isNotEmpty()) printableCount.toDouble() / filtered.length else 0.0
        return if (ratio > 0.65) filtered.trim() else ""
    }
}
