package com.refreshsoda.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.refreshsoda.app.ui.viewmodel.CategoryProfitStat
import com.refreshsoda.app.ui.viewmodel.DayTrendStat
import com.refreshsoda.app.ui.viewmodel.ShopViewModel
import com.refreshsoda.app.ui.viewmodel.TopProductRank
import java.util.Locale

@Composable
fun AnalyticsScreen(viewModel: ShopViewModel) {
    val overallSummary by viewModel.overallProfitSummary.collectAsState()
    val categoryStats by viewModel.categoryPerformanceStats.collectAsState()
    val weeklyTrends by viewModel.weeklySalesTrend.collectAsState()
    val topProducts by viewModel.topRankedProducts.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp)
    ) {
        // SCREEN TITLE
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = viewModel.getString("analytics_title"),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Business Health, Category Performance & Profit Margins",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 4 KPI FINANCIAL CARDS
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Total Sales
                    KPICard(
                        title = viewModel.getString("today_total_sales"),
                        amount = "₹${overallSummary.totalRevenue.toInt()}",
                        subtitle = "${overallSummary.totalSalesCount} Bills",
                        badgeText = "Total Revenue",
                        badgeColor = MaterialTheme.colorScheme.primaryContainer,
                        badgeTextColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )

                    // Gross Profit & Margin %
                    KPICard(
                        title = viewModel.getString("gross_profit"),
                        amount = "₹${overallSummary.grossProfit.toInt()}",
                        subtitle = "${String.format(Locale.US, "%.1f", overallSummary.grossMarginPercent)}% Margin",
                        badgeText = "Gross Profit",
                        badgeColor = Color(0xFFDCFCE7),
                        badgeTextColor = Color(0xFF166534),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Total Expenses
                    KPICard(
                        title = viewModel.getString("tab_hisaab"),
                        amount = "₹${overallSummary.totalExpenses.toInt()}",
                        subtitle = "Shop & Material Cost",
                        badgeText = "Expenses",
                        badgeColor = Color(0xFFFFE4E6),
                        badgeTextColor = Color(0xFF9F1239),
                        modifier = Modifier.weight(1f)
                    )

                    // Net Profit
                    val netProfit = overallSummary.netProfit
                    val netColor = if (netProfit >= 0) Color(0xFF16A34A) else Color(0xFFDC2626)
                    KPICard(
                        title = viewModel.getString("net_profit"),
                        amount = "₹${netProfit.toInt()}",
                        subtitle = if (netProfit >= 0) "Net Gain" else "Loss",
                        badgeText = "Net Profit",
                        badgeColor = if (netProfit >= 0) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                        badgeTextColor = netColor,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // CHART 1: DONUT CHART - WHICH CATEGORY SELLS MOST
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = viewModel.getString("chart_category_sales"),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Category-wise sales distribution & percentage",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.PieChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (categoryStats.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No category sales recorded yet.\nMake POS sales to see live donut chart!",
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        // Donut Chart Canvas
                        CategoryDonutChart(
                            stats = categoryStats,
                            totalRevenue = overallSummary.totalRevenue,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Category Legends Flow
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            categoryStats.forEach { stat ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .background(Color(stat.colorHex), CircleShape)
                                        )
                                        Text(
                                            text = stat.category,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "(${stat.itemsSold} sold)",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "₹${stat.totalRevenue.toInt()}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(stat.colorHex).copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "${String.format(Locale.US, "%.1f", stat.percentageOfTotalSales)}%",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(stat.colorHex),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // CHART 2: 7-DAY SALES & PROFIT TWIN BAR CHART
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = viewModel.getString("chart_weekly_trend"),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Weekly comparison of Revenue vs Gross Profit",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        // Legend
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(modifier = Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                                Text("Sales", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(modifier = Modifier.size(8.dp).background(Color(0xFF10B981), CircleShape))
                                Text("Profit", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    WeeklyTwinBarChart(
                        trends = weeklyTrends,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                    )
                }
            }
        }

        // CATEGORY PROFIT & MARGIN BREAKDOWN CARDS
        item {
            Text(
                text = "मुनाफ़ा ब्रेकडाउन (Profit by Category)",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (categoryStats.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(20.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            "No profit data available yet. Add products and sales to analyze margin.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(categoryStats) { stat ->
                CategoryProfitCard(stat = stat)
            }
        }

        // TOP 5 BEST SELLING PRODUCTS LEADERBOARD
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = viewModel.getString("top_selling_products"),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Highest selling items with sales volume & profit",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (topProducts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No product sales recorded yet.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            topProducts.take(5).forEach { product ->
                                TopProductItemRow(product = product)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KPICard(
    title: String,
    amount: String,
    subtitle: String,
    badgeText: String,
    badgeColor: Color,
    badgeTextColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = badgeColor
            ) {
                Text(
                    text = badgeText,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeTextColor,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = amount,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CategoryDonutChart(
    stats: List<CategoryProfitStat>,
    totalRevenue: Double,
    modifier: Modifier = Modifier
) {
    var animationPlayed by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "donut_anim"
    )

    LaunchedEffect(Unit) {
        animationPlayed = true
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(160.dp)) {
            val strokeWidth = 28.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2
            val center = Offset(size.width / 2, size.height / 2)
            val arcSize = Size(radius * 2, radius * 2)
            val arcTopLeft = Offset(center.x - radius, center.y - radius)

            if (totalRevenue <= 0.0) {
                drawCircle(
                    color = Color.LightGray.copy(alpha = 0.3f),
                    radius = radius,
                    center = center,
                    style = Stroke(width = strokeWidth)
                )
            } else {
                var currentAngle = -90f
                stats.forEach { stat ->
                    val sweepAngle = (stat.percentageOfTotalSales.toFloat() / 100f) * 360f * progress
                    if (sweepAngle > 0f) {
                        drawArc(
                            color = Color(stat.colorHex),
                            startAngle = currentAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            topLeft = arcTopLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                        )
                        currentAngle += sweepAngle
                    }
                }
            }
        }

        // Center label inside donut hole
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "₹${totalRevenue.toInt()}",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Total Sales",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun WeeklyTwinBarChart(
    trends: List<DayTrendStat>,
    modifier: Modifier = Modifier
) {
    if (trends.isEmpty()) return

    val maxVal = trends.maxOfOrNull { maxOf(it.revenue, it.profit) }?.takeIf { it > 0 } ?: 100.0

    var animationPlayed by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "bar_anim"
    )

    LaunchedEffect(Unit) {
        animationPlayed = true
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val profitColor = Color(0xFF10B981)
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height - 24.dp.toPx() // leave room for labels
        val barGroupWidth = width / trends.size
        val barWidth = (barGroupWidth * 0.32f).coerceAtMost(16.dp.toPx())

        // Draw baseline
        drawLine(
            color = gridColor,
            start = Offset(0f, height),
            end = Offset(width, height),
            strokeWidth = 1.dp.toPx()
        )

        trends.forEachIndexed { index, day ->
            val groupCenterX = (index * barGroupWidth) + (barGroupWidth / 2)

            val revHeight = ((day.revenue / maxVal).toFloat() * height * progress).coerceAtLeast(0f)
            val profitHeight = ((day.profit / maxVal).toFloat() * height * progress).coerceAtLeast(0f)

            // Sales bar (Terracotta orange)
            val revLeft = groupCenterX - barWidth - 2.dp.toPx()
            drawRoundRect(
                color = primaryColor,
                topLeft = Offset(revLeft, height - revHeight),
                size = Size(barWidth, revHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )

            // Profit bar (Emerald green)
            val profitLeft = groupCenterX + 2.dp.toPx()
            drawRoundRect(
                color = profitColor,
                topLeft = Offset(profitLeft, height - profitHeight),
                size = Size(barWidth, profitHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )

            // Day label
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.GRAY
                    textSize = 24f
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
                drawText(day.dayLabel, groupCenterX, size.height - 4.dp.toPx(), paint)
            }
        }
    }
}

@Composable
fun CategoryProfitCard(stat: CategoryProfitStat) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .background(Color(stat.colorHex), CircleShape)
                    )
                    Text(
                        text = stat.category,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "${stat.itemsSold} sold",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFDCFCE7)
                ) {
                    Text(
                        text = "+₹${stat.grossProfit.toInt()} (${String.format(Locale.US, "%.0f", stat.marginPercent)}%)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF166534),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Revenue, Cost, Profit row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Total Sales", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("₹${stat.totalRevenue.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("Cost of Goods", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("₹${stat.totalCost.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Gross Margin", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "${String.format(Locale.US, "%.1f", stat.marginPercent)}%",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF16A34A)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Profit margin progress bar
            val progressFraction = (stat.marginPercent / 100.0).toFloat().coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = Color(0xFF10B981),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
fun TopProductItemRow(product: TopProductRank) {
    val medal = when (product.rank) {
        1 -> "🥇"
        2 -> "🥈"
        3 -> "🥉"
        else -> "#${product.rank}"
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(medal, fontSize = 16.sp)
                Column {
                    Text(
                        text = product.productName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        text = "${product.category} • ${product.qtySold} sold",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹${product.revenue.toInt()}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Profit: +₹${product.profit.toInt()}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF16A34A)
                )
            }
        }
    }
}

