package net.lctafrica.claimsapis.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.sksamuel.avro4k.ScalePrecision
import com.sksamuel.avro4k.serializer.BigDecimalSerializer
import com.sksamuel.avro4k.serializer.LocalDateSerializer
import kotlinx.serialization.Serializable
import lombok.RequiredArgsConstructor
import kotlinx.serialization.descriptors.PrimitiveKind
import org.dom4j.Branch
import org.hibernate.annotations.CreationTimestamp
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import javax.validation.constraints.DecimalMin
import kotlin.collections.ArrayList
import net.lctafrica.claimsapis.enums.BenefitType
import net.lctafrica.claimsapis.enums.CapitationPeriod
import net.lctafrica.claimsapis.enums.CapitationType


data class StartVisitDTO(
    var memberNumber: String,
    var memberName: String,
    var hospitalProviderId: Long,
    var beneficiaryId: Long?,
    var staffId: String,
    var staffName: String,
    var aggregateId: String?,
    var benefitName: String,
    var beneficiaryType: String?,
    var categoryId: String,
    var payerId: String,
    var payerName: String?,
    var policyNumber: String?,
    var balanceAmount: BigDecimal,
    var benefitId: Long,
    var billingStation: BillingStation?,
    var providerMiddleware: MIDDLEWARENAME?,
    var visitType: VisitType? = VisitType.ONLINE,
    var offSystemReason: OffSystemReason?,
    var reimbursementProvider: String?,
    var reimbursementInvoiceDate: LocalDate?,
    var offlctInvoiceDate: LocalDate?,
    var reimbursementReason: String?,
    var facilityType:FacilityType? = FacilityType.SINGLE

    )

data class ReverseInvoiceDTO(
    var visitNumber: Long?,
    var invoiceNumber: String?,
)

data class ReverseLineItemDTO(
    var visitNumber: Long?,
    var invoiceNumber: String?,
    var invoiceId: Long?,
)

data class OffLCTInvoiceDTO(
    var invoiceNumber: String?,
    var amount: Long?,
    var service: String?
)

data class SaveAndCloseVisitDTO(
    var id: Long,
    var totalInvoiceAmount: BigDecimal,
    var invoiceNumber: String,
    var diagnosis: List<Diagnosis>? = null,
    var hospitalProviderId: Long,
    var billingStation: BillingStation?,
    var invoices: List<OffLCTInvoiceDTO>? = null,

//

    var balanceAmount: BigDecimal?,
    var aggregateId: String?,
    var claimProcessStatus: String?,
    var memberNumber: String?,
    var staffId: String?,
    var staffName: String?,
    var status: String?,
    var middlewareStatus: String?,
    var benefitName: String?,
    var beneficiaryType: String?,
    var categoryId: String?,
    var payerId: String?,
    var payerName: String?,
    var policyNumber: String?,
    var benefitId: Long?
)

data class InvoiceDTO(
    var hospitalProviderId: Long,
    var invoiceNumber: String,
    val visitNumber: Long,
    val totalInvoiceAmount: BigDecimal?
)

@JsonIgnoreProperties(
    "success",
    "msg",
    "benefitMngtStatus",
    "underwriterStatus",
    "createdAt",
    "updatedAt"
)
data class ClaimDTO(
    @JsonProperty("id")
    var id: Long = 0,

    @JsonProperty("memberNumber")
    var memberNumber: String? = null,

    @JsonProperty("hospitalProviderId")
    var hospitalProviderId: Long? = null,

    @JsonProperty("staffId")
    var staffId: Long? = null,

    @JsonProperty("staffName")
    var staffName: String? = null,

    @JsonProperty("aggregateID")
    var aggregateID: String? = null,

    @JsonProperty("categoryId")
    var categoryId: String? = null,

    @JsonProperty("benefitName")
    var benefitName: String? = null,

    @JsonProperty("policy")
    var policy: String? = null,

    @JsonProperty("policyNumber")
    var policyNumber: String? = null,

    @JsonProperty("beneficiaryType")
    var beneficiaryType: String? = null,

    @JsonProperty("totalInvoiceAmount")
    var totalInvoiceAmount: BigDecimal? = null,

    @JsonProperty("status")
    var status: Status? = Status.ACTIVE,

    @JsonProperty("diagnosis")
    var diagnosis: List<Diagnosis>? = null
)

@JsonIgnoreProperties("success", "msg")
data class ClaimList(
    @JsonProperty("data")
    val data: List<ClaimDTO>?
)

data class VisitDTO(
    var id: Long = 0,
    var memberNumber: String? = null,
    val memberName: String?,
    var hospitalProviderId: Long? = null,
    var staffId: String? = null,
    var staffName: String? = null,
    var aggregateID: String? = null,
    var totalInvoiceAmount: BigDecimal? = null,
    var status: Status? = Status.ACTIVE,
    var claimProcessStatus: ClaimProcessStatus? = ClaimProcessStatus.UNPROCESSED,
    var invoiceLines: List<InvoiceLine>? = null,
    var diagnosis: List<Diagnosis>? = null,
    var beneficiaryType: String? = null,
    var benefitName: String? = null,
    var categoryId: String? = null,
    var payerId: String? = null,
    var payerName: String? = null,
    var policyNumber: String? = null,
    val beneficiaryId: Long? = null,
    val benefitId: Long?,
    val visitType: VisitType?,
    val offSystemReason: OffSystemReason?,
    val reimbursementProvider: String?,
    val reimbursementInvoiceDate: LocalDate?,
    val reimbursementReason: String?,
    var balanceAmount: BigDecimal,
    var invoiceNumber: String? = null,
    val aggregateId: String,
    val preAuth: PreAuthData? = null,
    val invoices: MutableList<Invoice>? = null,
    val scheme: PlanData? = null,
    val createdAt: String? = null
    )

data class OffLctVisitDTO(
    var id: Long = 0,
    var memberNumber: String? = null,
    var memberName: String? = null,
    var hospitalProviderId: Long? = null,
    var staffId: String? = null,
    var staffName: String? = null,
    var aggregateID: String? = null,
    var totalInvoiceAmount: BigDecimal? = null,
    var status: Status? = Status.ACTIVE,
    var claimProcessStatus: ClaimProcessStatus? = ClaimProcessStatus.UNPROCESSED,
    var invoiceLines: List<InvoiceLine>? = null,
    var diagnosis: List<Diagnosis>? = null,
    var beneficiaryType: String? = null,
    var benefitName: String? = null,
    var categoryId: String? = null,
    var payerId: String? = null,
    var payerName: String? = null,
    var policyNumber: String? = null,
    var providerName: String? = null,
    var invoiceNumber: String? = null,
    var createdAt: LocalDate? = null,

    )

data class ReimbursementDTO(
    var id: Long = 0,
    var memberNumber: String? = null,
    var memberName: String? = null,
    var hospitalProviderId: Long? = null,
    var staffId: String? = null,
    var staffName: String? = null,
    var aggregateID: String? = null,
    var totalInvoiceAmount: BigDecimal? = null,
    var status: Status? = Status.ACTIVE,
    var claimProcessStatus: ClaimProcessStatus? = ClaimProcessStatus.UNPROCESSED,
    var invoiceLines: List<InvoiceLine>? = null,
    var diagnosis: List<Diagnosis>? = null,
    var beneficiaryType: String? = null,
    var benefitName: String? = null,
    var categoryId: String? = null,
    var payerId: String? = null,
    var payerName: String? = null,
    var policyNumber: String? = null,
    var providerName: String? = null,
    var invoiceNumber: String? = null

)

data class MemberStatementDTO(
    val memberName: String?,
    val memberNumber: String?,
    val benefitName: String?,
    val payerId: String?,
    val totalInvoiceAmount: BigDecimal?,
    val invoiceNumber: String?,
    val createdAt: String?,
    val hospitalProviderId: String?,
    val aggregateId: String?
)

data class VisitIntergration(
    var id: Long = 0,
    var memberNumber: String? = null,
    var hospitalProviderId: Long? = null,
    var staffId: String? = null,
    var staffName: String? = null,
    var aggregateID: String? = null,
    var totalInvoiceAmount: BigDecimal? = null,
    var status: Status? = Status.ACTIVE,
    var claimProcessStatus: ClaimProcessStatus? = ClaimProcessStatus.UNPROCESSED,
    var beneficiaryType: String? = null,
    var benefitName: String? = null,
    var categoryId: String? = null,
    var payerId: String? = null,
    var payerName: String? = null,
    var policyNumber: String? = null
)

data class LineItemDTO(
    val itemDescription: String?,
    @field:DecimalMin(
        value = "0.0",
        inclusive = false,
        message = "Line item amount must be more than zero"
    )
    val itemAmount: BigDecimal?,
    val invoiceNumber: String,
    val lineType: String?,
    val visitId: Long?,
    val providerId: Long?,
    val itemCode: String?,
    val itemQuantity: Long?,
    val itemUnitPrice: String?,
    var providerName: String?,
    var type: DocumentType?,
    var fileUrl: String? = null,
)

data class LineItemsArray(
    val invoiceNumber: String,
    val providerId: Long?,
    var lineItems: List<LineItemDTO>? = null,
    var documents: List<SaveDocumentDTO>? = null,

)
data class MultipleBillingInvoice(
    val invoiceNumber: String,
    val totalAmount: BigDecimal,
    val providerId: Long,
    val visitId: Long,
    var lineItems: List<LineItemDTO>? = null,
)

data class MultipleLineItemsArray(
    val invoiceNumber: String,
    val totalAmount: BigDecimal,
    val providerId: Long,
    val visitId: Long,
    var lineItems: List<LineItemDTO>? = null,
    )

data class MultipleStationLineItemDTO(
    val itemDescription: String?,
    @field:DecimalMin(
        value = "0.0",
        inclusive = false,
        message = "Line item amount must be more than zero"
    )
    val itemAmount: BigDecimal,
    val lineType: String,
    val visitId: Long,
    var hospitalProviderId: Long,
    var diagnosis: List<Diagnosis>? = null,
    val invoiceNumber: String,
    var documents: List<SaveDocumentDTO>? = null,
)

data class removeLineItemDTO(
    val lineItemId: String,
)

data class ClinicalInformationDTO(
    val visitId: Long,
    val itemDescription: String,
    val icd10code: String?

)

data class DiagnosisItemDTO(
    val visitId: Long,
    val description: String,
    val icd10code: String?
)

data class CancelVisitDTO(
    val visitId: Long,
    val reason: String?
)

data class DiagnosisDetailDTO(
    var icd10code: String?,
    var name: String,
)

data class DoctorDetailDTO(
    var icd10code: String?,
    var name: String,
)

data class ClaimStatusDTO(
    var message: String,
    val status: String
)

class CreateBenefitDTO(
    var aggregateId: String,
    val benefitName: String,
    val subBenefits: Set<SubBenefitDTO>,
    val beneficiaries: Set<CreateBeneficiaryDTO2>,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val status: String = "ACTIVE",
    @Serializable(with = BigDecimalSerializer::class)
    @ScalePrecision(2, 10)
    val suspensionThreshold: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    @ScalePrecision(2, 10)
    val balance: BigDecimal,
    val policyNumber: String,
    val payer: SchemePayerDTO,
    val categoryId: Long,
    val benefitId: Long,
    val catalogId: Long,
    var jicEntityId: Int?,
    var apaEntityId: Int?,
    var manualSubBenefitParentId:Long?,
    var benefitType: BenefitType? = null,
    var capitationType: CapitationType? = null,
    var capitationPeriod: CapitationPeriod? = null,
    var capitationMaxVisitCount:Int = 0,
    val requireBeneficiaryToSelectProvider: Boolean? = false,
    val daysOfAdmissionLimit: Int = 0,
    @Serializable(with = BigDecimalSerializer::class)
    @ScalePrecision(2, 10)
    val amountPerDayLimit: BigDecimal = BigDecimal(0.00)
)

@Serializable
data class CreateBeneficiaryDTO2(
    val id: Long,
    val name: String,
    val memberNumber: String,
    val beneficiaryType: MemberType,
    val email: String?,
    val phoneNumber: String?,
    val gender: Gender,
    val catalogId: Long
)

@Serializable
data class SubBenefitDTO(
    var name: String,
    val status: String = "ACTIVE",
    @Serializable(with = BigDecimalSerializer::class)
    @ScalePrecision(2, 10)
    val balance: BigDecimal,
    @Serializable(with = LocalDateSerializer::class)
    val startDate: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val endDate: LocalDate,
    @Serializable(with = BigDecimalSerializer::class)
    @ScalePrecision(2, 10)
    val suspensionThreshold: BigDecimal,
    val benefitId: Long,
    val gender: ApplicableGender,
    val memberType: ApplicableMember,
    val catalogId: Long
)

enum class BillingStation {
    MULTIPLE, SINGLE
}

enum class Gender { MALE, FEMALE }
enum class ApplicableGender { ALL, MALE, FEMALE }
enum class MemberType { PRINCIPAL, SPOUSE, CHILD, PARENT }
enum class DocumentType { INVOICE, CLAIM, OTHER }
enum class ApplicableMember { ALL, PRINCIPAL, SPOUSE, CHILD, PARENT, PRINCIPAL_AND_SPOUSE }

@Serializable
data class CreateBeneficiaryDTO(
    val id: Long,
    val name: String,
    val memberNumber: String,
    val email: String?,
    val phoneNumber: String?,
    val gender: Gender,
    val type: MemberType
)

@Serializable
data class SchemePayerDTO(
    val payerId: Long,
    val payerName: String
)

data class ConsumeBenefitDTO(
    val amount: BigDecimal,
    val aggregateId: String,
    val benefitId: Long,
    val memberNumber: String,
    val visitNumber: Long?
)
data class ReverseBenefitDTO(
    val amount: BigDecimal,
    val aggregateId: String,
    val benefitId: Long,
    val memberNumber: String,
    val visitNumber: Long?
)

data class ConsumePreAuthDTO(
    val amount: BigDecimal,
    val aggregateId: String,
    val benefitId: Long,
    val memberNumber: String,
    val visitNumber: Long?
)

data class CloseIntergratedWebClaimDTO(
    val encounterId: Long? = null,
    val amount: BigDecimal? = null,
    var invoiceNumber: String? = null,
    var claimRef: String? = null,
    var providerName: String? = null,
    var memberNumber: String? = null,

    )

data class IntergratedInvoiceProviderDTO(
    val encounterId: Long,
    var invoiceNumber: String,
    var providerName: String,
    )

data class PreAuthDTO(
    val id: Long?,
    val requestAmount: BigDecimal,
    val aggregateId: String,
    val benefitId: Long,
    val payerId: Long,
    val providerId: Long,
    val visitNumber: Long,
    val requester: String,
    val notes: String,
    val memberNumber: String,
    val memberName: String,
    val benefitName: String?,
    val draft: Boolean?,
    val service: String?,
    val requestType: String?,
    val reference: String?,
    val diagnosisDescriptionValue: String?,
    val medProcedureDescriptionValue: String?,
    val scheme: String?,
    val payer: String?,
)

data class AuthorizePreAuthDTO(val id: Long, val amount: BigDecimal, val notes: String)

data class ClaimImport(
    val claims: List<ClaimMigrationDTO>
)

data class ClaimMigrationDTO(
    val amount: BigDecimal,
    val date: LocalDate,
    val memberNumber: String,
    val benefit: String
)

data class ProviderResponseDto(
    @JsonProperty("data")
    val `data`: ProviderData?,
    @JsonProperty("msg")
    val msg: String?,
    @JsonProperty("success")
    val success: Boolean
)

data class PreauthResponseDto(
    @JsonProperty("data")
    val data: PreAuthData?,
    @JsonProperty("msg")
    val msg: String?,
    @JsonProperty("success")
    val success: Boolean
)

data class InvoiceResponseDto(
    @JsonProperty("data")
    val data: MutableList<Invoice>?,
    @JsonProperty("msg")
    val msg: String?,
    @JsonProperty("success")
    val success: Boolean
)

data class PlanResponseDto(
    @JsonProperty("data")
    val data: PlanData?,
    @JsonProperty("msg")
    val msg: String?,
    @JsonProperty("success")
    val success: Boolean
)

data class PayerResponseDto(
    @JsonProperty("data")
    val data: Payer?,
    @JsonProperty("msg")
    val msg: String?,
    @JsonProperty("success")
    val success: Boolean
)

data class PlanData(
    @JsonProperty("id")
    val id: String?,
    @JsonProperty("name")
    val name: String?,
)

data class PreAuthData(
    @JsonProperty("id")
    val id: Long?,
    @JsonProperty("status")
    val status: String?,
    @JsonProperty("requestAmount")
    val createdOn: Long?,
    @JsonProperty("authorizedAmount")
    val authorizedAmount: Long?,
    @JsonProperty("memberNumber")
    val memberNumber: String?,
    @JsonProperty("memberName")
    val memberName: String?,
    @JsonProperty("benefitName")
    val benefitName: String?,
    @JsonProperty("service")
    val service: String?,
    @JsonProperty("reference")
    val reference: String?
)

data class ProviderData(
    @JsonProperty("baseUrl")
    val baseUrl: String?,
    @JsonProperty("billType")
    val billType: String?,
    @JsonProperty("createdOn")
    val createdOn: String?,
    @JsonProperty("id")
    val id: Long,
    @JsonProperty("latitude")
    val latitude: Double,
    @JsonProperty("longitude")
    val longitude: Double,
    @JsonProperty("mainFacility")
    val mainFacility: ProviderMainFacility?,
    @JsonProperty("name")
    val name: String,
    @JsonProperty("region")
    val region: Region,
    @JsonProperty("tier")
    val tier: String?
)

data class ProviderMainFacility(
    @JsonProperty("baseUrl")
    val baseUrl: String?,
    @JsonProperty("billType")
    val billType: String?,
    @JsonProperty("createdOn")
    val createdOn: String?,
    @JsonProperty("id")
    val id: Long,
    @JsonProperty("latitude")
    val latitude: Double,
    @JsonProperty("longitude")
    val longitude: Double,
    @JsonProperty("mainFacility")
    val mainFacility: Any?,
    @JsonProperty("name")
    val name: String,
    @JsonProperty("region")
    val region: Region?,
    @JsonProperty("tier")
    val tier: String?
)

data class Region(
    @JsonProperty("country")
    val country: Country,
    @JsonProperty("id")
    val id: Int,
    @JsonProperty("name")
    val name: String
)

data class Country(
    @JsonProperty("id")
    val id: Int,
    @JsonProperty("name")
    val name: String
)

data class IntegratedClaimRequestDto(
    val visitNumber: String
)


data class IntegratedClaimResponseDto(
    @JsonProperty("control")
    val control: IntegratedClaimControl?,
    @JsonProperty("data")
    val `data`: IntegratedClaimData?
)

data class IntegratedClaimControl(
    @JsonProperty("custom_error")
    val customError: Boolean,
    @JsonProperty("error")
    val error: Boolean,
    @JsonProperty("message")
    val message: String
)

data class IntegratedClaimData(
    @JsonProperty("amount")
    val amount: String,
    @JsonProperty("claimRef")
    val claimRef: String,
    @JsonProperty("date")
    val date: String,
    @JsonProperty("invoiceNumber")
    val invoiceNumber: String,
    @JsonProperty("patientName")
    val patientName: String,
    @JsonProperty("visitNumber")
    val visitNumber: String
)

data class IntegratedClaimCloseRequestDto(
    val visitId: Long,
    val claimRef: String,
    val invoiceNumber: String,
    val totalInvoiceAmount: BigDecimal,
    val providerId: Long
)

data class IntegratedClaimRemoteCloseRequestDto(
    val claimRef: String,
    val memberNumber: String,
    val visitId: Long,
)

data class BenefitsDto(
    val beneficiaryId: Long,
    val mainBenefitId: Long,
)

data class VisitTransactionDto(
    val visitId: Long?,
    val visitInvoiceNumber: String?,
    val hospitalProviderId: Long?,
    val memberNumber: String?,
    val providerName: String?,
    val beneficiaryName: String?,
    val totalAmount: BigDecimal?,
    val benefitName: String?,
    val invoiceNumber: String?,
    val invoiceLines: Set<InvoiceLine>,
    val txnDate: LocalDateTime?
)

data class VisitTxnDto(
    val visitId: Long?,
    val hospitalProviderId: Long?,
    val providerName: String?,
    val memberNumber: String?,
    val beneficiaryName: String?,
    val benefitName: String?,
    val invoiceNumber: String?,
    val totalAmount: BigDecimal?,
    val invoices: Set<VisitInvoiceDto>,
    val txnDate: LocalDateTime?
)

data class VisitInvoiceDto(
    val id: Long,
    val number: String?,
    val amount: BigDecimal?,
    val invoiceLines: Set<InvoiceLine>
)

data class SaveDocumentDTO(
    var providerName: String,
    var providerId: Long,
    var type: DocumentType,
    var fileUrl: String,
    var invoiceNumber: String
)

data class UpdateMiddlewareStatusDTO(
    val aggregateId: String,
    val benefitName: String,
    val memberNumber: String,
    val visitNumber: Long
)

data class MiddlewareClaimDTO(
    val aggregateId: String,
    val benefitName: String,
    val memberNumber: String,
    val visitNumber: Long
)

data class SmsDTO(
    val phone: String,
    val msg: String
)

data class Mem(
    var phoneNumber: String?
)

data class MemRes(
    val data: MutableList<Mem>,
    val success: Boolean,
    val msg: String?
)

data class Payer(
    @JsonProperty("id")
    var id: Long,
    @JsonProperty("name")
    var name: String,
    @JsonProperty("contact")
    var contact: String,
    @JsonProperty("type")
    var type: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class VisitRes(
    @JsonProperty("data")
    val data: VisitDTO2,

    @JsonProperty("success")
    val success: Boolean,

    @JsonProperty("msg")
    val msg: String?
)

data class VisitDTO2(
    @JsonProperty("id")
    var id: Long,
    @JsonProperty("hospitalProviderId")
    var hospitalProviderId: String,

    )

@JsonIgnoreProperties(ignoreUnknown = true)
data class CategoryRes(
    @JsonProperty("data")
    val data: Category,

    @JsonProperty("success")
    val success: Boolean,

    @JsonProperty("msg")
    val msg: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PayerRes(
    @JsonProperty("data")
    val data: Payer,

    @JsonProperty("success")
    val success: Boolean,

    @JsonProperty("msg")
    val msg: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ProviderRes(
    @JsonProperty("data")
    val data: Provider,

    @JsonProperty("success")
    val success: Boolean,

    @JsonProperty("msg")
    val msg: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Provider(

    @JsonProperty("id")
    var id: Long? = 0,

    @JsonProperty("name")
    var name: String? = null,

    @JsonProperty("latitude")
    var latitude: BigDecimal? = BigDecimal.ZERO,

    @JsonProperty("longitude")
    var longitude: BigDecimal? = BigDecimal.ZERO,

    @JsonProperty("providerMiddleware")
    val providerMiddleware: String? = null,

    @JsonProperty("billingStation")
    val billingStation: Boolean? = null,
)

data class Plan(
    @JsonProperty("id")
    var id: Long,
    @JsonProperty("name")
    var name: String,
)

data class Category(
    @JsonProperty("id")
    var id: Long,
    @JsonProperty("name")
    var name: String,
    @JsonProperty("agakhanSchemeCode")
    var agakhanSchemeCode: String?,
    @JsonProperty("agakhanInsuranceCode")
    var agakhanInsuranceCode: String?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PlanRes(
    @JsonProperty("data")
    val data: Plan,

    @JsonProperty("success")
    val success: Boolean,

    @JsonProperty("msg")
    val msg: String?
)

data class ProviderUrlDTO(
    val providerName: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ProviderUrlDTORes(
    @JsonProperty("data")
    val data: Plan,

    @JsonProperty("success")
    val success: Boolean,

    @JsonProperty("msg")
    val msg: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ProviderUrlResponse(
    @JsonProperty("data")
    val data: ProviderUrl,

    @JsonProperty("success")
    val success: Boolean,

    @JsonProperty("msg")
    val msg: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ProviderUrlTypeResponse(
    @JsonProperty("data")
    val data: MutableList<ProviderUrl>,

    @JsonProperty("success")
    val success: Boolean,

    @JsonProperty("msg")
    val msg: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class IntegratedClaimResponseDto2(

    @JsonProperty("control")
    val control: IntegratedClaimControl?,

    )

data class IntegratedClaimRemoteCloseRequestDto2(
    val claimRef: String
)

data class IntegratedUnsuccessfulClaimCloseDTO(
    val claimRef: String,
    val reason: String,
    val memberNumber: String,
    val provider: String? = null
)

data class CoverBeneficiariesResponse(
    @JsonProperty("data")
    val `data`: List<CoverBeneficiaryData>?,
    @JsonProperty("msg")
    val msg: String?,
    @JsonProperty("success")
    val success: Boolean
)

data class CoverBeneficiaryData(
    val beneficiaryType: String?,
    val category: CoverCategory?,
    val dob: String?,
    val email: String?,
    val gender: String?,
    val id: Long,
    val memberNumber: String,
    val name: String,
    val nhifNumber: String?,
    val phoneNumber: String?,
    val principal: CoverPrincipal?,
    val processed: Boolean?,
    val processedTime: String?
)

data class CoverCategory(
    val description: String,
    val id: Long,
    val name: String,
    val policy: CoverPolicy,
    val status: String?
)


data class CoverPlan(
    val accessMode: String,
    val id: Long,
    val name: String,
    val type: String
)

data class CoverPolicy(
    val endDate: String,
    val id: Long,
    val plan: Plan,
    val policyNumber: String,
    val startDate: String
)

data class CoverPrincipal(
    val beneficiaryType: String?,
    val category: CoverCategory?,
    val dob: String?,
    val email: String?,
    val gender: String?,
    val id: Long,
    val memberNumber: String,
    val name: String,
    val nhifNumber: String?,
    val phoneNumber: String?,
    val processed: Boolean?,
    val processedTime: String?
)

data class AggregateRecord(
    var aggregateId: String,
    var amount: String,
    var benefitName: String,
    var memberNumber: String,
    var visitNumber: Long,
    var benefitId: Long,
    val visitType: String? = null
)


data class IntergrationVisit(
    var aggregateId: String,
    var providerMiddleware: String,
    var middlewareStatus: String,
    var balanceAmount: Double,
    var benefitName: String,
    var memberNumber: String,
    var visitNumber: Long,
    var benefitId: Long,
    var memberName: String,
    var payer_name: String,
    var scheme_name: String,
    var first_name: String,
    var last_name: String,
    var other_names: String,
    var scheme_code: String?,
    var member_type: String,
    var insurance_id: String,
    var principal_relationship: String,
    var outpatient_status: String,
    var encounterId: Long
)


data class GroupOne(
    var aggregateId: String,
    var benefitId: Long,
    var benefit_type: String,
    var membership_number: String,
    var full_name: String,
    var benefit_balance: BigDecimal,
    var payer_name: String,
    var scheme_name: String,
    var providerName: String,
    var first_name: String,
    var last_name: String,
    var encounterId: Long,
    var outpatient_status: String?,
    var other_names: String?,
    var principal_relationship: String? = null,
    var insurance_id: String?,
    var member_type: String?,
    var scheme_code: String?,
    var providerMiddleware: String? = null,
    var middle_name: String? = null,
    var principal: String? = null,
    var deviceId: String? = null,
    var location_id: String? = null

) {

    var benefitName: String? = null
}


data class GroupTwoKranium(
    var aggregateId: String, var benefitId: Long, var benefitName: String,
    var membership_number: String, var full_name: String, var benefit_balance: BigDecimal,
    var payer_name: String, var scheme_name: String, var providerName: String,
    var payer_code: String, var first_name: String, var last_name: String,
    var encounterId: Long, var deviceId: String?
) {

    var benefit_type1: String? = null
    var outpatient_status: String? = null
    var other_names: String? = null
    var scheme_code: String? = null
    var status: String? = null
    var benefit_balance1: BigDecimal
    var benefit_limit1: Long
    var benefit_type2: String
    var benefit_balance2: BigDecimal
    var benefit_limit2: Long
    var benefit_type3: String
    var benefit_balance3: BigDecimal
    var benefit_limit3: Long
    var benefit_type4: String
    var benefit_balance4: BigDecimal
    var benefit_limit4: Long
    var insurance_id: String? = null
    var member_type: String? = null

    init {
        benefit_type1 = benefitName.toString()
        benefit_balance1 = benefit_balance
        benefit_limit1 = 0L
        benefit_type2 = "N/A"
        benefit_balance2 = BigDecimal.valueOf(0L)
        benefit_limit2 = 0L
        benefit_type3 = "N/A"
        benefit_balance3 = BigDecimal.valueOf(0L)
        benefit_limit3 = 0L
        benefit_type4 = "N/A"
        benefit_balance4 = BigDecimal.valueOf(0L)
        benefit_limit4 = 0L
        outpatient_status = "Active"
        other_names = "N/A"
        scheme_code = "IL1/18"
        status = "Active"
        insurance_id = "IL1"
        member_type = "P"

    }
}

data class GetIntegratedClaimDto(
    val providerId: Long,
    val visitNumber: String
)


data class DeactivateBenefitDTO(
    val beneficiaryId: Long,
    val categoryId: Long
)
data class activateBenefitDTO(
    val beneficiaryId: Long,
    val categoryId: Long
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TopUpBenefitDTO(
    val id: Long,
    val amount: BigDecimal,
    val memberNumber: String,
    val benefitId: Long,
    val aggregateId: String,

    )

@JsonIgnoreProperties(ignoreUnknown = true)
data class TransferBenefitDTO(
    val fromBenefitBeneficiaryId: Long?,
    val toBenefitBeneficiaryId: Long?,
    val amount: BigDecimal?,
    val fromAggregate: String?,
    val toAggregate: String?,
    val memberNumber: String?,
    val benefitId: Long?
)

@JsonIgnoreProperties(ignoreUnknown = true)
@Serializable
class StatementDTO(

    @JsonProperty(value = "visit_number")
    val visitNumber: Long?,

    @JsonProperty(value = "member_name")
    val memberName: String,

    @JsonProperty(value = "member_number")
    val memberNumber: String,

    @JsonProperty(value = "provider_name")
    val providerName: String,

    @JsonProperty(value = "payer_name")
    val payerName: String,

    @JsonProperty(value = "total_invoice_amount")
    val totalInvoiceAmount: Long?,

    @JsonProperty(value = "invoice_number")
    val invoiceNumber: String? = null,

    @JsonProperty(value = "balance")
    val balance: Long?,

    @JsonProperty(value = "utilization")
    val utilization: Long?,

    @JsonProperty(value = "benefit_name")
    val benefitName: String,

    @JsonProperty(value = "parent_id")
    val parentId: Long?,
)

data class MemberStatement(
    val memberName: String?,
    val memberNumber: String?,
    val benefitName: String?,
    val subBenefit: String?,
    val schemeName: String?,
    val payerName: String?,
    val invoiceAmount: BigDecimal?,
    val invoiceNumber: String?,
    val createdAt: String?,
    val providerName: String?,
    val initialLimit: BigDecimal?,
    val fromDate: String?,
    val toDate: String?
)

interface EmailBeneficiaryDto{
    val memberName: String?
    val memberNumber: String
    val email: String?
    val startDate: String?
    val endDate: String?
    val categoryId:Long?
}

interface MembersStatementsDTO {
    val visitNumber: Long?
    val aggregateId: String?
    val benefitId: Long?
    val benefitName: String?
    val subBenefit: String?
    val categoryId: Long?
    val createdAt: String?
    val providerId: String?
    val invoiceNumber: String?
    val memberName: String?
    val memberNumber: String?
    val payerId: String?
    val totalAmount: BigDecimal?
    val initialLimit: BigDecimal?
    val fromDate: String?
    val toDate: String?
}

interface BeneficiaryBenefitDto{
    val startDate: LocalDate
    val endDate: LocalDate
}


interface SchemeStatementDTO {
    val visit_number: Long
    val member_name: String
    val member_number: String
    val category_id: Long
    val benefit_name: String
    val total_invoice_amount: BigDecimal
    val balance: BigDecimal
    val scheme: String
    val payer: String
    val payer_status: String
    val invoice_number: String
    val created_at: LocalDateTime
    val provider_name: String
    val status: String
    val category: String
    val categoryDesc: String
    val plan_name: String
    val visit_type: String
}

interface SchemeClinicalStatementDTO {
    val visit_number: Long
    val member_name: String
    val member_number: String
    val category_id: Long
    val benefit_name: String
    val total_invoice_amount: BigDecimal
    val balance: BigDecimal
    val scheme: String
    val payer: String
    val description: String
    val line_category: String
    val line_type: String
    val line_total: BigDecimal
    val quantity: BigDecimal
    val unit_price: BigDecimal
    val payer_status: String
    val invoice_number: String
    val created_at: LocalDateTime
    val provider_name: String
    val status: String
    val plan_name: String
    val visit_type: String
    val icd10_code: String
    val icd10_title: String
}

interface ProviderDTO{
    val provider_id:Int
    val provider_name:String
}

interface SingleClaimDTO {
    val visitNumber: Long
    val memberName: String
    val memberNumber: String
    val benefitName: String
    val scheme: String
    val payer: String
    val totalInvoiceAmount: BigDecimal
    val invoiceNumber: String
    val createdAt: String
    val provider: String

}



interface memberClaimDTO{
    val VisitNumber:Long
    val status: String
    val hospitalProviderId:Long
    val memberName: String
    val memberNumber: String
    val benefitName: String
    val claimProcessStatus: String
    val balanceAmount:BigDecimal
    val payerId:Long
    val aggregateId: String
    val createdAt: LocalDate
    val updatedAt: LocalDate
    val schemeName: String
    val payerName: String
    val totalInvoiceAmount:BigDecimal
    val invoiceNumber: String
    val providerName: String

}

interface ProviderClaimsDTO {
    val visitNumber: Long
    val memberName: String?
    val memberNumber: String?
    val benefitName: String?
    val schemeName: String?
    val payerName: String?
    val invoiceAmount: BigDecimal
    val invoiceNumber: String?
    val createdAt: String?
    val providerName: String?
    val status: String?
    val hospitalProviderId: Long
}

interface ProviderHistoryDTO {
    val visitNumber: Long
    val memberName: String
    val memberNumber: String
    val benefitName: String
    val schemeName: String
    val payerName: String
    val invoiceAmount: BigDecimal
    val invoiceNumber: String
    val createdAt: String
    val providerName: String
    val status: String
    val hospitalProviderId: Long
}

interface PayerProvidersDTO {
    val code: String
    val providerName: String

}

interface GeneralClaimDTO {
    val visitNumber: Long
    val memberName: String
    val memberNumber: String
    val benefitName: String
    val schemeName: String
    val payerName: String
    val invoiceAmount: BigDecimal
    val invoiceNumber: String
    val createdAt: String
    val providerName: String
    val status: String
    val hospitalProviderId: Long
    val visitType: VisitType?
}

interface ProviderReimbursementDTO {
    val visitNumber: Long
    val memberName: String
    val memberNumber: String
    val benefitName: String
    val schemeName: String
    val payerName: String
    val invoiceAmount: BigDecimal
    val invoiceNumber: String
    val createdAt: LocalDate
    val providerName: String
    val hospitalProviderId: String
    val reimbursementProvider: String
    val reimbursementInvoiceDate: String
}

interface MemberClaimsDTO {
    val visitNumber: Long
    val invoiceNumber: String?
    val benefitId:Long?
    val aggregateId:String?
    val createdAt: String?
    val categoryId:Long?
    val providerName: String?
    val memberName: String?
    val memberNumber: String?
    val benefitName: String?
    val status: String?
    val totalAmount:BigDecimal?
}

data class ClaimsDTO(

    val id: Long?,
    val memberNumber: String?,
    val memberName: String?,
    val hospitalProviderId: Long,
    val aggregateId: String?,
    val benefitName: String?,
    val payerId: String?,
    var balanceAmount: BigDecimal?,
    var totalInvoiceAmount: BigDecimal?,
    var invoiceNumber: String?,
    var status: Status?,
    var claimProcessStatus: ClaimProcessStatus?,
    val providerName: String?,
    val createdAt: LocalDate?,
    val updatedAt: LocalDate?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TransactionLineItemDto(
    val amount: BigDecimal,
    val description: String,
    val quantity: BigDecimal,
    val unitPrice: BigDecimal
)

data class TransactionItemDto(
    val chargeDate: String,
    val invoiceNumber: String,
    val lineItems: List<TransactionLineItemDto>,
    val totalAmount: BigDecimal
)

data class TransactionInvoiceDto(
    val items: List<TransactionItemDto>,
    val memberNumber: String,
    val invoiceTotal: BigDecimal,
    val visitNumber: Long,
    val invoiceDate: LocalDateTime?
)

data class TransactionInvoieItemDto(
    val invoice: TransactionInvoiceDto
)

data class TransactionMigrationDto(
    val invoices: List<TransactionInvoieItemDto>
)

@JsonIgnoreProperties(ignoreUnknown = true)
@Serializable
data class PayerProviderMapRes(
    @JsonProperty(value = "success")
    val success: Boolean,
    @JsonProperty(value = "msg")
    val msg: String?,
    @JsonProperty(value = "data")
    val data: PayerProviderMap
)

@JsonIgnoreProperties(ignoreUnknown = true)
@Serializable
data class PayerProviderMap(
    val payerId: Long,
    val providerId: Long,
    val code: String
)


data class ExistingTodayVisitDTO(
    @JsonProperty(value = "aggregate_id")
    val aggregateId: String,
    @JsonProperty(value = "benefit_name")
    val benefitName: String,
    @JsonProperty(value = "member_number")
    val memberNumber: String,
    @JsonProperty(value = "id")
    val visitNumber: Long
)

@Serializable
data class encounterVisitDTO(
    val id: Long,
    val aggregateId: String,
    val totalInvoiceAmount: String,
    val benefitName: String,
    val memberNumber: String,
    val benefitId: Long
)


@JsonIgnoreProperties(ignoreUnknown = true)
@Serializable
data class PayerMappingRes(
    @JsonProperty(value = "success")
    val success: Boolean,
    @JsonProperty(value = "msg")
    val msg: String?,
    @JsonProperty(value = "data")
    val data: Mappings
)

@JsonIgnoreProperties(ignoreUnknown = true)
@Serializable
data class Mappings(
    val payerId: Long,
    val providerCode: Long,
    val benefitCode: Long
)

@JsonIgnoreProperties(ignoreUnknown = true)
@Serializable
data class ProviderMappingRes(
    @JsonProperty(value = "success")
    val success: Boolean,
    @JsonProperty(value = "msg")
    val msg: String?,
    @JsonProperty(value = "data")
    val data: Mapping
)

@JsonIgnoreProperties(ignoreUnknown = true)
@Serializable
data class Mapping(
    val payerId: Long?,
    val payerName: String?,
    val providerCode: String?,
    val benefitCode: Long?,
    val name: String?,
    val providerName: String?,
    val schemeName: String?,
    val serviceId: Long,
    val serviceGroup: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
@Serializable
data class InvoiceRes(
    @JsonProperty(value = "success")
    val success: Boolean,
    @JsonProperty(value = "msg")
    val msg: String?,
    @JsonProperty(value = "data")
    val data: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
@Serializable
data class ProviderBranchesRes(
    @JsonProperty(value = "success")
    val success: Boolean,
    @JsonProperty(value = "msg")
    val msg: String?,
    @JsonProperty(value = "data")
    val data: MutableList<BranchesDTO>
)

@JsonIgnoreProperties(ignoreUnknown = true)
@Serializable
data class BranchesDTO(
    val providerId:Long,
    val providerName: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class BenBenefitDTO(

    @JsonProperty(value = "aggregate_id")
    val aggregateId: String? = null,
    @JsonProperty(value = "benefit_id")
    var benefitId: Long? = null,
    @JsonProperty(value = "beneficiary_id")
    val beneficiaryId: Long? = null,
    @JsonProperty(value = "member_name")
    val memberName: String? = null,
    @JsonProperty(value = "member_number")
    val memberNumber: String? = null,
    @JsonProperty(value = "balance")
    var balance: BigDecimal? = null,
    @JsonProperty(value = "category_id")
    val categoryId: Long? = null
)


 class ErrorResponse(value: Int, message: String?) {
    private val status = 0
    private val message: String? = null
    private val stackTrace: String? = null
    private var errors: MutableList<ValidationError>? = null

    private class ValidationError(s: String?, message: String?) {
        private val field: String? = null
        private val message: String? = null
    }

    fun addValidationError(field: String?, message: String?) {
        if (Objects.isNull(errors)) {
            errors = ArrayList()
        }
        errors!!.add(ValidationError(field, message))
    }
}