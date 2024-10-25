package net.lctafrica.claimsapis.dto

import com.sksamuel.avro4k.ScalePrecision
import com.sksamuel.avro4k.serializer.BigDecimalSerializer
import com.sksamuel.avro4k.serializer.LocalDateSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.LocalDate

@Serializable
data class LiaisonDTO(
    val claimId: Long,
    val memberNo: String,
    val benefit: String,
    @Serializable(with = LocalDateSerializer::class)
    val invoiceDate: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val dateReceived: LocalDate,
    val provider: String,
    val invoices: List<LiaisonInvoicesDto>,
    val diagnosis: List<LiaisonDiagnosisDto>,
    val document: List<LiaisonDocumentDto>
)

@Serializable
data class LiaisonDocumentDto (
    val type: String,
    val path: String
    )

@Serializable
data class LiaisonDiagnosisDto (
    val icd10: String,
    val description: String
    )

@Serializable
data class LiaisonInvoicesDto(
    val invoiceNo: String,
    @Serializable(with = BigDecimalSerializer::class)
    @ScalePrecision(2, 10)
    val claimAmount: BigDecimal,
    val lineItems: List<LiaisonInvoiceLinesDto>
)

@Serializable
data class LiaisonInvoiceLinesDto(
    @Serializable(with = BigDecimalSerializer::class)
    @ScalePrecision(2, 10)
    val unit: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    @ScalePrecision(2, 10)
    val quantity: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    @ScalePrecision(2, 10)
    val amount: BigDecimal,
    val description: String,
    val claimService: Long
    )
