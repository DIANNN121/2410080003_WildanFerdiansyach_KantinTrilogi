package com.trilogi.kantin.ui

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trilogi.kantin.KantinViewModel
import com.trilogi.kantin.data.OrderStatus
import com.trilogi.kantin.util.menuImageRes

/** Kartu standar aplikasi. */
@Composable
fun AppCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        content = content
    )
}

/** Menampilkan aset gambar dari folder res/drawable berdasarkan kunci di database. */
@Composable
fun FoodImage(imageKey: String, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = menuImageRes(imageKey)),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.clip(RoundedCornerShape(12.dp))
    )
}

fun statusColors(status: String): Pair<Color, Color> = when (status) {
    OrderStatus.MENUNGGU -> Color(0xFFFFC107) to Color(0xFF3E2723)
    OrderStatus.DIPROSES -> Color(0xFF1976D2) to Color.White
    OrderStatus.SIAP -> Color(0xFF2E7D32) to Color.White
    else -> Color(0xFF616161) to Color.White
}

@Composable
fun StatusBadge(status: String) {
    val colors = statusColors(status)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(colors.first)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text = OrderStatus.label(status),
            color = colors.second,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/** Indikator alur pesanan: Dipesan -> Diproses -> Siap -> Selesai. */
@Composable
fun StatusStepper(status: String, modifier: Modifier = Modifier) {
    val labels = listOf("Dipesan", "Diproses", "Siap", "Selesai")
    val current = OrderStatus.step(status)
    val activeColor = MaterialTheme.colorScheme.secondary
    val idleColor = MaterialTheme.colorScheme.surfaceVariant
    val lastIndex = labels.size - 1

    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        labels.forEachIndexed { index, label ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .background(
                                when {
                                    index == 0 -> Color.Transparent
                                    index <= current -> activeColor
                                    else -> idleColor
                                }
                            )
                    )
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(if (index <= current) activeColor else idleColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (index <= current) "✓" else (index + 1).toString(),
                            color = if (index <= current) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .background(
                                when {
                                    index == lastIndex -> Color.Transparent
                                    index < current -> activeColor
                                    else -> idleColor
                                }
                            )
                    )
                }
                Text(
                    text = label,
                    fontSize = 11.sp,
                    color = if (index <= current) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (index == current) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

/** Menampilkan toast untuk setiap UiEvent dari ViewModel (aman terhadap rotasi layar). */
@Composable
fun ObserveEvent(vm: KantinViewModel, onSuccess: () -> Unit = {}) {
    val context = LocalContext.current
    val event by vm.event.collectAsState()
    LaunchedEffect(event) {
        val e = event
        if (e != null) {
            Toast.makeText(context.applicationContext, e.message, Toast.LENGTH_SHORT).show()
            vm.consumeEvent()
            if (e.success) onSuccess()
        }
    }
}
