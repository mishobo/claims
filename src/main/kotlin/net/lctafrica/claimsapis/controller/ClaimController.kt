package net.lctafrica.claimsapis.controller

import io.swagger.v3.oas.annotations.Operation
import net.lctafrica.claimsapis.apacheCamel.route.benefitMngt.PushToPayer
import net.lctafrica.claimsapis.dto.*
import net.lctafrica.claimsapis.model.intergratedDocuments.InvoiceDetails
import net.lctafrica.claimsapis.model.reports.Requests.ReportFilters
import javax.validation.Valid
import net.lctafrica.claimsapis.service.IBenefitService
import net.lctafrica.claimsapis.service.IClaimsService
import net.lctafrica.claimsapis.util.ClaimRes
import net.lctafrica.claimsapis.util.DiagnosisClaimRes
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/v1/visit")
class ClaimController(
    val claimsService: IClaimsService,
    val service: IBenefitService,
    val pushToPayer: PushToPayer
) {

    @PostMapping(value = ["/reverseVisit"], produces = ["application/json"])
    fun reverseVisit(@RequestBody dto: ReverseInvoiceDTO) = claimsService.reverseClaim(dto)

    @PostMapping(value = ["/InvoiceReversal"], produces = ["application/json"])
    fun reverseInvoice(@RequestBody dto: ReverseInvoiceDTO) = claimsService.fullInvoiceReversal(dto)

    @PostMapping(value = ["/lineItemReversal"], produces = ["application/json"])
    fun reverseLineItem(@RequestBody dto: ReverseLineItemDTO) = claimsService.lineItemReversal(dto)

    @PostMapping(value = ["/push"], produces = ["application/json"])
    fun push() = pushToPayer.sendInBatch()

    @GetMapping(value = ["/statement/plan/{hospitalProviderId}"], produces = ["application/json"])
    fun planStatement(
        @RequestParam(value = "planId") planId: String,
        @RequestParam(value = "from") fromDate: String,
        @RequestParam(value = "to") toDate: String
    ) = claimsService.getStatementPlanClaims(planId, fromDate, toDate)

    @GetMapping(value = ["/statement/provider/{hospitalProviderId}"], produces = ["application/json"])
    fun providerStatement(
        @PathVariable(value = "hospitalProviderId") hospitalProviderId: Long,
        @RequestParam(value = "planId") planId: String,
        @RequestParam(value = "from") fromDate: String,
        @RequestParam(value = "to") toDate: String
    ) = claimsService.getClaimsByProvider(hospitalProviderId, planId, fromDate, toDate)

    @GetMapping(value = ["/statement/member"], produces = ["application/json"])
    fun memberStatement(@RequestParam(name = "memberNumber") memberNumber: String) =
        claimsService.getClaimsByMemberNumber(memberNumber)

    @GetMapping(value = ["/statement/memberData"], produces = ["application/json"])
    fun memberStatementData(@RequestParam(name = "memberNumber") memberNumber: String) =
        claimsService.getMemberStatementData(memberNumber)

    @GetMapping(value = ["/email/member"], produces = ["application/json"])
    fun emailMemberStatement() = claimsService.emailMemberStatement()

    @GetMapping(value = ["/statement/singleClaim"], produces = ["application/json"])
    fun singleClaimStatement(
        @RequestParam(value = "hospitalProviderId") hospitalProviderId: Long,
        @RequestParam(value = "visitNumber") visitNumber: Long,
        @RequestParam(value = "invoiceNumber") invoiceNumber: String?
    ) = claimsService.getIndividualProviderClaim(hospitalProviderId, visitNumber,invoiceNumber)

    @GetMapping(value = ["/statement/singleReimbursement"], produces = ["application/json"])
    fun singleReimbursementStatement(
        @RequestParam(value = "visitNumber") visitNumber: Long
    ) = claimsService.getIndividualReimbursementClaim(visitNumber)

    @GetMapping(value = ["/active"], produces = ["application/json"])
    @Operation(summary = "Get Client's active visit from a particular provider")
    fun getClientActiveVisit(
        @RequestParam(value = "memberNumber") memberNumber: String,
        @RequestParam(value = "hospitalProviderId") hospitalProviderId: Long
    ) = claimsService.getClientActiveVisit(memberNumber, hospitalProviderId)

    @PostMapping(value = ["/startVisit"], produces = ["application/json"])
    fun startVisit(@RequestBody dto: StartVisitDTO) = claimsService.startVisit(dto)

    @PostMapping(value = ["/saveLineItem"], produces = ["application/json"])
    @Operation(summary = "Save Line Item at integration/service provider")
    fun saveLineItem(@Valid @RequestBody dto: LineItemsArray) = claimsService.saveLineItem(dto)

    @PostMapping(value = ["/saveMultipleStationsLineItem"], produces = ["application/json"])
    @Operation(summary = "Save Line Item at integration/service provider with multiple stations")
    fun saveMultipleStationsLineItem(@Valid @RequestBody dto: MultipleStationLineItemDTO) = claimsService
        .saveMultipleStationsLineItem(dto)

    @PostMapping(value = ["/saveMultipleBillingInvoicePortal"], produces = ["application/json"])
    @Operation(summary = " Add an invoice at a multiple Billing type" + "service provider")
    fun saveMultipleBillingInvoicePortal(@RequestBody dto: MultipleBillingInvoice) = claimsService.saveMultipleBillingInvoicePortal(dto)

    @PostMapping(value = ["/saveMultipleStationsLineItemsPortal"], produces = ["application/json"])
    @Operation(summary = " Add a line item at a multiple Billing type" + "service provider")
    fun saveMultipleBillingLineItemsPortal(@RequestBody dto: MultipleLineItemsArray) = claimsService.saveMultipleBillingLineItemsPortal(dto)

    @PostMapping(value = ["/saveBillAndCloseVisitPortal"], produces = ["application/json"])
    @Operation(summary = "Save Line Item at integration/service provider with multiple stations")
    fun saveBillAndCloseVisitPortal(@Valid @RequestBody dto: SaveAndCloseVisitDTO) = claimsService
        .saveBillAndCloseVisitPortal(dto)

    @PostMapping(value = ["/saveInvoiceItem"], produces = ["application/json"])
    @Operation(summary = "Save saveInvoiceItem Item at integration/service provider")
    fun saveInvoice(@RequestBody dto: InvoiceDTO) = claimsService.saveInvoice(dto)

    @PostMapping(value = ["/saveDiagnosisItem"], produces = ["application/json"])
    @Operation(summary = "Save Diagnosis Information at integration/service provider")
    fun saveDiagnosisItem(@RequestBody dto: DiagnosisItemDTO) = claimsService.saveDiagnosisItem(dto)

    @PostMapping(value = ["/saveClinicalInformation"], produces = ["application/json"])
    @Operation(summary = "Save Clinical Information at integration/service provider")
    fun saveClinicalInformation(@RequestBody dto: ClinicalInformationDTO) = claimsService.saveClinicalInformation(dto)

    @PostMapping(value = ["/saveAndCloseBillItem"], produces = ["application/json"])
    @Operation(summary = "Bill and close Visit patient at integration/service provider")
    fun saveAndCloseBill(@RequestBody dto: SaveAndCloseVisitDTO) = claimsService.saveBillAndCloseVisit(dto)

    @PostMapping(value = ["/saveBillAndCloseMultipleStationsVisit"], produces = ["application/json"])
    @Operation(summary = "Bill and close Visit patient at integration/service provider")
    fun saveBillAndCloseMultipleStationsVisit(@RequestBody dto: SaveAndCloseVisitDTO) =
        claimsService.saveBillAndCloseMultipleStationsVisit(dto)

    @GetMapping(value = ["/{providerId}/{staffId}/active"], produces = ["application/json"])
    @Operation(
        summary = "Get Active visits by provider and StaffID at integration/service " +
                "provider"
    )
    fun getActiveVisits(
        @PathVariable("providerId") providerId: Long,
        @PathVariable("staffId")
        staffId: String
    ) = claimsService.getActiveVisits(providerId, staffId)


    @GetMapping(value = ["main/{mainFacilityId}/{staffId}/active"], produces = ["application/json"])
    @Operation(
        summary = "Get Active visits by provider and StaffID at integration/service " +
                "provider"
    )
    fun getMainAndBranchActiveVisits(
        @PathVariable("mainFacilityId") mainFacilityId: Long,
        @PathVariable("staffId") staffId: String

    ) = claimsService.getMainAndBranchActiveVisits(mainFacilityId, staffId)

    @GetMapping(value = ["/offlct/{payerId}/{staffId}/active"], produces = ["application/json"])
    @Operation(
        summary = "Get Active Off Lct visits by payerID "
    )
    fun getActiveOffLctVisits(
        @PathVariable("payerId") payerId: String, @PathVariable("staffId") staffId: Long
    ) = claimsService.getActiveOffLctVisits(payerId, staffId)

    @GetMapping(value = ["/reimbursement/{payerId}/{staffId}/active"], produces =
    ["application/json"])
    @Operation(
        summary = "Get Active Reimbursement visits by payerID "
    )
    fun getActiveReimbursementVisits(
        @PathVariable("payerId") payerId: String, @PathVariable("staffId") staffId: Long
    ) = claimsService.getActiveReimbursementVisits(payerId, staffId)

    @GetMapping(value = ["/{providerId}/{staffId}/{page}/{size}/active"], produces = ["application/json"])
    @Operation(
        summary = "Get Paged Active visits by provider at integration/service " +
                "provider"
    )
    fun getPagedActiveVisits(
        @PathVariable("providerId") providerId: Long, @PathVariable("staffId")
        staffId: String, @PathVariable("page") page: Int, @PathVariable("size") size: Int
    ) = claimsService.getPagedActiveVisits(providerId, staffId, page, size)

    @GetMapping(value = ["/{providerId}/{staffId}/closed"], produces = ["application/json"])
    @Operation(
        summary = "Get Active visits by provider at integration/service " +
                "provider"
    )
    fun getClosedVisits(
        @PathVariable("providerId") providerId: Long,
        @PathVariable("staffId") staffId: String
    ) = claimsService.getClosedVisits(providerId, staffId)

    @GetMapping(value = ["/{providerId}/{staffId}/{page}/{size}/closed"], produces = ["application/json"])
    @Operation(
        summary = "Get Paged Closed visits by provider at integration/service " +
                "provider"
    )
    fun getPagedClosedVisits(
        @PathVariable("providerId") providerId: Long,
        @PathVariable("staffId") staffId: String,
        @PathVariable("page") page: Int,
        @PathVariable("size") size: Int,
        @RequestParam(value = "invoiceNo") invoiceNo: String?,
        @RequestParam(value = "memberNo") memberNo: String?,
    ) = claimsService.getPagedClosedVisits(providerId, staffId, page, size,invoiceNo,memberNo)

    @GetMapping(value = ["pdf/{providerId}/{staffId}/{page}/{size}/closed"], produces =
    ["application/json"])
    @Operation(
        summary = "Get Paged Closed visits in pdf format by provider at integration/service " +
                "provider"
    )
    fun getPagedClosedVisitsPdf(
        @PathVariable("providerId") providerId: Long,
        @PathVariable("staffId") staffId: String,
        @PathVariable("page") page: Int,
        @PathVariable("size") size: Int,
        @RequestParam(value = "invoiceNo") invoiceNo: String?,
        @RequestParam(value = "memberNo") memberNo: String?,
    ) = claimsService.getPagedClosedVisitsPdf(providerId, staffId, page, size,invoiceNo,memberNo)

    @GetMapping(value = ["excel/{providerId}/{staffId}/{page}/{size}/closed"], produces =
    ["application/json"])
    @Operation(
        summary = "Get Paged Closed visits in Excel format by provider at integration/service " +
                "provider"
    )
    fun getPagedClosedVisitsExcel(
        @PathVariable("providerId") providerId: Long,
        @PathVariable("staffId") staffId: String,
        @PathVariable("page") page: Int,
        @PathVariable("size") size: Int,
        @RequestParam(value = "invoiceNo") invoiceNo: String?,
        @RequestParam(value = "memberNo") memberNo: String?,
    ) = claimsService.getPagedClosedVisitsExcel(providerId, staffId, page, size,invoiceNo,memberNo)


    @GetMapping(value = ["main/{mainFacilityId}/{staffId}/{page}/{size}/closed"], produces =
    ["application/json"])
    @Operation(
        summary = "Get Paged Closed visits by provider at integration/service " +
                "provider"
    )
    fun getMainAndBranchPagedClosedVisits(
        @PathVariable("mainFacilityId") mainFacilityId: Long, @PathVariable("staffId")
        staffId: String, @PathVariable("page") page: Int, @PathVariable("size") size: Int
    ) = claimsService.getMainAndBranchPagedClosedVisits(mainFacilityId, staffId, page, size)


    @GetMapping(value = ["/offlct/{payerId}/{staffId}/{page}/{size}/closed"], produces =
    ["application/json"])
    @Operation(
        summary = "Get Paged Closed offlct visits by payerId"
    )
    fun getPagedClosedOffLctVisits(
        @PathVariable("payerId") payerId: String, @PathVariable("staffId")
        staffId: Long, @PathVariable("page") page: Int, @PathVariable("size") size: Int
    ) = claimsService.getPagedClosedOffLctVisits(payerId, staffId, page, size)

    @GetMapping(value = ["/reimbursement/{payerId}/{staffId}/{page}/{size}/closed"], produces =
    ["application/json"])
    @Operation(
        summary = "Get Paged Closed Reimbursement visits by payerId"
    )
    fun getPagedClosedReimbursementVisits(
        @PathVariable("payerId") payerId: String, @PathVariable("staffId")
        staffId: Long, @PathVariable("page") page: Int, @PathVariable("size") size: Int
    ) = claimsService.getPagedClosedReimbursementVisits(payerId, staffId, page, size)

    @GetMapping(value = ["/{id}/visit"], produces = ["application/json"])
    @Operation(summary = "Get Visit by Id at integration/service provider")
    fun getVisitById(@PathVariable("id") id: Long) = claimsService.getVisitById(id);

    @GetMapping(value = ["/visitsByMemberNumber"], produces = ["application/json"])
    @Operation(summary = "Get Visits by memberNumber at integration/service provider")
    fun getVisitsByMemberNumber(@RequestParam(value = "memberNumber") memberNumber: String) = claimsService
        .getVisitByMemberNumber(memberNumber);

    @GetMapping(value = ["/visitsByFamilyNumber"], produces = ["application/json"])
    @Operation(summary = "Get Visits by familyNumber at integration/service provider")
    fun getVisitsByFamilyNumber(@RequestParam(value = "familyNumber") familyNumber: String) = claimsService
        .getVisitByFamilyNumber(familyNumber);

    @PostMapping(value = ["/saveIcd10Codes"], produces = ["application/json"], consumes = ["multipart/form-data"])
    @Operation(summary = "Save ICD10 codes")
    fun saveIcd10Codes(@RequestParam("file") file: MultipartFile) = claimsService.saveIcd10CompleteFile(file)

    @PostMapping(
        value = ["/saveMedicalProcedures"],
        produces = ["application/json"],
        consumes = ["multipart/form-data"]
    )
    @Operation(summary = "Save Medical Procedures")
    fun saveMedicalProcedures(@RequestParam("file") file: MultipartFile) = claimsService.saveMedicalProcedureFile(file)


    @PostMapping(value = ["/saveMedicalDrugs"], produces = ["application/json"], consumes = ["multipart/form-data"])
    @Operation(summary = "Save Medical Drugs")
    fun saveMedicalDrugs(@RequestParam("file") file: MultipartFile) = claimsService.saveMedicalDrugFile(file)

    @PostMapping(
        value = ["/saveLaboratory"], produces = ["application/json"], consumes =
        ["multipart/form-data"]
    )
    @Operation(summary = "Save Laboratory Items")
    fun saveLaboratory(@RequestParam("file") file: MultipartFile) = claimsService.saveLaboratoryFile(file)

    @PostMapping(value = ["/saveRadiology"], produces = ["application/json"], consumes = ["multipart/form-data"])
    @Operation(summary = "Save Radiology")
    fun saveRadiology(@RequestParam("file") file: MultipartFile) = claimsService.saveRadiologyFile(file)

    @PostMapping(
        value = ["/saveOtherBenefitDetail"],
        produces = ["application/json"],
        consumes = ["multipart/form-data"]
    )
    @Operation(summary = "Save Other Benefit Detail")
    fun saveOtherBenefitDetail(@RequestParam("file") file: MultipartFile) = claimsService
        .saveOtherBenefitDetailFile(file)

    @GetMapping(value = ["/searchOtherBenefitDetail"], produces = ["application/json"])
    fun searchOtherBenefitDetail(
        @RequestParam(name = "title") title: String?, page: Int, size:
        Int
    ) =
        claimsService.searchOtherBenefitDetail(title, page, size)

    @GetMapping(value = ["/searchMedicalProcedure"], produces = ["application/json"])
    fun searchMedicalProcedure(@RequestParam(name = "title") title: String?, page: Int, size: Int) =
        claimsService.searchMedicalProcedure(title, page, size)

    @GetMapping(value = ["/searchMedicalDrugs"], produces = ["application/json"])
    fun searchMedicalDrugs(@RequestParam(name = "title") title: String, page: Int, size: Int) =
        claimsService.searchMedicalDrugs(title, page, size)

    @GetMapping(value = ["/searchLaboratory"], produces = ["application/json"])
    fun searchLaboratory(@RequestParam(name = "title") title: String, page: Int, size: Int) =
        claimsService.searchLaboratory(title, page, size)

    @GetMapping(value = ["/searchRadiology"], produces = ["application/json"])
    fun searchRadiology(@RequestParam(name = "title") title: String, page: Int, size: Int) =
        claimsService.searchRadiology(title, page, size)

    @GetMapping(value = ["/searchICD10ByTitle/icd10code"], produces = ["application/json"])
    fun searchIcd10CodeByTitle(@RequestParam(name = "title") title: String, page: Int, size: Int) =
        claimsService.searchIcd10CodeByTitle(title, page, size)

    @GetMapping(value = ["/searchICD10ByCode/icd10code"], produces = ["application/json"])
    fun searchIcd10CodeByCode(
        @RequestParam(name = "code") code: String, page: Int, size:
        Int
    ) =
        claimsService.searchIcd10CodeByCode(code, page, size)

    @GetMapping(value = ["/lineItems"], produces = ["application/json"])
    @Operation(summary = "Get Line items by invoice Number at integration/service provider")
    fun getLineItemsByInvoiceNumber(@RequestParam(name = "invoiceNumber") invoiceNumber: String,
                                    @RequestParam(name = "providerId") providerId: Long) =
        claimsService.getLineItemsByInvoiceNumber(invoiceNumber,providerId);


    @GetMapping(value = ["/procedureCodes"], produces = ["application/json"])
    @Operation(summary = "Get Procedure Codes by invoice Number at integration/service provider")
    fun getProcedureCodesByInvoiceNumber(@RequestParam(name = "invoiceNumber") invoiceNumber: String) =
        claimsService.getInvoiceProcedureCodeByInvoiceNumber(invoiceNumber);

    @GetMapping(value = ["/{id}/diagnosis"], produces = ["application/json"])
    @Operation(summary = "Get Diagnosis by Visit Id at integration/service provider")
    fun getDiagnosisByVisitId(@PathVariable("id") id: Long) = claimsService.getDiagnosisByVisitId(id);

    @PostMapping(value = ["/benefit/register"], produces = ["application/json"])
    fun registerBenefits(@RequestBody dto: CreateBenefitDTO) = service.addNew(dto)

    @GetMapping(value = ["/claim/search/{beneficiaryId}"], produces = ["application/json"])
    fun searchClaims(
        @PathVariable(value = "beneficiaryId") beneficiaryId: Long,
    ) = claimsService.findByBeneficiaryId(beneficiaryId)
    //findActiveByBeneficiaryIdProviderId

    @GetMapping(value = ["/benefit/search/{beneficiaryId}"], produces = ["application/json"])
    fun searchActiveBenefits(
        @PathVariable(value = "beneficiaryId") beneficiaryId: Long,
    ) = service.findActiveByBeneficiaryId(beneficiaryId)

    @GetMapping(value = ["/benefit/search/{beneficiaryId}/{providerId}"], produces =
    ["application/json"])
    fun searchActiveBenefits(
        @PathVariable(value = "beneficiaryId") beneficiaryId: Long,
        @PathVariable(value = "providerId") providerId: Long,
    ) = service.findActiveByBeneficiaryIdProviderId(beneficiaryId,providerId)

    @GetMapping(value = ["/benefit/search/withoutStatus/all/{beneficiaryId}"], produces = ["application/json"])
    fun searchAllBenefits(
        @PathVariable(value = "beneficiaryId") beneficiaryId: Long,
    ) = service.findByBeneficiaryId(beneficiaryId)

    @GetMapping(
        value = ["/benefit/searchByMemberNumber/{memberNumber}"], produces =
        ["application/json"]
    )
    fun getBeneficiaryByMemberNumber(
        @PathVariable(value = "memberNumber") memberNumber: String,
    ) = service.findActiveByMemberNumber(memberNumber)

    @PostMapping(value = ["/benefit/consume"], produces = ["application/json"])
    fun consumeBenefit(@RequestBody dto: ConsumeBenefitDTO) = service.consumeBenefit(dto)

    @GetMapping(value = ["/{id}/invoice"], produces = ["application/json"])
    @Operation(summary = "Get Invoice by Visit Number at integration/service provider")
    fun getInvoiceByVisitNumber(@PathVariable("id") id: Long) = claimsService.getInvoiceByVisitNumber(id);

    @GetMapping(value = ["/{id}/lineItems"], produces = ["application/json"])
    @Operation(summary = "Get Line Items by Visit Number at integration/service provider")
    fun getLineItemsByVisitNumber(@PathVariable("id") id: Long) = claimsService.getLineItemsByVisitNumber(id);


    @GetMapping(value = ["/invoice/lineItems"], produces = ["application/json"])
    @Operation(summary = "Get Line Items by Visit Number And Invoice Number at " +
            "payer")
    fun getLineItemsByVisitNumberAndInvoiceNumber(
        @RequestParam(name = "invoiceNumber") invoiceNumber: String,
        @RequestParam(name = "visitNumber") visitNumber: Long
    ) = claimsService
        .getLineItemsByInvoiceNumberAndVisit(invoiceNumber,visitNumber);


    @GetMapping(value = ["/{visitNumber}/invoices"], produces = ["application/json"])
    @Operation(summary = "Get Invoices by visit Number at integration/service provider")
    fun getInvoicesByVisitNumber(@PathVariable(value = "visitNumber") visitNumber: Long) =
        claimsService.getInvoicesByVisitNumber(visitNumber);

    //	@GetMapping(value = ["/closedVisits"], produces = ["application/json"])
//	@Operation(summary = "Get visits closed in last 10 minutes")
//	fun getClosedVisits() = claimsService.getClosedVisits()


    @PostMapping(value = ["/integrated"], produces = ["application/json"])
    @Operation(summary = "Get claim from an Integrated Facility")
    fun getClaimFromIntegratedFacility(@RequestBody dto: GetIntegratedClaimDto) =
        claimsService.getClaimFromIntegratedFacility(dto)

    @PostMapping(value = ["/integrated/closeClaim"], produces = ["application/json"])
    fun closeClaimFromIntegratedFacility(@RequestBody dto: IntegratedClaimCloseRequestDto) =
        claimsService.closeClaimFromIntegratedFacility(dto)

    @PostMapping(value = ["/integrated/saveUnSuccessfulCloseClaimRef"], produces = ["application/json"])
    fun saveUnSuccessfulCloseClaimRef(@RequestBody dto: IntegratedUnsuccessfulClaimCloseDTO) = claimsService
        .saveUnSuccessfulCloseClaimRef(dto)

    @PostMapping(value = ["/transactions"], produces = ["application/json"])
    fun transactions(
        @RequestBody dto: BenefitsDto
    ) = claimsService.getVisitTransactions(dto)

    @GetMapping(value = ["/transactions"], produces = ["application/json"])
    fun getTransactions(
        @RequestParam("beneficiaryId") beneficiaryId: Long,
        @RequestParam("mainBenefitId") mainBenefitId: Long,
        @RequestParam(value = "page", defaultValue = "1") page: Int,
        @RequestParam(value = "size", defaultValue = "20") size: Int
    ) = claimsService.getVisitTransactions(beneficiaryId, mainBenefitId, page, size)

    @GetMapping(value = ["/payer/beneficiary"], produces = ["application/json"])
    fun getIndividualBeneficiaryBenefitByPayerId(
        @RequestParam("payerId") payerId: Long,
        @RequestParam("memberNumber") memberNumber: String
    ) = claimsService
        .getBeneficiaryBenefitByPayerId(payerId, memberNumber);

    @GetMapping(value = ["/payer/providers"], produces = ["application/json"])
    fun getProvidersByPayerId(
        @RequestParam("payerId") payerId: Long,
    ) = claimsService
        .getProvidersByPayer(payerId);

    @PostMapping(value = ["/cancelVisit"], produces = ["application/json"])
    @Operation(summary = "Cancel Visit at  integration/service provider")
    fun cancelVisit(@RequestBody dto: CancelVisitDTO) = claimsService.cancelVisit(dto)

    @PostMapping(value = ["/saveClaimFromMiddleware"], produces = ["application/json"])
    fun saveClaimFromMiddleware(@RequestBody dto: ClaimRes) = service.saveClaimFromMiddleware(dto)

    @PostMapping(value = ["/saveCompleteDiagnosisClaimFromMiddleware"], produces = ["application/json"])
    fun saveCompleteDiagnosisClaimFromMiddleware(@RequestBody dto: DiagnosisClaimRes) = service.saveCompleteDiagnosisClaimFromMiddleware(dto)

    @PostMapping(value = ["/deactivateBenefits"], produces = ["application/json"])
    fun deactivateBenefits(@RequestBody dto: DeactivateBenefitDTO) = service.deactivateBenefits(dto)

    @PostMapping(value = ["/activateBenefits"], produces = ["application/json"])
    fun activateBenefits(@RequestBody dto: activateBenefitDTO) = service.activateBenefits(dto)

    @PostMapping(value = ["/topUpBenefit"], produces = ["application/json"])
    fun topUpBenefit(@RequestBody dto: TopUpBenefitDTO) = service.topUpBenefit(dto)

    @PostMapping(value = ["/transferBenefit"], produces = ["application/json"])
    fun transferBenefit(@RequestBody dto: TransferBenefitDTO) = service.transferBenefit(dto)

    @GetMapping(value = ["/searchByInvoiceOrMemberNo"], produces = ["application/json"])
    fun getByInvoiceNumberOrMemberNo(@RequestParam(value = "payerId") payerId: String,
                                     @RequestParam(value = "search") search: String) =
        claimsService.getClaimBySearch(payerId,search)

    @GetMapping(value = ["/mainandbranchClaims"], produces = ["application/json"])
    fun MainAndBranchClaims(

        @RequestParam(value = "hospitalProviderId") hospitalProviderId: Long,
        @RequestParam(value = "dateFrom") dateFrom: String,
        @RequestParam(value = "dateTo") dateTo: String
    ) = claimsService.getMainAndBranchClaims( hospitalProviderId, dateFrom, dateTo)

    @GetMapping(value = ["/providerClaims"], produces = ["application/json"])
    fun providerClaims(
        @RequestParam(value = "planId") planId: String,
        @RequestParam(value = "hospitalProviderId") hospitalProviderId: Long,
        @RequestParam(value = "dateFrom") dateFrom: String,
        @RequestParam(value = "dateTo") dateTo: String,
        @RequestParam(value = "invoiceNo") invoiceNo: String?,
        @RequestParam(value = "memberNo") memberNo: String?,
        @RequestParam(value = "payerId") payerId: String?
    ) = claimsService.getProviderClaims(planId, hospitalProviderId, dateFrom, dateTo,invoiceNo,
        memberNo,payerId)

    @GetMapping(value = ["/mainandbranchClaimsPdf"], produces = ["application/json"])
    fun MainAndBranchClaimsPdf(

        @RequestParam(value = "hospitalProviderId") hospitalProviderId: Long,
        @RequestParam(value = "dateFrom") dateFrom: String,
        @RequestParam(value = "dateTo") dateTo: String
    ) = claimsService.getMainAndBranchClaimsPdf( hospitalProviderId, dateFrom, dateTo)

    @GetMapping(value = ["/providerClaimsPdf"], produces = ["application/json"])
    fun providerClaimsPdf(
        @RequestParam(value = "planId") planId: String,
        @RequestParam(value = "hospitalProviderId") hospitalProviderId: Long,
        @RequestParam(value = "dateFrom") dateFrom: String,
        @RequestParam(value = "dateTo") dateTo: String,
        @RequestParam(value = "invoiceNo") invoiceNo: String?,
        @RequestParam(value = "memberNo") memberNo: String?,
        @RequestParam(value = "payerId") payerId: String?
    ) = claimsService.getProviderClaimsPdf(planId, hospitalProviderId, dateFrom, dateTo,
        invoiceNo,memberNo,payerId)

    @GetMapping(value = ["/mainandbranchClaimsExcel"], produces = ["application/json"])
    fun MainAndBranchClaimsExcel(

        @RequestParam(value = "hospitalProviderId") hospitalProviderId: Long,
        @RequestParam(value = "dateFrom") dateFrom: String,
        @RequestParam(value = "dateTo") dateTo: String
    ) = claimsService.getMainAndBranchClaimsExcel( hospitalProviderId, dateFrom, dateTo)

    @GetMapping(value = ["/providerClaimsExcel"], produces=["application/json"])
    fun providerClaimsExcel(
        @RequestParam(value = "planId") planId: String,
        @RequestParam(value = "hospitalProviderId") hospitalProviderId: Long,
        @RequestParam(value = "dateFrom") dateFrom: String?,
        @RequestParam(value = "dateTo") dateTo: String?,
        @RequestParam(value = "invoiceNo") invoiceNo: String?,
        @RequestParam(value = "memberNo") memberNo: String?,
        @RequestParam(value = "payerId") payerId: String?
    ) = claimsService.getProviderClaimsExcel(planId, hospitalProviderId, dateFrom, dateTo,invoiceNo,
        memberNo,payerId)

    @GetMapping(value = ["/planOfflcts"], produces = ["application/json"])
    fun planOfflcts(
        @RequestParam(value = "planId") planId: String,
        @RequestParam(value = "hospitalProviderId") hospitalProviderId: String?,
        @RequestParam(value = "dateFrom") dateFrom: String,
        @RequestParam(value = "dateTo") dateTo: String,
        @RequestParam(value = "invoiceNo") invoiceNo: String?,
        @RequestParam(value = "memberNo") memberNo: String?,
        @RequestParam(value = "payerId") payerId: String?
    ) = claimsService.getPlanOfflcts(planId, hospitalProviderId, dateFrom, dateTo,invoiceNo,
        memberNo,payerId)
    @GetMapping(value = ["/planOfflctsPdf"], produces = ["application/json"])
    fun planOfflctsPdf(
        @RequestParam(value = "planId") planId: String,
        @RequestParam(value = "from") fromDate: String,
        @RequestParam(value = "to") toDate: String,
        @RequestParam(value = "hospitalProviderId") hospitalProviderId: String?,
    ) = claimsService.getPlanOfflctsPdf(planId,  fromDate, toDate,hospitalProviderId)

    @GetMapping(value = ["/getPlanReimbursementsPdf"], produces = ["application/json"])
    fun planReimbursementsPdf(
        @RequestParam(value = "planId") planId: String,
        @RequestParam(value = "from") fromDate: String,
        @RequestParam(value = "to") toDate: String,
    ) = claimsService.getPlanReimbursementsPdf(planId,  fromDate, toDate)

    @GetMapping(value = ["/planReimbursements"], produces = ["application/json"])
    fun getPlanReimbursements(
        @RequestParam(value = "planId") planId: String,
        @RequestParam(value = "dateFrom") dateFrom: String,
        @RequestParam(value = "dateTo") dateTo: String
    ) = claimsService.getPlanReimbursements(planId, dateFrom, dateTo)

    @GetMapping(value = ["/planClaims"], produces = ["application/json"])
    fun planClaims(
        @RequestParam(value = "planId") planId: String,
        @RequestParam(value = "dateFrom") dateFrom: String,
        @RequestParam(value = "dateTo") dateTo: String,
        @RequestParam(value = "invoiceNo") invoiceNo: String,
        @RequestParam(value = "memberNo") memberNo: String,
        @RequestParam(value = "payerId") payerId: String
    ) = claimsService.getPlanClaims(planId, dateFrom, dateTo,invoiceNo,memberNo,payerId)


    @GetMapping(value = ["/allTransactions"], produces = ["application/json"])
    fun allTransactions(
        @RequestParam(value = "planId") planId: String?,
        @RequestParam(value = "hospitalProviderId") hospitalProviderId: String?,
        @RequestParam(value = "dateFrom") dateFrom: String,
        @RequestParam(value = "dateTo") dateTo: String,
        @RequestParam(value = "invoiceNo") invoiceNo: String?,
        @RequestParam(value = "memberNo") memberNo: String?,
        @RequestParam(value = "payerId") payerId: String?,
        @RequestParam(value = "status") status: String
    ) = claimsService.allTransactions(planId, hospitalProviderId, dateFrom, dateTo,invoiceNo,
        memberNo,payerId,status)

    @GetMapping(value = ["/previewProviderInvoice"], produces = ["application/json"])
    fun previewProviderInvoice(
        @RequestParam(value = "invoiceNumber") invoiceNumber: String,
        @RequestParam(value = "hospitalProviderId") hospitalProviderId: Long
    ) = claimsService.previewProviderInvoice(invoiceNumber, hospitalProviderId)

    @GetMapping(value = ["/scheme_clinical_utilization"],produces = ["application/json"])
    fun schemeClinicalUtilization(filters: ReportFilters)
            =claimsService.getSchemeClinicalUtilizations(filters);

    @GetMapping(value = ["/scheme_financial_utilization"],produces = ["application/json"])
    fun schemeFinancialUtilization(filters: ReportFilters)
            =claimsService.getSchemeFinancialUtilizations(filters);

    @PostMapping(value = ["/find_invoice_provider"],produces = ["application/json"])
    fun findInvoiceProvider(@RequestBody invoiceDetails: InvoiceDetails)
            =claimsService.getInvoiceProvider(invoiceDetails);
}