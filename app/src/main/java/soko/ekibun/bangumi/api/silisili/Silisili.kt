package soko.ekibun.bangumi.api.silisili

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface Silisili {
    @FormUrlEncoded
    @POST("/e/search/index.php")
    fun search(@Field("keyboard") keywords: String,
               @Field("tempid") tempid: Int = 1,
               @Field("show") show: String = "title",
               @Field("tbname") tbname: String = "movie"): Call<String>

    companion object {
        private const val SERVER_API = "http://www.silisili.co"
        fun createInstance(): Silisili {
            return Retrofit.Builder().baseUrl(SERVER_API)
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .build().create(Silisili::class.java)
        }
    }
}