package soko.ekibun.bangumi.api.myanimelist

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import soko.ekibun.bangumi.api.myanimelist.bean.SearchResult

interface MyAnimeList {
    @GET("/search/prefix.json")
    fun searchPrefix(@Query("keyword") keyword: String,
                @Query("type") type: String = "anime"): Call<SearchResult>

    companion object {
        const val SERVER_API = "https://myanimelist.net"
        fun createInstance(): MyAnimeList {
            return Retrofit.Builder().baseUrl(SERVER_API)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build().create(MyAnimeList::class.java)
        }
    }
}