package soko.ekibun.bangumi.api.myanimelist.bean

data class SearchResult(
        val categories: List<Category>?= null
){
    data class Category(
            val type: String = "",
            val items: List<Item>? = null
    ){
        data class Item(
                val id: Int = 0,
                val type: String = "",
                val name: String = "",
                val payload: Payload? = null
        ){
            data class Payload(
                    val start_year: Int = 0,
                    val media_type: String = ""
            )
        }
    }
}