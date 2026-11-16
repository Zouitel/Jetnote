package zouitel.neighborhood.database.backup

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import zouitel.neighborhood.database.database.Database
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Creates and restores portable Jetnote archives through caller-provided SAF Uris.
 *
 * The Room database is copied as its SQLCipher-encrypted on-disk file. A restore closes
 * Room before replacing that file; the caller must restart the process after success so
 * Koin/Room can create a fresh database instance.
 */
class BackupArchiveManager(
    private val context: Context,
    private val database: Database
) {
    companion object {
        private const val ARCHIVE_FORMAT = 1
        private const val MANIFEST_PATH = "manifest.json"
        private const val DATABASE_ENTRY = "database/jetnote_database"
        private const val MAX_ENTRIES = 20_000
        private const val MAX_UNCOMPRESSED_BYTES = 2L * 1024 * 1024 * 1024
        private const val BUFFER_SIZE = 32 * 1024

        // Keep this list explicit: caches, preferences, auth tokens and databases from
        // unrelated SDKs must never silently become part of a personal-notes backup.
        private val MEDIA_DIRECTORIES = listOf(
            "media", "images", "attachments", "audio", "drawings"
        )
    }

    sealed interface RestoreResult {
        data object RestartRequired : RestoreResult
    }

    suspend fun createBackup(destination: Uri) = withContext(Dispatchers.IO) {
        require(destination.scheme == "content") { "Choose a document-provider destination." }
        val dbFile = File(database.openHelper.writableDatabase.path
            ?: error("Room database path is unavailable."))
        check(dbFile.isFile) { "The Jetnote database file does not exist." }

        // Flush committed WAL pages into the main SQLCipher database before copying it.
        database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { }

        val sources = linkedMapOf<String, File>()
        sources[DATABASE_ENTRY] = dbFile
        MEDIA_DIRECTORIES.forEach { name ->
            val directory = File(context.filesDir, name)
            if (directory.exists() && directory.isDirectory) {
                directory.walkTopDown()
                    .filter { it.isFile }
                    .forEach { file ->
                        val relative = file.relativeTo(directory).invariantSeparatorsPath
                        sources["media/$name/$relative"] = file
                    }
            }
        }

        val checksums = JSONObject()
        context.contentResolver.openOutputStream(destination, "wt")?.use { output ->
            ZipOutputStream(BufferedOutputStream(output)).use { zip ->
                sources.forEach { (archivePath, file) ->
                    val digest = MessageDigest.getInstance("SHA-256")
                    zip.putNextEntry(ZipEntry(archivePath))
                    BufferedInputStream(FileInputStream(file)).use { input ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            digest.update(buffer, 0, count)
                            zip.write(buffer, 0, count)
                        }
                    }
                    zip.closeEntry()
                    checksums.put(archivePath, digest.digest().toHex())
                }

                val manifest = JSONObject()
                    .put("format", "jetnote-backup")
                    .put("formatVersion", ARCHIVE_FORMAT)
                    .put("createdAt", System.currentTimeMillis())
                    .put("database", DATABASE_ENTRY)
                    .put("files", checksums)
                zip.putNextEntry(ZipEntry(MANIFEST_PATH))
                zip.write(manifest.toString().toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        } ?: error("Unable to open the selected backup destination.")
    }

    /**
     * Validates every entry and checksum before changing app data.
     * Successful restore intentionally closes Room; restart the app immediately afterward.
     */
    suspend fun restoreBackup(source: Uri): RestoreResult = withContext(Dispatchers.IO) {
        require(source.scheme == "content") { "Choose a backup document." }
        val staging = File(context.cacheDir, "restore-${UUID.randomUUID()}").apply {
            check(mkdirs()) { "Unable to create restore staging directory." }
        }

        try {
            val extracted = extractAndValidate(source, staging)
            val databaseFile = extracted[DATABASE_ENTRY]
                ?: error("Backup does not contain the Jetnote database.")

            val liveDb = File(database.openHelper.writableDatabase.path
                ?: error("Room database path is unavailable."))
            check(liveDb.name == "jetnote_database") {
                "Unexpected database filename; refusing to restore."
            }

            val stagedDb = File(staging, "validated-database")
            databaseFile.copyTo(stagedDb, overwrite = true)
            check(stagedDb.length() > 0L) { "Backup database is empty." }

            // Keep a rollback copy until the replacement has been moved into place.
            val rollback = File(liveDb.parentFile, "jetnote_database.restore-rollback")
            database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { }
            liveDb.copyTo(rollback, overwrite = true)
            database.close()
            File(liveDb.path + "-wal").delete()
            File(liveDb.path + "-shm").delete()
            try {
                check(liveDb.delete()) { "Unable to replace the existing database." }
                check(stagedDb.renameTo(liveDb)) { "Unable to install the restored database." }
                rollback.delete()
            } catch (failure: Throwable) {
                liveDb.delete()
                rollback.renameTo(liveDb)
                throw failure
            }

            // Restore only the media folders explicitly supported by this archive format.
            MEDIA_DIRECTORIES.forEach { name ->
                val archivedRoot = File(staging, "media/$name")
                if (archivedRoot.exists()) {
                    val target = File(context.filesDir, name)
                    val replacement = File(context.filesDir, "$name.restore")
                    replacement.deleteRecursively()
                    archivedRoot.copyRecursively(replacement, overwrite = true)
                    target.deleteRecursively()
                    check(replacement.renameTo(target)) { "Unable to restore $name." }
                }
            }
            RestoreResult.RestartRequired
        } finally {
            staging.deleteRecursively()
        }
    }

    private fun extractAndValidate(source: Uri, staging: File): Map<String, File> {
        val extracted = linkedMapOf<String, File>()
        var manifest: JSONObject? = null
        var entryCount = 0
        var totalBytes = 0L

        val input = context.contentResolver.openInputStream(source)
            ?: error("Unable to read the selected backup.")
        ZipInputStream(BufferedInputStream(input)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entryCount++
                require(entryCount <= MAX_ENTRIES) { "Backup contains too many files." }
                val path = entry.name
                require(!path.startsWith("/") && !path.contains("\\\\") &&
                    path.split('/').none { it == ".." || it == "." }) {
                    "Backup contains an unsafe file path."
                }

                if (entry.isDirectory) {
                    zip.closeEntry()
                    continue
                }

                if (path == MANIFEST_PATH) {
                    val bytes = zip.readBytes()
                    require(bytes.size <= 1_000_000) { "Backup manifest is too large." }
                    manifest = JSONObject(bytes.toString(Charsets.UTF_8))
                    zip.closeEntry()
                    continue
                }

                val allowed = path == DATABASE_ENTRY ||
                    MEDIA_DIRECTORIES.any { path.startsWith("media/$it/") }
                require(allowed) { "Backup contains an unsupported file: $path" }
                val target = File(staging, path)
                require(target.canonicalPath.startsWith(staging.canonicalPath + File.separator)) {
                    "Backup contains an unsafe file path."
                }
                target.parentFile?.mkdirs()
                val digest = MessageDigest.getInstance("SHA-256")
                FileOutputStream(target).use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    while (true) {
                        val count = zip.read(buffer)
                        if (count < 0) break
                        totalBytes += count
                        require(totalBytes <= MAX_UNCOMPRESSED_BYTES) {
                            "Backup expands beyond the supported size limit."
                        }
                        digest.update(buffer, 0, count)
                        output.write(buffer, 0, count)
                    }
                }
                extracted[path] = target
                extracted["sha256:$path"] = File(staging, digest.digest().toHex())
                zip.closeEntry()
            }
        }

        val header = manifest ?: error("Backup manifest is missing.")
        require(header.optString("format") == "jetnote-backup") {
            "This file is not a Jetnote backup."
        }
        require(header.optInt("formatVersion", -1) == ARCHIVE_FORMAT) {
            "This backup format is not supported by this app version."
        }
        val expected = header.optJSONObject("files")
            ?: error("Backup manifest has no file checksums.")
        require(expected.length() == extracted.keys.count { !it.startsWith("sha256:") }) {
            "Backup contents do not match its manifest."
        }
        extracted.keys.filterNot { it.startsWith("sha256:") }.forEach { path ->
            val actual = extracted["sha256:$path"]?.name
            require(actual == expected.optString(path)) { "Backup integrity check failed: $path" }
        }
        return extracted.filterKeys { !it.startsWith("sha256:") }
    }

    private fun ByteArray.toHex(): String =
        joinToString("") { byte -> "%02x".format(byte) }
}
