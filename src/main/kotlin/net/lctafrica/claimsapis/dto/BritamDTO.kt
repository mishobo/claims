package net.lctafrica.claimsapis.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.sksamuel.avro4k.ScalePrecision
import com.sksamuel.avro4k.serializer.BigDecimalSerializer
import com.sksamuel.avro4k.serializer.LocalDateSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

@Serializable
data class Req (
    val req: BritamDTO
        )


@Serializable
data class BritamDTO (
    val invoiceNumber: String,
    @Serializable(with = LocalDateSerializer::class)
    val invoiceDate: LocalDate,
    val eventType: String?,
    val providerName: String,
    val providerCode: Long,
    @Serializable(with = BigDecimalSerializer::class)
    @ScalePrecision(2, 10)
    val invoiceAmount: BigDecimal,
    val currency: String?,
    val patientName: String,
    var memberShipNo: String,
    var servicePlace: String?,
    val networkCode: Long?,
    val systemType: String?,
    val GUID: String?,
    val schemeName: String,
    val invoiceLines: List<BritamInvoiceLines>,
    val diagnosis: List<BritamDiagnosisDTO>
)

@Serializable
data class BritamInvoiceLines (
    val activityCode: String?,
    @Serializable(with = BigDecimalSerializer::class)
    @ScalePrecision(2, 10)
    val requestedPrice: BigDecimal,
    val requestedCurrency: String?,
    val activityName: String?,
    val quantity: String,
    val GUID: String?)

@Serializable
data class BritamDiagnosisDTO (
    val diagnosisCode: String,
    val description: String,
    val GUID: String?)

@JsonIgnoreProperties(ignoreUnknown = true)
@Serializable
data class BritamClaimResponse(

    @JsonProperty(value = "res")
    val res: Res
)

@JsonIgnoreProperties(ignoreUnknown = true)
@Serializable
data class Res(
    @JsonProperty(value = "invoiceId")
    val invoiceId: String,
    @JsonProperty(value = "message")
    val message: String
)