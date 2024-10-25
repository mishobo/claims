//package net.lctafrica.claimsapis.model
//
//import java.math.BigDecimal
//import javax.persistence.*
//
//
//enum class ClaimStatus {
//	PENDING, ABANDONED,TRANSMITTED,SETTLED,REJECTED
//}
//
//@Entity
//@Table(name = "claim")
//data class Claim(
//	@Id
//	@GeneratedValue(strategy = GenerationType.AUTO)
//	@Column(name = "claimId", nullable = false)
//	var claimId: Long = 0,
//
//	@Column(name = "amount")
//	var amount: String?,
//
//	@Column(name = "benefit_id")
//	var benefitId: String?,
//
//	@Column(name = "benefit_name")
//	var benefitName: String?,
//
//	@Column(name = "invoice_id")
//	var invoiceId: String?,
//
//	@Column(name = "id")
//	var id: Int,
//
//	var beneficiaryType: String?,
//
//	var description: String?,
//
//	var amountSettled: BigDecimal?,
//
//	var memberNumber: String,
//
//	var aggregateID: String,
//
//	var memberName: String?,
//
//	var deviceId: String?,
//
//	var lctStartDate: String?,
//	var lctEndDate: String?,
//	var providerStartDate: String?,
//	var providerEndDate: String?,
//	var statusId: String?,
//	var statusName: String?,
//
//	var patientNumber: String?,
//	var payerCode: String?,
//	var payerName: String?,
//	var schemeCode: String?,
//	var schemeName: String?,
//	var visitNumber: String?,
//	var visitType: String?,
//	var branchCode: String?,
//	var branchName: String?,
//
//	@Column(name = "claimStatus", nullable = true)
//	@Enumerated(EnumType.STRING)
//	var claimStatus: ClaimStatus,
//
//
//	@OneToMany(targetEntity = InvoiceDetail::class, fetch = FetchType.EAGER,cascade = [CascadeType.ALL],orphanRemoval = true)
//	var invoiceDetails: Set<InvoiceDetail>,
//
//	@OneToMany(targetEntity = DiagnosisDetail::class, fetch = FetchType.EAGER,cascade = [CascadeType.ALL],orphanRemoval = true)
//	var diagnosisDetails: Set<DiagnosisDetail>,
//
//	@OneToMany(targetEntity = DoctorDetail::class, fetch = FetchType.EAGER,cascade = [CascadeType.ALL],orphanRemoval = true)
//	var doctorDetails: Set<DoctorDetail>
//)
//
//@Entity
//@Table(name = "diagnosisDetail")
//data class DiagnosisDetail(
//	@Id
//	@GeneratedValue(strategy = GenerationType.IDENTITY)
//	@Column(name = "id", nullable = false)
//	var id: Long = 0,
//	var code: String?,
//	var name: String?,
//
////	@ManyToOne
////	@JoinColumn(name = "claim_id", nullable = true )
////	var claim: Claim?,
//)
//
//@Entity
//@Table(name = "doctorDetail")
//data class DoctorDetail(
//	@Id
//	@GeneratedValue(strategy = GenerationType.IDENTITY)
//	@Column(name = "id", nullable = false)
//	var id: Long = 0,
//	var code: String?,
//	var name: String?,
//)
//
//@Entity
//@Table(name = "invoiceDetail")
//data class InvoiceDetail(
//	@Id
//	@GeneratedValue(strategy = GenerationType.IDENTITY)
//	@Column(name = "id", nullable = false)
//	var id: Long = 0,
//	var invoiceNumber: String?,
//	var invoiceState: String?,
//	var invoiceAmount: BigDecimal?,
//	@OneToMany(fetch = FetchType.EAGER,cascade = [CascadeType.ALL], orphanRemoval = true)
//	@JoinColumn(name = "invoice_detail_id")
//	var invoiceLines: List<InvoiceLine>?,
//	)
//
//@Entity
//@Table(name = "invoiceLine")
//class InvoiceLine(
//	@Id
//	@GeneratedValue(strategy = GenerationType.IDENTITY)
//	@Column(name = "id", nullable = false)
//	var id: Long = 0,
//
//	var billCode: String?,
//	var billName: String?,
//
//
//	var billDate: String,
//
//	var quantity: BigDecimal?,
//	var unitPrice: BigDecimal?,
//	var lineTotal: BigDecimal?,
//	@Column(name = "description")
//	var description: String?,
//
//	)
//
