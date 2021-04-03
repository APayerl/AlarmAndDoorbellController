package se.payerl.alarmanddoorbellcontroller

import android.graphics.Bitmap

interface BitmapCallback {
    fun newFrame(bitmap: Bitmap);
}