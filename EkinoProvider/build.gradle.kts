apply(plugin = "com.android.library")
apply(plugin = "kotlin-android")
apply(plugin = "com.lagradost.cloudstream3.gradle")

cloudstream {
    language = "pl"
    description = "Ekino-tv.pl - Polskie kino, filmy i seriale"
    authors = listOf("Community")
    status = 3
    tvTypes = listOf("Movie", "TvSeries")
}

android {
    defaultConfig {
        minSdk = 21
        compileSdk = 35
        targetSdk = 35
    }
}
