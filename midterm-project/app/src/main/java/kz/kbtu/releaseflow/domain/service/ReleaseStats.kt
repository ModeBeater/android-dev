package kz.kbtu.releaseflow.domain.service

import kz.kbtu.releaseflow.domain.model.MobileApp
import kz.kbtu.releaseflow.domain.model.Platform
import kz.kbtu.releaseflow.domain.model.Release
import kz.kbtu.releaseflow.domain.model.ReleaseStage

/** One rejection with enough context to show it in a list. */
data class RejectionRecord(
    val appName: String,
    val version: String,
    val platform: Platform,
    val reason: String,
    val guideline: String?,
    val at: Long,
)

/** Numbers for the Stats screen, computed from the stage histories. */
data class ReleaseStatistics(
    val averageReviewMs: Map<Platform, Long?>,
    val longestReviewMs: Long?,
    val reviewCycles: Int,
    val rejections: List<RejectionRecord>,
    val approvalRate: Double?,
    val releasesPerMonth: List<Pair<String, Int>>,   // "2026-09" to count, oldest first
    val liveReleases: Int,
)

object ReleaseStats {

    fun compute(apps: List<MobileApp>, releases: List<Release>): ReleaseStatistics {
        val appNames = apps.associate { it.id to it.name }
        val durationsByPlatform = Platform.entries.associateWith { mutableListOf<Long>() }
        val rejections = mutableListOf<RejectionRecord>()
        var approvals = 0

        for (release in releases) {
            for (track in release.tracks.values) {
                durationsByPlatform.getValue(track.platform) += track.reviewDurations()
                for (event in track.history) {
                    when (val stage = event.stage) {
                        is ReleaseStage.Rejected -> rejections += RejectionRecord(
                            appName = appNames[release.appId] ?: "?",
                            version = release.version,
                            platform = track.platform,
                            reason = stage.reason,
                            guideline = stage.guideline,
                            at = event.at,
                        )
                        ReleaseStage.Approved -> approvals++
                        else -> Unit
                    }
                }
            }
        }

        val allDurations = durationsByPlatform.values.flatten()
        val verdicts = approvals + rejections.size

        return ReleaseStatistics(
            averageReviewMs = durationsByPlatform.mapValues { (_, list) -> if (list.isEmpty()) null else list.average().toLong() },
            longestReviewMs = allDurations.maxOrNull(),
            reviewCycles = allDurations.size,
            rejections = rejections.sortedByDescending { it.at },
            approvalRate = if (verdicts == 0) null else approvals.toDouble() / verdicts,
            releasesPerMonth = releases
                .groupingBy { monthKey(it.createdAt) }
                .eachCount()
                .entries
                .sortedBy { it.key }
                .map { it.key to it.value },
            liveReleases = releases.count { release -> release.tracks.values.any { it.stage == ReleaseStage.Live } },
        )
    }

    private fun monthKey(epochMs: Long): String {
        val date = java.time.Instant.ofEpochMilli(epochMs).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        return "%04d-%02d".format(date.year, date.monthValue)
    }
}
