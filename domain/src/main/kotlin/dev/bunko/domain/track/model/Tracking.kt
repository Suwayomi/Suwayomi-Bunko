/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.domain.track.model

import androidx.compose.runtime.Immutable

@Immutable
data class TrackStatus(
    val value: Int,
    val name: String,
)

@Immutable
data class Tracker(
    val id: Int,
    val name: String,
    val icon: String,
    val isLoggedIn: Boolean,
    val isTokenExpired: Boolean,
    val authUrl: String?,
    val scores: List<String>,
    val statuses: List<TrackStatus>,
    val supportsTrackDeletion: Boolean,
)

@Immutable
data class TrackRecord(
    val id: Int,
    val trackerId: Int,
    val mangaId: Long,
    val remoteId: Long,
    val remoteUrl: String,
    val title: String,
    val status: Int,
    val score: Double,
    val displayScore: String,
    val lastChapterRead: Double,
    val totalChapters: Int,
)

@Immutable
data class TrackSearch(
    val remoteId: Long,
    val title: String,
    val coverUrl: String,
    val summary: String,
    val publishingStatus: String,
    val publishingType: String,
    val startDate: String,
    val totalChapters: Int,
    val trackingUrl: String,
)

data class TrackUpdate(
    val status: Int? = null,
    val score: String? = null,
    val lastChapterRead: Double? = null,
)
