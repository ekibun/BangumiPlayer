package soko.ekibun.bangumi.api.dilidili

import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface Dilidili {
    @FormUrlEncoded
    @POST("/Aftersome/api_sosuoarctype")
    fun search(@Field("keywords") keywords: String,
               @Field("pagesize") pagesize: Int = 10,
               @Field("page") page: Int = 0): Call<JsonObject>

    companion object {
        private const val SERVER_API = "http://usr.005.tv"
        fun createInstance(): Dilidili {
            return Retrofit.Builder().baseUrl(SERVER_API)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build().create(Dilidili::class.java)
        }
    }
}