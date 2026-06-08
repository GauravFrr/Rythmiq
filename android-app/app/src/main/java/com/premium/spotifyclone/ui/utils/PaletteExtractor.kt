package com.premium.spotifyclone.ui.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun extractDominantColor(context: Context, imageUrl: String): Color? {
    return withContext(Dispatchers.IO) {
        try {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(imageUrl)
                .allowHardware(false) // Hardware bitmaps don't work with Palette
                .build()

            val result = loader.execute(request)
            if (result is SuccessResult) {
                val drawable = result.drawable
                val bitmap = (drawable as? BitmapDrawable)?.bitmap
                
                bitmap?.let { b ->
                    val palette = Palette.from(b).generate()
                    // Prefer Dark Muted -> Dark Vibrant -> Dominant
                    val colorInt = palette.getDarkMutedColor(
                        palette.getDarkVibrantColor(
                            palette.getDominantColor(android.graphics.Color.DKGRAY)
                        )
                    )
                    // Blend heavily with black for a rich, dark premium aesthetic
                    val darkenedColor = androidx.core.graphics.ColorUtils.blendARGB(colorInt, android.graphics.Color.BLACK, 0.65f)
                    Color(darkenedColor)
                }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
