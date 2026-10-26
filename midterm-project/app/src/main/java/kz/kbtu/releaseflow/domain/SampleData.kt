package kz.kbtu.releaseflow.domain

import kz.kbtu.releaseflow.domain.model.MobileApp
import kz.kbtu.releaseflow.domain.model.Platform
import kz.kbtu.releaseflow.domain.model.Release
import kz.kbtu.releaseflow.domain.model.ReleaseStage
import kz.kbtu.releaseflow.domain.service.ReleaseService

/**
 * Demo data: three fictional apps and six releases in different states, with histories
 * spread over the last two months so the dashboard and the statistics have something to show.
 */
object SampleData {

    private const val HOUR = 60 * 60 * 1000L
    private const val DAY = 24 * HOUR

    val apps: List<MobileApp> = listOf(
        MobileApp(1, "Nomad Pay", "kz.nomadpay.app", "kz.nomadpay.app"),
        MobileApp(2, "Dala Market", "kz.dala.market", "kz.dala.market"),
        MobileApp(3, "Steppe Fitness", "kz.steppe.fitness", "kz.steppe.fitness"),
    )

    /**
     * Builds the releases through the real service so every object is consistent,
     * then rewinds their histories with explicit timestamps relative to [now].
     */
    fun releases(service: ReleaseService, now: Long): List<Release> {
        val list = mutableListOf<Release>()

        // Nomad Pay 3.4.0 — shipped a month ago on both platforms, iOS rejected once.
        list += service.createRelease(101, 1, "3.4.0", mapOf(Platform.IOS to "340", Platform.ANDROID to "3400"), null, "Card-to-card transfers, new onboarding", createdAt = now - 41 * DAY).apply {
            checklist.forEach { toggleChecklistItem(it.id) }
            track(Platform.IOS)!!.apply {
                advance(ReleaseStage.Building("pipeline-1011"), now - 40 * DAY)
                advance(ReleaseStage.Testing("TestFlight"), now - 40 * DAY + 2 * HOUR)
                advance(ReleaseStage.Submitted, now - 38 * DAY)
                advance(ReleaseStage.InReview, now - 37 * DAY - 6 * HOUR)
                advance(ReleaseStage.Rejected("Login demo account did not work", "2.1 App Completeness"), now - 36 * DAY)
                advance(ReleaseStage.Submitted, now - 35 * DAY, "resubmitted after rejection")
                advance(ReleaseStage.InReview, now - 34 * DAY - 10 * HOUR)
                advance(ReleaseStage.Approved, now - 34 * DAY)
                advance(ReleaseStage.Live, now - 33 * DAY)
            }
            track(Platform.ANDROID)!!.apply {
                advance(ReleaseStage.Building("pipeline-1012"), now - 40 * DAY)
                advance(ReleaseStage.Testing("Internal testing"), now - 40 * DAY + 1 * HOUR)
                advance(ReleaseStage.Submitted, now - 38 * DAY)
                advance(ReleaseStage.InReview, now - 38 * DAY + 5 * HOUR)
                advance(ReleaseStage.Approved, now - 37 * DAY)
                advance(ReleaseStage.RollingOut(10), now - 36 * DAY)
                advance(ReleaseStage.RollingOut(50), now - 35 * DAY)
                advance(ReleaseStage.Live, now - 33 * DAY)
            }
        }

        // Nomad Pay 3.5.0 — iOS waiting in review for 2 days, Android rolling out 20 %.
        list += service.createRelease(102, 1, "3.5.0", mapOf(Platform.IOS to "351", Platform.ANDROID to "3510"), now + 3 * DAY, "QR payments, dark theme fixes", createdAt = now - 6 * DAY).apply {
            checklist.filter { it.platform != Platform.IOS || it.guideline == null }.forEach { toggleChecklistItem(it.id) }
            track(Platform.IOS)!!.apply {
                advance(ReleaseStage.Building("pipeline-1021"), now - 5 * DAY)
                advance(ReleaseStage.Testing("TestFlight"), now - 5 * DAY + 3 * HOUR)
                advance(ReleaseStage.Submitted, now - 2 * DAY - 4 * HOUR)
                advance(ReleaseStage.InReview, now - 2 * DAY)
            }
            track(Platform.ANDROID)!!.apply {
                advance(ReleaseStage.Building("pipeline-1022"), now - 5 * DAY)
                advance(ReleaseStage.Testing("Internal testing"), now - 5 * DAY + 1 * HOUR)
                advance(ReleaseStage.Submitted, now - 4 * DAY)
                advance(ReleaseStage.InReview, now - 4 * DAY + 6 * HOUR)
                advance(ReleaseStage.Approved, now - 3 * DAY)
                advance(ReleaseStage.RollingOut(10), now - 2 * DAY)
                advance(ReleaseStage.RollingOut(20), now - 1 * DAY)
            }
        }

        // Dala Market 1.2.0 — Android live, iOS rejected for screenshots; needs attention.
        list += service.createRelease(201, 2, "1.2.0", mapOf(Platform.IOS to "120", Platform.ANDROID to "1200"), null, "Seller chat, order tracking", createdAt = now - 10 * DAY).apply {
            checklist.filter { it.title != "App Store screenshots updated" }.forEach { toggleChecklistItem(it.id) }
            track(Platform.IOS)!!.apply {
                advance(ReleaseStage.Building("pipeline-2011"), now - 9 * DAY)
                advance(ReleaseStage.Testing("TestFlight"), now - 9 * DAY + 2 * HOUR)
                advance(ReleaseStage.Submitted, now - 7 * DAY)
                advance(ReleaseStage.InReview, now - 6 * DAY - 8 * HOUR)
                advance(ReleaseStage.Rejected("Screenshots show an outdated UI", "2.3.3 Accurate metadata"), now - 6 * DAY)
            }
            track(Platform.ANDROID)!!.apply {
                advance(ReleaseStage.Building("pipeline-2012"), now - 9 * DAY)
                advance(ReleaseStage.Testing("Internal testing"), now - 9 * DAY + 1 * HOUR)
                advance(ReleaseStage.Submitted, now - 7 * DAY)
                advance(ReleaseStage.InReview, now - 7 * DAY + 4 * HOUR)
                advance(ReleaseStage.Approved, now - 6 * DAY - 12 * HOUR)
                advance(ReleaseStage.RollingOut(10), now - 6 * DAY)
                advance(ReleaseStage.Live, now - 4 * DAY)
            }
        }

        // Dala Market 1.3.0 — planned in 5 days, still a draft with a half-done checklist.
        list += service.createRelease(202, 2, "1.3.0", mapOf(Platform.IOS to "130", Platform.ANDROID to "1300"), now + 5 * DAY, "Promo codes, Kazakh localisation", createdAt = now - 2 * DAY).apply {
            checklist.take(3).forEach { toggleChecklistItem(it.id) }
        }

        // Steppe Fitness 2.0.1 — hotfix building on both platforms right now.
        list += service.createRelease(301, 3, "2.0.1", mapOf(Platform.IOS to "2001", Platform.ANDROID to "20010"), now + 1 * DAY, "Hotfix: crash on workout summary", createdAt = now - 4 * HOUR).apply {
            checklist.forEach { toggleChecklistItem(it.id) }
            track(Platform.IOS)!!.advance(ReleaseStage.Building("pipeline-3011"), now - 3 * HOUR)
            track(Platform.ANDROID)!!.advance(ReleaseStage.Building("pipeline-3012"), now - 3 * HOUR)
        }

        // Steppe Fitness 2.0.0 — shipped 20 days ago, Android rollout was halted once.
        list += service.createRelease(302, 3, "2.0.0", mapOf(Platform.IOS to "2000", Platform.ANDROID to "20000"), null, "New workout engine", createdAt = now - 27 * DAY).apply {
            checklist.forEach { toggleChecklistItem(it.id) }
            track(Platform.IOS)!!.apply {
                advance(ReleaseStage.Building("pipeline-3021"), now - 26 * DAY)
                advance(ReleaseStage.Testing("TestFlight"), now - 26 * DAY + 2 * HOUR)
                advance(ReleaseStage.Submitted, now - 24 * DAY)
                advance(ReleaseStage.InReview, now - 23 * DAY - 2 * HOUR)
                advance(ReleaseStage.Approved, now - 22 * DAY - 20 * HOUR)
                advance(ReleaseStage.Live, now - 22 * DAY)
            }
            track(Platform.ANDROID)!!.apply {
                advance(ReleaseStage.Building("pipeline-3022"), now - 26 * DAY)
                advance(ReleaseStage.Testing("Internal testing"), now - 26 * DAY + 1 * HOUR)
                advance(ReleaseStage.Submitted, now - 24 * DAY)
                advance(ReleaseStage.InReview, now - 24 * DAY + 3 * HOUR)
                advance(ReleaseStage.Approved, now - 23 * DAY - 15 * HOUR)
                advance(ReleaseStage.RollingOut(10), now - 23 * DAY)
                advance(ReleaseStage.Halted(10), now - 22 * DAY - 12 * HOUR, "crash rate spike")
                advance(ReleaseStage.RollingOut(10), now - 21 * DAY)
                advance(ReleaseStage.RollingOut(50), now - 20 * DAY)
                advance(ReleaseStage.Live, now - 19 * DAY)
            }
        }

        return list
    }
}
