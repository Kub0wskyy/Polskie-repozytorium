package com.polskie

import android.content.Context
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import org.jsoup.nodes.Element

class FilmanProvider(private val context: Context) : MainAPI() {
    override var mainUrl = "https://filman.cc"
    override var name = "Filman.cc"
    override val hasMainPage = true
    override var lang = "pl"
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)

    private fun getHeaders(): Map<String, String> {
        val prefs = context.getSharedPreferences("FilmanPrefs", Context.MODE_PRIVATE)
        val customCookie = prefs.getString("filman_cookie", "") ?: ""
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
        "$mainUrl/filmy-online-pl" to "Filmy",
        "$mainUrl/seriale-online-pl" to "Seriale"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page <= 1) request.data else "${request.data}/strona-$page"
        val doc = app.get(url, headers = getHeaders()).document
        val elements = doc.select(".item, .poster, .film-item, #items .item")
        val homeList = elements.mapNotNull { it.toSearchResult() }
        return newHomePageResponse(request.name, homeList)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst(".title, h3, .name")?.text() ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src") ?: this.selectFirst("img")?.attr("data-src"))
        val isTv = href.contains("/serial") || this.selectFirst(".badge-series, .series") != null
        return if (isTv) {
            newTvSeriesSearchResponse(title, href, TvType.TvSeries) { this.posterUrl = posterUrl }
        } else {
            newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/wyszukiwarka?phrase=$query"
        val doc = app.get(url, headers = getHeaders()).document
        return doc.select(".item, .film-item, #items .item").mapNotNull { it.toSearchResult() }
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url, headers = getHeaders()).document
        val title = doc.selectFirst("h1.title, .entry-title, .title")?.text()?.trim() ?: "Nieznany tytuł"
        val poster = fixUrlNull(doc.selectFirst(".poster img, .cover img, .film-poster img")?.attr("src"))
        val description = doc.selectFirst(".description, .plot, #description, .entry-content")?.text()?.trim()
        val isSeries = url.contains("/serial") || doc.select(".episode-list, #episodes, .season").isNotEmpty()

        return if (isSeries) {
            val episodes = mutableListOf<Episode>()
            doc.select(".episode-item, .episodes li, #episodes a").forEach { ep ->
                val epHref = fixUrlNull(ep.attr("href")) ?: return@forEach
                val epName = ep.text().trim()
                val seasonNum = ep.attr("data-season").toIntOrNull() ?: 1
                val epNum = ep.attr("data-episode").toIntOrNull() ?: 1
                episodes.add(Episode(epHref, epName, seasonNum, epNum))
            }
            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = poster
                this.plot = description
            }
        } else {
            newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl = poster
                this.plot = description
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
        val links = doc.select("a.link-to-video, iframe, [data-iframe], [data-url], .links a")
        for (link in links) {
            val rawUrl = link.attr("data-iframe").ifBlank { link.attr("data-url").ifBlank { link.attr("href").ifBlank { link.attr("src") } } }
            val cleanUrl = fixUrlNull(rawUrl) ?: continue
            loadExtractor(cleanUrl, mainUrl, subtitleCallback, callback)
        }
        return true
    }
}
