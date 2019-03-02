package soko.ekibun.bangumi.api.sakugabooru.bean

data class Post(
     val id: Int = 0,
     val tags: String = "",
     val file_url: String = "",
     val preview_url: String = "",
     val source: String = "",
     val actual_preview_width: Int = 0,
     val actual_preview_height: Int = 0
)