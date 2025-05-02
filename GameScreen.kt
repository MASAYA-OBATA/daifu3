package com.example.daifu3.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.daifu3.data.Card
import com.example.daifu3.data.Player
import com.example.daifu3.data.PlayerRank
import com.example.daifu3.viewmodel.GameViewModel

@Composable
fun GameScreen(viewModel: GameViewModel) {
    val gameState by viewModel.gameState
    val players by viewModel.players
    val currentPlayerIndex by viewModel.currentPlayerIndex
    val cardsOnTable by viewModel.cardsOnTable
    val isRevolution by viewModel.isRevolution
    val selectedCards by viewModel.selectedCards
    val gameMessage by viewModel.gameMessage

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF076324))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ゲームステータス表示
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF2E7D32)
                )
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = when (gameState) {
                            GameViewModel.GameState.WAITING_FOR_START -> "大富豪ゲーム - 準備完了"
                            GameViewModel.GameState.IN_PROGRESS -> "大富豪ゲーム" +
                                    if (isRevolution) " - 革命中！" else ""
                            GameViewModel.GameState.FINISHED -> "ゲーム終了"
                        },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    if (gameMessage.isNotEmpty()) {
                        Text(
                            text = gameMessage,
                            fontSize = 16.sp,
                            color = Color.Yellow,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // CPUプレイヤー表示
            if (players.size > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (i in 1 until players.size) {
                        val player = players[i]
                        Card(
                            modifier = Modifier
                                .width(110.dp)
                                .padding(4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (i == currentPlayerIndex) Color(0xFFFFD700) else Color(0xFF388E3C)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = player.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (i == currentPlayerIndex) Color.Black else Color.White
                                )
                                Text(
                                    text = "${player.hand.size}枚",
                                    fontSize = 12.sp,
                                    color = if (i == currentPlayerIndex) Color.Black else Color.White
                                )
                                if (player.rankThisRound != null) {
                                    Text(
                                        text = when (player.rankThisRound) {
                                            PlayerRank.DAIFUGO -> "大富豪"
                                            PlayerRank.FUGO -> "富豪"
                                            PlayerRank.HEIMIN -> "平民"
                                            PlayerRank.HINMIN -> "貧民"
                                            PlayerRank.DAIHINMIN -> "大貧民"
                                        },
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Yellow
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 場のカード表示
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .padding(8.dp),
                border = BorderStroke(2.dp, Color.White),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1B5E20)
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (cardsOnTable.isEmpty()) {
                        Text(
                            text = "場にカードはありません",
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            for (card in cardsOnTable) {
                                SimpleCard(card = card)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // プレイヤーの手札表示
            if (players.isNotEmpty() && gameState == GameViewModel.GameState.IN_PROGRESS) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "あなたの手札: ${players[0].hand.size}枚",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 手札の表示
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        val playerHand = players[0].hand
                        for (card in playerHand) {
                            SimpleCard(
                                card = card,
                                isSelected = selectedCards.contains(card),
                                onClick = { viewModel.toggleCardSelection(card) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // プレイヤーのアクションボタン
                if (currentPlayerIndex == 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = { viewModel.playSelectedCards() },
                            enabled = selectedCards.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF9800),
                                disabledContainerColor = Color.Gray
                            )
                        ) {
                            Text("プレイ", fontSize = 16.sp)
                        }

                        Button(
                            onClick = { viewModel.pass() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF607D8B)
                            )
                        ) {
                            Text("パス", fontSize = 16.sp)
                        }
                    }
                } else {
                    Text(
                        text = "CPUのターン...",
                        color = Color.White,
                        fontSize = 18.sp
                    )
                }
            }

            // ゲーム開始・再開ボタン
            if (gameState == GameViewModel.GameState.WAITING_FOR_START ||
                gameState == GameViewModel.GameState.FINISHED) {
                Button(
                    onClick = { viewModel.initializeGame() },
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = if (gameState == GameViewModel.GameState.WAITING_FOR_START)
                            "ゲーム開始" else "もう一度プレイ",
                        fontSize = 18.sp
                    )
                }
            }

            // ゲーム結果表示
            if (gameState == GameViewModel.GameState.FINISHED) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFD54F)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "ゲーム結果",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        val sortedPlayers = players.sortedBy {
                            it.rankThisRound?.ordinal ?: Int.MAX_VALUE
                        }

                        for (player in sortedPlayers) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = player.name,
                                    fontSize = 16.sp,
                                    color = Color.Black
                                )

                                val rankText = when (player.rankThisRound) {
                                    PlayerRank.DAIFUGO -> "大富豪"
                                    PlayerRank.FUGO -> "富豪"
                                    PlayerRank.HEIMIN -> "平民"
                                    PlayerRank.HINMIN -> "貧民"
                                    PlayerRank.DAIHINMIN -> "大貧民"
                                    null -> "未確定"
                                }

                                Text(
                                    text = rankText,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (player.rankThisRound) {
                                        PlayerRank.DAIFUGO -> Color(0xFFFF6F00)
                                        PlayerRank.FUGO -> Color(0xFF2962FF)
                                        PlayerRank.HEIMIN -> Color(0xFF2E7D32)
                                        PlayerRank.HINMIN -> Color(0xFF6A1B9A)
                                        PlayerRank.DAIHINMIN -> Color(0xFFB71C1C)
                                        null -> Color.Gray
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SimpleCard(
    card: Card,
    isSelected: Boolean = false,
    onClick: () -> Unit = {}
) {
    val cardColor = when (card.suit) {
        Card.Suit.HEARTS, Card.Suit.DIAMONDS -> Color.Red
        Card.Suit.CLUBS, Card.Suit.SPADES -> Color.Black
        Card.Suit.JOKER -> Color.Blue
    }

    val suitSymbol = when (card.suit) {
        Card.Suit.HEARTS -> "♥"
        Card.Suit.DIAMONDS -> "♦"
        Card.Suit.CLUBS -> "♣"
        Card.Suit.SPADES -> "♠"
        Card.Suit.JOKER -> ""
    }

    val rankSymbol = when (card.rank) {
        Card.Rank.THREE -> "3"
        Card.Rank.FOUR -> "4"
        Card.Rank.FIVE -> "5"
        Card.Rank.SIX -> "6"
        Card.Rank.SEVEN -> "7"
        Card.Rank.EIGHT -> "8"
        Card.Rank.NINE -> "9"
        Card.Rank.TEN -> "10"
        Card.Rank.JACK -> "J"
        Card.Rank.QUEEN -> "Q"
        Card.Rank.KING -> "K"
        Card.Rank.ACE -> "A"
        Card.Rank.TWO -> "2"
        Card.Rank.JOKER -> "JOKER"
    }

    Card(
        modifier = Modifier
            .width(60.dp)
            .height(90.dp)
            .padding(horizontal = 2.dp)
            .offset(y = if (isSelected) (-8).dp else 0.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        border = if (isSelected) BorderStroke(2.dp, Color.Yellow) else null
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (card.isJoker) {
                    Text(
                        text = "JOKER",
                        color = cardColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = rankSymbol,
                        color = cardColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = suitSymbol,
                        color = cardColor,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}