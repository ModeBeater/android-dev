package kz.kbtu.releaseflow.domain.model

/** Semantic-ish version comparison: "3.10.0" > "3.9.2", "1.2" == "1.2.0". */
fun compareVersions(a: String, b: String): Int {
    val pa = versionParts(a)
    val pb = versionParts(b)
    for (i in 0 until maxOf(pa.size, pb.size)) {
        val x = pa.getOrElse(i) { 0 }
        val y = pb.getOrElse(i) { 0 }
        if (x != y) return x.compareTo(y)
    }
    return 0
}

private fun versionParts(version: String): List<Int> =
    version.trim().split('.').map { part -> part.takeWhile { it.isDigit() }.toIntOrNull() ?: 0 }

/** The first "1.2" / "1.2.3" group in a tag such as "v1.2.3", "release/2.0.1" or "android-7.52.1". */
fun versionFromTag(tag: String): String? = Regex("""\d+\.\d+(\.\d+)?""").find(tag)?.value

/** Compares the live store version with the newest version shipped on `platform`. */
fun compareWithStore(storeVersion: String, releases: List<Release>, platform: Platform): StoreComparison {
    val shipped = releases
        .filter { it.track(platform)?.stage == ReleaseStage.Live }
        .maxWithOrNull { x, y -> compareVersions(x.version, y.version) }
        ?: return StoreComparison.NothingTracked
    return when {
        compareVersions(storeVersion, shipped.version) > 0 -> StoreComparison.StoreAhead
        compareVersions(storeVersion, shipped.version) < 0 -> StoreComparison.StoreBehind
        else -> StoreComparison.Matches
    }
}
