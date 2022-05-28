
package com.example.engage2022_face_recog

import android.graphics.Rect

data class Prediction( var bbox : Rect, var label : String , var maskLabel : String = "" )