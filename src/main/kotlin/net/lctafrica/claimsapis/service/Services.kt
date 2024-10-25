package net.lctafrica.claimsapis.service

import net.lctafrica.claimsapis.dto.AuthorizePreAuthDTO
import net.lctafrica.claimsapis.dto.BeneficiaryBenefit
import net.lctafrica.claimsapis.dto.BenefitsDto
import net.lctafrica.claimsapis.dto.ClaimError
import net.lctafrica.claimsapis.dto.ClaimImport
import net.lctafrica.claimsapis.dto.ClinicalInformation
import net.lctafrica.claimsapis.dto.ClinicalInformationDTO
import net.lctafrica.claimsapis.dto.ConsumeBenefitDTO
import net.lctafrica.claimsapis.dto.CreateBenefitDTO
import net.lctafrica.claimsapis.dto.Diagnosis
import net.lctafrica.claimsapis.dto.DiagnosisItemDTO
import net.lctafrica.claimsapis.dto.Icd10code
import net.lctafrica.claimsapis.dto.IntegratedClaimCloseRequestDto
import net.lctafrica.claimsapis.dto.IntegratedClaimData
import net.lctafrica.claimsapis.dto.IntegratedClaimResponseDto
import net.lctafrica.claimsapis.dto.Invoice
import net.lctafrica.claimsapis.dto.InvoiceDTO
import net.lctafrica.claimsapis.dto.InvoiceLine
import net.lctafrica.claimsapis.dto.Laboratory
import net.lctafrica.claimsapis.dto.MedicalDrug
import net.lctafrica.claimsapis.dto.MedicalProcedure
import net.lctafrica.claimsapis.dto.MultipleStationLineItemDTO
import net.lctafrica.claimsapis.dto.PreAuthDTO
import net.lctafrica.claimsapis.dto.Preauthorization
import net.lctafrica.claimsapis.dto.Radiology
import net.lctafrica.claimsapis.dto.SaveAndCloseVisitDTO
import net.lctafrica.claimsapis.dto.StartVisitDTO
import net.lctafrica.claimsapis.dto.Visit
import net.lctafrica.claimsapis.dto.VisitDTO
import net.lctafrica.claimsapis.dto.VisitTransactionDto
import net.lctafrica.claimsapis.dto.*
import net.lctafrica.claimsapis.model.intergratedDocuments.InvoiceDetails
import net.lctafrica.claimsapis.model.reports.Financial
import net.lctafrica.claimsapis.model.reports.Requests.ReportFilters
import net.lctafrica.claimsapis.util.ClaimRes
import net.lctafrica.claimsapis.util.DiagnosisClaimRes
import net.lctafrica.claimsapis.util.Result
import org.springframework.core.io.Resource
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.web.multipart.MultipartFile

interface IClaimsService {
    fun reverseClaim(reverseVisitDTO: ReverseInvoiceDTO): Result<Boolean>
    fun fullInvoiceReversal(reverseInvoiceDTO: ReverseInvoiceDTO): Result<Boolean>
    fun lineItemReversal(reverseLineItemDTO: ReverseLineItemDTO): Result<Boolean>
    //fun startVisit(startVisitDTO: StartVisitDTO): Result<Visit>
    fun startVisit(startVisitDTO: StartVisitDTO): Result<Visit?>
    fun getActiveVisits(providerId: Long, staffId: String): Result<MutableList<VisitDTO?>>
    fun getMainAndBranchActiveVisits(mainFacilityId: Long, status: String): Result<MutableList<VisitDTO?>>
    fun getActiveOffLctVisits(payerId: String, staffId: Long): Result<MutableList<OffLctVisitDTO?>>
    fun getActiveReimbursementVisits(payerId: String, staffId: Long): Result<MutableList<ReimbursementDTO?>>
    fun getPagedActiveVisits(providerId: Long, staffId: String, page: Int, size: Int): Result<Page<Visit?>>
    fun getClosedVisits(providerId: Long, staffId: String): Result<MutableList<Visit?>>
    fun getPagedClosedOffLctVisits(payerId: String, staffId: Long, page: Int, size: Int): Result<Page<OffLctVisitDTO?>>
    fun getPagedClosedReimbursementVisits(payerId: String, staffId: Long, page: Int, size: Int):
            Result<Page<ReimbursementDTO?>>
    fun getPagedClosedVisits(providerId: Long, staffId: String, page: Int, size: Int, invoiceNo:
    String?, memberNo: String?): Result<Page<Visit?>?>
    fun getPagedClosedVisitsPdf(providerId: Long, staffId: String, page: Int, size: Int, invoiceNo:
    String?, memberNo: String?): ResponseEntity<ByteArray?>?
    fun getPagedClosedVisitsExcel(providerId: Long, staffId: String, page: Int, size: Int,
                                  invoiceNo: String?, memberNo: String?): ResponseEntity<Resource>?
    fun getMainAndBranchPagedClosedVisits(mainFacilityId: Long, staffId: String, page: Int, size:
    Int): Result<Page<Visit?>>
    fun getVisitById(id: Long): Result<Visit>
    fun getLineItemsByInvoiceNumber(invoiceNumber: String,providerId: Long):
            Result<MutableList<InvoiceLine>?>
    fun getInvoiceProcedureCodeByInvoiceNumber(invoiceNumber: String): Result<MutableList<InvoiceNumberProcedureCode>?>
    fun getLineItemsByVisitNumber(visitNumber: Long): Result<MutableList<InvoiceLine>?>
    fun getLineItemsByInvoiceNumberAndVisit(invoiceNumber: String,visitNumber: Long): Result<MutableList<InvoiceLine>?>
    fun getDiagnosisByVisitId(id: Long): Result<MutableList<Diagnosis>?>
    fun saveLineItem(billVisit: LineItemsArray): Result<Boolean>
    fun saveMultipleStationsLineItem(billVisit: MultipleStationLineItemDTO): Result<InvoiceLine>
    fun saveMultipleBillingLineItemsPortal(multipleLineItemsArray: MultipleLineItemsArray): Result<Boolean>

    fun saveMultipleBillingInvoicePortal(multipleBillingInvoice: MultipleBillingInvoice):
            Result<Boolean>

    fun saveInvoice(billVisit: InvoiceDTO): Result<Invoice>
    fun saveDiagnosisItem(diagnosisDetail: DiagnosisItemDTO): Result<Diagnosis>
    fun saveClinicalInformation(clinicalInformationDTO: ClinicalInformationDTO): Result<ClinicalInformation>
    fun saveBillAndCloseVisit(billDTO: SaveAndCloseVisitDTO): Result<Visit>?
    fun saveBillAndCloseVisitPortal(billDTO: SaveAndCloseVisitDTO): Result<Visit>?
    fun saveBillAndCloseMultipleStationsVisit(billDTO: SaveAndCloseVisitDTO): Result<Visit>?
    fun saveIcd10CompleteFile(file: MultipartFile): Result<List<Icd10code>>

    fun saveMedicalProcedureFile(file: MultipartFile): Result<List<MedicalProcedure>>
    fun searchMedicalProcedure(search: String?, page: Int, size: Int):
            Result<Page<MedicalProcedure>>
    fun saveMedicalDrugFile(file: MultipartFile): Result<List<MedicalDrug>>
    fun saveLaboratoryFile(file: MultipartFile): Result<List<Laboratory>>
    fun saveOtherBenefitDetailFile(file: MultipartFile): Result<List<OtherBenefitDetail>>
    fun searchMedicalDrugs(search: String, page: Int, size: Int): Result<Page<MedicalDrug>>
    fun searchLaboratory(search: String, page: Int, size: Int): Result<Page<Laboratory>>
    fun saveRadiologyFile(file: MultipartFile): Result<List<Radiology>>
    fun searchRadiology(search: String, page: Int, size: Int): Result<Page<Radiology>>
    fun searchOtherBenefitDetail(search: String?, page: Int, size: Int):
            Result<Page<OtherBenefitDetail>>
    fun searchIcd10CodeByTitle(search: String, page: Int, size: Int): Result<Page<Icd10code>>
    fun searchIcd10CodeByCode(search: String, page: Int, size: Int): Result<Page<Icd10code>>
    fun getVisitByMemberNumber(memberNumber: String): Result<List<ClaimsDTO?>>
    fun getVisitByFamilyNumber(familyNumber: String): Result<List<ClaimsDTO?>>
    fun getInvoiceByVisitNumber(visit: Long): Result<Invoice>
    fun getInvoicesByVisitNumber(visitNumber: Long): Result<MutableList<Invoice>?>
    fun getClaimFromIntegratedFacility(dto: GetIntegratedClaimDto): Result<IntegratedClaimData?>
    fun closeClaimFromIntegratedFacility(dto: IntegratedClaimCloseRequestDto): Result<IntegratedClaimResponseDto?>
    fun getVisitTransactions(dto: BenefitsDto): Result<List<VisitTransactionDto?>>

    fun getVisitTransactions(beneficiaryId: Long, mainBenefitId: Long, page:Int, size:Int): Result<List<VisitTxnDto?>>

    fun updateVisitMiddlewareStatus(dto:UpdateMiddlewareStatusDTO):Result<Boolean>
    fun saveUnSuccessfulCloseClaimRef(dto:IntegratedUnsuccessfulClaimCloseDTO):Result<Boolean>
    fun findByBeneficiaryId(beneficiaryId: Long): Result<List<Visit?>>
    fun getBeneficiaryBenefitByPayerId(payerId:Long,memberNumber: String): Result<List<BeneficiaryBenefit?>>
    fun getMemberStatementByMemberNumber(memberNumber: String): MutableList<MembersStatementsDTO>
    fun getClaimsByProvider(hospitalProviderId: Long, planId: String, fromDate : String, toDate : String): ResponseEntity<ByteArray?>?
    fun getProvidersByPayer(payerId: Long): ResponseEntity<ByteArray?>?
    fun getStatementPlanClaims(planId: String, fromDate : String, toDate : String): ResponseEntity<ByteArray?>?
    fun getClaimsByProvider1(hospitalProviderId: Long): List<Visit?>?
    fun cancelVisit(cancelVisitDTO: CancelVisitDTO): Result<Visit>
    fun getClaimsByMemberNumber(memberNumber: String): ResponseEntity<ByteArray?>?
    fun generateMemberStatement(memberNumber: String): ByteArray?
    fun getProviderClaims(planId: String, hospitalProviderId: Long, dateFrom: String, dateTo:
    String, invoiceNo: String?, memberNo: String?, payerId: String?):
            Result<MutableList<ProviderClaimsDTO>>

    fun getMainAndBranchClaims(hospitalProviderId: Long, dateFrom: String, dateTo:
    String):Result<MutableList<ProviderClaimsDTO>>

    fun getProviderClaimsPdf(planId: String, hospitalProviderId: Long, dateFrom: String, dateTo:
    String, invoiceNo: String?, memberNo: String?, payerId: String?): ResponseEntity<ByteArray?>?

    fun getProviderClaimsExcel(planId: String, hospitalProviderId: Long, dateFrom: String?, dateTo:
    String?, invoiceNo: String?, memberNo: String?, payerId: String?):ResponseEntity<Resource>

    fun getPlanOfflcts(planId: String, hospitalProviderId: String?, dateFrom: String, dateTo:
    String, invoiceNo: String?, memberNo: String?, payerId: String?): Result<MutableList<GeneralClaimDTO>>

    fun getPlanOfflctsPdf(planId: String,
                          fromDate: String,
                          toDate: String,
                          hospitalProviderId: String?): ResponseEntity<ByteArray?>?

    fun getPlanReimbursementsPdf(planId: String,
                          fromDate: String,
                          toDate: String): ResponseEntity<ByteArray?>?

    fun allTransactions(planId: String?, hospitalProviderId: String?, dateFrom: String, dateTo:
    String, invoiceNo: String?, memberNo: String?, payerId: String?,status: String):
            Result<MutableList<GeneralClaimDTO>>
    fun getClaimBySearch(payerId: String,search: String): Result<MutableList<GeneralClaimDTO>>
    ///fun getClaimByMemberNumber(search: String): Result<MutableList<ProviderClaimsDTO>>
    fun getPlanReimbursements(planId: String, dateFrom: String, dateTo: String): Result<MutableList<ProviderReimbursementDTO>>
    fun getPlanClaims(planId: String, dateFrom: String, dateTo: String,invoiceNo: String?,
                      memberNo: String?, payerId: String?): Result<MutableList<ProviderClaimsDTO>>
    fun getIndividualProviderClaim(hospitalProviderId: Long, visitNumber: Long,invoiceNumber: String?):
            ResponseEntity<ByteArray?>?
    fun getIndividualReimbursementClaim(visitNumber: Long): ResponseEntity<ByteArray?>?
    fun getClientActiveVisit(memberNumber:String, hospitalProviderId:Long):Result<Visit>
    fun previewProviderInvoice(invoiceNumber: String, hospitalProviderId: Long): ResponseEntity<ByteArray?>?
    fun getMemberStatementData(memberNumber: String): MutableList<MemberStatement>?
    fun emailMemberStatement(): MutableList<EmailBeneficiaryDto>?
    fun getSchemeFinancialUtilizations(filters:ReportFilters):ResponseEntity<Resource>
    fun getSchemeClinicalUtilizations(filters:ReportFilters):ResponseEntity<Resource>
    fun getInvoiceProvider(invoiceDetails: InvoiceDetails):Result<ProviderDTO>
    fun getMainAndBranchClaimsPdf(
        hospitalProviderId: Long,
        dateFrom: String,
        dateTo: String
    ): ResponseEntity<ByteArray?>?

    fun getMainAndBranchClaimsExcel(
        hospitalProviderId: Long,
        dateFrom: String,
        dateTo: String
    ): ResponseEntity<Resource>
}

interface IBenefitService {
    fun addNew(dto: CreateBenefitDTO): Result<Boolean>
    fun findActiveByMemberNumber(memberNumber: String): Result<List<BeneficiaryBenefit>>
    fun findActiveByBeneficiaryId(beneficiaryId: Long): Result<List<BeneficiaryBenefit>>
    fun findActiveByBeneficiaryIdProviderId(beneficiaryId: Long,providerId:Long): Result<List<BeneficiaryBenefit>>
    fun findByBeneficiaryId(beneficiaryId: Long): Result<List<BeneficiaryBenefit>>
    fun consumeBenefit(dto: ConsumeBenefitDTO): Result<Boolean>
    fun reverseBenefit(dto: ReverseBenefitDTO): Result<Boolean>
    fun closeClaimFromIntegratedFacilityWeb(dto: CloseIntergratedWebClaimDTO): Result<Visit>
    fun checkVisitClaimStatus(dto: CloseIntergratedWebClaimDTO): Result<Visit?>
    fun checkIntegratedProviderInvoice(dto: IntergratedInvoiceProviderDTO): Result<Boolean>
    fun saveUnsuccessfulClaim(dto: IntegratedUnsuccessfulClaimCloseDTO): Result<Boolean>
    fun saveClaimFromMiddleware(dto:ClaimRes):Result<Boolean>
    fun saveCompleteDiagnosisClaimFromMiddleware(dto: DiagnosisClaimRes):Result<Boolean>
    fun deactivateBenefits(dto:DeactivateBenefitDTO):Result<Boolean>
    fun activateBenefits(dto:activateBenefitDTO):Result<Boolean>
    fun topUpBenefit(dto:TopUpBenefitDTO):Result<Boolean>
    fun transferBenefit(dto:TransferBenefitDTO):Result<Boolean>

    fun findBeneficiaryBenefitByMemberNumberAndBenefit(memberNumber: String,benefit: String,period:
    String):
            Result<BeneficiaryBenefit?>?

}

interface IPreAuthService {
    fun add(dto: PreAuthDTO): Result<Preauthorization?>
    fun findByAggregate(aggregateId: String): Result<List<Preauthorization>>
    fun findByVisitNumber(visitNumber: Long): Result<Preauthorization?>
    fun findById(id: Long): Result<Preauthorization?>
    fun findByMemberNumber(memberNumber: String): Result<List<Preauthorization>>
    fun authorize(dto: AuthorizePreAuthDTO): Result<Preauthorization>
    fun decline(dto: AuthorizePreAuthDTO): Result<Preauthorization>
    fun release(dto: AuthorizePreAuthDTO): Result<Preauthorization>
    fun findPendingByProviderId(providerId: Long): Result<MutableList<Preauthorization?>>
    fun findAllByProviderId(providerId: Long): Result<MutableList<Preauthorization?>>
    fun findPendingByPayerId(payerId: Long): Result<MutableList<Preauthorization?>>
    fun findAllByPayerId(payerId: Long): Result<MutableList<Preauthorization?>>
    fun consumePreauth(dto: ConsumePreAuthDTO): Result<Preauthorization?>
}

interface IDataMigrationService {
    fun saveClaims(dto: ClaimImport): Result<Boolean>
    fun getErrors(): Result<MutableList<ClaimError>>
    fun migrateTransactions(dto: TransactionMigrationDto): Result<Boolean>
    fun getTransactionErrors(page:Int, size:Int): Result<Page<TransactionError>>
    fun savePreviousPeriodVisitsFromFile(file: MultipartFile): Result<Boolean?>
    fun saveCurrentPeriodVisitsFromFile(file: MultipartFile): Result<Boolean?>
}

interface IDocumentService{
    fun saveDocument(dto: SaveDocumentDTO): Result<Boolean>
}

interface IProviderUrlService{
    fun findByProviderAndUrlType(providerName:String,urlType: MIDDLWARE_URL_TYPE): Result<ProviderUrl>
    fun getAllByUrlType(urlType: MIDDLWARE_URL_TYPE):Result<MutableList<ProviderUrl>>
}