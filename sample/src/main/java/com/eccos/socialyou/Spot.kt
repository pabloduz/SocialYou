package com.eccos.socialyou

data class Spot(
        val id: Long = counter++,
        val title: String,
        val date: String,
        val time: String,
        val description: String,
        val url: String
) {
    companion object {
        private var counter = 0L
    }
}
