package com.polskie

import android.content.Context
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor

class EkinoProvider(private val context: Context) : MainAPI() {
    override var mainUrl = "https://ekino-tv.pl"
    override var name = "Ekino-tv.pl"
    override val hasMainPage = true
    override var lang = "pl"
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)

    private fun getHeaders(): Map<String, String> {
        val prefs = context.getSharedPreferences("EkinoPrefs", Context.MODE_PRIVATE)
        val customCookie = prefs.getString("ekino_cookie", "") ?: ""
        val headers = mutableMapOf(
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Referer" to "$mainUrl/"
        )
        if (customCookie.isNotBlank()) {
            headers["Cookie"] = customCookie
        }
        return headers
    }

    override val mainPage = mainPageOf(
        "$mainUrl/movie/cat/all" to "Ostatnio dodane filmy",
        "$mainUrl/serie/cat/all" to "Ostatnio dodane seriale"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page <= 1) request.data else "${request.data}/page/$page"
        val doc = app.get(url, headers = getHeaders()).document
        val list = doc.select(".movies-list .item, .list-items .item, .boxtitle").mapNotNull { el ->
            val a = el.selectFirst("a") ?: return@mapNotNull null
            val href = fixUrlNull(a.attr("href")) ?: return@mapNotNull null
            val title = el.selectFirst(".title, .name")?.text() ?: a.text()
            val poster = fixUrlNull(el.selectFirst("img")?.attr("src"))
            val isTv = href.contains("/serie/")
            if (isTv) {
                newTvSeriesSearchResponse(title, href, TvType.TvSeries) { this.posterUrl = poster }
            } else {
                newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = poster }
            }
        }
        return newHomePageResponse(request.name, list)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/szukaj?q=$query"
        val doc = app.get(url, headers = getHeaders()).document
        return doc.select(".item, .search-result").mapNotNull { el ->
            val a = el.selectFirst("a") ?: return@mapNotNull null
            val href = fixUrlNull(a.attr("href")) ?: return@mapNotNull null
            val title = el.selectFirst(".title, .name")?.text() ?: a.text()
            val poster = fixUrlNull(el.selectFirst("img")?.attr("src"))
            newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = poster }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url, headers = getHeaders()).document
        val title = doc.selectFirst("h1, .movie-title")?.text()?.trim() ?: "Tytuł"
        val poster = fixUrlNull(doc.selectFirst(".poster img, .movie-poster img")?.attr("src"))
        val plot = doc.selectFirst(".description, .plot, .movie-desc")?.text()?.trim()
        val isSeries = url.contains("/serie/")

        return if (isSeries) {
            val episodes = mutableListOf<Episode>()
            doc.select(".episodes a, .episode-item a").forEach { ep ->
                val href = fixUrlNull(ep.attr("href")) ?: return@forEach
                episodes.add(Episode(href, ep.text().trim()))
            }
            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = poster
                this.plot = plot
            }
        } else {
            newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl = poster
                this.plot = plot
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val doc = app.get(data, headers = getHeaders()).document
        val players = doc.select(".players a, .player-item, iframe, [data-url]")
        for (player in players) {
            val target = player.attr("data-url").ifBlank { player.attr("src").ifBlank { player.attr("href") } }
            val link = fixUrlNull(target) ?: continue
            loadExtractor(link, mainUrl, subtitleCallback, callback)
        }
        return true
    }
}
