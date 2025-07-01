package com.Aman.myapplication


import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = Color(0xFFBB86FC),
            background = Color(0xFF121212),
            onBackground = Color.White,
            onPrimary = Color.Black,
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF6200EE),
            background = Color.White,
            onBackground = Color.Black,
            onPrimary = Color.White,
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
