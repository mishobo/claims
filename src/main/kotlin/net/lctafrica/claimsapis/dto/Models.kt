package net.lctafrica.claimsapis.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import org.hibernate.Hibernate
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.LazyCollection
import org.hibernate.annotations.LazyCollectionOption
import org.hibernate.annotations.UpdateTimestamp
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import javax.persistence.*
import net.lctafrica.claimsapis.enums.BenefitType
import net.lctafrica.claimsapis.enums.CapitationPeriod
import net.lctafrica.claimsapis.enums.CapitationType


enum class Status {
	INACTIVE,ACTIVE, CLOSED, LINE_ITEMS_ADDED, DIAGNOSIS_ADDED, PENDING, CANCELLED,
	TRANSMITTED, SETTLED, REJECTED
}

enum class ClaimProcessStatus {
	PROCESSED, UNPROCESSED
}

enum class MiddlewareStatus {
	SENT, UNSENT
}

enum class LINE_TYPE {
	PHARMACY, LABORATORY, RADIOLOGY, CONSULTATION, MEDICALPROCEDURE, DIAGNOSIS, OTHER,INPATIENT
}

enum class MIDDLEWARENAME {
	AVENUE, MATER, NAIROBIHOSPITAL, GETRUDES, MPSHAH, METROPOLITAN, AGAKHANKISUMU, AGAKHANMOMBASA,
	AGAKHANNAIROBI, NONE,AGAKHANNAIROBITEST,AKUH,GETRUDESTEST
}

enum class MIDDLWARE_URL_TYPE {
	BENEFIT, CLAIM, CLOSE
}

enum class INVOICE_STATUS {
	REJECTED, BALANCE_DEDUCTED
}



@Entity
@Table(
	name = "visit"
)
data class Visit(
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "visit_number")
	val id: Long = 0,

	@Column(name = "member_number")
	val memberNumber: String,

	@Column(name = "member_name")
	val memberName: String,

	@Column(name = "hospital_provider_id")
	val hospitalProviderId: Long? = null,

	@Column(name = "staff_id")
	val staffId: String,

	@Column(name = "staff_name")
	val staffName: String,

	@Column(name = "aggregate_id")
	val aggregateId: String,

	@Column(name = "category_id")
	val categoryId: String,

	@Column(name = "benefit_name")
	val benefitName: String,

	@Column(name = "beneficiaryId")
	val beneficiaryId: Long? = null,

	@Column(name = "benefit_id")
	val benefitId: Long? = null,

	@Column(name = "payer_id")
	val payerId: String,

	@Column(name = "policy_number")
	val policyNumber: String? = null,

	@Column(name = "balance_amount")
	var balanceAmount: BigDecimal,

	@Column(name = "beneficiary_type")
	val beneficiaryType: String? = null,

	@Column(name = "total_invoice_amount")
	var totalInvoiceAmount: BigDecimal? = null,

	@Enumerated(EnumType.STRING)
	@Column(name = "provider_middleware")
	val providerMiddleware: MIDDLEWARENAME?,

	@Column(name = "invoice_number")
	var invoiceNumber: String? = null,

	@Enumerated(EnumType.STRING)
	@Column(name = "status")
	var status: Status? = null,

	@Enumerated(EnumType.STRING)
	@Column(name = "middlewareStatus")
	var middlewareStatus: MiddlewareStatus? = MiddlewareStatus.UNSENT,

	@Enumerated(EnumType.STRING)
	@Column(name = "claim_process_status")
	var claimProcessStatus: ClaimProcessStatus? = ClaimProcessStatus.UNPROCESSED,

	@Column(name = "cancellation_reason")
	var cancelReason: String? = null,

	@CreationTimestamp
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
	@Column(name = "created_at", updatable = false, nullable = false)
	var createdAt: LocalDateTime? = LocalDateTime.now(),

	@UpdateTimestamp
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
	@Column(name = "updated_at", insertable = false)
	val updatedAt: LocalDateTime? = null,

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
	@Column(name = "visit_end")
	var visitEnd: LocalDateTime? = null,

	@OneToMany(mappedBy = "visit", fetch = FetchType.EAGER)
	var diagnosis: List<Diagnosis>? = null,

	@Column(name = "visit_type")
	@Enumerated(EnumType.STRING)
	val visitType: VisitType? = VisitType.ONLINE,

	@Column(name = "off_system_reason")
	@Enumerated(EnumType.STRING)
	val offSystemReason: OffSystemReason? = null,

	@Column(name = "reimbursement_provider")
	val reimbursementProvider: String?,

	@Column(name = "reimbursement_invoice_date")
	val reimbursementInvoiceDate: LocalDate?,

	@Column(name = "reimbursement_reason")
	val reimbursementReason: String?,

	@Column(name = "payer_status")
	@Enumerated(EnumType.STRING)
	var payerStatus: PayerStatus? = null,

	@Column(name = "facility_type")
	@Enumerated(EnumType.STRING)
	val facilityType: FacilityType? = FacilityType.SINGLE,

	@JsonIgnore
	@OneToMany(mappedBy = "visit", fetch = FetchType.EAGER)
	val invoices: Set<Invoice> = mutableSetOf(),

	@Column(name = "provider_mapping")
	val providerMapping: String? = null,

	@Column(name = "benefit_mapping")
	val benefitMapping: String? = null,

	@Column(name = "payer_claim_reference")
	val payerClaimReference: String? = null,

	@Column(name = "invoice_date")
	val invoiceDate: LocalDateTime? = LocalDateTime.now(),

	) {
	init{
		fun checkOffSystemHasReason(): Boolean {
			if ( visitType != null && visitType == VisitType.OFF_LCT && offSystemReason == null) return false
			return true
		}
		fun checkNoZeroBalance(): Boolean {
			if (totalInvoiceAmount === BigDecimal.ZERO && status!! == Status.CLOSED) return false
			return true
		}
		require(checkNoZeroBalance()) { "Invoice amount Cannot be zero" }
		require(checkOffSystemHasReason()) { "OFF LCT must have a reason" }
	}

}

enum class PayerStatus {
	SENT,FAILED
}

enum class VisitType{
	ONLINE,
	OFF_LCT,
	REIMBURSEMENT
}

enum class OffSystemReason{
	SYSTEM_DOWNTIME,
	PROVIDER_NOT_SETUP,
	FAULTY_DEVICE,
	EMERGENCY_CASE
}

enum class FacilityType{
	SINGLE,
	MULTIPLE
}


@Entity
@Table(
	name = "invoice", uniqueConstraints = [
		UniqueConstraint(
			name = "provider_invoiceNumber_UNQ",
			columnNames = ["hospital_provider_id", "invoice_number"]
		)
	]
)
data class Invoice(
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "invoice_id", nullable = false)
	var id: Long = 0,

	@Column(name = "hospital_provider_id")
	var hospitalProviderId: Long?,

	@Column(name = "invoice_number")
	var invoiceNumber: String?,

	@Column(name = "service")
	var service: String?,

	@Column(name = "total_amount")
	var totalAmount: BigDecimal?,

	@Column(name = "claim_ref")
	var claimRef: String? = null,

	@Column(name = "status")
	@Enumerated(EnumType.STRING)
	var status: INVOICE_STATUS? = INVOICE_STATUS.BALANCE_DEDUCTED,

	@JsonIgnore
	@OneToMany(mappedBy = "invoice", fetch = FetchType.EAGER)
	var invoiceLines: Set<InvoiceLine> = mutableSetOf(),

	@ManyToOne
	@JoinColumn(name = "visit_number", nullable = false)
	var visit: Visit,
){
	override fun toString(): String {
		return "InvoiceLine (id=$id)"
	}

	override fun hashCode(): Int {
		return (id.toInt() * 31)
	}
}

@Entity
@Table(name = "invoice_line")
data class InvoiceLine(
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	var id: Long = 0,
	var lineTotal: BigDecimal?,
	@Column(name = "description")
	var description: String?,
	@Column(name = "invoice_number")
	var invoiceNumber: String?,

	@Column(name = "quantity")
	var quantity: BigDecimal?,

	@Column(name = "unitPrice")
	var unitPrice: BigDecimal?,

	@Column(name = "line_type")
	@Enumerated(EnumType.STRING)
	var lineType: LINE_TYPE?,

	@Column(name = "claim_ref")
	var claimRef: String? = null,
	@Column(name = "line_Category")
	var lineCategory: String?,

	@ManyToOne
	@JoinColumn(name = "invoice_id", nullable = false)
	var invoice: Invoice?,
) {
	override fun toString(): String {
		return "Bill (id=$id,  description=$lineTotal)"
	}

	override fun hashCode(): Int {
		return (id.toInt() * 31)
	}
}

@Entity
@Table(name = "medical_procedure")
data class MedicalProcedure(
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	var id: Long = 0,

	@Column(name = "procedure_code")
	var procedure_code: String? = null,

	@Column(name = "procedure_description")
	var procedure_description: String? = null,

	) {
	override fun toString(): String {
		return "MedicalProcedure (id=$id,  procedure_Description=$procedure_description)"
	}
}

@Entity
@Table(name = "medical_drug")
data class MedicalDrug(
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	var id: Long = 0,

	@Column(name = "name")
	var name: String? = null,

	) {
	override fun toString(): String {
		return "MedicalProcedure (id=$id,  name=$name)"
	}
}

@Entity
@Table(name = "radiology")
data class Radiology(
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	var id: Long = 0,

	@Column(name = "detail")
	var detail: String? = null,

	) {
	override fun toString(): String {
		return "Radiology (id=$id,  name=$detail)"
	}
}

@Entity
@Table(name = "laboratory")
data class Laboratory(
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	var id: Long = 0,

	@Column(name = "name")
	var name: String? = null,

	) {
	override fun toString(): String {
		return "Laboratory (id=$id,  name=$name)"
	}
}

@Entity
@Table(name = "other_benefit_detail")
data class OtherBenefitDetail(
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	var id: Long = 0,

	@Column(name = "benefit_detail")
	var benefit_detail: String? = null,

	) {
	override fun toString(): String {
		return "OtherBenefitDetail (id=$id,  name=$benefit_detail)"
	}
}

@Entity
@Table(name = "document")
data class Document(
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	var id: Long = 0,
	@Column(name = "provider_name")
	var providerName: String? = null,
	@Column(name = "provider_id")
	var providerId: Long? = null,
	@Column(name = "type")
	@Enumerated(EnumType.STRING)
	var type: DocumentType? = null,
	@Column(name = "file_url")
	var fileUrl: String? = null,
	@Column(name = "invoice_number")
	var invoiceNumber: String? = null,

	@CreatedDate
	@Column(name = "created_at", updatable = false, nullable = true)
	val createdAt: LocalDateTime? = null,

	@UpdateTimestamp
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
	@Column(name = "updated_at", insertable = false)
	val updatedAt: LocalDateTime? = null,
) {
	override fun toString(): String {
		return "Document (id=$id,  file_url=$fileUrl)"
	}
}

@Entity
@Table(name = "diagnosis")
data class Diagnosis(
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	var id: Long = 0,
	val code: String?,
	var title: String?,
	@Column(name = "invoice_number")
	var invoiceNumber: String? = null,

	@Column(name = "claim_ref")
	var claimRef: String? = null,

	@JsonIgnore
	@ManyToOne()
	@JoinColumn(name = "visit_number", nullable = false)
	var visit: Visit?,

	){
	@Override
	override fun toString(): String {
		return this::class.simpleName + "(code = $code , title = $title )"
	}
}


@Entity
@Table(name = "clinical_information")
data class ClinicalInformation(
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	var id: Long = 0,

	var icd10code: String?,

	var name: String?,

	@JsonIgnore
	@ManyToOne
	@JoinColumn(name = "visit_number", nullable = false)
	var visit_number: Visit?,
)

@Entity
@Table(name = "icd10code")
data class Icd10code(
	@Id
	@Column(name = "id", nullable = false)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	var id: Long? = null,

	@Column(name = "code")
	var code: String? = null,
	@Column(name = "title") var title: String? = null
) {
	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other == null || Hibernate.getClass(this) != Hibernate.getClass(
				other
			)
		) return false
		other as Icd10code

		return code != null && code == other.code
	}

	override fun hashCode(): Int = javaClass.hashCode()

	@Override
	override fun toString(): String {
		return this::class.simpleName + "(code = $code , title = $title )"
	}

}

@Entity
@Table(
	name = "benefit_beneficiary",
	uniqueConstraints = [
		UniqueConstraint(
			name = "Benefit_Beneficiary_UNQ",
			columnNames = ["benefit_id", "beneficiary_id"]
		)
	],
	indexes = [Index(name = "benefit_member_no", columnList = "member_number,benefit_name")]
)

class BeneficiaryBenefit(
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	val id: Long = 0,
	@Column(name = "aggregate_id")
	val aggregateId: String,
	@Column(name = "benefit_id")
	val benefitId: Long,
	@Column(name = "beneficiary_id")
	val beneficiaryId: Long,
	@Column(name = "member_name")
	val memberName: String,
	@Column(name = "member_number")
	val memberNumber: String,
	@Column(name = "benefit_name")
	val benefitName: String,
	@Column(name = "status")
	@Enumerated(EnumType.STRING)
	var status: BenefitStatus,
	@Column(name = "balance")
	var balance: BigDecimal,
	@Column(name = "suspension_threshold")
	val suspensionThreshold: BigDecimal,
	@Column(name = "initial_limit")
	var initialLimit: BigDecimal,
	@Column(name = "category_id")
	val categoryId: Long,
	@Column(name = "payer_id")
	val payerId: Long,
	/*@Column(name = "sharing")
	@Enumerated(EnumType.STRING)
	val sharing: Sharing,*/
	@Column(name = "utilization")
	var utilization: BigDecimal,

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "parent_id", nullable = true)
	val parent: BeneficiaryBenefit?,

	@JsonIgnore
	@OneToMany(mappedBy = "parent")
	val subBenefits: Set<BeneficiaryBenefit>,

	@Column(name = "start_date")
	val startDate: LocalDate,
	@Column(name = "end_date")
	val endDate: LocalDate,
	@Column(name = "gender")
	@Enumerated(EnumType.STRING)
	val gender: Gender,
	@Column(name = "member_type")
	@Enumerated(EnumType.STRING)
	val memberType: MemberType,
	@Column(name = "catalog_id")
	val catalogId: Long? = null,

	@Column(name = "jic_entity_id", nullable = true)
	var jicEntityId: Int?,

	@Column(name = "apa_entity_id", nullable = true)
	var apaEntityId: Int?,

	@Column(name = "benefit_type", nullable = true)
	@Enumerated(EnumType.STRING)
	var benefitType: BenefitType? = BenefitType.INSURED,

	@Column(name = "capitation_type", nullable = true)
	@Enumerated(EnumType.STRING)
	var capitationType: CapitationType? = null,

	@Column(name = "capitation_period", nullable = true)
	@Enumerated(EnumType.STRING)
	var capitationPeriod: CapitationPeriod? = null,

	var capitationMaxVisitCount:Int = 0,

	var requireBeneficiaryToSelectProvider: Boolean?,

	var daysOfAdmissionLimit: Int = 0,

	var amountPerDayLimit: BigDecimal

	) {
	enum class BenefitStatus {
		ACTIVE, SUSPENDED, CANCELED
	}

	enum class Sharing {
		FAMILY, INDIVIDUAL
	}
}

@Entity
@Table(name = "pre_authorization")
data class Preauthorization(
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	val id: Long = 0,
	var time: LocalDateTime,
	@Enumerated(EnumType.STRING)
	var status: PreauthStatus? = PreauthStatus.PENDING,
	var requestAmount: BigDecimal,
	var authorizedAmount: BigDecimal? = BigDecimal.ZERO,
	var aggregateId: String,
	var benefitId: Long,
	var payerId: Long,
	var providerId: Long,
	var requester: String,
	var authorizer: String? = null,
	var notes: String,
	var authorizationNotes: String? = null,
	var validity: Period? = null,
	var visitNumber: Long?,
	var memberNumber: String?,
	var memberName: String?,
	var benefitName: String?,
	var service: String?,
	var requestType: String?,
	var reference: String?,
	var diagnosis: String?,
	var medProcedure: String?,
	var schemeName: String?,
	var payerName: String?,
	var utilization: BigDecimal? = BigDecimal.ZERO,

	@CreationTimestamp
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
	@Column(name = "created_at", updatable = false, nullable = false)
	val createdAt: LocalDateTime? = null,
	@UpdateTimestamp
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
	@Column(name = "updated_at", insertable = false)
	val updatedAt: LocalDateTime? = null,
) {
	enum class PreauthStatus {
		INACTIVE,PENDING, DECLINED, AUTHORIZED, CLAIMED,CANCELLED
	}
}

@Entity
@Table(name = "claim_error")
data class ClaimError(
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	val id: Long = 0,
	val amount: BigDecimal,
	val date: LocalDate,
	val memberNumber: String,
	val benefit: String
)

@Entity
@Table(name = "transaction_lineitem_error")
data class LineItemError(
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	val id: Long = 0,
	val amount: BigDecimal,
	val description: String?,
	val quantity: BigDecimal?=null,
	val unitPrice: BigDecimal?=null,
	val note: String?,
	@JsonIgnore
	@ManyToOne()
	@JoinColumn(name = "transaction_error", nullable = false)
	var transactionError: TransactionError?,
	val createDate: LocalDateTime = LocalDateTime.now()
)
@Entity
@Table(name = "transaction_error")
data class TransactionError(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val totalAmount: BigDecimal,
    val invoiceNumber: String,
    val memberNumber: String,
    val visitNumber: Long?,
    val chargeDate: String?=null,
	val invoiceDate: LocalDateTime?=null,
    val note: String?,
	@OneToMany(mappedBy = "transactionError", fetch = FetchType.EAGER)
	var lineItemErrors: List<LineItemError>? = null,
    val createDate: LocalDateTime = LocalDateTime.now()
)


@Entity
@Table(name = "provider_url")
data class ProviderUrl(
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	val id: Long = 0,
	val ipaddress: String?,
	val port: String?,
	val provider: String?,
	@Enumerated(EnumType.STRING)
	val urlType: MIDDLWARE_URL_TYPE?
)

@Entity
@Table(name = "claim_close_failed",uniqueConstraints = [
	UniqueConstraint(
		name = "claim_ref_UNQ",
		columnNames = ["claim_ref"]
	)
])
class ClaimCloseFailed(
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	val id: Long = 0,
	@Column(name = "claim_ref")
	val claimRef: String,
	val provider: String,
	val closed: String,
	val reason: String,
	val memberNumber: String,
	@CreationTimestamp
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
	@Column(name = "created_at", updatable = false, nullable = false)
	val createdAt: LocalDateTime? = null,
	@UpdateTimestamp
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
	@Column(name = "updated_at", insertable = false)
	val updatedAt: LocalDateTime? = null,
)


@Entity
@Table(name = "invoice_procedure_code")
data class InvoiceNumberProcedureCode(
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	var id: Long = 0,

	@Column(name = "procedure_code")
	var procedure_code: String? = null,

	@Column(name = "procedure_description")
	var procedure_description: String? = null,

	@Column(name = "invoice_number")
	var invoiceNumber: String?,
	@CreationTimestamp
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
	@Column(name = "created_at", updatable = false, nullable = false)
	val createdAt: LocalDateTime? = null,
	@UpdateTimestamp
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
	@Column(name = "updated_at", insertable = false)
	val updatedAt: LocalDateTime? = null,

	) {
	override fun toString(): String {
		return "InvoiceNumberProceureCode (id=$id,  procedure_Description=$procedure_description)"
	}
}

@Entity
@Table(name = "doctor")
data class Doctor(
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	var id: Long = 0,

	@Column(name = "code")
	var code: String?,

	@Column(name = "name")
	var name: String?,

	@Column(name = "speciality")
	var speciality: String?,

	@JsonIgnore
	@ManyToOne
	@JoinColumn(name = "visit_number", nullable = false)
	var visit_number: Visit?,
)


@Entity
@Table(name = "shedlock")
class shedlock(

	@Id
	@Column(name = "name", nullable = false)
	var name: String? = null,

	@Column(name = "lock_until")
	var lock_until : Timestamp,

	@Column(name = "locked_at")
	var locked_at : Timestamp,

	@Column(name = "locked_by")
	var locked_by: String,
)




