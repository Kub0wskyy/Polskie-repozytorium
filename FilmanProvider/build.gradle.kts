apply(plugin = "com.android.library")
apply(plugin = "kotlin-android")
apply(plugin = "com.lagradost.cloudstream3.gradle")

cloudstream {
    language = "pl"
    description = "Filman.cc - Polskie filmy i seriale online z obsługa konta"
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
