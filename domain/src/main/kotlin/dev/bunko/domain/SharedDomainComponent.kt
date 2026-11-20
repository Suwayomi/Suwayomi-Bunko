/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.domain

import dev.bunko.domain.base.ServerSubscriptions
import dev.bunko.domain.discord.DiscordPresence
import dev.bunko.domain.discord.service.DiscordPreferences
import dev.bunko.core.CoreComponent
import dev.bunko.core.di.AppScope
import dev.bunko.domain.download.service.DownloadService
import dev.bunko.domain.extension.service.ExtensionPreferences
import dev.bunko.domain.library.service.LibraryPreferences
import dev.bunko.domain.library.service.LibraryUpdateService
import dev.bunko.domain.migration.interactor.RunMigrations
import dev.bunko.domain.migration.service.MigrationPreferences
import dev.bunko.domain.reader.service.ReaderPreferences
import dev.bunko.domain.server.Http
import dev.bunko.domain.server.HttpNoAuth
import dev.bunko.domain.server.httpClient
import dev.bunko.domain.server.httpClientNoAuth
import dev.bunko.domain.server.service.ServerPreferences
import dev.bunko.domain.source.service.CatalogPreferences
import dev.bunko.domain.ui.service.UiPreferences
import dev.bunko.domain.updates.interactor.UpdateChecker
import dev.bunko.domain.updates.service.UpdatePreferences
import dev.bunko.domain.user.interactor.UserRefreshUI
import dev.bunko.domain.user.service.UserPreferences
import kotlinx.serialization.json.Json
import me.tatarka.inject.annotations.Provides

interface SharedDomainComponent : CoreComponent {
    val migrations: RunMigrations

    val updateChecker: UpdateChecker

    val downloadService: DownloadService

    val libraryUpdateService: LibraryUpdateService

    val http: Http

    val httpNoAuth: HttpNoAuth

    val serverPreferences: ServerPreferences

    val extensionPreferences: ExtensionPreferences

    val catalogPreferences: CatalogPreferences

    val libraryPreferences: LibraryPreferences

    val readerPreferences: ReaderPreferences

    val uiPreferences: UiPreferences

    val discordPreferences: DiscordPreferences

    val discordPresence: DiscordPresence

    val migrationPreferences: MigrationPreferences

    val updatePreferences: UpdatePreferences

    val serverListeners: ServerListeners

    val serverSubscriptions: ServerSubscriptions

    val json: Json

    val userRefreshUI: UserRefreshUI

    @get:AppScope
    @get:Provides
    val lazyUserRefreshUIFactory: Lazy<UserRefreshUI>
        get() = lazy { userRefreshUI }

    @AppScope
    @Provides
    fun httpFactory(
        serverPreferences: ServerPreferences,
        userPreferences: UserPreferences,
        userRefreshUI: Lazy<UserRefreshUI>,
        json: Json,
    ): Http = httpClient(serverPreferences, userPreferences, userRefreshUI, json)

    @AppScope
    @Provides
    fun httpNoAuthFactory(
        serverPreferences: ServerPreferences,
        userPreferences: UserPreferences,
        userRefreshUI: Lazy<UserRefreshUI>,
        json: Json,
    ): HttpNoAuth = httpClientNoAuth(serverPreferences, userPreferences, userRefreshUI, json)

    @get:AppScope
    @get:Provides
    val serverPreferencesFactory: ServerPreferences
        get() = ServerPreferences(preferenceFactory.create("server"))

    @get:AppScope
    @get:Provides
    val extensionPreferencesFactory: ExtensionPreferences
        get() = ExtensionPreferences(preferenceFactory.create("extension"))

    @get:AppScope
    @get:Provides
    val catalogPreferencesFactory: CatalogPreferences
        get() = CatalogPreferences(preferenceFactory.create("catalog"))

    @get:AppScope
    @get:Provides
    val libraryPreferencesFactory: LibraryPreferences
        get() = LibraryPreferences(preferenceFactory.create("library"))

    @get:AppScope
    @get:Provides
    val readerPreferencesFactory: ReaderPreferences
        get() = ReaderPreferences(preferenceFactory.create("reader")) { name ->
            preferenceFactory.create("reader", name)
        }

    @get:AppScope
    @get:Provides
    val uiPreferencesFactory: UiPreferences
        get() = UiPreferences(preferenceFactory.create("ui"))

    @get:AppScope
    @get:Provides
    val discordPreferencesFactory: DiscordPreferences
        get() = DiscordPreferences(preferenceFactory.create("discord"))

    @get:AppScope
    @get:Provides
    val migrationPreferencesFactory: MigrationPreferences
        get() = MigrationPreferences(preferenceFactory.create("migration"))

    @get:AppScope
    @get:Provides
    val updatePreferencesFactory: UpdatePreferences
        get() = UpdatePreferences(preferenceFactory.create("update"))

    @get:AppScope
    @get:Provides
    val userPreferencesFactory: UserPreferences
        get() = UserPreferences(preferenceFactory.create("user"))

    @get:AppScope
    @get:Provides
    val serverListenersFactory: ServerListeners
        get() = ServerListeners()

    @get:AppScope
    @get:Provides
    val jsonFactory: Json
        get() = Json {
            isLenient = false
            ignoreUnknownKeys = true
            allowSpecialFloatingPointValues = true
            useArrayPolymorphism = false
        }
}
