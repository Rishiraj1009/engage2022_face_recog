package com.example.engage2022_face_recog

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class MissingInfo(
    var name: String? = "",
    var images: String? = "",
    var gender: String? = "",
    var contact: Long? = 0L,
    var age: Int? = 0,
    var date: String? = ""
    )
