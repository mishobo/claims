package net.lctafrica.claimsapis.dto

import com.sksamuel.avro4k.ScalePrecision
import com.sksamuel.avro4k.serializer.BigDecimalSerializer
import com.sksamuel.avro4k.serializer.LocalDateSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.LocalDate

@Serializable
data class PacisDTO (
    val memberNumber: String?,
    val billId: String?,
    val providerCode: Long?,
    val benefitCode: Long?,
    @Serializable(with = BigDecimalSerializer::class)
    @ScalePrecision(2, 10)
    val totalAmount: BigDecimal?,
    @Serializable(with = LocalDateSerializer::class)
    val invoiceDate: LocalDate,
    val patientNumber: String?,
    var patientSigned: String?,
    var doctorSigned: String?,
    val documentLink: String?,
    val preAuthNumber: Long?,
    val apiBillDetails: List<PacisInvoiceLines>?
)

@Serializable
data class PacisInvoiceLines(
    val invoiceNumber: String?,
    val service: Long?,
    val hasDiagnosis: Long?,
    @Serializable(with = BigDecimalSerializer::class)
    @ScalePrecision(2, 10)
    val quantity: BigDecimal?,
    @Serializable(with = BigDecimalSerializer::class)
    @ScalePrecision(2, 10)
    val cost: BigDecimal?,
    val apiDiagnosis: List<PacisDiagnosisDTO>?
)

@Serializable
data class PacisDiagnosisDTO(
    val codingStandard: String?,
    val diagnosisName: String?,
    val diagnosisCode: String?
)