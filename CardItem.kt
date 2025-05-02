package com.example.daifu3.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.daifu3.data.Card
import com.example.daifu3.data.Suit

@Composable
fun CardItem(card: Card, isSelected: Boolean, onClick: () -> Unit) {
    val cardColor = when (card.suit) {
        Suit.HEARTS, Suit.DIAMONDS -> Color.Red
        Suit.SPADES, Suit.CLUBS -> Color.Black
        Suit.JOKER -> Color.Blue
        else -> Color.Black
    }

    // 選択状態によって背景色と枠線を変える
    val backgroundColor = if (isSelected) Color.Cyan.copy(alpha = 0.3f) else Color.White
    val borderColor = if (isSelected) Color.Cyan else Color.Gray
    val borderWidth = if (isSelected) 3.dp else 1.dp
    
    // カード角を丸くする
    val shape = RoundedCornerShape(8.dp)

    Box(
        modifier = Modifier
            .padding(4.dp)
            .size(width = 56.dp, height = 80.dp)
            .shadow(2.dp, shape)
            .background(backgroundColor, shape)
            .border(BorderStroke(borderWidth, borderColor), shape)
            .clickable(
                indication = rememberRipple(bounded = true, color = if (isSelected) Color.Red else Color.Cyan),
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize().padding(4.dp)
        ) {
            if (card.isJoker) {
                // ジョーカーの場合は特別な表示
                Text(
                    text = "JOKER",
                    color = Color.Blue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Icon(
                    imageVector = Icons.Default.Star, 
                    contentDescription = "Joker",
                    tint = Color.Blue,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                // 通常のカードの場合はスートとランクをはっきりと表示
                // 左上
                Text(
                    text = "${card.suit.symbol}${card.rank.symbol}",
                    color = cardColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.Start)
                )
                
                // 中央にスート記号を大きく
                Text(
                    text = card.suit.symbol,
                    color = cardColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
                
                // 右下
                Text(
                    text = "${card.rank.symbol}${card.suit.symbol}",
                    color = cardColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }

        // 選択されている場合は目立つマーカーを追加
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
                    .border(
                        BorderStroke(2.dp, Color.Cyan),
                        shape = shape
                    )
            )
        }
    }
}