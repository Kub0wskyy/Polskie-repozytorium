package com.polskie

import android.content.Context
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor

class ZaluknijProvider(private val context: Context) : MainAPI() {
    private val prefs = context.getSharedPreferences("ZaluknijPrefs", Context.MODE_PRIVATE)
    override var mainUrl = prefs.getString("zaluknij_domain", "https://zaluknij.cc") ?: "https://zaluknij.cc"
    override var name = "Zaluknij.cc"
    override val hasMainPage = true
    override var lang = "pl"
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)

    private fun getHeaders(): Map<String, String> {
        val customCookie = prefs.getString("zaluknij_cookie", "") ?: ""
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
        "$mainUrl/filmy-online" to "Filmy",
        "$mainUrl/seriale-online" to "Seriale"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page <= 1) request.data else "${request.data}?page=$page"
        val doc = app.get(url, headers = getHeaders()).document
        val list = doc.select(".film-item, .item, .poster-item").mapNotNull { el ->
            val a = el.selectFirst("a") ?: return@mapNotNull null
            val href = fixUrlNull(a.attr("href")) ?: return@mapNotNull null
            val title = el.selectFirst(".title, h3, .name")?.text() ?: a.text()
            val poster = fixUrlNull(el.selectFirst("img")?.attr("src") ?: el.selectFirst("img")?.attr("data-src"))
            val isTv = href.contains("/serial")
            if (isTv) {
                newTvSeriesSearchResponse(title, href, TvType.TvSeries) { this.posterUrl = poster }
            } else {
                newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = poster }
            }
        }
        return newHomePageResponse(request.name, list)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/szukaj?query=$query"
        val doc = app.get(url, headers = getHeaders()).document
        return doc.select(".film-item, .item").mapNotNull { el ->
            val a = el.selectFirst("a") ?: return@mapNotNull null
            val href = fixUrlNull(a.attr("href")) ?: return@mapNotNull null
            val title = el.selectFirst(".title")?.text() ?: a.text()
            val poster = fixUrlNull(el.selectFirst("img")?.attr("src"))
            newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = poster }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url, headers = getHeaders()).document
        val title = doc.selectFirst("h1.title, .film-title")?.text()?.trim() ?: "Tytuł"
        val poster = fixUrlNull(doc.selectFirst(".poster img, .film-poster img")?.attr("src"))
        val plot = doc.selectFirst(".description, .film-desc")?.text()?.trim()
        val isSeries = url.contains("/serial")

        return if (isSeries) {
            val episodes = mutableListOf<Episode>()
            doc.select(".episode-item a, .episodes a").forEach { ep ->
                val epHref = fixUrlNull(ep.attr("href")) ?: return@forEach
                episodes.add(Episode(epHref, ep.text().trim()))
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
        val players = doc.select(".player-links a, iframe, [data-iframe]")
        for (item in players) {
            val target = item.attr("data-iframe").ifBlank { item.attr("src").ifBlank { item.attr("href") } }
            val link = fixUrlNull(target) ?: continue
            loadExtractor(link, mainUrl, subtitleCallback, callback)
        }
        return true
    }
}
