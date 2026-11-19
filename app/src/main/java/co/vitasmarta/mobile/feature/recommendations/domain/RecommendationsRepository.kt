package co.vitasmarta.mobile.feature.recommendations.domain

import co.vitasmarta.mobile.core.common.AppResult
import java.math.BigDecimal

data class RecommendedAttraction(
    val id: String,
    val name: String,
    val explanation: String,
    val categories: List<String>,
    val saturation: String?,
    val occupancy: Double?,
    val environmentalWarning: String?,
    val alternatives: List<String>,
    val minimumPrice: BigDecimal?
)
data class RecommendationFeed(
    val mode: String,
    val degraded: Boolean,
    val personalized: Boolean,
    val transparency: String,
    val generatedByAi: Boolean,
    val environmentalStateVerified: Boolean,
    val warnings: List<String>,
    val items: List<RecommendedAttraction>
)
interface RecommendationsRepository {
    suspend fun recommendations(): AppResult<RecommendationFeed>
}
