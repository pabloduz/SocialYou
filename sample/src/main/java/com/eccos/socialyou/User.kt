package com.eccos.socialyou

data class User(
        val key: String,
        val name: String,
        val profile: String,
        val url: String
) {

    data class User(
            val id: Long = 0,
            val key: String = "",
            val name: String = "",
            val profile: String = "",
            val url: String= ""
    )
}
