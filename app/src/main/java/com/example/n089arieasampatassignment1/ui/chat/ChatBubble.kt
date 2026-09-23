package com.example.n089arieasampatassignment1.ui.chat

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import com.example.n089arieasampatassignment1.model.ChatMessage

@Composable
fun ChatBubble(
    message: ChatMessage
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                vertical = 4.dp,
                horizontal = 8.dp
            ),
        horizontalArrangement =
            if (message.isUser) {
                androidx.compose.foundation.layout
                    .Arrangement.End
            } else {
                androidx.compose.foundation.layout
                    .Arrangement.Start
            },
        verticalAlignment = Alignment.CenterVertically
    ) {

        Card(
            modifier = Modifier.fillMaxWidth(
                0.82f
            ),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor =
                    if (message.isUser) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme
                            .surfaceVariant
                    }
            )
        ) {

            Text(
                text = message.text,
                modifier = Modifier.padding(14.dp),
                color =
                    if (message.isUser) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                    },
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
