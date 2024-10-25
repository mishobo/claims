package net.lctafrica.claimsapis.repository

import net.lctafrica.claimsapis.dto.*
import org.apache.commons.lang3.mutable.Mutable
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*


interface InvoiceRepository : JpaRepository<Invoice, Long> {
	@Query("SELECT DISTINCT i FROM Invoice i LEFT JOIN i.invoiceLines WHERE i.visit = :visit")
	fun fetchByVisit(@Param("visit") visit: Visit): MutableList<Invoice>

	@Query("SELECT i FROM Invoice i LEFT JOIN i.invoiceLines WHERE i.invoiceNumber = :num")
	fun findByInvoiceNumber(@Param("num") name: String): Optional<Invoice>
	fun findByInvoiceNumberAndHospitalProviderId(
		invoiceNumber: String,
		hospital_provider_id: Long
	): Optional<Invoice>

	fun findByInvoiceNumberAndVisit(
		@Param("invoice_number")invoiceNumber: String,
		@Param("visit_number") visit_number: Visit
	): Optional<Invoice>

	fun findByClaimRef(claim_ref: String): MutableList<Invoice>

	fun findByVisit(@Param("visit_number") visit_number: Visit): Optional<Invoice>


	@Query("select i from Invoice i where i.visit = :visit_number")
	fun findByVisitNumber(@Param("visit_number") visit_number: Visit): MutableList<Invoice>

	@Query(
		"SELECT i FROM Invoice i INNER JOIN Visit d on i.visit.id = d.id WHERE d.benefitId in (:benefitIds) " +
				"AND d.memberNumber in (:memberNumbers) AND d.claimProcessStatus = :claimProcessStatus " +
				"AND (d.status = :status OR d.status = :status2) ORDER BY i.id DESC"
	)
	fun findBeneficiaryInvoices(
		@Param("benefitIds") benefitIds: List<Long>,
		@Param("memberNumbers") memberNumbers: List<String>,
		@Param("claimProcessStatus") claimProcessStatus: ClaimProcessStatus,
		@Param("status") status: Status,
		@Param("status2") status2: Status
	): List<Invoice>
}

interface InvoiceLineRepository : JpaRepository<InvoiceLine, Long> {
	fun findByInvoiceId(@Param("invoice_id") invoice_id: Long): MutableList<InvoiceLine>
	fun findByInvoiceNumber(name: String): Optional<InvoiceLine>
}

interface DiagnosisRepository : JpaRepository<Diagnosis, Long> {
	@Query("SELECT d FROM Diagnosis d WHERE d.visit = :visit")
	fun fetchByVisit(@Param("visit") visit: Visit): MutableList<Diagnosis>
	fun findByVisit(@Param("visit_number") visit_number: Visit): MutableList<Diagnosis>
}

interface DoctorRepository : JpaRepository<Doctor, Long> {

}

interface ClinicalInfoRepository : JpaRepository<ClinicalInformation, Long> {}
interface ICD10Repository : JpaRepository<Icd10code, Long> {
	@Query(
		value = """
		SELECT c FROM Icd10code c WHERE UPPER(c.code) LIKE UPPER(:search) OR UPPER(c.title) LIKE UPPER(:search)
	"""
	)
	fun findByTitleLike(@Param("search") search: String, request: Pageable): Page<Icd10code>
	fun findByCodeLike(search: String, request: Pageable): Page<Icd10code>
}

interface MedicalProcedureRepository : JpaRepository<MedicalProcedure, Long> {
	@Query(
		value = "SELECT c FROM MedicalProcedure c WHERE UPPER(c.procedure_code) LIKE UPPER" +
				"(:search) OR UPPER(c.procedure_description) LIKE UPPER(:search) "
	)
	fun findByProcedureCodeORProcedureDescriptionLike(
		@Param("search") search: String, request:
		Pageable
	): Page<MedicalProcedure>
}

interface MedicalDrugRepository : JpaRepository<MedicalDrug, Long> {
	@Query(value = "SELECT c FROM MedicalDrug c WHERE UPPER(c.name) LIKE UPPER(:search)")
	fun findByNameLike(@Param("search") search: String, request: Pageable): Page<MedicalDrug>
}

interface LaboratoryRepository : JpaRepository<Laboratory, Long> {
	@Query(value = "SELECT c FROM Laboratory c WHERE UPPER(c.name) LIKE UPPER(:search)")
	fun findByNameLike(@Param("search") search: String, request: Pageable): Page<Laboratory>
}

interface OtherBenefitDetailRepository : JpaRepository<OtherBenefitDetail, Long> {
	@Query(
		value = "SELECT c FROM OtherBenefitDetail c WHERE UPPER(c.benefit_detail) LIKE UPPER" +
				"(:search)"
	)
	fun findByBenefitDetailLike(@Param("search") search: String, request: Pageable):
			Page<OtherBenefitDetail>
}

interface RadiologyRepository : JpaRepository<Radiology, Long> {
	@Query(value = "SELECT c FROM Radiology c WHERE UPPER(c.detail) LIKE UPPER(:search)")
	fun findByDetailLike(@Param("search") search: String, request: Pageable): Page<Radiology>
}

interface VisitRepository : JpaRepository<Visit, Long> {

//	@Query("SELECT v.visit_number as visitNumber, i.invoice_number as invoiceNumber, i.total_amount as totalAmount, v.aggregate_id as aggregateId, v.benefit_id as benefitId, v.benefit_name as benefitName," +
//			"v.category_id as categoryId, v.created_at as createdAt, p.provider_name as providerName, v.member_name as memberName, v.member_number as memberNumber, v.status" +
//			"FROM claims.visit v inner join claims.invoice i on i.visit_number = v.visit_number " +
//			"inner join membership.provider p on p.provider_id = v.hospital_provider_id where v" +
//			".status != 'REJECTED' and v.member_number = ?1  order by v.visit_number desc")
//	fun findMemberClaimsByMemberNumber(member_number: String):MutableList<MemberClaimsDTO>

	@Query("SELECT * from visit where status ='ACTIVE' and hospital_provider_id IN ( SELECT " +
			"provider_id from membership.provider where (main_facility_id = ?1 OR provider_id = " +
			"?1)) AND (visit_type !='OFF_LCT'AND visit_type !='REIMBURSEMENT') order by visit_number DESC", nativeQuery
	= true)
	fun findMainAndBranchActiveVisits(mainFacilityId: Long, status: Status):MutableList<Visit?>

	@Query("SELECT * from visit where status !='ACTIVE' and hospital_provider_id IN ( SELECT " +
			"provider_id from membership.provider where (main_facility_id = ?1 OR provider_id = " +
			"?1)) AND (visit_type !='OFF_LCT' AND visit_type !='REIMBURSEMENT') order by " +
			"visit_number DESC",
		countQuery = "SELECT * from visit where status !='ACTIVE' and hospital_provider_id IN ( " +
				"SELECT " +
				"provider_id from membership.provider where (main_facility_id = ?1 OR provider_id = " +
				"?1)) AND (visit_type !='OFF_LCT' AND visit_type !='REIMBURSEMENT')",
		nativeQuery = true)
	fun findMainAndBranchClosedVisits(
		@Param
			("hospital_provider_id") hospital_provider_id: Long,
		@Param("status") status: Status,
		pageable: Pageable
	): Page<Visit?>

	@Query("SELECT * FROM visit  where member_number = ?1 and benefit_id = ?2 and " +
			"hospital_provider_id = ?3 and status='CLOSED' and created_at >= now() - INTERVAL 1 " +
			"DAY", nativeQuery = true)
	fun findMemberVisitsByMemberNumberAndBenefitAndFacilityAndToday(memberNumber: String,benefitId: Long, providerId: Long):MutableList<Visit?>

	@Query(
		"UPDATE visit set status='INACTIVE' WHERE created_at <= now() - INTERVAL 1 DAY and " +
				"status='ACTIVE' and visit_type='ONLINE' and total_invoice_amount = 0.00",
		nativeQuery = true
	)
	fun updateVisitToInactiveAfter24Hours()

	@Query(
		"UPDATE visit set status='CLOSED' WHERE created_at <= now() - INTERVAL 1 DAY and " +
				"status='ACTIVE' and visit_type='ONLINE' and total_invoice_amount > 0.00",
		nativeQuery = true
	)
	fun updateVisitWithBillToClosedAfter24Hours()

	@Query(
		"UPDATE visit set status='CLOSED' WHERE " +
				"facility_type = 'MULTIPLE' and created_at <= now() - INTERVAL 1 DAY and  " +
				"visit_type='ONLINE' and status='ACTIVE' ", nativeQuery = true
	)
	fun updateMultipleTypeVisitToClosed()

	@Query("update visit set payer_claim_reference = ?1 where visit_number = ?2", nativeQuery = true)
	fun updateVisitInvoiceIdSentToPayer(@Param("invoiceId") invoiceId: String,@Param("visitNumber") visitNumber: Long)

	fun findByInvoiceNumber(name: String): Optional<Visit>

	fun findByIdAndMemberNumber(id: Long, memberNumber: String): Optional<Visit>

	fun findByMemberNumber(name: String): MutableList<Visit?>

	@Query("SELECT v.visit_number as visitNumber, v.status as status, v.hospital_provider_id as hospitalProviderId," +
			" v.member_name as memberName, v.member_number as memberNumber," +
			" v.benefit_name as benefitName, v.claim_process_status as claimProcessStatus, v.balance_amount as balanceAmount, " +
			"v.payer_id as payerId, v.aggregate_id as aggregateId, v.created_at as createdAt, v.updated_at as updatedAt, " +
			"pn.plan_name as schemeName, ( select payer_name from membership.payer where payer_id = v.payer_id ) as payerName, " +
			"inv.total_amount as totalInvoiceAmount, inv.invoice_number as invoiceNumber," +
			"( select provider_name from membership.provider where provider_id = v.hospital_provider_id ) as providerName FROM visit v" +
			" inner join membership.category c on c.category_id = v.category_id inner join membership.policy p on p.policy_id = c.policy_id " +
			"inner join membership.plan pn on pn.plan_id = p.plan_id " +
			"inner join claims.invoice inv on inv.visit_number = v.visit_number where v" +
			".member_number LIKE ?1% order by v.visit_number DESC", nativeQuery = true)
	fun findFamilyClaimsByFamilyNumber(@Param("familyNumber") familyNumber: String):MutableList<memberClaimDTO>

	@Query("SELECT v.visit_number as visitNumber, v.status as status, v.hospital_provider_id as hospitalProviderId," +
			" v.member_name as memberName, v.member_number as memberNumber," +
			" v.benefit_name as benefitName, v.claim_process_status as claimProcessStatus, v.balance_amount as balanceAmount, " +
			"v.payer_id as payerId, v.aggregate_id as aggregateId, v.created_at as createdAt, v.updated_at as updatedAt, " +
			"pn.plan_name as schemeName, ( select payer_name from membership.payer where payer_id = v.payer_id ) as payerName, " +
			"inv.total_amount as totalInvoiceAmount, inv.invoice_number as invoiceNumber," +
			"( select provider_name from membership.provider where provider_id = v.hospital_provider_id ) as providerName FROM visit v" +
			" inner join membership.category c on c.category_id = v.category_id inner join membership.policy p on p.policy_id = c.policy_id " +
			"inner join membership.plan pn on pn.plan_id = p.plan_id " +
			"inner join claims.invoice inv on inv.visit_number = v.visit_number where v" +
			".member_number =?1 order by v.visit_number DESC", nativeQuery = true)
	fun findMemberClaimsByMemberNumber(@Param("memberNumber") memberNumber: String):
			MutableList<memberClaimDTO>


	fun findByBeneficiaryId(id: Long): List<Visit?>

	fun findAllByAggregateIdAndBenefitIdAndMemberNumberAndStatus(
		@Param("aggregateId") aggregateId: String,
		@Param("benefitId") benefitId: Long,
		@Param("memberNumber") memberNumber: String,
		@Param("status") status: Status
	): Optional<Visit>

	fun findByMemberNumberAndBenefitIdAndStatus(
		@Param("memberNumber") memberNumber: String,
		@Param("benefit_id") benefitId: Long,
		@Param("status") status: Status
	): Optional<Visit>

	fun findByAggregateIdAndStatus(
		@Param("aggregateId") aggregateId: String,
		@Param("status") status: Status
	): Optional<Visit>

//    fun findAllByAggregateIdAndBenefitIdAndMemberNumberAndStatus(
//        @Param("aggregateId") aggregateId: String,
//        @Param("benefitId") benefitId: Long,
//        @Param("memberNumber") memberNumber: String,
//        @Param("status") status: Status
//    ): MutableList<Visit?>

	//off-lct
	fun findAllByPayerIdAndStatusAndVisitTypeOrderByIdDesc(
		@Param("payerId") payerId: String,
		@Param("status") status: Status,
		@Param("visit_type") visitType: VisitType,
	): MutableList<Visit?>

	fun findAllByPayerIdAndStatusAndVisitTypeOrderByCreatedAtDesc(
		@Param("payerId") payerId: String,
		@Param("status") status: Status,
		@Param("visit_type") visitType: VisitType,
	): MutableList<Visit?>

	fun findAllByPayerIdAndStatusAndVisitTypeOrderByIdDesc(
		@Param("payerId") payerId: String,
		@Param("status") status: Status,
		@Param("visit_type") visitType: VisitType,
		request: Pageable
	): Page<Visit?>


	fun findAllByHospitalProviderIdAndStatusOrderByCreatedAtDesc(
		@Param
			("hospital_provider_id") hospital_provider_id: Long,
		@Param("status") status: Status
	): MutableList<Visit?>

	fun findAllByHospitalProviderIdAndStatusOrderByCreatedAtDesc(
		@Param
			("hospital_provider_id") hospital_provider_id: Long,
		@Param("status") status: Status,
		request: Pageable
	): Page<Visit?>

	fun findAllByStatusAndMiddlewareStatus(
		@Param("status") status: Status,
		@Param("middlewarestatus") middlewareStatus: MiddlewareStatus
	): MutableList<Visit?>

	fun findAllByStatusAndMiddlewareStatusAndProviderMiddleware(
		@Param("status") status: Status,
		@Param("middlewarestatus") middlewareStatus: MiddlewareStatus,
		@Param("providerMIDDLEWARENAME") providerMIDDLEWARENAME: MIDDLEWARENAME
	): MutableList<Visit?>

	fun findAllByUpdatedAtGreaterThanEqualAndStatusAndClaimProcessStatus(
		@Param("updated_at") updated_at: LocalDateTime,
		@Param("status") status: Status,
		@Param("claimProcessStatus") claimProcessStatus: ClaimProcessStatus
	): MutableList<Visit?>


	fun findAllByStatusAndClaimProcessStatusAndProviderMiddleware(
		@Param("status") status: Status,
		@Param("claimProcessStatus") claimProcessStatus: ClaimProcessStatus,
		@Param("providerMiddleware") providerMiddleware: MIDDLEWARENAME
	): MutableList<Visit?>

	fun findAllByStatusAndClaimProcessStatusAndProviderMiddlewareNot(
		@Param("status") status: Status,
		@Param("claimProcessStatus") claimProcessStatus: ClaimProcessStatus,
		@Param("providerMiddleware") providerMiddleware: MIDDLEWARENAME
	): MutableList<Visit?>

	fun findByAggregateIdAndBenefitNameAndMemberNumberAndId(
		@Param("aggregateId") aggregateId: String,
		@Param("benefitName") benefitName: String,
		@Param("memberNumber") memberNumber: String,
		@Param("visitNumber") id: Long
	): Optional<Visit>


	@Query(
		"SELECT v.category_id, v.visit_number, v.hospital_provider_id, v.member_name as " +
				"memberName, v.member_number as memberNumber, v.created_at as createdAt, v" +
				".benefit_name as benefitName, v" +
				".invoice_number as invoiceNumber, pn.plan_name as scheme,( select payer_name from membership.payer where payer_id = v.payer_id ) as payer, + v.total_invoice_amount as totalInvoiceAmount,( select provider_name from membership.provider where provider_id = v.hospital_provider_id ) as provider FROM visit v inner join membership.category c on c.category_id = v.category_id inner join membership.policy p on p.policy_id = c.policy_id inner join membership.plan pn on pn.plan_id = p.plan_id where v.visit_number = ?1  and v.hospital_provider_id = ?2 ",
		nativeQuery = true
	)
	fun findSingleProviderClaim(@Param("visitNumber") id: Long, @Param("hospitalProviderId") hospitalProviderId: Long):
			Optional<SingleClaimDTO>


	@Query(
		"SELECT v.category_id, v.visit_number, v.hospital_provider_id, v.member_name as memberName, v.member_number as memberNumber, " +
				"v.created_at as createdAt, v.benefit_name as benefitName, inv.invoice_number as invoiceNumber, inv.total_amount as totalInvoiceAmount," +
				" pn.plan_name as scheme, ( select payer_name from membership.payer where payer_id = v.payer_id ) as payer, ( select provider_name from membership.provider" +
				" where provider_id = v.hospital_provider_id ) as provider FROM visit v inner join membership.category c on c.category_id = v.category_id inner join membership.policy p " +
				"on p.policy_id = c.policy_id inner join membership.plan pn on pn.plan_id = p.plan_id inner join claims.invoice inv on inv.visit_number = v.visit_number " +
				"where v.visit_number = ?1 and inv.invoice_number = ?2",
		nativeQuery = true
	)
	fun findSingleProviderClaimByInvoiceNumber(@Param("visitNumber") id: Long, @Param("invoiceNumber") invoiceNumber: String):
			Optional<SingleClaimDTO>

	@Query(
		"SELECT v.category_id, v.visit_number, v.hospital_provider_id, v.member_name as " +
				"memberName, v.member_number as memberNumber, v.created_at as createdAt, v" +
				".benefit_name as benefitName, v" +
				".invoice_number as invoiceNumber, pn.plan_name as scheme,( select payer_name " +
				"from membership.payer where payer_id = v.payer_id ) as payer, + v" +
				".total_invoice_amount as totalInvoiceAmount,( select provider_name from " +
				"membership.provider where provider_id = v.hospital_provider_id ) as provider " +
				"FROM visit v inner join membership.category c on c.category_id = v.category_id " +
				"inner join membership.policy p on p.policy_id = c.policy_id inner join " +
				"membership.plan pn on pn.plan_id = p.plan_id where v.visit_number = ?1 and " +
				"visit_type = 'REIMBURSEMENT'",
		nativeQuery = true
	)
	fun findSingleReimbursementClaim(@Param("visitNumber") id: Long): Optional<SingleClaimDTO>


/*	@Query("""
			SELECT * FROM visit WHERE payer_id = '7' AND status IN ('CLOSED','LINE_ITEMS_ADDED') AND
			hospital_provider_id IN 
			(1607) 
			AND payer_status IS NULL LIMIT 200
		""", nativeQuery = true)
	fun findCustomVisits() : MutableList<Visit>


	@Query("""
		SELECT v FROM Visit v WHERE v.id in (:ids) AND v.payerStatus IS NULL AND v.payerId = '7'
	""")
	fun findSpecificVisits(@Param("ids") ids: List<Long>): MutableList<Visit>*/

	@Query("""
		SELECT * FROM visit WHERE `status` = :status AND payer_id IN (:payerIds) AND payer_status IS NULL LIMIT 100
	""" , nativeQuery = true)
	fun findAllByStatusAndPayerIdIn(
		@Param("status") status: Status,
		@Param("payerIds") payerIds: List<String>,
	): MutableList<Visit>

	fun findTop200ByPayerIdInAndStatusAndPayerStatusIsNullAndProviderMiddlewareNot(
		payerId: List<String>, status: Status, providerMiddleware: MIDDLEWARENAME = MIDDLEWARENAME.NONE
	) : MutableList<Visit>

	fun findTop200ByPayerIdInAndStatusAndPayerStatusIsNullAndInvoiceNumberIsNotNullAndProviderMiddleware(
		payerId: List<String>, status: Status = Status.LINE_ITEMS_ADDED,
		providerMiddleware: MIDDLEWARENAME = MIDDLEWARENAME.NONE
	): MutableList<Visit>

	@Query(
		value = """
			SELECT v FROM Visit v WHERE v.memberNumber = :memberNumber AND  v.hospitalProviderId = :hospitalProviderId
			AND v.status =:status
		"""
	)
	fun findActiveVisitsForMember(
		@Param("memberNumber") memberNumber: String,
		@Param("hospitalProviderId") hospitalProviderId: Long,
		@Param("status") status: Status,
	): Optional<Visit>

	@Query("SELECT v.visit_number as visitNumber, v.status as status, v.hospital_provider_id as hospitalProviderId, v.member_name as memberName," +
			 "v.member_number as memberNumber, v.benefit_name as benefitName, pn.plan_name as " +
			"schemeName, ( select payer_name from membership.payer where " +
			 "payer_id = v.payer_id ) as payerName, inv.total_amount as invoiceAmount, inv.invoice_number as invoiceNumber, v.created_at as createdAt," +
			 "( select provider_name from membership.provider where provider_id = v.hospital_provider_id ) as providerName FROM visit v " +
			"inner join membership.category c on c.category_id = v.category_id " +
			"inner join membership.policy p on p.policy_id = c.policy_id " +
			"inner join membership.plan pn on pn.plan_id = p.plan_id " +
			" inner join claims.invoice inv on inv.visit_number = v.visit_number " +
			"where v.hospital_provider_id =?1 and v.status !='ACTIVE' and v.status !='CANCELLED' and v.status " +
			"!='INACTIVE' order by v.visit_number DESC", nativeQuery = true)
	fun findPagedClosedVisitsNoInvoiceMemberNo(
		@Param("hospital_provider_id") hospital_provider_id: Long
	): MutableList<ProviderClaimsDTO>

	@Query("SELECT v.*,(select provider_name from membership.provider where provider_id = v.hospital_provider_id ) as provider_name,(select category_name from membership.category where category_id = v.category_id ) as category,(select description from membership.category where category_id = v.category_id ) as categoryDesc,(select plan_name from membership.plan where plan_id=?1) as plan_name,(select payer_name from membership.payer where payer_id = v.payer_id ) as payer FROM visit v  where v.category_id in (select c.category_id from membership.category c inner join membership.policy p on c.policy_id = p.policy_id and p.plan_id=?1) and DATE(v.created_at) >= ?2 and DATE(v.created_at) <=?3 and v.payer_id=?4 order by v.visit_number desc",nativeQuery = true)
	fun findSchemeFinancialUtilizations(planId:Int,startDate:LocalDate,endDate: LocalDate,payerId:Int): List<SchemeStatementDTO>


	@Query("SELECT l.description,l.line_total,l.line_type,l.quantity,l.unit_price,l.line_category,v.*,(select provider_name from membership.provider where provider_id = v.hospital_provider_id ) as provider_name,(select GROUP_CONCAT(d.code SEPARATOR '\\n\\n') from claims.diagnosis d where d.visit_number = v.visit_number and d.code is not null and d.code != 'null' ) as icd10_code,(select GROUP_CONCAT(d.title SEPARATOR '\\n\\n') from claims.diagnosis d where d.visit_number = v.visit_number and d.code is not null and d.code != 'null') as icd10_title,(select plan_name from membership.plan where plan_id=?1) as plan_name,(select payer_name from membership.payer where payer_id = v.payer_id ) as payer FROM invoice_line l inner join invoice i on l.invoice_id = i.invoice_id inner join visit v on i.visit_number = v.visit_number where v.category_id in (select c.category_id from membership.category c inner join membership.policy p on c.policy_id = p.policy_id and p.plan_id=?1) and DATE(v.created_at) >= ?2 and DATE(v.created_at) <=?3 and v.payer_id=?4 order by v.visit_number desc",nativeQuery = true)
	fun findSchemeClinicalUtilizations(planId:Int,startDate:LocalDate,endDate: LocalDate,payerId:Int): List<SchemeClinicalStatementDTO>

	@Query("SELECT p.provider_id,p.provider_name from visit v inner join membership.provider p on v.hospital_provider_id = p.provider_id and v.invoice_number = ?1 and p.provider_middleware = ?2",nativeQuery = true)
	fun findProviderByInvoice(invoiceNumber:String,mainProvider:String): ProviderDTO?


	@Query("SELECT v.visit_number as visitNumber, v.status as status, v.hospital_provider_id as hospitalProviderId, v.member_name as memberName," +
			"v.member_number as memberNumber, v.benefit_name as benefitName, pn.plan_name as " +
			"schemeName, ( select payer_name from membership.payer where " +
			"payer_id = v.payer_id ) as payerName, inv.total_amount as invoiceAmount, inv.invoice_number as invoiceNumber, v.created_at as createdAt," +
			"( select provider_name from membership.provider where provider_id = v.hospital_provider_id ) as providerName FROM visit v " +
			"inner join membership.category c on c.category_id = v.category_id " +
			"inner join membership.policy p on p.policy_id = c.policy_id " +
			"inner join membership.plan pn on pn.plan_id = p.plan_id " +
			" inner join claims.invoice inv on inv.visit_number = v.visit_number " +
			"where v.hospital_provider_id =?1 and inv.invoice_number = ?2 and v.status !='ACTIVE'" +
			" and v.status !='CANCELLED' and v.status " +
			"!='INACTIVE' order by v.visit_number DESC", nativeQuery = true)
	fun findPagedClosedVisitsWithInvoiceNoPdf(
		@Param("hospital_provider_id") hospital_provider_id: Long,
		@Param("invoiceNo") invoiceNo: String?,
	): MutableList<ProviderClaimsDTO>

	@Query("SELECT v.visit_number as visitNumber, v.status as status, v.hospital_provider_id as hospitalProviderId, v.member_name as memberName," +
			"v.member_number as memberNumber, v.benefit_name as benefitName, pn.plan_name as " +
			"schemeName, ( select payer_name from membership.payer where " +
			"payer_id = v.payer_id ) as payerName, inv.total_amount as invoiceAmount, inv.invoice_number as invoiceNumber, v.created_at as createdAt," +
			"( select provider_name from membership.provider where provider_id = v.hospital_provider_id ) as providerName FROM visit v " +
			"inner join membership.category c on c.category_id = v.category_id " +
			"inner join membership.policy p on p.policy_id = c.policy_id " +
			"inner join membership.plan pn on pn.plan_id = p.plan_id " +
			" inner join claims.invoice inv on inv.visit_number = v.visit_number " +
			"where v.hospital_provider_id =?1 and v.member_number = ?2 and v.status !='ACTIVE'" +
			" and v.status !='CANCELLED' and v.status " +
			"!='INACTIVE' order by v.visit_number DESC", nativeQuery = true)
	fun findPagedClosedVisitsWithMemberNo(
		@Param("hospital_provider_id") hospital_provider_id: Long,
		@Param("memberNo") memberNo: String?,
	): MutableList<ProviderClaimsDTO>

	@Query("SELECT v.visit_number as visitNumber, v.status as status, v.hospital_provider_id as hospitalProviderId, v.member_name as memberName," +
			"v.member_number as memberNumber, v.benefit_name as benefitName, pn.plan_name as " +
			"schemeName, ( select payer_name from membership.payer where " +
			"payer_id = v.payer_id ) as payerName, inv.total_amount as invoiceAmount, inv.invoice_number as invoiceNumber, v.created_at as createdAt," +
			"( select provider_name from membership.provider where provider_id = v.hospital_provider_id ) as providerName FROM visit v " +
			"inner join membership.category c on c.category_id = v.category_id " +
			"inner join membership.policy p on p.policy_id = c.policy_id " +
			"inner join membership.plan pn on pn.plan_id = p.plan_id " +
			" inner join claims.invoice inv on inv.visit_number = v.visit_number " +
			"where v.hospital_provider_id =?1 and inv.invoice_number = ?2 and v.member_number = " +
			"?3 and v.status !='ACTIVE'" +
			" and v.status !='CANCELLED' and v.status " +
			"!='INACTIVE' order by v.visit_number DESC", nativeQuery = true)
	fun findPagedClosedVisitsWithMemberNoAndInvoiceNo(
		@Param("hospital_provider_id") hospital_provider_id: Long,
		@Param("invoiceNo") invoiceNo: String?,
		@Param("memberNo") memberNo: String?,
	): MutableList<ProviderClaimsDTO>


	@Query("SELECT * from visit where hospital_provider_id  = ?1 and status != ?2 order by visit_number DESC",
	nativeQuery = true)
	fun findClosedVisitsList(
		@Param
			("hospital_provider_id") hospital_provider_id: Long,
		@Param("status") status: Status
	):
			MutableList<Visit?>

	@Query("SELECT * from visit where hospital_provider_id  = ?1 and status != 'ACTIVE' order by visit_number DESC",
		countQuery = "SELECT * from visit where hospital_provider_id  = ?1 and status != 'ACTIVE'  order by visit_number DESC",
		nativeQuery = true)
	fun findPagedClosedVisits(
		@Param("hospital_provider_id") hospital_provider_id: Long,
		pageable: Pageable
	): Page<Visit?>

	@Query("SELECT * from visit  where hospital_provider_id = ?1 and status != 'ACTIVE' and " +
			"member_number LIKE %?2% order by visit_number DESC",
		countQuery = "SELECT * from visit  where hospital_provider_id = ?1 and status != 'ACTIVE' and " +
				"member_number LIKE %?2% order by visit_number DESC",
		nativeQuery = true)
	fun findPagedClosedVisitsWithMemberNo(
		@Param
			("hospital_provider_id") hospital_provider_id: Long,
		@Param("memberNo") memberNo: String,
		pageable: Pageable
	): Page<Visit?>

	@Query("SELECT v.*, inv.invoice_number as invoiceNumber from visit v inner join claims.invoice inv on inv.visit_number = v" +
			".visit_number where v.hospital_provider_id = ?1 and v.status != 'ACTIVE' and " +
			"inv.invoice_number LIKE %?2% order by visit_number DESC",
		countQuery = "SELECT v.*, inv.invoice_number as invoiceNumber from visit v inner join claims.invoice inv on inv.visit_number = v" +
				".visit_number where v.hospital_provider_id = ?1 and v.status != 'ACTIVE' and " +
				"inv.invoice_number LIKE %?2%  order by visit_number DESC",
		nativeQuery = true)
	fun findPagedClosedVisitsWithInvoiceNo(
		@Param
			("hospital_provider_id") hospital_provider_id: Long,
		@Param("invoiceNo") invoiceNo: String,
		pageable: Pageable
	): Page<Visit?>

	@Query("SELECT v.*, inv.invoice_number as invoiceNumber from visit v inner join claims.invoice inv on inv.visit_number = v" +
			".visit_number where v.hospital_provider_id = ?1 and v.status != 'ACTIVE' and " +
			"inv.invoice_number LIKE %?2% and v.member_number LIKE %?3% order by visit_number DESC",
		countQuery = "SELECT v.*, inv.invoice_number as invoiceNumber from visit v inner join claims.invoice inv on inv.visit_number" +
				" = v.visit_number where v.hospital_provider_id = ?1 and v.status != 'ACTIVE' and" +
				" " +
				"inv.invoice_number LIKE %?2%  and v.member_number LIKE %?3%  order by visit_number DESC",
		nativeQuery = true)
	fun findPagedClosedVisitsWithInvoiceNoAndMemberNo(
		@Param
			("hospital_provider_id") hospital_provider_id: Long,
		@Param("invoiceNo") invoiceNo: String,
		@Param("memberNo") memberNo: String,
		pageable: Pageable
	): Page<Visit?>

	fun findAllByStatus(
		@Param("status") status: Status,
	): MutableList<Visit?>

	fun findAllByHospitalProviderIdAndInvoiceNumber(
		@Param
			("hospital_provider_id") hospital_provider_id: Long,
		@Param("invoiceNumber") invoiceNumber: String
	): Optional<Visit>

	@Query(
		"SELECT * FROM visit  where member_number LIKE ?1%",
		nativeQuery = true
	)
	fun findByMemberNumberLike(familyNumber: String): MutableList<Visit?>


	@Query(
		"SELECT * FROM visit  where member_number LIKE ?1%",
		nativeQuery = true
	)
	fun findByMemberNumberLikeAndCreatedAtGreaterThanEqualAndLessThanEqual(familyNumber: String, startDate: LocalDate, endDate: LocalDate): MutableList<Visit?>


	fun findByHospitalProviderId(hospitalProviderId: Long): List<Visit>





	@Query(
		"SELECT v.member_name, v.member_number, v.benefit_name, v.status, ( select plan_name from" +
				" membership.plan " +
				"where plan_id = ( select plan_id from membership.policy where policy_id =( select policy_id from membership.category " +
				"where category_id = v.category_id ) ) ) as scheme, ( select payer_name from membership.payer where payer_id = v.payer_id ) as payer," +
				" v.total_invoice_amount, v.invoice_number, v.created_at, ( select provider_name from membership.provider " +
				"where provider_id = v.hospital_provider_id ) as provider FROM visit v " +
				"where v.member_number LIKE ?1% and v.status !='CANCELLED' and v.status !='INACTIVE' " +
				"order by v.benefit_name",
		nativeQuery = true
	)
	fun findMemberStatementByMemberNumber(memberNumber: String): MutableList<MembersStatementsDTO>

	@Query(
		"SELECT v.visit_number as visitNumber,v.status as status,v.hospital_provider_id as hospitalProviderId, v.member_name as memberName," +
				" v.member_number as" +
				" memberNumber, v.benefit_name as benefitName, pn.plan_name as schemeName, v" +
				".visit_type as visitType," +
				"(select payer_name from membership.payer where payer_id = v.payer_id) as payerName,v.total_invoice_amount as invoiceAmount,v.invoice_number as invoiceNumber, v.created_at as createdAt," +
				"(select provider_name from membership.provider where provider_id = v.hospital_provider_id) as providerName FROM visit v " +
				"inner join membership.category c on c.category_id = v.category_id inner join membership.policy p on p.policy_id = c.policy_id " +
				"inner join membership.plan pn on pn.plan_id = p.plan_id where date(v.created_at) >= " +
				"?2 and date(v.created_at) " +
				"<= ?3 and p.plan_id = ?1 " + "and v.status != 'ACTIVE' and v.visit_type = 'OFF_LCT'" +
				" order by v.visit_number DESC",
		nativeQuery = true
	)
	fun findPlanOfflcts(
		@Param("planId") planId: String,
		@Param("dateFrom") dateFrom: String,
		@Param("dateTo") dateTo: String
	): MutableList<GeneralClaimDTO>


	@Query(
		"SELECT v.visit_number as visitNumber,v.status as status,v.hospital_provider_id as hospitalProviderId, v.member_name as memberName," +
				" v.member_number as" +
				" memberNumber, v.benefit_name as benefitName, pn.plan_name as schemeName, v" +
				".visit_type as visitType," +
				"(select payer_name from membership.payer where payer_id = v.payer_id) as payerName,v.total_invoice_amount as invoiceAmount,v.invoice_number as invoiceNumber, v.created_at as createdAt," +
				"(select provider_name from membership.provider where provider_id = v.hospital_provider_id) as providerName FROM visit v " +
				"inner join membership.category c on c.category_id = v.category_id inner join membership.policy p on p.policy_id = c.policy_id " +
				"inner join membership.plan pn on pn.plan_id = p.plan_id where date(v.created_at) >= " +
				"?2 and date(v.created_at) " +
				"<= ?3 and p.plan_id = ?1 " + "and v.status != 'ACTIVE' and v.visit_type = 'REIMBURSEMENT'" +
				" order by v.visit_number DESC",
		nativeQuery = true
	)
	fun findPlanReimbursementsPdf(
		@Param("planId") planId: String,
		@Param("dateFrom") dateFrom: String,
		@Param("dateTo") dateTo: String
	): MutableList<GeneralClaimDTO>


	@Query(
		"SELECT v.visit_number as visitNumber,v.status as status,v.hospital_provider_id as hospitalProviderId, v.member_name as memberName," +
				" v.member_number as" +
				" memberNumber, v.benefit_name as benefitName, pn.plan_name as schemeName, v" +
				".visit_type as visitType," +
				"(select payer_name from membership.payer where payer_id = v.payer_id) as payerName,v.total_invoice_amount as invoiceAmount,v.invoice_number as invoiceNumber, v.created_at as createdAt," +
				"(select provider_name from membership.provider where provider_id = v.hospital_provider_id) as providerName FROM visit v " +
				"inner join membership.category c on c.category_id = v.category_id inner join membership.policy p on p.policy_id = c.policy_id " +
				"inner join membership.plan pn on pn.plan_id = p.plan_id where date(v.created_at) >= ?3 and date(v.created_at) " +
				"<= ?4 and p.plan_id = ?1 " + "and v.status != 'ACTIVE' and v.visit_type = 'OFF_LCT' " +
				" and v.hospital_provider_id= ?2 " +
				" order by v.visit_number DESC",
		nativeQuery = true
	)
	fun findPlanOfflctsWithProvider(
		@Param("planId") planId: String,
		@Param("hospitalProviderId") hospitalProviderId: String,
		@Param("dateFrom") dateFrom: String,
		@Param("dateTo") dateTo: String
	): MutableList<GeneralClaimDTO>

	@Query("SELECT v.visit_number as visitNumber, v.status as status, v.hospital_provider_id as hospitalProviderId, v.member_name as memberName," +
			" v.member_number as memberNumber, v.benefit_name as benefitName, pn.plan_name as schemeName, ( select payer_name from membership.payer where" +
			" payer_id = v.payer_id ) as payerName, inv.total_amount as invoiceAmount, inv.invoice_number as invoiceNumber, v.created_at as createdAt," +
			" ( select provider_name from membership.provider where provider_id = v.hospital_provider_id ) as providerName FROM visit v " +
			"inner join membership.category c on c.category_id = v.category_id " +
			"inner join membership.policy p on p.policy_id = c.policy_id " +
			"inner join membership.plan pn on pn.plan_id = p.plan_id " +
			" inner join claims.invoice inv on inv.visit_number = v.visit_number " +
			"where  date(v.created_at) >= ?2 and date(v" +
			".created_at) <= ?3 and v.status !='ACTIVE' and v.status !='CANCELLED' and v" +
			".hospital_provider_id in (select provider_id from membership.provider where " +
			"main_facility_id = ?1) " +
			" and v.status!='INACTIVE' order by v.visit_number DESC", nativeQuery = true)
	fun findProviderClaimsTest(
		@Param("hospitalProviderId") hospitalProviderId: Long,
		@Param("dateFrom") dateFrom: String,
		@Param("dateTo") dateTo: String
	): MutableList<ProviderClaimsDTO>



	@Query("SELECT v.visit_number as visitNumber, v.status as status, v.hospital_provider_id as hospitalProviderId, v.member_name as memberName," +
			" v.member_number as memberNumber, v.benefit_name as benefitName, pn.plan_name as schemeName, ( select payer_name from membership.payer where" +
			" payer_id = v.payer_id ) as payerName, inv.total_amount as invoiceAmount, inv.invoice_number as invoiceNumber, v.created_at as createdAt," +
			" ( select provider_name from membership.provider where provider_id = v.hospital_provider_id ) as providerName FROM visit v " +
			"inner join membership.category c on c.category_id = v.category_id " +
			"inner join membership.policy p on p.policy_id = c.policy_id " +
			"inner join membership.plan pn on pn.plan_id = p.plan_id " +
			" inner join claims.invoice inv on inv.visit_number = v.visit_number " +
			"where v.hospital_provider_id =?2 and date(v.created_at) >= ?3 and date(v" +
			".created_at) <= ?4 and " +
			"p.plan_id = ?1 and v.status !='ACTIVE' and v.status !='CANCELLED' and v.status " +
			"!='INACTIVE' order by v.visit_number DESC", nativeQuery = true)
	fun findProviderClaims(
		@Param("planId") planId: String,
		@Param("hospitalProviderId") hospitalProviderId: Long,
		@Param("dateFrom") dateFrom: String,
		@Param("dateTo") dateTo: String
	): MutableList<ProviderClaimsDTO>


	@Query("SELECT v.visit_number as visitNumber, v.status as status, v.hospital_provider_id as hospitalProviderId, v.member_name as memberName," +
			" v.member_number as memberNumber, v.benefit_name as benefitName, pn.plan_name as schemeName, ( select payer_name from membership.payer where" +
			" payer_id = v.payer_id ) as payerName, inv.total_amount as invoiceAmount, inv.invoice_number as invoiceNumber, v.created_at as createdAt," +
			" ( select provider_name from membership.provider where provider_id = v.hospital_provider_id ) as providerName FROM visit v " +
			"inner join membership.category c on c.category_id = v.category_id " +
			"inner join membership.policy p on p.policy_id = c.policy_id " +
			"inner join membership.plan pn on pn.plan_id = p.plan_id " +
			" inner join claims.invoice inv on inv.visit_number = v.visit_number " +
			"where v.hospital_provider_id =?2 and date(v.created_at) >= ?3 and date(v" +
			".created_at) <= ?4 and " +
			"p.plan_id = ?1 and v.status !='ACTIVE' and v.status !='CANCELLED' and v.status and v" +
			".hospital_provider_id IN ?5 " +
			"!='INACTIVE' order by v.visit_number DESC", nativeQuery = true)
	fun findProviderClaimsStatement(
		@Param("planId") planId: String,
		@Param("hospitalProviderId") hospitalProviderId: Long,
		@Param("dateFrom") dateFrom: String,
		@Param("dateTo") dateTo: String
	): MutableList<ProviderClaimsDTO>

	@Query("SELECT pp.code,pp.payer_id,pp.provider_id, p.provider_name as providerName FROM membership" +
			".payer_provider_mapping pp" +
			" inner join membership.provider p on p.provider_id = pp.provider_id where payer_id =" +
			" ?1 AND code is not null;", nativeQuery = true)
	fun findPayerProviders(
		@Param("payerId") planId: Long,

	): MutableList<PayerProvidersDTO>


	@Query(
		"SELECT v.visit_number as visitNumber,v.status as status,v.hospital_provider_id as " +
				"hospitalProviderId,v.payer_id as payerId, v.member_name as memberName," +
				" v.member_number as" +
				" memberNumber, v.benefit_name as benefitName, pn.plan_name as schemeName," +
				"(select payer_name from membership.payer where payer_id = v.payer_id) as payerName,inv.total_amount as invoiceAmount,inv.invoice_number as invoiceNumber, v.created_at as createdAt," +
				"(select provider_name from membership.provider where provider_id = v.hospital_provider_id) as providerName FROM visit v " +
				"inner join membership.category c on c.category_id = v.category_id inner join membership.policy p on p.policy_id = c.policy_id " +
				"inner join membership.plan pn on pn.plan_id = p.plan_id " +
				" inner join claims.invoice inv on inv.visit_number = v.visit_number " +
				" where v.hospital_provider_id" +
				" = ?2 and date(v.created_at) >= ?3 and date(v.created_at) <= ?4 and p.plan_id = ?1 " +
				"and v.status != 'ACTIVE'  and v.status != 'REJECTED' and v.status != 'CANCELLED'  and v.status != 'INACTIVE'" +
				" and v.payer_id = ?5 and v" +
				".member_number LIKE ?6 and v" +
				".invoice_number = ?7" +
				" order by v.visit_number DESC",
		nativeQuery = true
	)
	fun findProviderClaimWithPayerMemberNoInvoiceNo(
		@Param("planId") planId: String,
		@Param("hospitalProviderId") hospitalProviderId: Long,
		@Param("dateFrom") dateFrom: String,
		@Param("dateTo") dateTo: String,
		@Param("payerId") payerId: String?,
		@Param("memberNo") memberNo: String?,
		@Param("invoiceNo") invoiceNo: String?
	): MutableList<ProviderClaimsDTO>


	@Query(
		"SELECT v.visit_number as visitNumber,v.status as status,v.hospital_provider_id as " +
				"hospitalProviderId,v.payer_id as payerId, v.member_name as memberName," +
				" v.member_number as" +
				" memberNumber, v.benefit_name as benefitName, pn.plan_name as schemeName," +
				"(select payer_name from membership.payer where payer_id = v.payer_id) as payerName,inv.total_amount as invoiceAmount,inv.invoice_number as invoiceNumber, v.created_at as createdAt," +
				"(select provider_name from membership.provider where provider_id = v.hospital_provider_id) as providerName FROM visit v " +
				"inner join membership.category c on c.category_id = v.category_id inner join membership.policy p on p.policy_id = c.policy_id " +
				"inner join membership.plan pn on pn.plan_id = p.plan_id " +
				" inner join claims.invoice inv on inv.visit_number = v.visit_number " +
				"where v.hospital_provider_id" +
				" = ?2 and date(v.created_at) >= ?3 and date(v.created_at) <= ?4 and p.plan_id = ?1 " +
				"and v.status != 'ACTIVE'  and v.status != 'REJECTED' and v.status != 'CANCELLED'  and v.status != 'INACTIVE'" +
				" and v.payer_id = ?5 and v" +
				".member_number LIKE ?6" +
				" order by v.visit_number DESC",
		nativeQuery = true
	)
	fun findProviderClaimWithPayerMemberNo(
		@Param("planId") planId: String,
		@Param("hospitalProviderId") hospitalProviderId: Long,
		@Param("dateFrom") dateFrom: String,
		@Param("dateTo") dateTo: String,
		@Param("payerId") payerId: String?,
		@Param("memberNo") memberNo: String?
	): MutableList<ProviderClaimsDTO>


	@Query(
		"SELECT v.visit_number as visitNumber,v.status as status,v.hospital_provider_id as " +
				"hospitalProviderId,v.payer_id as payerId, v.member_name as memberName," +
				" v.member_number as" +
				" memberNumber, v.benefit_name as benefitName, pn.plan_name as schemeName," +
				"(select payer_name from membership.payer where payer_id = v.payer_id) as payerName,inv.total_amount as invoiceAmount,inv.invoice_number as invoiceNumber, v.created_at as createdAt," +
				"(select provider_name from membership.provider where provider_id = v.hospital_provider_id) as providerName FROM visit v " +
				"inner join membership.category c on c.category_id = v.category_id inner join membership.policy p on p.policy_id = c.policy_id " +
				"inner join membership.plan pn on pn.plan_id = p.plan_id " +
				" inner join claims.invoice inv on inv.visit_number = v.visit_number " +
				"where v.hospital_provider_id" +
				" = ?2 and date(v.created_at) >= ?3 and date(v.created_at) <= ?4 and p.plan_id = ?1 " +
				"and v.status != 'ACTIVE'  and v.status != 'REJECTED' and v.status != 'CANCELLED'  and v.status != 'INACTIVE'" +
				" and v.payer_id = ?5  and " +
				"v" +
				".invoice_number = ?6" +
				" order by v.visit_number DESC",
		nativeQuery = true
	)
	fun findProviderClaimWithPayerInvoiceNo(
		@Param("planId") planId: String,
		@Param("hospitalProviderId") hospitalProviderId: Long,
		@Param("dateFrom") dateFrom: String,
		@Param("dateTo") dateTo: String,
		@Param("payerId") payerId: String?,
		@Param("invoiceNo") invoiceNo: String?
	): MutableList<ProviderClaimsDTO>

	@Query(
		"SELECT v.visit_number as visitNumber,v.status as status,v.hospital_provider_id as " +
				"hospitalProviderId,v.payer_id as payerId, v.member_name as memberName," +
				" v.member_number as" +
				" memberNumber, v.benefit_name as benefitName, pn.plan_name as schemeName," +
				"(select payer_name from membership.payer where payer_id = v.payer_id) as payerName,inv.total_amount as invoiceAmount,inv.invoice_number as invoiceNumber, v.created_at as createdAt," +
				"(select provider_name from membership.provider where provider_id = v.hospital_provider_id) as providerName FROM visit v " +
				"inner join membership.category c on c.category_id = v.category_id inner join membership.policy p on p.policy_id = c.policy_id " +
				"inner join membership.plan pn on pn.plan_id = p.plan_id " +
				" inner join claims.invoice inv on inv.visit_number = v.visit_number " +
				"where v.hospital_provider_id" +
				" = ?2 and date(v.created_at) >= ?3 and date(v.created_at) <= ?4 and p.plan_id = ?1 " +
				"and v.status != 'ACTIVE'  and v.status != 'CANCELLED'  and v.status != " +
				"'INACTIVE'  and v.status != 'REJECTED'  and v" +
				".invoice_number = ?5" +
				" order by v.visit_number DESC",
		nativeQuery = true
	)
	fun findProviderClaimWithInvoiceNo(
		@Param("planId") planId: String,
		@Param("hospitalProviderId") hospitalProviderId: Long,
		@Param("dateFrom") dateFrom: String,
		@Param("dateTo") dateTo: String,
		@Param("invoiceNo") invoiceNo: String?
	): MutableList<ProviderClaimsDTO>

	@Query(
		"SELECT v.visit_number as visitNumber,v.status as status,v.hospital_provider_id as " +
				"hospitalProviderId,v.payer_id as payerId, v.member_name as memberName," +
				" v.member_number as" +
				" memberNumber, v.benefit_name as benefitName, pn.plan_name as schemeName," +
				"(select payer_name from membership.payer where payer_id = v.payer_id) as payerName,inv.total_amount as invoiceAmount,inv.invoice_number as invoiceNumber, v.created_at as createdAt," +
				"(select provider_name from membership.provider where provider_id = v.hospital_provider_id) as providerName FROM visit v " +
				"inner join membership.category c on c.category_id = v.category_id inner join membership.policy p on p.policy_id = c.policy_id " +
				"inner join membership.plan pn on pn.plan_id = p.plan_id " +
				" inner join claims.invoice inv on inv.visit_number = v.visit_number " +
				"where v.hospital_provider_id" +
				" = ?2 and date(v.created_at) >= ?3 and date(v.created_at) <= ?4 and p.plan_id = ?1 " +
				"and v.status != 'ACTIVE'  and v.status != 'CANCELLED'  and v.status != " +
				"'INACTIVE'  and v.status != 'REJECTED'  and v.member_number LIKE " +
				"?5" +
				" order by v.visit_number DESC",
		nativeQuery = true
	)
	fun findProviderClaimWithMemberNo(
		@Param("planId") planId: String,
		@Param("hospitalProviderId") hospitalProviderId: Long,
		@Param("dateFrom") dateFrom: String,
		@Param("dateTo") dateTo: String,
		@Param("memberNo") memberNo: String?
	): MutableList<ProviderClaimsDTO>

	@Query(
		"SELECT v.visit_number as visitNumber,v.status as status,v.hospital_provider_id as " +
				"hospitalProviderId,v.payer_id as payerId, v.member_name as memberName," +
				" v.member_number as" +
				" memberNumber, v.benefit_name as benefitName, pn.plan_name as schemeName," +
				"(select payer_name from membership.payer where payer_id = v.payer_id) as payerName,inv.total_amount as invoiceAmount,inv.invoice_number as invoiceNumber, v.created_at as createdAt," +
				"(select provider_name from membership.provider where provider_id = v.hospital_provider_id) as providerName FROM visit v " +
				"inner join membership.category c on c.category_id = v.category_id inner join membership.policy p on p.policy_id = c.policy_id " +
				"inner join membership.plan pn on pn.plan_id = p.plan_id" +
				" inner join claims.invoice inv on inv.visit_number = v.visit_number " +
				" where v.hospital_provider_id" +
				" = ?2 and date(v.created_at) >= ?3 and date(v.created_at) <= ?4 and p.plan_id = ?1 " +
				"and v.status != 'ACTIVE'  and v.status != 'CANCELLED'  and v.status != " +
				"'INACTIVE' and v.status != 'REJECTED' and v.payer_id = ?5 " +
				" order by v.visit_number DESC",
		nativeQuery = true
	)
	fun findProviderClaimWithPayer(
		@Param("planId") planId: String,
		@Param("hospitalProviderId") hospitalProviderId: Long,
		@Param("dateFrom") dateFrom: String,
		@Param("dateTo") dateTo: String,
		@Param("payerId") payerId: String?
	): MutableList<ProviderClaimsDTO>


	@Query(
		"SELECT v.visit_number as visitNumber,v.status as status,v.hospital_provider_id as hospitalProviderId, v.member_name as memberName," +
				" v.member_number as" +
				" memberNumber, v.benefit_name as benefitName, pn.plan_name as schemeName, v" +
				".visit_type as visitType, v.visit_type as visitType," +
				"(select payer_name from membership.payer where payer_id = v.payer_id) as payerName,v.total_invoice_amount as invoiceAmount,v.invoice_number as invoiceNumber, v.created_at as createdAt," +
				"(select provider_name from membership.provider where provider_id = v.hospital_provider_id) as providerName FROM visit v " +
				"inner join membership.category c on c.category_id = v.category_id inner join membership.policy p on p.policy_id = c.policy_id " +
				"inner join membership.plan pn on pn.plan_id = p.plan_id where date(v.created_at)" +
				" >= ?1 and date(v.created_at) " +
				"<= ?2 and v.status = ?3 order by v.visit_number DESC",
		nativeQuery = true
	)
	fun findAllTransactionsDateOnly(
		@Param("dateFrom") dateFrom: String,
		@Param("dateTo") dateTo: String,
		@Param("status") status: String
	): MutableList<GeneralClaimDTO>

	@Query(
		"SELECT v.visit_number as visitNumber,v.status as status,v.hospital_provider_id as hospitalProviderId, v.member_name as memberName," +
				" v.member_number as" +
				" memberNumber, v.benefit_name as benefitName, pn.plan_name as schemeName, v" +
				".visit_type as visitType," +
				"(select payer_name from membership.payer where payer_id = v.payer_id) as payerName,v.total_invoice_amount as invoiceAmount,v.invoice_number as invoiceNumber, v.created_at as createdAt," +
				"(select provider_name from membership.provider where provider_id = v.hospital_provider_id) as providerName FROM visit v " +
				"inner join membership.category c on c.category_id = v.category_id inner join membership.policy p on p.policy_id = c.policy_id " +
				"inner join membership.plan pn on pn.plan_id = p.plan_id where date(v.created_at)" +
				" >= ?1 and date(v.created_at) " +
				"<= ?2 and v.status = ?4 and v.hospital_provider_id= ?3 order by v" +
				".visit_number DESC",
		nativeQuery = true
	)
	fun findAllTransactionsDateAndProvider(
		@Param("dateFrom") dateFrom: String,
		@Param("dateTo") dateTo: String,
		@Param("hospitalProviderId") hospitalProviderId: String,
		@Param("status") status: String
	): MutableList<GeneralClaimDTO>

	@Query(
		"SELECT v.visit_number as visitNumber,v.status as status,v.hospital_provider_id as hospitalProviderId, v.member_name as memberName," +
				" v.member_number as" +
				" memberNumber, v.benefit_name as benefitName, pn.plan_name as schemeName, v" +
				".visit_type as visitType," +
				"(select payer_name from membership.payer where payer_id = v.payer_id) as payerName,v.total_invoice_amount as invoiceAmount,v.invoice_number as invoiceNumber, v.created_at as createdAt," +
				"(select provider_name from membership.provider where provider_id = v.hospital_provider_id) as providerName FROM visit v " +
				"inner join membership.category c on c.category_id = v.category_id inner join membership.policy p on p.policy_id = c.policy_id " +
				"inner join membership.plan pn on pn.plan_id = p.plan_id where date(v.created_at)" +
				" >= ?1 and date(v.created_at) " +
				"<= ?2 and p.plan_id = ?4 " + "and v.status = ?5 " +
				" and v.hospital_provider_id= ?3 " +
				" order by v.visit_number DESC",
		nativeQuery = true
	)
	fun findAllTransactionsWithDateAndProviderAndPlan(
		@Param("dateFrom") dateFrom: String,
		@Param("dateTo") dateTo: String,
		@Param("hospitalProviderId") hospitalProviderId: String,
		@Param("planId") planId: String,
		@Param("status") status: String

	): MutableList<GeneralClaimDTO>

	@Query(
		"SELECT v.visit_number as visitNumber,v.status as status,v.hospital_provider_id as hospitalProviderId, v.member_name as memberName," +
				" v.member_number as" +
				" memberNumber, v.benefit_name as benefitName, pn.plan_name as schemeName, v" +
				".visit_type as visitType," +
				"(select payer_name from membership.payer where payer_id = v.payer_id) as payerName,v.total_invoice_amount as invoiceAmount,v.invoice_number as invoiceNumber, v.created_at as createdAt," +
				"(select provider_name from membership.provider where provider_id = v.hospital_provider_id) as providerName FROM visit v " +
				"inner join membership.category c on c.category_id = v.category_id inner join membership.policy p on p.policy_id = c.policy_id " +
				"inner join membership.plan pn on pn.plan_id = p.plan_id where date(v.created_at)" +
				" >= ?1 and date(v.created_at) " +
				"<= ?2 and p.plan_id = ?4 " + "and v.status = ?6 " +
				" and v.hospital_provider_id= ?3 and v.payer_id = ?5 " +
				" order by v.visit_number DESC",
		nativeQuery = true
	)
	fun findAllTransactionsWithDateAndProviderAndPlanAndPayer(
		@Param("dateFrom") dateFrom: String,
		@Param("dateTo") dateTo: String,
		@Param("hospitalProviderId") hospitalProviderId: String,
		@Param("planId") planId: String,
		@Param("payerId") payerId: String?,
		@Param("status") status: String
		): MutableList<GeneralClaimDTO>

	@Query(
		"SELECT v.visit_number as visitNumber,v.status as status,v.hospital_provider_id as hospitalProviderId, v.member_name as memberName," +
				" v.member_number as" +
				" memberNumber, v.benefit_name as benefitName, pn.plan_name as schemeName, v" +
				".visit_type as visitType," +
				"(select payer_name from membership.payer where payer_id = v.payer_id) as payerName,v.total_invoice_amount as invoiceAmount,v.invoice_number as invoiceNumber, v.created_at as createdAt," +
				"(select provider_name from membership.provider where provider_id = v.hospital_provider_id) as providerName FROM visit v " +
				"inner join membership.category c on c.category_id = v.category_id inner join membership.policy p on p.policy_id = c.policy_id " +
				"inner join membership.plan pn on pn.plan_id = p.plan_id where date(v.created_at)" +
				" >= ?1 and date(v.created_at) " +
				"<= ?2 and p.plan_id = ?4 " + "and v.status = ?7 " +
				" and v.hospital_provider_id= ?3 and v.payer_id = ?5 and v.invoice_number =?6" +
				" order by v.visit_number DESC",
		nativeQuery = true
	)
	fun findAllTransactionsWithDateAndProviderAndPlanAndPayerAndInvoice(
		@Param("dateFrom") dateFrom: String,
		@Param("dateTo") dateTo: String,
		@Param("hospitalProviderId") hospitalProviderId: String,
		@Param("planId") planId: String,
		@Param("payerId") payerId: String?,
		@Param("invoiceNo") invoiceNo: String?,
		@Param("status") status: String
	): MutableList<GeneralClaimDTO>

	@Query(
		"SELECT v.visit_number as visitNumber,v.status as status,v.hospital_provider_id as hospitalProviderId, v.member_name as memberName," +
				" v.member_number as" +
				" memberNumber, v.benefit_name as benefitName, pn.plan_name as schemeName, v" +
				".visit_type as visitType," +
				"(select payer_name from membership.payer where payer_id = v.payer_id) as payerName,v.total_invoice_amount as invoiceAmount,v.invoice_number as invoiceNumber, v.created_at as createdAt," +
				"(select provider_name from membership.provider where provider_id = v.hospital_provider_id) as providerName FROM visit v " +
				"inner join membership.category c on c.category_id = v.category_id inner join membership.policy p on p.policy_id = c.policy_id " +
				"inner join membership.plan pn on pn.plan_id = p.plan_id where date(v.created_at)" +
				" >= ?1 and date(v.created_at) " +
				"<= ?2 and p.plan_id = ?4 " + "and v.status = ?8 " +
				" and v.hospital_provider_id= ?3 and v.payer_id = ?5 and (v.invoice_number =?6 or" +
				" v.member_number =?7)" +
				" order by v.visit_number DESC",
		nativeQuery = true
	)
	fun findAllTransactionsWithDateAndProviderAndPlanAndPayerAndInvoiceAndMemberNo(
		@Param("dateFrom") dateFrom: String,
		@Param("dateTo") dateTo: String,
		@Param("hospitalProviderId") hospitalProviderId: String,
		@Param("planId") planId: String,
		@Param("payerId") payerId: String?,
		@Param("invoiceNo") invoiceNo: String?,
		@Param("memberNo") memberNo: String?,
		@Param("status") status: String
	): MutableList<GeneralClaimDTO>


	@Query(
		"SELECT v.visit_number as visitNumber,v.status as status,v.hospital_provider_id as " +
				"hospitalProviderId, " +
				"v.member_name as " +
				"memberName," +
				" v.member_number as" +
				" memberNumber, v.benefit_name as benefitName, pn.plan_name as schemeName," +
				"(select payer_name from membership.payer where payer_id = v.payer_id) as payerName,v.total_invoice_amount as invoiceAmount,v.invoice_number as invoiceNumber, v.created_at as createdAt," +
				"(select provider_name from membership.provider where provider_id = v.hospital_provider_id) as providerName FROM visit v " +
				"inner join membership.category c on c.category_id = v.category_id inner join membership.policy p on p.policy_id = c.policy_id " +
				"inner join membership.plan pn on pn.plan_id = p.plan_id where date(v.created_at) >= ?2 and date(v.created_at) <= ?3 and p.plan_id = ?1 " +
				"and v.status != 'ACTIVE'" +
				"order by v.visit_number DESC",
		nativeQuery = true
	)
	fun findPlanClaims(
		@Param("planId") planId: String,
		@Param("dateFrom") dateFrom: String,
		@Param("dateTo") dateTo: String
	): MutableList<ProviderClaimsDTO>


	@Query(
		"SELECT v.visit_number as visitNumber, v.status as status, v.payer_id as payerId, v" +
				".hospital_provider_id as hospitalProviderId, v.member_name as memberName, v.member_number as memberNumber, v.benefit_name as benefitName," +
				" pn.plan_name as schemeName, ( select payer_name from membership.payer where payer_id = v.payer_id ) as payerName, v.total_invoice_amount as" +
				" invoiceAmount, v.invoice_number as invoiceNumber, v.created_at as createdAt, ( select provider_name from membership.provider where provider_id " +
				"= v.hospital_provider_id ) as providerName FROM visit v inner join membership.category c on c.category_id = v.category_id inner join membership.policy" +
				" p on p.policy_id = c.policy_id inner join membership.plan pn on pn.plan_id = p.plan_id where date(v.created_at) >= ?2 and date(v.created_at)" +
				" <= ?3 and p.plan_id = ?1 and v.status !='ACTIVE' and v.payer_id " +
				"= ?4 and v.member_number LIKE ?5 and v.invoice_number = ?6 order " +
				"by v.visit_number DESC",
		nativeQuery = true
	)
	fun findPlanClaimsWithPayerMemberNoInvoiceNo(
		@Param("planId") planId: String,
		@Param("dateFrom") dateFrom: String,
		@Param("dateTo") dateTo: String,
		@Param("payerId") payerId: String,
		@Param("memberNo") memberNo: String,
		@Param("invoiceNo") invoiceNo: String
	): MutableList<ProviderClaimsDTO>

	@Query(
		"SELECT v.visit_number as visitNumber, v.status as status, v.payer_id as payerId, v" +
				".hospital_provider_id as hospitalProviderId, v.member_name as memberName, v.member_number as memberNumber, v.benefit_name as benefitName," +
				" pn.plan_name as schemeName, ( select payer_name from membership.payer where payer_id = v.payer_id ) as payerName, v.total_invoice_amount as" +
				" invoiceAmount, v.invoice_number as invoiceNumber, v.created_at as createdAt, ( select provider_name from membership.provider where provider_id " +
				"= v.hospital_provider_id ) as providerName FROM visit v inner join membership.category c on c.category_id = v.category_id inner join membership.policy" +
				" p on p.policy_id = c.policy_id inner join membership.plan pn on pn.plan_id = p.plan_id where date(v.created_at) >= ?2 and date(v.created_at)" +
				" <= ?3 and p.plan_id = ?1 and v.status !='ACTIVE' and v.payer_id " +
				"= ?4 and v.member_number LIKE ?5 order " +
				"by v.visit_number DESC",
		nativeQuery = true
	)
	fun findPlanClaimsWithPayerMemberNo(
		@Param("planId") planId: String,
		@Param("dateFrom") dateFrom: String,
		@Param("dateTo") dateTo: String,
		@Param("payerId") payerId: String,
		@Param("memberNo") memberNumber: String,
	): MutableList<ProviderClaimsDTO>


	@Query(
		"SELECT v.visit_number as visitNumber, v.status as status, v.payer_id as payerId, v" +
				".hospital_provider_id as hospitalProviderId, v.member_name as memberName, v.member_number as memberNumber, v.benefit_name as benefitName," +
				" pn.plan_name as schemeName, ( select payer_name from membership.payer where payer_id = v.payer_id ) as payerName, v.total_invoice_amount as" +
				" invoiceAmount, v.invoice_number as invoiceNumber, v.created_at as createdAt, ( select provider_name from membership.provider where provider_id " +
				"= v.hospital_provider_id ) as providerName FROM visit v inner join membership.category c on c.category_id = v.category_id inner join membership.policy" +
				" p on p.policy_id = c.policy_id inner join membership.plan pn on pn.plan_id = p.plan_id where date(v.created_at) >= ?2 and date(v.created_at)" +
				" <= ?3 and p.plan_id = ?1 and v.status !='ACTIVE' and v.payer_id " +
				"= ?4 and v.invoice_number LIKE ?5 order " +
				"by v.visit_number DESC",
		nativeQuery = true
	)
	fun findPlanClaimsWithPayerInvoiceNo(
		@Param("planId") planId: String,
		@Param("dateFrom") dateFrom: String,
		@Param("dateTo") dateTo: String,
		@Param("payerId") payerId: String,
		@Param("invoiceNo") invoiceNumber: String,
	): MutableList<ProviderClaimsDTO>


	@Query(
		"SELECT v.visit_number as visitNumber,v.status as status,v.visit_type as visitType,v" +
				".reimbursement_provider as reimbursementProvider,v.reimbursement_invoice_date as " +
				"reimbursementInvoiceDate,v" +
				".hospital_provider_id as " +
				"hospitalProviderId, " +
				"v.member_name as " +
				"memberName," +
				" v.member_number as" +
				" memberNumber, v.benefit_name as benefitName, pn.plan_name as schemeName," +
				"(select payer_name from membership.payer where payer_id = v.payer_id) as payerName,v.total_invoice_amount as invoiceAmount,v.invoice_number as invoiceNumber, v.created_at as createdAt," +
				"(select provider_name from membership.provider where provider_id = v.hospital_provider_id) as providerName FROM visit v " +
				"inner join membership.category c on c.category_id = v.category_id inner join membership.policy p on p.policy_id = c.policy_id " +
				"inner join membership.plan pn on pn.plan_id = p.plan_id where date(v.created_at) >= ?2 and date(v.created_at) <= ?3 and p.plan_id = ?1 " +
				"and v.status != 'ACTIVE' and v.visit_type = 'REIMBURSEMENT'" +
				" order by v.visit_number DESC",
		nativeQuery = true
	)
	fun findPlanReimbursements(
		@Param("planId") planId: String,
		@Param("dateFrom") dateFrom: String,
		@Param("dateTo") dateTo: String
	): MutableList<ProviderReimbursementDTO>


	@Query(
		"SELECT v.visit_number as visitNumber, inv.status as status, v.hospital_provider_id as " +
				"hospitalProviderId, v.payer_id as payerId, " +
				"v.member_name as memberName, v.member_number as memberNumber, v.benefit_name as benefitName, pn.plan_name as schemeName," +
				" ( select payer_name from membership.payer where payer_id = v.payer_id ) as payerName, inv.total_amount as invoiceAmount," +
				" inv.invoice_number as invoiceNumber,inv.invoice_id as invoiceId, v.created_at as createdAt, " +
				"( select provider_name from membership.provider where provider_id = v.hospital_provider_id ) as providerName FROM visit v " +
				"inner join membership.category c on c.category_id = v.category_id inner join membership.policy p on p.policy_id = c.policy_id " +
				"inner join membership.plan pn on pn.plan_id = p.plan_id " +
				"inner join claims.invoice inv on inv.visit_number = v.visit_number where v" +
				".payer_id = ?1 and (inv.invoice_number LIKE ?2% OR v.member_number LIKE ?2%)",
		nativeQuery = true
	)
	fun findClaimWithPayerAndSearch(
		@Param("payerId") payerId: String?,
		@Param("search") search: String?
	):
			MutableList<GeneralClaimDTO>

	@Query(
		"SELECT v FROM Visit v WHERE v.benefitId in (:benefitIds) " +
		"AND v.memberNumber in (:memberNumbers) AND v.claimProcessStatus = :claimProcessStatus " +
		"AND (v.status = :status OR v.status = :status2) AND v.totalInvoiceAmount > 0 " +
		"AND createdAt BETWEEN :dateFrom AND :dateTo ORDER BY v.id DESC"
	)
	fun findVisitTransactions(
		@Param("benefitIds") benefitIds: List<Long>,
		@Param("memberNumbers") memberNumbers: List<String>,
		@Param("claimProcessStatus") claimProcessStatus: ClaimProcessStatus,
		@Param("status") status: Status,
		@Param("status2") status2: Status,
		@Param("dateFrom") dateFrom: LocalDateTime,
		@Param("dateTo") dateTo: LocalDateTime,
		request: Pageable
	): Page<Visit>

	@Query(
		"select v.visit_number as visitNumber, v.created_at as createdAt , v.hospital_provider_id as providerId,i.invoice_number as invoiceNumber,i.total_amount as totalAmount,v.member_name as memberName," +
				"v.member_number as memberNumber,bb.benefit_id as benefitId, bb.benefit_name as benefitName,v.benefit_name as subBenefit,bb.aggregate_id as aggregateId,bb.balance, bb.beneficiary_id as beneficiaryId," +
				"bb.start_date as fromDate, bb.end_date as toDate, bb.initial_limit as initialLimit, bb.payer_id payerId, bb.category_id as categoryId from claims.benefit_beneficiary bb " +
				"inner join claims.visit v on v.aggregate_id = bb.aggregate_id inner join claims.invoice i on i.visit_number = v.visit_number where bb.parent_id is null and v.status != 'REJECTED' " +
				"and v.status != 'INACTIVE' and v.status != 'CANCELLED' and bb.member_number = ?1 and v.created_at >= bb.start_date and v.created_at <= bb.end_date",
		nativeQuery = true
	)
	fun findByFamilyNumber(memberNumber: String): MutableList<MembersStatementsDTO>?

	@Query(value = "SELECT v from Visit v WHERE v.aggregateId = :aggregateId AND v.benefitId = :benefitId " +
			"AND (v.status = :status OR v.status = :status2) AND v.claimProcessStatus = :claimProcessStatus " +
			"AND v.createdAt BETWEEN :startDate AND :endDate")
	fun findVisitsBetweenGivenPeriod(@Param("aggregateId") aggregateId: String,
									 @Param("benefitId") benefitId: Long,
									 @Param("status") status: Status,
									 @Param("status2") status2: Status,
									 @Param("claimProcessStatus") claimProcessStatus: ClaimProcessStatus,
									 @Param("startDate") startDate: LocalDateTime,
									 @Param("endDate") endDate: LocalDateTime
									 ):MutableList<Visit>

}

interface BeneficiaryBenefitRepository : JpaRepository<BeneficiaryBenefit, Long> {

	@Query("SELECT * FROM claims.benefit_beneficiary WHERE category_id " +
			"=" +
			" ?1 AND benefit_id = ?2 " +
			"AND aggregate_id = ?3", nativeQuery = true)
	fun findMainBenefit(@Param("categoryId") categoryId: Long,@Param("benefitId")
	benefitId: Long,@Param("aggregateId") aggregateId: String):MutableList<BeneficiaryBenefit>


	@Query(
		value = """
		SELECT DISTINCT b FROM BeneficiaryBenefit b WHERE b.beneficiaryId =:beneficiaryId AND b.status =:status
	"""
	)
	fun findActiveByBeneficiaryId(
		@Param("beneficiaryId") beneficiaryId: Long,
		@Param("status") status: BeneficiaryBenefit.BenefitStatus
	): MutableList<BeneficiaryBenefit>

	@Query(
		value = """
		SELECT DISTINCT b FROM BeneficiaryBenefit b WHERE b.beneficiaryId =:beneficiaryId
	"""
	)
	fun findAllByBeneficiaryId(
		@Param("beneficiaryId") beneficiaryId: Long
	): MutableList<BeneficiaryBenefit>


	@Query(
		value = """
		SELECT DISTINCT b FROM BeneficiaryBenefit b WHERE b.memberNumber =:memberNumber AND b.status =:status
	"""
	)
	fun findActiveByMemberNumber(
		@Param("memberNumber") memberNumber: String,
		@Param("status") status: BeneficiaryBenefit.BenefitStatus
	): MutableList<BeneficiaryBenefit>

	//@Query(value = "SELECT DISTINCT b FROM BeneficiaryBenefit b LEFT JOIN b.subBenefits s LEFT JOIN s.subBenefits t WHERE b.aggregateID =:aggregateId AND b.benefitId =:benefitId")
//    fun findByAggregateIdAndBenefitId(
//        @Param("aggregateId") aggregateId: String,
//        @Param("benefitId") benefitId: Long
//    ): MutableList<BeneficiaryBenefit>

	@Query(
		"""
		SELECT b FROM BeneficiaryBenefit b LEFT JOIN b.subBenefits 
		WHERE b.aggregateId =:aggregateId AND b.benefitId = :benefitId
	"""
	)
	fun findAllByAggregateId(
		@Param("aggregateId") aggregateId: String,
		@Param("benefitId") benefitId: Long,
	): MutableSet<BeneficiaryBenefit>

	@Query(
		"""
		SELECT b FROM BeneficiaryBenefit b LEFT JOIN b.subBenefits 
		WHERE b.aggregateId =:aggregateId
	"""
	)
	fun findAllByAggregateIdOnly(
		@Param("aggregateId") aggregateId: String
	): MutableSet<BeneficiaryBenefit>

	@Query(value = "SELECT b FROM BeneficiaryBenefit b WHERE b.aggregateId = :aggregateId AND b.benefitId = :benefitId")
	fun findByAggregateIdAndBenefitId(
		@Param("aggregateId") aggregateId: String,
		@Param("benefitId") benefitId: Long
	): MutableSet<BeneficiaryBenefit>

	@Query(
		value = """
			SELECT b FROM BeneficiaryBenefit b LEFT JOIN b.subBenefits
			WHERE b.aggregateId =:aggregateId AND b.parent IS NULL AND b.benefitId <> :benefitId
		"""
	)
	fun findMainByAggregateWithoutId(
		@Param("aggregateId") aggregateId: String,
		@Param("benefitId") benefitId: Long
	): MutableSet<BeneficiaryBenefit>

	@Query(
		value = """
			SELECT b FROM BeneficiaryBenefit b WHERE UPPER(b.benefitName) = UPPER(:benefitName) 
			AND b.memberNumber =:memberNumber
		"""
	)
	fun findAggregateFromMemberAndBenefit(
		@Param("benefitName") benefitName: String,
		@Param("memberNumber") memberNumber: String
	): BeneficiaryBenefit?

	fun findByCategoryIdAndBeneficiaryId(
		@Param("categoryId") categoryId: Long,
		@Param("beneficiaryId") beneficiaryId: Long
	): MutableList<BeneficiaryBenefit>?

	@Query(
		value = """
		SELECT b FROM BeneficiaryBenefit b WHERE b.aggregateId = :aggregateId AND b.benefitId = :benefitId 
		AND b.memberNumber =:memberNumber
	"""
	)
	fun findByAggregateIdAndBenefitIdAndMemberNumber(
		@Param("aggregateId") aggregateId: String,
		@Param("benefitId") benefitId: Long,
		@Param("memberNumber") memberNumber: String
	): BeneficiaryBenefit?


	@Query(
		"SELECT * FROM  benefit_beneficiary " +
				"WHERE payer_id = ?1  AND member_number" +
				" LIKE ?2 GROUP BY member_number,id LIMIT 1",
		nativeQuery = true
	)
	fun findByPayerIdAndMemberNumber(payerId: Long, memberNumber: String): MutableList<BeneficiaryBenefit>

	fun findByMemberNumberAndBenefitId(memberNumber: String, benefitId: Long) : BeneficiaryBenefit

	@Query("SELECT * FROM `claims`.`benefit_beneficiary` WHERE member_number = ?1 and " +
			"benefit_name = ?2 and status='CANCELED'", nativeQuery = true)
	fun findAllByMemberNumberAndBenefitNamePreviousPeriod(memberNumber: String,benefitName: String): MutableList<BeneficiaryBenefit?>

	@Query("SELECT * FROM `claims`.`benefit_beneficiary` WHERE member_number = ?1 and " +
			"benefit_name = ?2 and status='ACTIVE'", nativeQuery = true)
	fun findAllByMemberNumberAndBenefitNameCurrentPeriod(memberNumber: String,benefitName: String): MutableList<BeneficiaryBenefit?>


	@Query(
		"SELECT distinct start_date as startDate, end_date as endDate FROM benefit_beneficiary where member_number = ?1 and status = 'ACTIVE' ",
		nativeQuery = true
	)
	fun findDistinctByMemberNumber(memberNumber: String) : BeneficiaryBenefitDto?


	@Query(
		"SELECT distinct bb.member_name as memberName, bb.category_id as categoryId, bb" +
				".member_number as memberNumber, b" +
				".email, bb.start_date as startDate, bb.end_date as endDate FROM claims.benefit_beneficiary bb inner join membership.beneficiary b on b.member_number = bb.member_number where bb.payer_id = ?1 and bb.member_type = 'PRINCIPAL' and bb.status = 'ACTIVE' and b.status = 'ACTIVE' and b.email is not null",
		nativeQuery = true
	)
	fun getPrincipalInfor(payerId: String?) : MutableList<EmailBeneficiaryDto>?

	@Query(
		"SELECT distinct bb.member_name as memberName, bb.category_id as categoryId, bb" +
				".member_number as memberNumber, b" +
				".email, bb.start_date as startDate, bb.end_date as endDate FROM claims" +
				".benefit_beneficiary bb inner join membership.beneficiary b on b.member_number =" +
				" bb.member_number where bb.category_id = ?1 and bb.member_type = 'PRINCIPAL' and" +
				" bb.status = 'ACTIVE' and b.status = 'ACTIVE' and b.email is not null",
		nativeQuery = true
	)
	fun getPrincipalInCategory(categoryId: Long?) : MutableList<EmailBeneficiaryDto>?




}

interface PreAuthRepo : JpaRepository<Preauthorization, Long> {
	fun findByAggregateIdAndBenefitId(
		aggregateId: String,
		benefitId: Long
	): MutableList<Preauthorization>

	fun findByAggregateId(aggregateId: String): MutableList<Preauthorization>
	fun findByMemberNumber(memberNumber: String): MutableList<Preauthorization>
	fun findByVisitNumber(visit_number: Long): Preauthorization?
	fun findByProviderId(providerId: Long): MutableList<Preauthorization>

	fun findAllByProviderIdAndStatusOrderByCreatedAtDesc(
		@Param
			("provider_id") hospital_provider_id: Long,
		@Param("status") status: Preauthorization.PreauthStatus
	): MutableList<Preauthorization?>

	fun findAllByProviderIdAndStatusNotOrderByCreatedAtDesc(
		@Param
			("provider_id") hospital_provider_id: Long,
		@Param("status") status: Preauthorization.PreauthStatus
	): MutableList<Preauthorization?>

	fun findAllByPayerIdAndStatusOrderByCreatedAtDesc(
		@Param
			("payer_id") payer_id: Long,
		@Param("status") status: Preauthorization.PreauthStatus
	): MutableList<Preauthorization?>

	fun findAllByPayerIdAndStatusNotOrderByCreatedAtDesc(
		@Param
			("payer_id") payer_id: Long,
		@Param("status") status: Preauthorization.PreauthStatus
	): MutableList<Preauthorization?>

	fun findAllByPayerIdOrderByCreatedAtDesc(
		@Param
			("payer_id") payer_id: Long
	): MutableList<Preauthorization?>
}


interface ClaimErrorRepo : JpaRepository<ClaimError, Int> {

}

interface DocumentsRepo : JpaRepository<Document, Long> {
	fun findByInvoiceNumberAndProviderIdAndType(invoiceNumber: String, hospitalProviderId: Long, type: DocumentType): Optional<Document>
}

interface ProviderUrlRepo : JpaRepository<ProviderUrl, Long> {
	fun findByProviderAndUrlType(
		provider: String,
		urlType: MIDDLWARE_URL_TYPE
	): Optional<ProviderUrl>

	fun findAllByUrlType(urlType: MIDDLWARE_URL_TYPE): MutableList<ProviderUrl>
}

interface ClaimCloseFailedRepo : JpaRepository<ClaimCloseFailed, Long> {
	fun findByClaimRef(claim_ref: String): Optional<ClaimCloseFailed>
}

interface InvoiceProcedureCodeRepo : JpaRepository<InvoiceNumberProcedureCode, Long> {
	fun findByInvoiceNumber(name: String): MutableList<InvoiceNumberProcedureCode>
}

interface TransactionErrorRepo : JpaRepository<TransactionError, Long> {
	fun findByVisitNumber(visitNumber: Long): List<TransactionError>
	fun findAllByOrderByIdDesc(pageable: Pageable): Page<TransactionError>
}

interface LineItemErrorRepo : JpaRepository<LineItemError, Long> {
}
