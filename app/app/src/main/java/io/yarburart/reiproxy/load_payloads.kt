package io.yarburart.reiproxy

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.zip.ZipInputStream

data class IntruderPayload(
    val name: String,
    val path: String,
    val lineCount: Int,
)

data class PayloadRecord(
    val vulnType: String, // This is the folder name (e.g. SQL Injection)
    val readmePath: String?,
    val intruders: List<IntruderPayload>,
)

private const val PAYLOAD_CACHE_FILE = "payloads_cache.json"
private const val GITHUB_ZIP_URL = "https://github.com/swisskyrepo/PayloadsAllTheThings/archive/refs/heads/master.zip"

/**
 * Loads the payload index from the cache. Very fast because it doesn't load the actual strings.
 */
fun loadPayloads(context: Context): List<PayloadRecord> {
    val file = File(context.filesDir, PAYLOAD_CACHE_FILE)
    if (!file.exists()) return emptyList()

    return try {
        val root = JSONArray(file.readText())
        buildList {
            for (i in 0 until root.length()) {
                val item = root.getJSONObject(i)
                val intrudersJson = item.getJSONArray("intruders")
                val intruders = buildList {
                    for (j in 0 until intrudersJson.length()) {
                        val intruder = intrudersJson.getJSONObject(j)
                        add(
                            IntruderPayload(
                                name = intruder.getString("name"),
                                path = intruder.getString("path"),
                                lineCount = intruder.optInt("lineCount", 0)
                            )
                        )
                    }
                }
                add(
                    PayloadRecord(
                        vulnType = item.getString("vulnType"),
                        readmePath = item.optString("readmePath").ifBlank { null },
                        intruders = intruders,
                    )
                )
            }
        }
    } catch (e: Exception) {
        emptyList()
    }
}

/**
 * Reads the actual payload lines from a file. Call this only when needed.
 */
fun getFileContent(path: String): String {
    val file = File(path)
    return if (file.exists()) file.readText() else ""
}

fun getPayloadLines(path: String): List<String> {
    return getFileContent(path).lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .toList()
}

/**
 * Downloads and indexes the repo. Runs on Dispatchers.IO to avoid blocking the UI.
 */
suspend fun fetchPayloadsFromGithub(context: Context, onProgress: (String) -> Unit): List<PayloadRecord> = withContext(Dispatchers.IO) {
    val targetDir = File(context.filesDir, "payloads_repo")
    if (targetDir.exists()) targetDir.deleteRecursively()
    targetDir.mkdirs()

    onProgress("Downloading repository...")
    val zipFile = File(context.cacheDir, "payloads.zip")
    try {
        URL(GITHUB_ZIP_URL).openStream().use { input ->
            FileOutputStream(zipFile).use { output ->
                input.copyTo(output)
            }
        }
    } catch (e: Exception) {
        onProgress("Download failed: ${e.message}")
        return@withContext emptyList()
    }

    onProgress("Extracting...")
    unzip(zipFile, targetDir)
    zipFile.delete()

    val repoRoot = targetDir.listFiles()?.firstOrNull { it.isDirectory } ?: targetDir
    
    onProgress("Indexing...")
    val payloads = repoRoot.listFiles()
        .orEmpty()
        .filter { it.isDirectory && !it.name.startsWith(".") && !it.name.startsWith("_") }
        .sortedBy { it.name.lowercase() }
        .mapNotNull { dir ->
            val readme = File(dir, "README.md").takeIf { it.exists() } ?: File(dir, "readme.md").takeIf { it.exists() }
            val intruderDir = File(dir, "Intruder").takeIf { it.exists() } ?: File(dir, "Intruders").takeIf { it.exists() }
            
            val intruders = intruderDir?.walkTopDown()
                ?.filter { it.isFile && !it.name.startsWith(".") }
                ?.sortedBy { it.name.lowercase() }
                ?.mapNotNull { file ->
                    val lines = file.readLines().map { it.trim() }.filter { it.isNotEmpty() }
                    if (lines.isEmpty()) null else {
                        IntruderPayload(
                            name = file.nameWithoutExtension,
                            path = file.absolutePath,
                            lineCount = lines.size
                        )
                    }
                }?.toList().orEmpty()

            if (readme == null && intruders.isEmpty()) null else {
                PayloadRecord(
                    vulnType = dir.name,
                    readmePath = readme?.absolutePath,
                    intruders = intruders
                )
            }
        }

    persistPayloads(context, payloads)
    onProgress("Done!")
    payloads
}

private fun unzip(zipFile: File, targetDir: File) {
    ZipInputStream(BufferedInputStream(zipFile.inputStream())).use { zis ->
        var entry = zis.nextEntry
        while (entry != null) {
            val file = File(targetDir, entry.name)
            if (entry.isDirectory) {
                file.mkdirs()
            } else {
                file.parentFile?.mkdirs()
                FileOutputStream(file).use { fos ->
                    zis.copyTo(fos)
                }
            }
            zis.closeEntry()
            entry = zis.nextEntry
        }
    }
}

private fun persistPayloads(context: Context, payloads: List<PayloadRecord>) {
    val json = JSONArray()
    payloads.forEach { payload ->
        json.put(
            JSONObject().apply {
                put("vulnType", payload.vulnType)
                put("readmePath", payload.readmePath ?: "")
                put(
                    "intruders",
                    JSONArray().apply {
                        payload.intruders.forEach { intruder ->
                            put(
                                JSONObject().apply {
                                    put("name", intruder.name)
                                    put("path", intruder.path)
                                    put("lineCount", intruder.lineCount)
                                }
                            )
                        }
                    }
                )
            }
        )
    }
    File(context.filesDir, PAYLOAD_CACHE_FILE).writeText(json.toString())
}
