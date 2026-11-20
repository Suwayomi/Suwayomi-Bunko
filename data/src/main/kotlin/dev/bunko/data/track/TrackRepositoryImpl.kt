/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.data.track

import com.apollographql.apollo.ApolloClient
import dev.bunko.data.ApolloAppClient
import dev.bunko.data.graphql.BindTrackMutation
import dev.bunko.data.graphql.GetMangaTrackRecordsQuery
import dev.bunko.data.graphql.GetTrackersQuery
import dev.bunko.data.graphql.LoginTrackerCredentialsMutation
import dev.bunko.data.graphql.LoginTrackerOAuthMutation
import dev.bunko.data.graphql.LogoutTrackerMutation
import dev.bunko.data.graphql.SearchTrackerQuery
import dev.bunko.data.graphql.UnbindTrackMutation
import dev.bunko.data.graphql.UpdateTrackMutation
import dev.bunko.data.graphql.fragment.TrackRecordFragment
import dev.bunko.data.graphql.fragment.TrackerFragment
import dev.bunko.data.util.dataOrRejected
import dev.bunko.data.util.toOptional
import dev.bunko.domain.track.model.TrackRecord
import dev.bunko.domain.track.model.TrackSearch
import dev.bunko.domain.track.model.TrackStatus
import dev.bunko.domain.track.model.TrackUpdate
import dev.bunko.domain.track.model.Tracker
import dev.bunko.domain.track.service.TrackRepository

class TrackRepositoryImpl(
    private val apolloAppClient: ApolloAppClient,
) : TrackRepository {
    private val apolloClient: ApolloClient
        get() = apolloAppClient.value

    override suspend fun trackers(): List<Tracker> =
        apolloClient.query(GetTrackersQuery()).execute().dataOrRejected
            .trackers.nodes.map { it.trackerFragment.toTracker() }

    override suspend fun records(mangaId: Long): List<TrackRecord> =
        apolloClient.query(GetMangaTrackRecordsQuery(mangaId.toInt())).execute().dataOrRejected
            .manga.trackRecords.nodes.map { it.trackRecordFragment.toTrackRecord() }

    override suspend fun search(trackerId: Int, query: String): List<TrackSearch> =
        apolloClient.query(SearchTrackerQuery(trackerId, query)).execute().dataOrRejected
            .searchTracker.trackSearches.map {
                TrackSearch(
                    remoteId = it.remoteId,
                    title = it.title,
                    coverUrl = it.coverUrl,
                    summary = it.summary,
                    publishingStatus = it.publishingStatus,
                    publishingType = it.publishingType,
                    startDate = it.startDate,
                    totalChapters = it.totalChapters,
                    trackingUrl = it.trackingUrl,
                )
            }

    override suspend fun bind(mangaId: Long, trackerId: Int, remoteId: Long): TrackRecord =
        apolloClient.mutation(BindTrackMutation(mangaId.toInt(), trackerId, remoteId)).execute().dataOrRejected
            .bindTrack.trackRecord.trackRecordFragment.toTrackRecord()

    override suspend fun update(recordId: Int, update: TrackUpdate): TrackRecord? =
        apolloClient.mutation(
            UpdateTrackMutation(
                recordId = recordId,
                status = update.status.toOptional(),
                scoreString = update.score.toOptional(),
                lastChapterRead = update.lastChapterRead.toOptional(),
            ),
        ).execute().dataOrRejected
            .updateTrack.trackRecord?.trackRecordFragment?.toTrackRecord()

    override suspend fun unbind(recordId: Int, deleteRemote: Boolean) {
        apolloClient.mutation(UnbindTrackMutation(recordId, deleteRemote.toOptional())).execute().dataOrRejected
    }

    override suspend fun loginOAuth(trackerId: Int, callbackUrl: String): Boolean =
        apolloClient.mutation(LoginTrackerOAuthMutation(trackerId, callbackUrl)).execute().dataOrRejected
            .loginTrackerOAuth.isLoggedIn

    override suspend fun loginCredentials(trackerId: Int, username: String, password: String): Boolean =
        apolloClient.mutation(LoginTrackerCredentialsMutation(trackerId, username, password)).execute().dataOrRejected
            .loginTrackerCredentials.isLoggedIn

    override suspend fun logout(trackerId: Int) {
        apolloClient.mutation(LogoutTrackerMutation(trackerId)).execute().dataOrRejected
    }

    private fun TrackerFragment.toTracker() =
        Tracker(
            id = id,
            name = name,
            icon = icon,
            isLoggedIn = isLoggedIn,
            isTokenExpired = isTokenExpired,
            authUrl = authUrl,
            scores = scores,
            statuses = statuses.map { TrackStatus(it.value, it.name) },
            supportsTrackDeletion = supportsTrackDeletion,
        )

    private fun TrackRecordFragment.toTrackRecord() =
        TrackRecord(
            id = id,
            trackerId = trackerId,
            mangaId = mangaId.toLong(),
            remoteId = remoteId,
            remoteUrl = remoteUrl,
            title = title,
            status = status,
            score = score,
            displayScore = displayScore,
            lastChapterRead = lastChapterRead,
            totalChapters = totalChapters,
        )
}
