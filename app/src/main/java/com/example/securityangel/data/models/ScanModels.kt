package com.example.securityangel.data.models

import com.google.gson.annotations.SerializedName

data class VtResponse(
    val data: VtData?
)

data class VtData(
    val id: String?,
    val type: String?,
    val attributes: VtAttributes?
)

data class VtAttributes(
    @SerializedName("last_analysis_stats") val stats: VtStats?,
    @SerializedName("last_analysis_results") val results: Map<String, VtEngineResult>?,
    val status: String?
)

data class VtStats(
    val malicious: Int = 0,
    val suspicious: Int = 0,
    val harmless: Int = 0,
    val undetected: Int = 0
)

data class VtEngineResult(
    @SerializedName("engine_name") val engineName: String?,
    val result: String?,
    val category: String?
)