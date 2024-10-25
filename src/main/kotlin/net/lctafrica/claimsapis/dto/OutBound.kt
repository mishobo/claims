package net.lctafrica.claimsapis.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.sksamuel.avro4k.ScalePrecision
import com.sksamuel.avro4k.serializer.BigDecimalSerializer
import com.sksamuel.avro4k.serializer.LocalDateSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.LocalDate

@Serializable
data class OutBoundClaimDTO(
    val memberNumber: String,
    val benefit: String,
    val payerCode: Int,
    val diagnoses: List<OutBoundDiagnosisDTO>,
    val invoices: List<OutBoundInvoiceDTO>,
    @Serializable(with = LocalDateSerializer::class)
    val claimDate: LocalDate,
    val providerName: String,
    val providerCode: String,
    val memberName: String
)

@Serializable
data class OutBoundDiagnosisDTO(
    val icd10code: String,
    val description: String? = null
)

@Serializable
data class OutBoundInvoiceDTO(
    val invoiceNumber: String,
    @Serializable(with = BigDecimalSerializer::class)
    @ScalePrecision(2, 10)
    val total: BigDecimal,
    val items: List<OutBoundInvoiceLineDTO>
)

@Serializable
data class OutBoundInvoiceLineDTO(
    @Serializable(with = BigDecimalSerializer::class)
    @ScalePrecision(2, 10)
    val unit: BigDecimal?,
    val quantity: Int? = 1,
    @Serializable(with = BigDecimalSerializer::class)
    @ScalePrecision(2, 10)
    val amount: BigDecimal,
    val description: String,
    val itemName: String,
)
