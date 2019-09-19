package com.eccos.socialyou

data class Spot(
        val id: Long = counter++,
        val key: String,
        val title: String,
        val date: String,
        val time: String,
        val description: String,
        val url: String
) {
    companion object {
        private var counter = 0L
    }

    data class Spot(
            val id: Long = 0,
            val key: String = "",
            val title: String = "",
            val date: String = "",
            val time:  String = "",
            val description:  String = "",
            val url:  String = ""
    )
}
