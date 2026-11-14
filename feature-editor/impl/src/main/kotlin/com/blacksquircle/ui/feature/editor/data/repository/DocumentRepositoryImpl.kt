/*
 * Copyright Squircle CE contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.blacksquircle.ui.feature.editor.data.repository

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.blacksquircle.ui.core.database.dao.document.DocumentDao
import com.blacksquircle.ui.core.extensions.PermissionException
import com.blacksquircle.ui.core.extensions.isStorageAccessGranted
import com.blacksquircle.ui.core.provider.coroutine.DispatcherProvider
import com.blacksquircle.ui.core.settings.SettingsManager
import com.blacksquircle.ui.feature.editor.data.extensions.charsetFor
import com.blacksquircle.ui.feature.editor.data.extensions.openUriAsContent
import com.blacksquircle.ui.feature.editor.data.extensions.openUriAsDocument
import com.blacksquircle.ui.feature.editor.data.extensions.openUriAsFile
import com.blacksquircle.ui.feature.editor.data.manager.CacheManager
import com.blacksquircle.ui.feature.editor.data.mapper.DocumentMapper
import com.blacksquircle.ui.feature.editor.data.model.FileAssociation
import com.blacksquircle.ui.feature.editor.data.model.LanguageScope
import com.blacksquircle.ui.feature.editor.domain.model.DocumentModel
import com.blacksquircle.ui.feature.editor.domain.repository.DocumentRepository
import com.blacksquircle.ui.feature.editor.ui.editor.view.selectionEnd
import com.blacksquircle.ui.feature.editor.ui.editor.view.selectionStart
import com.blacksquircle.ui.feature.explorer.api.factory.FilesystemFactory
import com.blacksquircle.ui.feature.git.api.extensions.findGitRepository
import com.blacksquircle.ui.filesystem.base.model.FileModel
import com.blacksquircle.ui.filesystem.base.model.FileParams
import com.blacksquircle.ui.filesystem.base.model.LineBreak
import com.blacksquircle.ui.filesystem.local.LocalFilesystem
import com.blacksquircle.ui.filesystem.saf.SAFFilesystem
import io.github.rosemoe.sora.text.Content
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.withContext

internal class DocumentRepositoryImpl(
    private val dispatcherProvider: DispatcherProvider,
    private val settingsManager: SettingsManager,
    private val cacheManager: CacheManager,
    private val documentDao: DocumentDao,
    private val filesystemFactory: FilesystemFactory,
    private val context: Context,
) : DocumentRepository {

    override suspend fun loadDocuments(): List<DocumentModel> {
        return withContext(dispatcherProvider.io()) {
            documentDao.loadAll().map(DocumentMapper::toModel)
        }
    }

    override suspend fun openDocument(fileModel: FileModel, position: Int): DocumentModel {
        return withContext(dispatcherProvider.io()) {
            val entity = documentDao.load(fileModel.fileUri)
            if (entity != null) {
                DocumentMapper.toModel(entity)
            } else {
                val document = DocumentModel(
                    uuid = UUID.randomUUID().toString(),
                    fileUri = fileModel.fileUri,
                    filesystemUuid = fileModel.filesystemUuid,
                    displayName = fileModel.name,
                    language = FileAssociation.guessLanguage(fileModel.extension)
                        ?: LanguageScope.TEXT,
                    modified = false,
                    position = position,
                    scrollX = 0,
                    scrollY = 0,
                    selectionStart = 0,
                    selectionEnd = 0,
                    gitRepository = if (fileModel.filesystemUuid == LocalFilesystem.LOCAL_UUID) {
                        fileModel.findGitRepository()
                    } else {
                        null
                    },
                )
                val documentEntity = DocumentMapper.toEntity(document)
                documentDao.insert(documentEntity)
                document
            }
        }
    }

    override suspend fun openUntitled(name: String, position: Int): DocumentModel {
        return withContext(dispatcherProvider.io()) {
            val uuid = UUID.randomUUID().toString()
            val document = DocumentModel(
                uuid = uuid,
                fileUri = DocumentModel.UNTITLED_PREFIX + uuid,
                filesystemUuid = LocalFilesystem.LOCAL_UUID,
                displayName = name,
                language = LanguageScope.TEXT,
                modified = false,
                position = position,
                scrollX = 0,
                scrollY = 0,
                selectionStart = 0,
                selectionEnd = 0,
                gitRepository = null,
            )
            documentDao.insert(DocumentMapper.toEntity(document))
            cacheDocument(document, Content())
            document
        }
    }

    override suspend fun saveUntitledAs(
        document: DocumentModel,
        content: Content,
        fileUri: Uri,
    ): DocumentModel {
        return withContext(dispatcherProvider.io()) {
            val fileModel = resolveUri(fileUri)
            val savedDocument = document.copy(
                fileUri = fileModel.fileUri,
                filesystemUuid = fileModel.filesystemUuid,
                displayName = fileModel.name,
                language = FileAssociation.guessLanguage(fileModel.extension)
                    ?: document.language,
                modified = false,
            )
            documentDao.update(DocumentMapper.toEntity(savedDocument))
            saveDocument(savedDocument, content)
            savedDocument
        }
    }

    /** Fingerprint of each open file's contents on disk, as last loaded or saved by the app */
    private val diskFingerprints = ConcurrentHashMap<String, Long>()

    private fun fingerprint(text: String): Long {
        return (text.length.toLong() shl 32) or (text.hashCode().toLong() and 0xFFFFFFFFL)
    }

    private fun canWatch(document: DocumentModel): Boolean {
        return !document.isUntitled && document.filesystemUuid in WATCHED_FILESYSTEMS
    }

    private fun readDiskText(document: DocumentModel): String {
        val filesystem = filesystemFactory.create(document.filesystemUuid)
        val fileModel = DocumentMapper.toModel(document)
        val fileParams = FileParams(
            chardet = settingsManager.encodingAutoDetect,
            charset = charsetFor(settingsManager.encodingForOpening),
        )
        return filesystem.loadFile(fileModel, fileParams)
    }

    override suspend fun isChangedExternally(document: DocumentModel): Boolean {
        return withContext(dispatcherProvider.io()) {
            if (!canWatch(document)) {
                return@withContext false
            }
            val current = try {
                fingerprint(readDiskText(document))
            } catch (e: Exception) {
                /** Missing or unreadable files aren't reported here */
                return@withContext false
            }
            val known = diskFingerprints.putIfAbsent(document.uuid, current)
            known != null && known != current
        }
    }

    override suspend fun acceptExternalChange(document: DocumentModel) {
        withContext(dispatcherProvider.io()) {
            if (!canWatch(document)) {
                return@withContext
            }
            try {
                diskFingerprints[document.uuid] = fingerprint(readDiskText(document))
            } catch (e: Exception) {
                diskFingerprints.remove(document.uuid)
            }
        }
    }

    override suspend fun loadDocument(document: DocumentModel): Content {
        return withContext(dispatcherProvider.io()) {
            settingsManager.selectedUuid = document.uuid

            if (cacheManager.isCached(document)) {
                cacheManager.loadContent(document)
            } else if (document.isUntitled) {
                Content().also { content ->
                    cacheDocument(document, content)
                }
            } else {
                if (!context.isStorageAccessGranted()) {
                    throw PermissionException()
                }
                val filesystem = filesystemFactory.create(document.filesystemUuid)
                val fileModel = DocumentMapper.toModel(document)
                val fileParams = FileParams(
                    chardet = settingsManager.encodingAutoDetect,
                    charset = charsetFor(settingsManager.encodingForOpening),
                )
                val text = filesystem.loadFile(fileModel, fileParams)
                diskFingerprints[document.uuid] = fingerprint(text)
                Content(text).also { content ->
                    cacheDocument(document, content)
                }
            }
        }
    }

    override suspend fun saveDocument(document: DocumentModel, content: Content) {
        withContext(dispatcherProvider.io()) {
            if (document.isUntitled) {
                /** Untitled tabs only live in the cache until they're saved somewhere */
                cacheDocument(document, content)
                return@withContext
            }
            val filesystem = filesystemFactory.create(document.filesystemUuid)
            val fileModel = DocumentMapper.toModel(document)
            val fileParams = FileParams(
                charset = charsetFor(settingsManager.encodingForSaving),
                linebreak = LineBreak.of(settingsManager.lineBreakForSaving),
            )
            filesystem.saveFile(fileModel, content.toString(), fileParams).also {
                cacheDocument(document, content)
            }
            /** Remember what our own save wrote, so it isn't reported as an outside change */
            if (canWatch(document)) {
                try {
                    diskFingerprints[document.uuid] = fingerprint(readDiskText(document))
                } catch (e: Exception) {
                    diskFingerprints.remove(document.uuid)
                }
            }
        }
    }

    override suspend fun cacheDocument(document: DocumentModel, content: Content) {
        withContext(dispatcherProvider.io()) {
            cacheManager.create(document)
            cacheManager.saveContent(document, content)

            documentDao.updateProperties(
                uuid = document.uuid,
                modified = document.modified,
                scrollX = content.scrollX,
                scrollY = content.scrollY,
                selectionStart = content.selectionStart,
                selectionEnd = content.selectionEnd
            )
        }
    }

    override suspend fun updateDocument(document: DocumentModel) {
        withContext(dispatcherProvider.io()) {
            val documentEntity = DocumentMapper.toEntity(document)
            documentDao.update(documentEntity)
        }
    }

    override suspend fun refreshDocument(document: DocumentModel) {
        withContext(dispatcherProvider.io()) {
            cacheManager.delete(document)
            documentDao.updateProperties(
                uuid = document.uuid,
                modified = false,
                scrollX = 0,
                scrollY = 0,
                selectionStart = 0,
                selectionEnd = 0,
            )
        }
    }

    override suspend fun reorderDocuments(from: DocumentModel, to: DocumentModel) {
        withContext(dispatcherProvider.io()) {
            documentDao.reorderDocuments(
                fromUuid = from.uuid,
                toUuid = to.uuid,
                fromIndex = from.position,
                toIndex = to.position,
            )
        }
    }

    override suspend fun closeDocument(document: DocumentModel) {
        withContext(dispatcherProvider.io()) {
            documentDao.closeDocument(document.uuid, document.position)
            cacheManager.delete(document)
        }
    }

    override suspend fun closeOtherDocuments(document: DocumentModel) {
        withContext(dispatcherProvider.io()) {
            documentDao.closeOtherDocuments(document.uuid)
            settingsManager.selectedUuid = document.uuid
            cacheManager.deleteAll { file ->
                !file.name.startsWith(document.uuid, ignoreCase = true)
            }
        }
    }

    override suspend fun closeAllDocuments() {
        withContext(dispatcherProvider.io()) {
            documentDao.deleteAll()
            cacheManager.deleteAll()
            settingsManager.selectedUuid = ""
        }
    }

    override suspend fun changeModified(document: DocumentModel, modified: Boolean) {
        withContext(dispatcherProvider.io()) {
            documentDao.updateModified(document.uuid, modified)
        }
    }

    override suspend fun changeLanguage(document: DocumentModel, language: String) {
        withContext(dispatcherProvider.io()) {
            documentDao.updateLanguage(document.uuid, language)
        }
    }

    override suspend fun openExternal(fileUri: Uri, position: Int): DocumentModel {
        return withContext(dispatcherProvider.io()) {
            openDocument(resolveUri(fileUri), position)
        }
    }

    private fun resolveUri(fileUri: Uri): FileModel {
        return when {
            DocumentsContract.isDocumentUri(context, fileUri) -> {
                    context.openUriAsDocument(fileUri)
                }

                fileUri.scheme == ContentResolver.SCHEME_CONTENT -> {
                    context.openUriAsContent(fileUri)
                }

                fileUri.scheme == ContentResolver.SCHEME_FILE -> {
                    openUriAsFile(fileUri)
                }

            else -> throw IllegalArgumentException("File $fileUri not found")
        }
    }

    override suspend fun saveExternal(document: DocumentModel, content: Content, fileUri: Uri) {
        withContext(dispatcherProvider.io()) {
            val filesystemUuid = SAFFilesystem.SAF_UUID
            val filesystem = filesystemFactory.create(filesystemUuid)
            val fileModel = DocumentMapper.toModel(
                document.copy(
                    fileUri = fileUri.toString(),
                    filesystemUuid = filesystemUuid,
                )
            )
            val fileParams = FileParams(
                charset = charsetFor(settingsManager.encodingForSaving),
                linebreak = LineBreak.of(settingsManager.lineBreakForSaving),
            )
            filesystem.saveFile(fileModel, content.toString(), fileParams)
        }
    }

    companion object {
        private val WATCHED_FILESYSTEMS = setOf(
            LocalFilesystem.LOCAL_UUID,
            SAFFilesystem.SAF_UUID,
            "root",
        )
    }
}
