/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.data.extension

import dev.bunko.data.ApolloAppClient
import dev.bunko.data.graphql.FetchExtensionsMutation
import dev.bunko.data.graphql.InstallExtensionMutation
import dev.bunko.data.graphql.InstallExternalExtensionMutation
import dev.bunko.data.graphql.UninstallExtensionMutation
import dev.bunko.data.graphql.UpdateExtensionMutation
import dev.bunko.data.graphql.fragment.ExtensionFragment
import dev.bunko.domain.extension.model.Extension
import dev.bunko.domain.extension.service.ExtensionRepository
import dev.bunko.domain.server.Http
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.DefaultUpload
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okio.Source

class ExtensionRepositoryImpl(
    private val apolloAppClient: ApolloAppClient,
    private val http: Http,
) : ExtensionRepository {
    val apolloClient: ApolloClient
        get() = apolloAppClient.value

    override fun getExtensionList(): Flow<List<Extension>> =
        apolloClient.mutation(
            FetchExtensionsMutation(),
        )
            .toFlow()
            .map {
                it.dataAssertNoErrors.fetchExtensions!!.extensions.map { it.extensionFragment.toExtension() }
            }

    override fun installExtension(source: Source): Flow<Unit> =
        apolloClient.mutation(
            InstallExternalExtensionMutation(
                DefaultUpload.Builder()
                    .content {
                        it.writeAll(source)
                    }
                    .fileName("extension.apk")
                    .contentType("application/octet-stream")
                    .build(),
            ),
        )
            .toFlow()
            .map {
                it.dataAssertNoErrors.installExternalExtension!!
            }

    override fun installExtension(pkgName: String): Flow<Unit> =
        apolloClient.mutation(
            InstallExtensionMutation(pkgName),
        )
            .toFlow()
            .map {
                it.dataAssertNoErrors.updateExtension
            }

    override fun updateExtension(pkgName: String): Flow<Unit> =
        apolloClient.mutation(
            UpdateExtensionMutation(pkgName),
        )
            .toFlow()
            .map {
                it.dataAssertNoErrors.updateExtension
            }

    override fun uninstallExtension(pkgName: String): Flow<Unit> =
        apolloClient.mutation(
            UninstallExtensionMutation(pkgName),
        )
            .toFlow()
            .map {
                it.dataAssertNoErrors.updateExtension
            }

    companion object {
        internal fun ExtensionFragment.toExtension(): Extension =
            Extension(
                name = name,
                pkgName = pkgName,
                versionName = versionName,
                versionCode = versionCode,
                lang = lang,
                apkName = apkName,
                iconUrl = iconUrl,
                installed = isInstalled,
                hasUpdate = hasUpdate,
                obsolete = isObsolete,
                isNsfw = isNsfw,
            )
    }
}
