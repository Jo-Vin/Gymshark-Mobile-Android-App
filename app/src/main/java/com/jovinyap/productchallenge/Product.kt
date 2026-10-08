package com.jovinyap.productchallenge

data class Product(
    val id: String,
    val title: String,
    val rawPrice: Long,
    val colour: String?,
    val descriptionHtml: String?,
    val imageUrl: String?,
    val imageAlt: String?,
    val labels: List<String>
)