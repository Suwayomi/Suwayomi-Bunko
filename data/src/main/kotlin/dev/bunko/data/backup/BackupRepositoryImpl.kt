package dev.bunko.data.backup

import dev.bunko.core.io.toSource
import dev.bunko.core.prefs.Preference
import dev.bunko.data.ApolloAppClient
import dev.bunko.data.graphql.CreateBackupMutation
import dev.bunko.data.graphql.RestoreBackupMutation
import dev.bunko.data.graphql.RestoreStatusQuery
import dev.bunko.data.graphql.ValidateBackupQuery
import dev.bunko.data.graphql.fragment.RestoreStatusFragment
import dev.bunko.data.graphql.type.BackupRestoreState
import dev.bunko.domain.backup.model.BackupValidationResult
import dev.bunko.domain.backup.model.RestoreState
import dev.bunko.domain.backup.model.RestoreStatus
import dev.bunko.domain.backup.service.BackupRepository
import dev.bunko.domain.server.Http
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.DefaultUpload
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.Url
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okio.Source
import okio.buffer

class BackupRepositoryImpl(
    private val apolloAppClient: ApolloAppClient,
    private val http: Http,
    private val serverUrl: Preference<Url>,
) : BackupRepository {
    val apolloClient: ApolloClient
        get() = apolloAppClient.value

    override fun validateBackup(source: Source): Flow<BackupValidationResult> =
        apolloClient.query(
            ValidateBackupQuery(
                DefaultUpload.Builder()
                    .content {
                        it.writeAll(source.buffer())
                    }
                    .fileName("backup.tachibk")
                    .contentType("application/octet-stream")
                    .build(),
            ),
        ).toFlow()
            .map {
                BackupValidationResult(
                    missingSources = it.dataAssertNoErrors.validateBackup.missingSources.map { source ->
                        "${source.name} (${source.id})"
                    },
                    missingTrackers = emptyList(),
                )
            }

    override fun restoreBackup(source: Source): Flow<Pair<String, RestoreStatus>> =
        apolloClient.mutation(
            RestoreBackupMutation(
                DefaultUpload.Builder()
                    .content {
                        it.writeAll(source.buffer())
                    }
                    .fileName("backup.tachibk")
                    .contentType("application/octet-stream")
                    .build(),
            ),
        ).toFlow()
            .map {
                val data = it.dataAssertNoErrors
                data.restoreBackup.id to data.restoreBackup.status!!
                    .restoreStatusFragment
                    .toRestoreStatus()
            }

    override fun restoreStatus(id: String): Flow<RestoreStatus> =
        apolloClient.query(
            RestoreStatusQuery(id),
        ).toFlow()
            .map {
                val data = it.dataAssertNoErrors
                data.restoreStatus!!.restoreStatusFragment.toRestoreStatus()
            }

    override fun createBackup(
        includeCategories: Boolean,
        includeChapters: Boolean,
        block: HttpRequestBuilder.() -> Unit,
    ): Flow<Pair<String, Source>> =
        apolloClient
            .mutation(
                CreateBackupMutation(includeCategories, includeChapters),
            )
            .toFlow()
            .map {
                val url = it.dataAssertNoErrors.createBackup.url
                val response = http.value.get(
                    Url("${serverUrl.get()}$url"),
                )
                backupFileName(response.headers["content-disposition"]) to response.bodyAsChannel().toSource()
            }

    companion object {
        private fun RestoreStatusFragment.toRestoreStatus() =
            RestoreStatus(
                when (state) {
                    BackupRestoreState.IDLE -> RestoreState.IDLE
                    BackupRestoreState.SUCCESS -> RestoreState.SUCCESS
                    BackupRestoreState.FAILURE -> RestoreState.FAILURE
                    BackupRestoreState.RESTORING_CATEGORIES -> RestoreState.RESTORING_CATEGORIES
                    BackupRestoreState.RESTORING_MANGA -> RestoreState.RESTORING_MANGA
                    BackupRestoreState.RESTORING_META -> RestoreState.RESTORING_META
                    BackupRestoreState.RESTORING_SETTINGS -> RestoreState.RESTORING_SETTINGS
                    BackupRestoreState.UNKNOWN__ -> RestoreState.UNKNOWN
                },
                mangaProgress,
                totalManga,
            )
    }
}

internal fun backupFileName(contentDisposition: String?): String {
    val name = contentDisposition
        ?.substringAfter("filename=", "")
        ?.substringAfterLast('/')
        ?.substringAfterLast('\\')
        ?.trim()
        ?.trim('"')
        ?.trim()
        .orEmpty()
    return name.takeIf { it.isNotBlank() && it != "." && it != ".." }
        ?: "bunko_backup_${System.currentTimeMillis()}.tachibk"
}
