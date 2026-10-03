package com.example.expensetracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensetracker.domain.model.Category
import com.example.expensetracker.ui.theme.categoryColors

/** Круглий значок категорії: емодзі на тональному фоні кольору категорії. */
@Composable
fun CategoryAvatar(category: Category, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    val colors = categoryColors(category)
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(colors.container),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = category.emoji, fontSize = (size.value * 0.45f).sp)
    }
}
