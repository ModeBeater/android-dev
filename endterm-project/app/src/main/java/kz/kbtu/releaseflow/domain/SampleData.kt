package kz.kbtu.releaseflow.domain

import kz.kbtu.releaseflow.domain.model.ChecklistItem
import kz.kbtu.releaseflow.domain.model.MobileApp
import kz.kbtu.releaseflow.domain.model.Platform
import kz.kbtu.releaseflow.domain.model.PlatformTrack
import kz.kbtu.releaseflow.domain.model.Release
import kz.kbtu.releaseflow.domain.model.ReleaseStage
import kz.kbtu.releaseflow.domain.service.ReleaseService

/**
 * Demo data inserted into the database on the first launch: three fictional apps with six
 * releases in different states, plus one real open-source app (Signal) so the live
 * App Store lookup and the GitHub releases list have something real to show.
 *
 * Apps are keyed by name here; the repository assigns database ids and wires `appId`.
 */
object SampleData {

    private const val HOUR = 60 * 60 * 1000L
    private const val DAY = 24 * HOUR

    val apps: List<MobileApp> = listOf(
        MobileApp(0, "Nomad Pay", "kz.nomadpay.app", "kz.nomadpay.app"),
        MobileApp(0, "Dala Market", "kz.dala.market", "kz.dala.market"),
        MobileApp(0, "Steppe Fitness", "kz.steppe.fitness", "kz.steppe.fitness"),
        MobileApp(0, "Signal", "org.whispersystems.signal", "org.thoughtcrime.securesms", githubRepo = "signalapp/Signal-Android"),
    )

    /**
     * Builds the releases through the real service so every object is consistent, then
     * rewrites their histories with explicit timestamps relative to [now].
     * Returns app name → releases; the repository maps names to the inserted ids.
     */
    fun releases(service: ReleaseService, now: Long): Map<String, List<Release>> {
        val result = mutableMapOf<String, MutableList<Release>>()
        fun add(appName: String, release: Release) {
            result.getOrPut(appName) { mutableListOf() } += release
        }

        // Nomad Pay 3.4.0 — shipped a month ago on both platforms, iOS rejected once.
        add(
            "Nomad Pay",
            service.newRelease(0, "3.4.0", mapOf(Platform.IOS to "340", Platform.ANDROID to "3400"), null, "Card-to-card transfers, new onboarding", createdAt = now - 41 * DAY)
                .allDone()
                .withHistory(Platform.IOS) {
                    advanced(ReleaseStage.Building("pipeline-1011"), now - 40 * DAY)
                        .advanced(ReleaseStage.Testing("TestFlight"), now - 40 * DAY + 2 * HOUR)
                        .advanced(ReleaseStage.Submitted, now - 38 * DAY)
                        .advanced(ReleaseStage.InReview, now - 37 * DAY - 6 * HOUR)
                        .advanced(ReleaseStage.Rejected("Login demo account did not work", "2.1 App Completeness"), now - 36 * DAY)
                        .advanced(ReleaseStage.Submitted, now - 35 * DAY, "resubmitted after rejection")
                        .advanced(ReleaseStage.InReview, now - 34 * DAY - 10 * HOUR)
                        .advanced(ReleaseStage.Approved, now - 34 * DAY)
                        .advanced(ReleaseStage.Live, now - 33 * DAY)
                }
                .withHistory(Platform.ANDROID) {
                    advanced(ReleaseStage.Building("pipeline-1012"), now - 40 * DAY)
                        .advanced(ReleaseStage.Testing("Internal testing"), now - 40 * DAY + 1 * HOUR)
                        .advanced(ReleaseStage.Submitted, now - 38 * DAY)
                        .advanced(ReleaseStage.InReview, now - 38 * DAY + 5 * HOUR)
                        .advanced(ReleaseStage.Approved, now - 37 * DAY)
                        .advanced(ReleaseStage.RollingOut(10), now - 36 * DAY)
                        .advanced(ReleaseStage.RollingOut(50), now - 35 * DAY)
                        .advanced(ReleaseStage.Live, now - 33 * DAY)
                },
        )

        // Nomad Pay 3.5.0 — iOS waiting in review for 2 days, Android rolling out 20 %.
        add(
            "Nomad Pay",
            service.newRelease(0, "3.5.0", mapOf(Platform.IOS to "351", Platform.ANDROID to "3510"), now + 3 * DAY, "QR payments, dark theme fixes", createdAt = now - 6 * DAY)
                .done { it.platform != Platform.IOS || it.guideline == null }
                .withHistory(Platform.IOS) {
                    advanced(ReleaseStage.Building("pipeline-1021"), now - 5 * DAY)
                        .advanced(ReleaseStage.Testing("TestFlight"), now - 5 * DAY + 3 * HOUR)
                        .advanced(ReleaseStage.Submitted, now - 2 * DAY - 4 * HOUR)
                        .advanced(ReleaseStage.InReview, now - 2 * DAY)
                }
                .withHistory(Platform.ANDROID) {
                    advanced(ReleaseStage.Building("pipeline-1022"), now - 5 * DAY)
                        .advanced(ReleaseStage.Testing("Internal testing"), now - 5 * DAY + 1 * HOUR)
                        .advanced(ReleaseStage.Submitted, now - 4 * DAY)
                        .advanced(ReleaseStage.InReview, now - 4 * DAY + 6 * HOUR)
                        .advanced(ReleaseStage.Approved, now - 3 * DAY)
                        .advanced(ReleaseStage.RollingOut(10), now - 2 * DAY)
                        .advanced(ReleaseStage.RollingOut(20), now - 1 * DAY)
                },
        )

        // Dala Market 1.2.0 — Android live, iOS rejected for screenshots; needs attention.
        add(
            "Dala Market",
            service.newRelease(0, "1.2.0", mapOf(Platform.IOS to "120", Platform.ANDROID to "1200"), null, "Seller chat, order tracking", createdAt = now - 10 * DAY)
                .done { it.title != "App Store screenshots updated" }
                .withHistory(Platform.IOS) {
                    advanced(ReleaseStage.Building("pipeline-2011"), now - 9 * DAY)
                        .advanced(ReleaseStage.Testing("TestFlight"), now - 9 * DAY + 2 * HOUR)
                        .advanced(ReleaseStage.Submitted, now - 7 * DAY)
                        .advanced(ReleaseStage.InReview, now - 6 * DAY - 8 * HOUR)
                        .advanced(ReleaseStage.Rejected("Screenshots show an outdated UI", "2.3.3 Accurate metadata"), now - 6 * DAY)
                }
                .withHistory(Platform.ANDROID) {
                    advanced(ReleaseStage.Building("pipeline-2012"), now - 9 * DAY)
                        .advanced(ReleaseStage.Testing("Internal testing"), now - 9 * DAY + 1 * HOUR)
                        .advanced(ReleaseStage.Submitted, now - 7 * DAY)
                        .advanced(ReleaseStage.InReview, now - 7 * DAY + 4 * HOUR)
                        .advanced(ReleaseStage.Approved, now - 6 * DAY - 12 * HOUR)
                        .advanced(ReleaseStage.RollingOut(10), now - 6 * DAY)
                        .advanced(ReleaseStage.Live, now - 4 * DAY)
                },
        )

        // Dala Market 1.3.0 — planned in 5 days, still a draft with a half-done checklist.
        add(
            "Dala Market",
            service.newRelease(0, "1.3.0", mapOf(Platform.IOS to "130", Platform.ANDROID to "1300"), now + 5 * DAY, "Promo codes, Kazakh localisation", createdAt = now - 2 * DAY)
                .let { release -> release.copy(checklist = release.checklist.mapIndexed { index, item -> if (index < 3) item.copy(done = true) else item }) },
        )

        // Steppe Fitness 2.0.1 — hotfix building on both platforms right now.
        add(
            "Steppe Fitness",
            service.newRelease(0, "2.0.1", mapOf(Platform.IOS to "2001", Platform.ANDROID to "20010"), now + 1 * DAY, "Hotfix: crash on workout summary", createdAt = now - 4 * HOUR)
                .allDone()
                .withHistory(Platform.IOS) { advanced(ReleaseStage.Building("pipeline-3011"), now - 3 * HOUR) }
                .withHistory(Platform.ANDROID) { advanced(ReleaseStage.Building("pipeline-3012"), now - 3 * HOUR) },
        )

        // Steppe Fitness 2.0.0 — shipped 20 days ago, Android rollout was halted once.
        add(
            "Steppe Fitness",
            service.newRelease(0, "2.0.0", mapOf(Platform.IOS to "2000", Platform.ANDROID to "20000"), null, "New workout engine", createdAt = now - 27 * DAY)
                .allDone()
                .withHistory(Platform.IOS) {
                    advanced(ReleaseStage.Building("pipeline-3021"), now - 26 * DAY)
                        .advanced(ReleaseStage.Testing("TestFlight"), now - 26 * DAY + 2 * HOUR)
                        .advanced(ReleaseStage.Submitted, now - 24 * DAY)
                        .advanced(ReleaseStage.InReview, now - 23 * DAY - 2 * HOUR)
                        .advanced(ReleaseStage.Approved, now - 22 * DAY - 20 * HOUR)
                        .advanced(ReleaseStage.Live, now - 22 * DAY)
                }
                .withHistory(Platform.ANDROID) {
                    advanced(ReleaseStage.Building("pipeline-3022"), now - 26 * DAY)
                        .advanced(ReleaseStage.Testing("Internal testing"), now - 26 * DAY + 1 * HOUR)
                        .advanced(ReleaseStage.Submitted, now - 24 * DAY)
                        .advanced(ReleaseStage.InReview, now - 24 * DAY + 3 * HOUR)
                        .advanced(ReleaseStage.Approved, now - 23 * DAY - 15 * HOUR)
                        .advanced(ReleaseStage.RollingOut(10), now - 23 * DAY)
                        .advanced(ReleaseStage.Halted(10), now - 22 * DAY - 12 * HOUR, "crash rate spike")
                        .advanced(ReleaseStage.RollingOut(10), now - 21 * DAY)
                        .advanced(ReleaseStage.RollingOut(50), now - 20 * DAY)
                        .advanced(ReleaseStage.Live, now - 19 * DAY)
                },
        )

        // Signal 7.52.0 — a real app: live on both stores two weeks ago, so the App Store lookup
        // and the GitHub releases can be compared with what is tracked here.
        add(
            "Signal",
            service.newRelease(0, "7.52.0", mapOf(Platform.IOS to "7520", Platform.ANDROID to "152000"), null, "Imported from GitHub release v7.52.0", createdAt = now - 15 * DAY)
                .allDone()
                .withHistory(Platform.IOS) {
                    advanced(ReleaseStage.Building("pipeline-4011"), now - 15 * DAY + 1 * HOUR)
                        .advanced(ReleaseStage.Testing("TestFlight"), now - 15 * DAY + 3 * HOUR)
                        .advanced(ReleaseStage.Submitted, now - 14 * DAY)
                        .advanced(ReleaseStage.InReview, now - 14 * DAY + 5 * HOUR)
                        .advanced(ReleaseStage.Approved, now - 13 * DAY)
                        .advanced(ReleaseStage.Live, now - 13 * DAY + 2 * HOUR)
                }
                .withHistory(Platform.ANDROID) {
                    advanced(ReleaseStage.Building("pipeline-4012"), now - 15 * DAY + 1 * HOUR)
                        .advanced(ReleaseStage.Testing("Internal testing"), now - 15 * DAY + 2 * HOUR)
                        .advanced(ReleaseStage.Submitted, now - 14 * DAY)
                        .advanced(ReleaseStage.InReview, now - 14 * DAY + 3 * HOUR)
                        .advanced(ReleaseStage.Approved, now - 13 * DAY - 6 * HOUR)
                        .advanced(ReleaseStage.RollingOut(10), now - 13 * DAY)
                        .advanced(ReleaseStage.RollingOut(50), now - 12 * DAY)
                        .advanced(ReleaseStage.Live, now - 11 * DAY)
                },
        )

        return result
    }

    // ---- small builders that keep the data above readable --------------------

    private fun Release.allDone(): Release = copy(checklist = checklist.map { it.copy(done = true) })

    private fun Release.done(predicate: (ChecklistItem) -> Boolean): Release =
        copy(checklist = checklist.map { if (predicate(it)) it.copy(done = true) else it })

    private fun Release.withHistory(platform: Platform, build: PlatformTrack.() -> PlatformTrack): Release =
        withTrack(requireNotNull(track(platform)).build())
}
