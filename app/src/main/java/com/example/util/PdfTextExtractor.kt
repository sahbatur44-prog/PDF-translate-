package com.example.util

import android.content.Context
import android.net.Uri
import android.util.Log

object PdfTextExtractor {

    /**
     * Extracts text page by page from a local PDF Uri or InputStream.
     * Returns a map or list of string page contents.
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
                        if (extracted.isNotBlank()) {
                            pagesText.add(extracted)
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
                        val cleaned = decodePdfString(rawText)
                        if (cleaned.isNotBlank()) {
                            sb.append(cleaned).append(" ")
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
                                    val cleaned = decodePdfString(rawText)
                                    if (cleaned.isNotBlank()) {
                                        sb.append(cleaned)
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
}
