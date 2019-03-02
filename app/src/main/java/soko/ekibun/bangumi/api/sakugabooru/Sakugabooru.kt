package soko.ekibun.bangumi.api.sakugabooru

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import soko.ekibun.bangumi.api.sakugabooru.bean.Post

interface Sakugabooru {
    @GET("/post.json")
    fun getPost(@Query("tags") tags: String,
                    @Query("limit") limit: Int,
                    @Query("page") page: Int): Call<List<Post>>

    companion object {
        private const val SERVER_API = "https://www.sakugabooru.com"
        fun createInstance(): Sakugabooru {
            return Retrofit.Builder().baseUrl(SERVER_API)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build().create(Sakugabooru::class.java)
        }
    }
}