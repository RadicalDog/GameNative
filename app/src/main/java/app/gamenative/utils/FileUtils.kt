package app.gamenative.utils

import android.content.res.AssetManager
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
import java.io.IOException
import java.io.OutputStreamWriter
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Stream
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import timber.log.Timber
import java.util.stream.Collectors

object FileUtils {

    fun makeDir(dirName: String) {
        val homeItemsDir = File(dirName)
        homeItemsDir.mkdirs()
    }

    fun makeFile(fileName: String, errorTag: String? = "FileUtils", errorMsg: ((Exception) -> String)? = null) {
        try {
            val file = File(fileName)
            if (!file.exists()) {
                file.createNewFile()
            }
        } catch (e: Exception) {
            Timber.e("%s encountered an issue in makeFile()", errorTag)
            Timber.e(errorMsg?.invoke(e) ?: "Error creating file: $e")
        }
    }

    fun createPathIfNotExist(filepath: String) {
        val file = File(filepath)
        var dirs = filepath

        // if the file path is not a directory and if we're not at the root directory then get the parent directory
        if (!filepath.endsWith('/') && filepath.lastIndexOf('/') > 0) {
            dirs = file.parent!!
        }

        makeDir(dirs)
    }

    fun readFileAsString(path: String, errorTag: String = "FileUtils", errorMsg: ((Exception) -> String)? = null): String? {
        var fileData: String? = null
        try {
            val r = BufferedReader(FileReader(path))
            val total = StringBuilder()
            var line: String?

            while ((r.readLine().also { line = it }) != null) {
                total.append(line).append('\n')
            }

            fileData = total.toString()
        } catch (e: Exception) {
            Timber.e("%s encountered an issue in readFileAsString()", errorTag)
            Timber.e(errorMsg?.invoke(e) ?: "Error reading file: $e")
        }

        return fileData
    }

    fun writeStringToFile(data: String, path: String, errorTag: String? = "FileUtils", errorMsg: ((Exception) -> String)? = null) {
        createPathIfNotExist(path)

        try {
            val fOut = FileOutputStream(path)
            val myOutWriter = OutputStreamWriter(fOut)
            myOutWriter.append(data)
            myOutWriter.close()
            fOut.flush()
            fOut.close()
        } catch (e: Exception) {
            Timber.e("%s encounted an issue in writeStringToFile()", errorTag)
            Timber.e(errorMsg?.invoke(e) ?: "Error writing to file: $e")
        }
    }

    /**
     * Traverse through a directory and perform an action on each file
     *
     * @param rootPath The start path
     * @param maxDepth How deep to go in the directory tree, a value of -1 keeps going
     * @param action The action to perform on each file
     */
    fun walkThroughPath(rootPath: Path, maxDepth: Int = 0, action: (Path) -> Unit) {
        Files.list(rootPath).forEach {
            action(it)
            if (maxDepth != 0 && it.exists() && it.isDirectory()) {
                walkThroughPath(
                    rootPath = it,
                    maxDepth = if (maxDepth > 0) maxDepth - 1 else maxDepth,
                    action = action,
                )
            }
        }
    }

    fun findFiles(rootPath: Path, pattern: String, includeDirectories: Boolean = false): List<Path> {
        val patternParts = pattern.split("*").filter { it.isNotEmpty() }
        Timber.i("$pattern -> $patternParts")
        if (!Files.exists(rootPath)) return emptyList()

        return Files.list(rootPath).use { stream ->
            stream.filter { path ->
                if (path.isDirectory() && !includeDirectories) {
                    false
                } else {
                    val fileName = path.name
                    Timber.i("Checking $fileName for pattern $pattern")
                    var startIndex = 0
                    !patternParts.map {
                        val index = fileName.indexOf(it, startIndex)
                        if (index >= 0) {
                            startIndex = index + it.length
                        }
                        index
                    }.any { it < 0 }
                }
            }.collect(Collectors.toList())
        }
    }

    fun findFilesRecursive(
        rootPath: Path,
        pattern: String,
        maxDepth: Int = -1,
        includeDirectories: Boolean = false,
    ): Stream<Path> {
        val patternParts = pattern.split("*").filter { it.isNotEmpty() }
        Timber.i("$pattern -> $patternParts (recursive, depth=$maxDepth)")
        if (!Files.exists(rootPath)) return emptyList<Path>().stream()

        val results = mutableListOf<Path>()

        fun matches(fileName: String): Boolean {
            var startIndex = 0
            for (part in patternParts) {
                val index = fileName.indexOf(part, startIndex)
                if (index < 0) return false
                startIndex = index + part.length
            }
            return true
        }

        walkThroughPath(rootPath, maxDepth) { path ->
            if (path.isDirectory()) {
                if (includeDirectories && matches(path.name)) {
                    results.add(path)
                }
            } else {
                val fileName = path.name
                Timber.i("Checking $fileName for pattern $pattern (recursive)")
                if (matches(fileName)) {
                    results.add(path)
                }
            }
        }

        return results.stream()
    }

    fun assetExists(assetManager: AssetManager, assetPath: String): Boolean {
        return try {
            assetManager.open(assetPath).use {
                true
            }
        } catch (e: IOException) {
            // Timber.e(e)
            false
        }
    }
}
