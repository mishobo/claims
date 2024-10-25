package net.lctafrica.claimsapis.service


import com.google.gson.Gson
import com.google.gson.GsonBuilder
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import net.lctafrica.claimsapis.dto.*
import net.lctafrica.claimsapis.dto.InvoiceLine
import net.lctafrica.claimsapis.dto.InvoiceRes
import net.lctafrica.claimsapis.dto.ProviderMappingRes
import net.lctafrica.claimsapis.helper.ExcelHelper
import net.lctafrica.claimsapis.model.intergratedDocuments.InvoiceDetails
import net.lctafrica.claimsapis.model.reports.Clinical
import net.lctafrica.claimsapis.model.reports.Financial
import net.lctafrica.claimsapis.model.reports.Requests.ReportFilters
import net.lctafrica.claimsapis.repository.*
import net.lctafrica.claimsapis.util.*
import net.sf.jasperreports.engine.JasperCompileManager
import net.sf.jasperreports.engine.JasperExportManager
import net.sf.jasperreports.engine.JasperFillManager
import net.sf.jasperreports.engine.JasperReport
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter
import net.sf.jasperreports.export.SimpleExporterInput
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput
import net.sf.jasperreports.export.SimpleXlsxReportConfiguration
import org.apache.camel.ProducerTemplate
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.json.JSONObject
import org.json.XML
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.Resource
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.ResourceUtils
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.io.*
import java.math.BigDecimal
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.stream.Collectors
import net.lctafrica.claimsapis.enums.BenefitType
import net.lctafrica.claimsapis.enums.CapitationPeriod


@Service("claimsService")
@Transactional
class ClaimsService(
	val invoiceRepo: InvoiceRepository,
	val visitRepo: VisitRepository,
	val invoiceLineRepo: InvoiceLineRepository,
	val diagnosisRepository: DiagnosisRepository,
	val clinicalInfoRepository: ClinicalInfoRepository,
	val excelHelper: ExcelHelper,
	val icD10Repository: ICD10Repository,
	val medicalProcedureRepository: MedicalProcedureRepository,
	val medicalDrugRepository: MedicalDrugRepository,
	val laboratoryRepository: LaboratoryRepository,
	val radiologyRepository: RadiologyRepository,
	val otherBenefitDetailRepository: OtherBenefitDetailRepository,
	val claimCloseFailedRepo: ClaimCloseFailedRepo,
	val beneficiaryBenefitRepository: BeneficiaryBenefitRepository,
	val invoiceProcedureCodeRepo: InvoiceProcedureCodeRepo,
	val jdbcTemplate: JdbcTemplate,
	val producerTemplate: ProducerTemplate,
	val benefitService: BenefitService,
	val preAuthService: PreAuthService,
	val documentsRepo: DocumentsRepo,
	val preAuthRepo: PreAuthRepo
) : IClaimsService {

	private val modelMapper = MyModelMapper.init()

	@Value("\${lct-africa.member.search.url}")
	lateinit var memberSearchUrl: String

	@Value("\${lct-africa.notification.baseUrl}")
	lateinit var notificationBaseUrl: String

	@Value("\${lct-africa.notification.email-attachment-endpoint}")
	lateinit var emailUrl: String

	@Value("\${lct-africa.document.url}")
	lateinit var documentUrl: String

	@Value("\${lct-africa.claims.url}")
	lateinit var claimsUrl: String

	@Value("\${lct-africa.claims.start_visit.url}")
	lateinit var startVisit: String

	@Value("\${lct-africa.claims.agakhanNairobi}")
	lateinit var agakhanNairobi: String

	var SHEET = "CLAIMS"
	var FILEHEADER = arrayOf("Member Number", "Member Name","Benefit", "Invoice Amount",
		"Invoice Date", "Invoice Number", "Scheme","Provider")


	@Transactional(readOnly = false, rollbackFor = [Exception::class])
	override fun reverseClaim(reverseVisitDTO: ReverseInvoiceDTO): Result<Boolean> {
		val visit = visitRepo.findById(reverseVisitDTO.visitNumber!!)
		if (visit.isPresent) {
			val theVisit = visit.get()
			if(theVisit.status!! == Status.REJECTED)
				return ResultFactory.getFailResult(false,"Claim already reversed!")
			val reverseBenefitDTO = ReverseBenefitDTO(
				amount = theVisit.totalInvoiceAmount!!,
				aggregateId = theVisit.aggregateId,
				benefitId = theVisit.benefitId!!,
				memberNumber = theVisit.memberNumber,
				visitNumber = theVisit.id

			)
			val reverseRes = benefitService.reverseBenefit(reverseBenefitDTO)
			val invoice = invoiceRepo.findByInvoiceNumber(theVisit.invoiceNumber!!)
			if(invoice.isPresent){
				val theInvoice = invoice.get()
				theInvoice.apply {
					this.invoiceNumber = "REV-" +this.invoiceNumber
				}
				invoiceRepo.save(theInvoice)
			}

			return if (reverseRes.success) {
				theVisit.apply {
					this.invoiceNumber = "REV-" + this.invoiceNumber
					this.status = Status.REJECTED
				}
				visitRepo.save(theVisit)
				ResultFactory.getSuccessResult(data = true)
			} else {
				ResultFactory.getFailResult(msg = "Could not perform Reversal")
			}
		} else {
			return ResultFactory.getFailResult(msg = "No visit found with Number ${visit.get().id}")
		}
	}

	@Transactional(readOnly = false, rollbackFor = [Exception::class])
	override fun fullInvoiceReversal(reverseInvoiceDTO: ReverseInvoiceDTO): Result<Boolean> {
		val visit = visitRepo.findById(reverseInvoiceDTO.visitNumber!!)
		if (visit.isPresent) {
			val theVisit = visit.get()
			if (theVisit.status!! == Status.REJECTED)
				return ResultFactory.getFailResult(false, "Claim already reversed!")

			val invoiceOptional = invoiceRepo.findByInvoiceNumberAndVisit(
				reverseInvoiceDTO.invoiceNumber.toString(),
				theVisit
			)

			if (invoiceOptional.isPresent) {
				val theInvoice = invoiceOptional.get()
				val reverseBenefitDTO = ReverseBenefitDTO(
					amount = theInvoice.totalAmount!!,
					aggregateId = theVisit.aggregateId,
					benefitId = theVisit.benefitId!!,
					memberNumber = theVisit.memberNumber,
					visitNumber = theVisit.id

				)
				val reverseRes = benefitService.reverseBenefit(reverseBenefitDTO)

				if (reverseRes.success) {
					theInvoice.apply {
						this.invoiceNumber = "REV-" + this.invoiceNumber
						this.status = INVOICE_STATUS.REJECTED
					}
					invoiceRepo.save(theInvoice)

					theInvoice.invoiceLines.forEach {
						it.apply {
							this.invoiceNumber = "REVERSED-" + this.invoiceNumber
						}
						invoiceLineRepo.save(it)
					}
					return ResultFactory.getSuccessResult(data = true)
				} else {
					return ResultFactory.getFailResult(msg = "Could not perform Reversal")
				}
			}
			return ResultFactory.getFailResult(
				msg = "No Invoice found with ${
					reverseInvoiceDTO
						.invoiceNumber
				}"
			)
		}
		return ResultFactory.getFailResult(msg = "Could not find Visit")


	}

	@Transactional(readOnly = false, rollbackFor = [Exception::class])
	override fun lineItemReversal(reverseLineItemDTO: ReverseLineItemDTO): Result<Boolean> {
		val visit = visitRepo.findById(reverseLineItemDTO.visitNumber!!)
		var newInvoice: Invoice? = null
		if (visit.isPresent) {
			val theVisit = visit.get()
			if (theVisit.status!! == Status.REJECTED)
				return ResultFactory.getFailResult(false, "Claim already reversed!")

			val invoiceOptional = invoiceRepo.findByInvoiceNumberAndVisit(
				reverseLineItemDTO.invoiceNumber.toString(),
				theVisit
			)
			if (invoiceOptional.isPresent) {
				val theInvoice = invoiceOptional.get()
				theInvoice.apply {
					this.invoiceNumber = "REV-" + this.invoiceNumber
					this.status = INVOICE_STATUS.REJECTED
				}
				invoiceRepo.save(theInvoice)
				theInvoice.invoiceLines.forEach {

					it.invoiceNumber = "REVERSED-" + it.invoiceNumber
					invoiceLineRepo.save(it)
					if (it.id == reverseLineItemDTO.invoiceId) {
						newInvoice = Invoice(
							invoiceNumber = theInvoice.invoiceNumber!!.replace("REV-", ""),
							totalAmount = theInvoice.totalAmount!! - it.lineTotal!!,
							visit = theVisit,
							service = theInvoice.service,
							hospitalProviderId = null
						)
						invoiceRepo.save(newInvoice!!)

					}
				}
				theInvoice.invoiceLines.forEach {

					if (it.id == reverseLineItemDTO.invoiceId) {
						val reverseBenefitDTO = ReverseBenefitDTO(
							amount = it.lineTotal!!,
							aggregateId = theVisit.aggregateId,
							benefitId = theVisit.benefitId!!,
							memberNumber = theVisit.memberNumber,
							visitNumber = theVisit.id
						)
						benefitService.reverseBenefit(reverseBenefitDTO)

					}

					if (newInvoice != null && it.id != reverseLineItemDTO.invoiceId!!.toLong()) {
						val invoiceline = InvoiceLine(
							description = it.description,
							lineTotal = it.lineTotal,
							invoiceNumber = newInvoice!!.invoiceNumber,
							invoice = newInvoice,
							lineType = it.lineType,
							claimRef = it.claimRef,
							lineCategory = it.lineCategory,
							unitPrice = it.unitPrice,
							quantity = it.quantity

						)
						invoiceLineRepo.save(invoiceline)
					}

				}
				return ResultFactory.getSuccessResult(data = true)
			}
			return ResultFactory.getFailResult(
				msg = "No Invoice found with ${
					reverseLineItemDTO
						.invoiceNumber
				}"
			)
		}
		return ResultFactory.getFailResult(msg = "Could not find Visit")
	}


	@Transactional(readOnly = false, rollbackFor = [Exception::class])
	override fun startVisit(dto: StartVisitDTO): Result<Visit?> {
//		val membershipClient = WebClient.builder()
//			.baseUrl(memberSearchUrl).build()

		val beneficiaryBenefit = beneficiaryBenefitRepository.findByAggregateIdAndBenefitIdAndMemberNumber("${dto.aggregateId}",dto.benefitId,dto.memberNumber)
		if(beneficiaryBenefit?.benefitType == BenefitType.CAPITATION){
			beneficiaryBenefit.capitationPeriod?.let {
				val visitDateRange = getDateRange(it)
				val maxVisits = beneficiaryBenefit.capitationMaxVisitCount
				val visitsCount = visitRepo.findVisitsBetweenGivenPeriod(
					aggregateId = "${dto.aggregateId}",
					benefitId = dto.benefitId,
					status = Status.CLOSED,
					status2 = Status.LINE_ITEMS_ADDED,
					claimProcessStatus = ClaimProcessStatus.PROCESSED,
					startDate = visitDateRange.first.atStartOfDay(),
					endDate = visitDateRange.second.atStartOfDay()
					)
				if(visitsCount.size >= maxVisits){
					return ResultFactory.getFailResult("Client has exhausted the ${it.name} maximum number ($maxVisits) of visits allowed")
				}
			}

			if(!checkIfDateTodayIsInRange(beneficiaryBenefit.startDate,beneficiaryBenefit.endDate)){
				return ResultFactory.getFailResult("The expected benefit consumption date is from ${beneficiaryBenefit.startDate} to ${beneficiaryBenefit.endDate}")
			}
		}

		when {
			dto.staffName.contains("inpatient") -> {
				val newVisit = Visit(
					memberNumber = dto.memberNumber.trim(),
					memberName = dto.memberName.trim(),
					hospitalProviderId = dto.hospitalProviderId,
					staffId = dto.staffId,
					staffName = dto.staffName,
					status = Status.ACTIVE,
					middlewareStatus = MiddlewareStatus.SENT,
					totalInvoiceAmount = BigDecimal.ZERO,
					aggregateId = dto.aggregateId!!,
					diagnosis = mutableListOf(),
					claimProcessStatus = ClaimProcessStatus.UNPROCESSED,
					beneficiaryType = dto.beneficiaryType,
					benefitName = dto.benefitName,
					categoryId = dto.categoryId,
					invoiceNumber = null,
					payerId = dto.payerId,
					policyNumber = dto.policyNumber,
					balanceAmount = dto.balanceAmount,
					benefitId = dto.benefitId,
					beneficiaryId = dto.beneficiaryId,
					providerMiddleware = MIDDLEWARENAME.NONE,
					visitType = VisitType.valueOf(dto.visitType.toString()),
					offSystemReason = dto.offSystemReason,
					reimbursementProvider = dto.reimbursementProvider,
					reimbursementInvoiceDate = dto.reimbursementInvoiceDate,
					reimbursementReason = dto.reimbursementReason,
					facilityType = dto.facilityType,
					providerMapping = null,
					benefitMapping = null,
					invoiceDate =  LocalDateTime.now(),
					createdAt = LocalDateTime.now()


				)
				val savedVisit = visitRepo.save(newVisit)
				//println("save visit tracker---------------")
				//println(savedVisit)
				return ResultFactory.getSuccessResult(savedVisit)

			}
			dto.visitType?.equals(VisitType.OFF_LCT) == true -> {
				//println(dto)
				val newVisit = Visit(
					memberNumber = dto.memberNumber.trim(),
					memberName = dto.memberName.trim(),
					hospitalProviderId = dto.hospitalProviderId,
					staffId = dto.staffId,
					staffName = dto.staffName,
					status = Status.ACTIVE,
					middlewareStatus = MiddlewareStatus.SENT,
					totalInvoiceAmount = BigDecimal.ZERO,
					aggregateId = dto.aggregateId!!,
					diagnosis = mutableListOf(),
					claimProcessStatus = ClaimProcessStatus.UNPROCESSED,
					beneficiaryType = dto.beneficiaryType,
					benefitName = dto.benefitName,
					categoryId = dto.categoryId,
					invoiceNumber = null,
					payerId = dto.payerId,
					policyNumber = dto.policyNumber,
					balanceAmount = dto.balanceAmount,
					benefitId = dto.benefitId,
					beneficiaryId = dto.beneficiaryId,
					providerMiddleware = MIDDLEWARENAME.NONE,
					visitType = VisitType.valueOf(dto.visitType.toString()),
					offSystemReason = dto.offSystemReason,
					reimbursementProvider = null,
					reimbursementInvoiceDate = null,
					reimbursementReason = null,
					facilityType = dto.facilityType,
					providerMapping = null,
					benefitMapping = null,
					invoiceDate = dto.offlctInvoiceDate!!.atStartOfDay(),
					createdAt = LocalDateTime.now()


				)
				val savedVisit = visitRepo.save(newVisit)
				//println("save visit tracker---------------")
				//println(savedVisit)

				return ResultFactory.getSuccessResult(savedVisit)

			}
			dto.visitType?.equals(VisitType.REIMBURSEMENT) == true -> {
				//println(dto)
				val newVisit = Visit(
					memberNumber = dto.memberNumber.trim(),
					memberName = dto.memberName.trim(),
					hospitalProviderId = dto.hospitalProviderId,
					staffId = dto.staffId,
					staffName = dto.staffName,
					status = Status.ACTIVE,
					middlewareStatus = MiddlewareStatus.SENT,
					totalInvoiceAmount = BigDecimal.ZERO,
					aggregateId = dto.aggregateId!!,
					diagnosis = mutableListOf(),
					claimProcessStatus = ClaimProcessStatus.UNPROCESSED,
					beneficiaryType = dto.beneficiaryType,
					benefitName = dto.benefitName,
					categoryId = dto.categoryId,
					invoiceNumber = null,
					payerId = dto.payerId,
					policyNumber = dto.policyNumber,
					balanceAmount = dto.balanceAmount,
					benefitId = dto.benefitId,
					beneficiaryId = dto.beneficiaryId,
					providerMiddleware = MIDDLEWARENAME.NONE,
					visitType = VisitType.valueOf(dto.visitType.toString()),
					offSystemReason = dto.offSystemReason,
					reimbursementProvider = dto.reimbursementProvider,
					reimbursementInvoiceDate = dto.reimbursementInvoiceDate,
					reimbursementReason = dto.reimbursementReason,
					facilityType = dto.facilityType,
					providerMapping = null,
					benefitMapping = null,
					invoiceDate = dto.reimbursementInvoiceDate!!.atStartOfDay(),
					createdAt = LocalDateTime.now()

				)
				val savedVisit = visitRepo.save(newVisit)
				//println("save visit tracker---------------")
				//println(savedVisit)

				return ResultFactory.getSuccessResult(savedVisit)

			}
			else -> {

				try {
					val active = visitRepo.findActiveVisitsForMember(
						memberNumber = dto.memberNumber.trim(),
						hospitalProviderId = dto.hospitalProviderId,
						status = Status.ACTIVE
					)
					if (active.isPresent)
						return ResultFactory.getFailResult("Member has an open visit. Please proceed to billing")

					val membershipClient = WebClient.builder()
						.baseUrl(memberSearchUrl).build()
					val providerId = dto.hospitalProviderId

					val providerResponse = membershipClient
						.get()
						.uri { u ->
							u
								.path("/api/v1/provider")
								.queryParam("providerId", providerId)
								.build()
						}
						.retrieve()
						.bodyToMono(String::class.java)
						.block()
					//println(providerResponse)
					//println(providerResponse.toString())
					val provider = gson.fromJson(providerResponse, ProviderRes::class.java)
					var response = false
					var providerMiddlewareCategory = ""
					//				println("SendToMiddleware------$providerId --------${provider.data.providerMiddleware}")
					//				println("MID ${provider.data.providerMiddleware}")
					val webClient: WebClient?
					if (provider.data.billingStation == true && (provider.data.providerMiddleware ==
								null || provider
							.data.providerMiddleware == MIDDLEWARENAME.valueOf("NONE").toString())) {
						var savedVisit: Visit?

						//Check if member has had another visit with same benefit in same Facility
						val previousVisitExists = visitRepo
							.findMemberVisitsByMemberNumberAndBenefitAndFacilityAndToday(
								dto
									.memberNumber.trim(), dto.benefitId, dto.hospitalProviderId
							)

						if (previousVisitExists.isNotEmpty()) {

							val theVisit = previousVisitExists[0]
							savedVisit = reopenStandaloneMultipleProviderVisitSetToActive(
								ExistingTodayVisitDTO(
									aggregateId = theVisit!!.aggregateId,
									benefitName = theVisit.benefitName,
									memberNumber = theVisit.memberNumber,
									visitNumber = theVisit.id
								)
							)

						} else {
							//println(dto)
							val newVisit = Visit(
								memberNumber = dto.memberNumber.trim(),
								memberName = dto.memberName.trim(),
								hospitalProviderId = dto.hospitalProviderId,
								staffId = dto.staffId,
								staffName = dto.staffName,
								status = Status.ACTIVE,
								middlewareStatus = MiddlewareStatus.SENT,
								totalInvoiceAmount = BigDecimal.ZERO,
								aggregateId = dto.aggregateId!!,
								diagnosis = mutableListOf(),
								claimProcessStatus = ClaimProcessStatus.UNPROCESSED,
								beneficiaryType = dto.beneficiaryType,
								benefitName = dto.benefitName,
								categoryId = dto.categoryId,
								invoiceNumber = null,
								payerId = dto.payerId,
								policyNumber = dto.policyNumber,
								balanceAmount = dto.balanceAmount,
								benefitId = dto.benefitId,
								beneficiaryId = dto.beneficiaryId,
								providerMiddleware = MIDDLEWARENAME.NONE,
								visitType = dto.visitType,
								offSystemReason = dto.offSystemReason,
								reimbursementProvider = null,
								reimbursementInvoiceDate = null,
								reimbursementReason = dto.reimbursementReason,
								facilityType = dto.facilityType,
								providerMapping = null,
								benefitMapping = null,
								invoiceDate =  LocalDateTime.now(),
								createdAt = LocalDateTime.now()
							)
							savedVisit = visitRepo.save(newVisit)
							//						println("save visit tracker---------------")
							//						println(savedVisit)

						}

						return ResultFactory.getSuccessResult(savedVisit)

					} else if (provider.data.providerMiddleware == null || provider.data.providerMiddleware == MIDDLEWARENAME.valueOf("NONE").toString()) {
						var savedVisit: Visit?

						//println(dto)
						val newVisit = Visit(
							memberNumber = dto.memberNumber.trim(),
							memberName = dto.memberName.trim(),
							hospitalProviderId = dto.hospitalProviderId,
							staffId = dto.staffId,
							staffName = dto.staffName,
							status = Status.ACTIVE,
							middlewareStatus = MiddlewareStatus.SENT,
							totalInvoiceAmount = BigDecimal.ZERO,
							aggregateId = dto.aggregateId!!,
							diagnosis = mutableListOf(),
							claimProcessStatus = ClaimProcessStatus.UNPROCESSED,
							beneficiaryType = dto.beneficiaryType,
							benefitName = dto.benefitName,
							categoryId = dto.categoryId,
							invoiceNumber = null,
							payerId = dto.payerId,
							policyNumber = dto.policyNumber,
							balanceAmount = dto.balanceAmount,
							benefitId = dto.benefitId,
							beneficiaryId = dto.beneficiaryId,
							providerMiddleware = MIDDLEWARENAME.NONE,
							visitType = dto.visitType,
							offSystemReason = dto.offSystemReason,
							reimbursementProvider = null,
							reimbursementInvoiceDate = null,
							reimbursementReason = dto.reimbursementReason,
							facilityType = dto.facilityType,
							providerMapping = null,
							benefitMapping = null,
							invoiceDate =  LocalDateTime.now(),
							createdAt = LocalDateTime.now()
						)
						savedVisit = visitRepo.save(newVisit)
						//					println("save visit tracker---------------")
						//					println(savedVisit)
						return ResultFactory.getSuccessResult(savedVisit)

					} else if (!provider.data.providerMiddleware.isNullOrBlank()) {
						//			Build payload
						val payerId = dto.payerId
						val categoryId = dto.categoryId

						//	        using categoryId and payer id
						val payerResponse = membershipClient
							.get()
							.uri { u ->
								u
									.path("/api/v1/membership/payers/${payerId}/payer")
									.build()
							}
							.retrieve()
							.bodyToMono(String::class.java)
							.block()

						val planResponse = membershipClient
							.get()
							.uri { u ->
								u
									.path("/api/v1/membership/category/${categoryId}/plan")
									.build()
							}
							.retrieve()
							.bodyToMono(String::class.java)
							.block()

						val categoryResponse = membershipClient
							.get()
							.uri { u ->
								u
									.path("/api/v1/membership/category/${categoryId}/category")
									.build()
							}
							.retrieve()
							.bodyToMono(String::class.java)
							.block()

						val payerName = gson.fromJson(payerResponse, PayerRes::class.java).data.name
						val schemeName = gson.fromJson(planResponse, PlanRes::class.java).data.name
						val schemeCode =
							gson.fromJson(
								categoryResponse,
								CategoryRes::class.java
							).data.agakhanSchemeCode

						val insuranceId =
							gson.fromJson(
								categoryResponse,
								CategoryRes::class.java
							).data.agakhanInsuranceCode


						var firstName: String = ""
						var lastName: String = ""
						try {
							firstName = dto.memberName.trim().split(" ")[0]
							lastName = dto.memberName.trim().split(" ")[1]
						} catch (e: Exception) {
							//println("Member has only one name" + e.message)
						}
						val otherName = "N/A"
						var middleware = ""

						middleware = if (provider.data.providerMiddleware == null) {
							"NONE"
						} else {
							provider.data.providerMiddleware.toString()
						}

						var savedVisit: Visit? = null

						//					Check if member has had another visit with same benefit in same Facility

						val previousVisitExists = visitRepo
							.findMemberVisitsByMemberNumberAndBenefitAndFacilityAndToday(
								dto
									.memberNumber.trim(), dto.benefitId, dto.hospitalProviderId
							)

						if (previousVisitExists.isNotEmpty()) {

							val theVisit = previousVisitExists[0]
							savedVisit = reopenIntergratedProviderVisitSetToInactive(
								ExistingTodayVisitDTO(
									aggregateId = theVisit!!.aggregateId,
									benefitName = theVisit.benefitName,
									memberNumber = theVisit.memberNumber,
									visitNumber = theVisit.id
								)
							)

						} else {
							//			SAVE VISIT INTERIM STATUS AS INACTIVE
							val newVisit = Visit(
								memberNumber = dto.memberNumber.trim(),
								memberName = dto.memberName.trim(),
								hospitalProviderId = dto.hospitalProviderId,
								staffId = dto.staffId,
								staffName = dto.staffName,
								status = Status.INACTIVE,
								middlewareStatus = MiddlewareStatus.UNSENT,
								totalInvoiceAmount = BigDecimal.ZERO,
								aggregateId = dto.aggregateId!!,
								diagnosis = mutableListOf(),
								claimProcessStatus = ClaimProcessStatus.UNPROCESSED,
								beneficiaryType = dto.beneficiaryType,
								benefitName = dto.benefitName,
								categoryId = dto.categoryId,
								invoiceNumber = null,
								payerId = dto.payerId,
								policyNumber = dto.policyNumber,
								balanceAmount = dto.balanceAmount,
								benefitId = dto.benefitId,
								beneficiaryId = dto.beneficiaryId,
								providerMiddleware = MIDDLEWARENAME.valueOf(middleware),
								visitType = dto.visitType,
								offSystemReason = dto.offSystemReason,
								reimbursementProvider = null,
								reimbursementInvoiceDate = null,
								reimbursementReason = dto.reimbursementReason,
								providerMapping = null,
								benefitMapping = null,
								invoiceDate =  LocalDateTime.now(),
								createdAt = LocalDateTime.now()
							)
							savedVisit = visitRepo.save(newVisit)

						}


						when (provider.data.providerMiddleware) {
							MIDDLEWARENAME.AVENUE.toString() -> {
								providerMiddlewareCategory = "AVENUE"

							}

							MIDDLEWARENAME.AGAKHANNAIROBI.toString() -> {
								providerMiddlewareCategory = "AGAKHANNAIROBI"

							}

							MIDDLEWARENAME.AGAKHANNAIROBITEST.toString() -> {
								providerMiddlewareCategory = "AGAKHANNAIROBITEST"

							}

							MIDDLEWARENAME.AKUH.toString() -> {
								providerMiddlewareCategory = "AKUH"

							}

							MIDDLEWARENAME.AGAKHANMOMBASA.toString() -> {
								providerMiddlewareCategory = "AGAKHANMOMBASA"

							}

							MIDDLEWARENAME.AGAKHANKISUMU.toString() -> {
								providerMiddlewareCategory = "AGAKHANKISUMU"

							}

							MIDDLEWARENAME.METROPOLITAN.toString() -> {
								providerMiddlewareCategory = "METROPOLITAN"

							}

							MIDDLEWARENAME.MPSHAH.toString() -> {
								providerMiddlewareCategory = "MPSHAH"

							}

							MIDDLEWARENAME.GETRUDES.toString() -> {
								providerMiddlewareCategory = "GETRUDES"

							}
							MIDDLEWARENAME.GETRUDESTEST.toString() -> {
								providerMiddlewareCategory = "GETRUDESTEST"

							}

							MIDDLEWARENAME.NAIROBIHOSPITAL.toString() -> {
								providerMiddlewareCategory = "NAIROBIHOSPITAL"

							}

							MIDDLEWARENAME.MATER.toString() -> {
								providerMiddlewareCategory = "MATER"

							}

							MIDDLEWARENAME.NONE.toString() -> {
								providerMiddlewareCategory = "NONE"

							}
						}
						//					println(
						//						"SendToMiddleware------providerName$providerMiddlewareCategory -------  " +
						//								"${provider.data.providerMiddleware}"
						//					)


						//			Get url to send benefits to


						val claimsClient = WebClient.builder()
							.baseUrl(claimsUrl).build()

						val providerRes = claimsClient
							.get()
							.uri { u ->
								u
									.path("/api/v1/visit/providerUrl/getConfig")
									.queryParam("providerName", providerMiddlewareCategory)
									.queryParam("urlType", "BENEFIT")
									.build()
							}
							.retrieve()
							.bodyToMono(String::class.java)
							.block()
						//println("SendToMiddleware------tracker$providerRes")

						if (provider.data.providerMiddleware == MIDDLEWARENAME.AVENUE.toString()) {

							val providerUrl =
								gson.fromJson(providerRes, ProviderUrlResponse::class.java)
							val ipaddress: String? = providerUrl.data.ipaddress
							val port: String? = providerUrl.data.port
							webClient = WebClient.create("http://$ipaddress:$port")

							val smartFile = JSONObject()
							//println("AVENUE---$dto")
							smartFile.put("category_name", dto.categoryId.toString())
							smartFile.put("encounterId", savedVisit!!.id.toString())
							smartFile.put("has_inpatient", "N")
							smartFile.put("has_outpatient", "Y")
							smartFile.put("member_name", dto.memberName.toString())
							smartFile.put("member_number", dto.memberNumber.toString())
							smartFile.put("payer_code", 569845)
							smartFile.put("payer_name", dto.payerName)
							smartFile.put("scheme_code", 123)
							smartFile.put("scheme_name", schemeName)
							smartFile.put("slade_authentication_token", Math.random().toLong())
							smartFile.put("visit_limit", dto.balanceAmount.toString())
							smartFile.put("benefitName", dto.benefitName.toString())
							smartFile.put("benefitId", dto.benefitId.toString())
							smartFile.put("payerId", dto.payerId.toString())
							smartFile.put(
								"aggregateId",
								dto.aggregateId.toString()
							)
							smartFile.put("valid_from", LocalDateTime.now())
							smartFile.put(
								"valid_to", LocalDateTime.now(
									Clock.offset(
										Clock.systemUTC(),
										Duration.ofDays(2)
									)
								)
							)

							val sXML: String = XML.toString(smartFile, "slade_id_member")
							val smXML: ByteArray = sXML.toByteArray()
							val smartXML: String = Base64.getEncoder().encodeToString(smXML)

							val jsonObject: JSONObject = JSONObject()
							jsonObject.put("global_id", "0")
							jsonObject.put("member_nr", dto.memberNumber.toString())
							jsonObject.put("admit_id", 0)
							jsonObject.put("progress_flag", 1)
							jsonObject.put("rejection_reason", "")
							jsonObject.put("exchange_type", 1)
							jsonObject.put("inOut_type", 1)
							jsonObject.put("location_id", 0)
							jsonObject.put("smart_date", "")
							jsonObject.put("smartFile", smartXML)
							jsonObject.put("exchange_date", "")
							jsonObject.put("exchange_file", "")
							jsonObject.put("result_date", "0")
							jsonObject.put("result_file", "")
							jsonObject.put("mtiba_number", 0)
							jsonObject.put("deviceId", provider.data.providerMiddleware.toString())
							//println(jsonObject)

							val avenueResponse = webClient
								.post()
								.uri { u ->
									u.path(startVisit)
										.build()
								}
								.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
								.body(Mono.just(jsonObject.toString()), String::class.java)
								.retrieve()
								.bodyToMono(IntegratedClaimResponseDto2::class.java)
								.block()
							//println("SendToMiddlewareWebClientRes$avenueResponse")

							response = if (avenueResponse?.control?.error == false) {
								//SET VISIT TO ACTIVE & SENT
								updateVisitMiddlewareStatus(
									UpdateMiddlewareStatusDTO(
										aggregateId = savedVisit.aggregateId,
										benefitName = savedVisit.benefitName,
										memberNumber = savedVisit.memberNumber,
										visitNumber = savedVisit.id
									)
								)
								true

							} else {
								false
							}
						} else if (provider.data.providerMiddleware == MIDDLEWARENAME.AGAKHANNAIROBI.toString()) {
							val providerUrl =
								gson.fromJson(providerRes, ProviderUrlResponse::class.java)
							val ipaddress: String? = providerUrl.data.ipaddress
							val port: String? = providerUrl.data.port
							webClient = WebClient.create("http://$ipaddress:$port")

							val remoteResponse = webClient
								.post()
								.uri { u ->
									u
										.path(startVisit)
										.build()
								}
								.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
								.bodyValue(
									GroupOne(
										dto.aggregateId!!,
										dto.benefitId,
										dto.benefitName,
										dto.memberNumber,
										dto.memberName,
										dto.balanceAmount,
										payerName,
										schemeName,
										provider.data.providerMiddleware,
										firstName,
										lastName,
										savedVisit!!.id,
										"Active",
										"N/A",
										"Family",
										insuranceId,
										"P",
										schemeCode,
										"",
										lastName,
										deviceId = provider.data.providerMiddleware
									)
								)
								.retrieve()
								.bodyToMono(IntegratedClaimResponseDto2::class.java)
								.block()

							response = if (remoteResponse?.control?.error == false) {
								//					SET VISIT TO ACTIVE & SENT
								updateVisitMiddlewareStatus(
									UpdateMiddlewareStatusDTO(
										aggregateId = savedVisit.aggregateId,
										benefitName = savedVisit.benefitName,
										memberNumber = savedVisit.memberNumber,
										visitNumber = savedVisit.id
									)
								)
								true

							} else {
								false
							}
						} else if (provider.data.providerMiddleware == MIDDLEWARENAME.AGAKHANMOMBASA.toString()) {
							val providerUrl =
								gson.fromJson(providerRes, ProviderUrlResponse::class.java)
							val ipaddress: String? = providerUrl.data.ipaddress
							val port: String? = providerUrl.data.port
							webClient = WebClient.create("http://$ipaddress:$port")
							val remoteResponse = webClient
								.post()
								.uri { u ->
									u
										.path(startVisit)
										.build()
								}
								.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
								.bodyValue(
									GroupOne(
										dto.aggregateId!!,
										dto.benefitId,
										dto.benefitName,
										dto.memberNumber,
										dto.memberName,
										dto.balanceAmount,
										payerName,
										schemeName,
										provider.data.providerMiddleware,
										firstName,
										lastName,
										savedVisit!!.id,
										"Active",
										"N/A",
										"Family",
										insuranceId,
										"P",
										schemeCode,
										"",
										lastName,
										deviceId = provider.data.providerMiddleware
									)
								)
								.retrieve()
								.bodyToMono(IntegratedClaimResponseDto2::class.java)
								.block()

							response = if (remoteResponse?.control?.error == false) {
								//					SET VISIT TO ACTIVE & SENT
								updateVisitMiddlewareStatus(
									UpdateMiddlewareStatusDTO(
										aggregateId = savedVisit.aggregateId,
										benefitName = savedVisit.benefitName,
										memberNumber = savedVisit.memberNumber,
										visitNumber = savedVisit.id
									)
								)
								true

							} else {
								false
							}

						} else if (provider.data.providerMiddleware == MIDDLEWARENAME.AGAKHANKISUMU.toString()) {
							val providerUrl =
								gson.fromJson(providerRes, ProviderUrlResponse::class.java)
							val ipaddress: String? = providerUrl.data.ipaddress
							val port: String? = providerUrl.data.port
							webClient = WebClient.create("http://$ipaddress:$port")
							val remoteResponse = webClient
								.post()
								.uri { u ->
									u
										.path(startVisit)
										.build()
								}
								.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
								.bodyValue(
									GroupOne(
										dto.aggregateId!!,
										dto.benefitId,
										dto.benefitName,
										dto.memberNumber,
										dto.memberName,
										dto.balanceAmount,
										payerName,
										schemeName,
										provider.data.providerMiddleware,
										firstName,
										lastName,
										savedVisit!!.id,
										"Active",
										"N/A",
										"Family",
										insuranceId,
										"P",
										schemeCode,
										"",
										lastName,
										deviceId = provider.data.providerMiddleware
									)
								)
								.retrieve()
								.bodyToMono(IntegratedClaimResponseDto2::class.java)
								.block()

							response = if (remoteResponse?.control?.error == false) {
								//					SET VISIT TO ACTIVE & SENT
								updateVisitMiddlewareStatus(
									UpdateMiddlewareStatusDTO(
										aggregateId = savedVisit.aggregateId,
										benefitName = savedVisit.benefitName,
										memberNumber = savedVisit.memberNumber,
										visitNumber = savedVisit.id
									)
								)
								true

							} else {
								false
							}

						} else if (provider.data.providerMiddleware == MIDDLEWARENAME.METROPOLITAN.toString()) {
							val providerUrl =
								gson.fromJson(providerRes, ProviderUrlResponse::class.java)
							val ipaddress: String? = providerUrl.data.ipaddress
							val port: String? = providerUrl.data.port
							webClient = WebClient.create("http://$ipaddress:$port")

							val remoteResponse = webClient
								.post()
								.uri { u ->
									u
										.path(startVisit)
										.build()
								}
								.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
								.bodyValue(
									GroupOne(
										dto.aggregateId!!,
										dto.benefitId,
										dto.benefitName,
										dto.memberNumber,
										dto.memberName,
										dto.balanceAmount,
										payerName,
										schemeName,
										provider.data.providerMiddleware,
										firstName,
										lastName,
										savedVisit!!.id,
										"Active",
										"N/A",
										"Family",
										insuranceId,
										"P",
										schemeCode,
										"",
										lastName,
										deviceId = provider.data.providerMiddleware
									)
								)
								.retrieve()
								.bodyToMono(IntegratedClaimResponseDto2::class.java)
								.block()

							response = if (remoteResponse?.control?.error == false) {
								//					SET VISIT TO ACTIVE & SENT
								updateVisitMiddlewareStatus(
									UpdateMiddlewareStatusDTO(
										aggregateId = savedVisit.aggregateId,
										benefitName = savedVisit.benefitName,
										memberNumber = savedVisit.memberNumber,
										visitNumber = savedVisit.id
									)
								)
								true

							} else {
								false
							}
						} else if (provider.data.providerMiddleware == MIDDLEWARENAME.MPSHAH.toString()) {
							val providerUrl =
								gson.fromJson(providerRes, ProviderUrlResponse::class.java)
							val ipaddress: String? = providerUrl.data.ipaddress
							val port: String? = providerUrl.data.port
							webClient = WebClient.create("http://$ipaddress:$port")
							val payerCode = "INS00011"
							val remoteResponse = webClient
								.post()
								.uri { u ->
									u
										.path(startVisit)
										.build()
								}
								.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
								.bodyValue(
									GroupTwoKranium(
										dto.aggregateId!!,
										dto.benefitId,
										dto.benefitName,
										dto.memberNumber,
										dto.memberName,
										dto.balanceAmount,
										payerName,
										schemeName,
										provider.data.providerMiddleware,
										payerCode,
										firstName,
										lastName,
										savedVisit!!.id,
										deviceId = provider.data.providerMiddleware
									)
								)
								.retrieve()
								.bodyToMono(IntegratedClaimResponseDto2::class.java)
								.block()

							response = if (remoteResponse?.control?.error == false) {
								//					SET VISIT TO ACTIVE & SENT
								updateVisitMiddlewareStatus(
									UpdateMiddlewareStatusDTO(
										aggregateId = savedVisit.aggregateId,
										benefitName = savedVisit.benefitName,
										memberNumber = savedVisit.memberNumber,
										visitNumber = savedVisit.id
									)
								)
								true

							} else {
								false
							}

						} else if (provider.data.providerMiddleware == MIDDLEWARENAME.GETRUDES.toString()) {
							val providerUrl =
								gson.fromJson(providerRes, ProviderUrlResponse::class.java)
							val ipaddress: String? = providerUrl.data.ipaddress
							val port: String? = providerUrl.data.port
							webClient = WebClient.create("http://$ipaddress:$port")
							val payerCode = "INS00011"
							val remoteResponse = webClient
								.post()
								.uri { u ->
									u
										.path(startVisit)
										.build()
								}
								.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
								.bodyValue(
									GroupTwoKranium(
										dto.aggregateId!!,
										dto.benefitId,
										dto.benefitName,
										dto.memberNumber,
										dto.memberName,
										dto.balanceAmount,
										payerName,
										schemeName,
										provider.data.providerMiddleware,
										payerCode,
										firstName,
										lastName,
										savedVisit!!.id,
										deviceId = provider.data.providerMiddleware
									)
								)
								.retrieve()
								.bodyToMono(IntegratedClaimResponseDto2::class.java)
								.block()

							response = if (remoteResponse?.control?.error == false) {
								//					SET VISIT TO ACTIVE & SENT
								updateVisitMiddlewareStatus(
									UpdateMiddlewareStatusDTO(
										aggregateId = savedVisit.aggregateId,
										benefitName = savedVisit.benefitName,
										memberNumber = savedVisit.memberNumber,
										visitNumber = savedVisit.id
									)
								)
								true

							} else {
								false
							}

						} else if (provider.data.providerMiddleware == MIDDLEWARENAME.NAIROBIHOSPITAL.toString()) {
							val providerUrl =
								gson.fromJson(providerRes, ProviderUrlResponse::class.java)
							val ipaddress: String? = providerUrl.data.ipaddress
							val port: String? = providerUrl.data.port
							webClient = WebClient.create("http://$ipaddress:$port")
							val payerCode = "INS00011"
							val remoteResponse = webClient
								.post()
								.uri { u ->
									u
										.path(startVisit)
										.build()
								}
								.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
								.bodyValue(
									GroupTwoKranium(
										dto.aggregateId!!,
										dto.benefitId,
										dto.benefitName,
										dto.memberNumber,
										dto.memberName,
										dto.balanceAmount,
										payerName,
										schemeName,
										provider.data.providerMiddleware,
										payerCode,
										firstName,
										lastName,
										savedVisit!!.id,
										deviceId = provider.data.providerMiddleware
									)
								)
								.retrieve()
								.bodyToMono(IntegratedClaimResponseDto2::class.java)
								.block()

							response = if (remoteResponse?.control?.error == false) {
								//					SET VISIT TO ACTIVE & SENT
								updateVisitMiddlewareStatus(
									UpdateMiddlewareStatusDTO(
										aggregateId = savedVisit.aggregateId,
										benefitName = savedVisit.benefitName,
										memberNumber = savedVisit.memberNumber,
										visitNumber = savedVisit.id
									)
								)
								true

							} else {
								false
							}

						} else if (provider.data.providerMiddleware == MIDDLEWARENAME.MATER.toString()) {

							val providerUrl =
								gson.fromJson(providerRes, ProviderUrlResponse::class.java)
							val ipaddress: String? = providerUrl.data.ipaddress
							val port: String? = providerUrl.data.port
							webClient = WebClient.create("http://$ipaddress:$port")

							val remoteResponse = webClient
								.post()
								.uri { u ->
									u
										.path(startVisit)
										.build()
								}
								.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
								.bodyValue(
									GroupOne(
										dto.aggregateId!!,
										dto.benefitId,
										dto.benefitName,
										dto.memberNumber,
										dto.memberName,
										dto.balanceAmount,
										payerName,
										schemeName,
										provider.data.providerMiddleware,
										firstName,
										lastName,
										savedVisit!!.id,
										"Active",
										"N/A",
										"Family",
										insuranceId,
										"P",
										schemeCode,
										"",
										lastName,
										deviceId = provider.data.providerMiddleware
									)
								)
								.retrieve()
								.bodyToMono(IntegratedClaimResponseDto2::class.java)
								.block()

							response = if (remoteResponse?.control?.error == false) {
								//					SET VISIT TO ACTIVE & SENT
								updateVisitMiddlewareStatus(
									UpdateMiddlewareStatusDTO(
										aggregateId = savedVisit.aggregateId,
										benefitName = savedVisit.benefitName,
										memberNumber = savedVisit.memberNumber,
										visitNumber = savedVisit.id
									)
								)
								true

							} else {
								false
							}

						} else if (provider.data.providerMiddleware == MIDDLEWARENAME.NONE.toString()) {

							//println(dto)
							val newVisit = Visit(
								memberNumber = dto.memberNumber.trim(),
								memberName = dto.memberName.trim(),
								hospitalProviderId = dto.hospitalProviderId,
								staffId = dto.staffId,
								staffName = dto.staffName,
								status = Status.ACTIVE,
								middlewareStatus = MiddlewareStatus.SENT,
								totalInvoiceAmount = BigDecimal.ZERO,
								aggregateId = dto.aggregateId!!,
								diagnosis = mutableListOf(),
								claimProcessStatus = ClaimProcessStatus.UNPROCESSED,
								beneficiaryType = dto.beneficiaryType,
								benefitName = dto.benefitName,
								categoryId = dto.categoryId,
								invoiceNumber = null,
								payerId = dto.payerId,
								policyNumber = dto.policyNumber,
								balanceAmount = dto.balanceAmount,
								benefitId = dto.benefitId,
								beneficiaryId = dto.beneficiaryId,
								providerMiddleware = MIDDLEWARENAME.NONE,
								reimbursementProvider = null,
								reimbursementInvoiceDate = null,
								reimbursementReason = dto.reimbursementReason,
								facilityType = dto.facilityType,
								providerMapping = null,
								benefitMapping = null,
								invoiceDate =  LocalDateTime.now(),
								createdAt = LocalDateTime.now()
							)
							visitRepo.save(newVisit)
							//						println("save visit tracker---------------")
							//						println(savedVisit)

							response = true

						} else if (provider.data.providerMiddleware == MIDDLEWARENAME.AGAKHANNAIROBITEST.toString()) {
							val providerUrl =
								gson.fromJson(providerRes, ProviderUrlResponse::class.java)
							val ipaddress: String? = providerUrl.data.ipaddress
							val port: String? = providerUrl.data.port
							webClient = WebClient.create("http://$ipaddress:$port")

							val remoteResponse = webClient
								.post()
								.uri { u ->
									u
										.path(startVisit)
										.build()
								}
								.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
								.bodyValue(
									GroupOne(
										dto.aggregateId!!,
										dto.benefitId,
										dto.benefitName,
										dto.memberNumber,
										dto.memberName,
										dto.balanceAmount,
										payerName,
										schemeName,
										provider.data.providerMiddleware,
										firstName,
										lastName,
										savedVisit!!.id,
										"Active",
										"N/A",
										"Family",
										insuranceId,
										"P",
										schemeCode,
										"",
										lastName,
										deviceId = provider.data.providerMiddleware
									)
								)
								.retrieve()
								.bodyToMono(IntegratedClaimResponseDto2::class.java)
								.block()

							response = if (remoteResponse?.control?.error == false) {
								//					SET VISIT TO ACTIVE & SENT
								updateVisitMiddlewareStatus(
									UpdateMiddlewareStatusDTO(
										aggregateId = savedVisit.aggregateId,
										benefitName = savedVisit.benefitName,
										memberNumber = savedVisit.memberNumber,
										visitNumber = savedVisit.id
									)
								)
								true

							} else {
								false
							}

						} else if (provider.data.providerMiddleware == MIDDLEWARENAME.AKUH.toString()) {
							val providerUrl =
								gson.fromJson(providerRes, ProviderUrlResponse::class.java)
							val ipaddress: String? = providerUrl.data.ipaddress
							val port: String? = providerUrl.data.port
							webClient = WebClient.create("http://$ipaddress:$port")

							val remoteResponse = webClient
								.post()
								.uri { u ->
									u
										.path(startVisit)
										.build()
								}
								.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
								.bodyValue(
									GroupOne(
										dto.aggregateId!!,
										dto.benefitId,
										dto.benefitName,
										dto.memberNumber,
										dto.memberName,
										dto.balanceAmount,
										payerName,
										schemeName,
										provider.data.providerMiddleware,
										firstName,
										lastName,
										savedVisit!!.id,
										"Active",
										"N/A",
										"Family",
										insuranceId,
										"P",
										schemeCode,
										"",
										lastName,
										deviceId = provider.data.providerMiddleware
									)
								)
								.retrieve()
								.bodyToMono(IntegratedClaimResponseDto2::class.java)
								.block()

							response = if (remoteResponse?.control?.error == false) {
								//					SET VISIT TO ACTIVE & SENT
								updateVisitMiddlewareStatus(
									UpdateMiddlewareStatusDTO(
										aggregateId = savedVisit.aggregateId,
										benefitName = savedVisit.benefitName,
										memberNumber = savedVisit.memberNumber,
										visitNumber = savedVisit.id
									)
								)
								true

							} else {
								false
							}

						} else if (provider.data.providerMiddleware == MIDDLEWARENAME.GETRUDESTEST.toString()) {
							val providerUrl =
								gson.fromJson(providerRes, ProviderUrlResponse::class.java)
							val ipaddress: String? = providerUrl.data.ipaddress
							val port: String? = providerUrl.data.port
							webClient = WebClient.create("http://$ipaddress:$port")

							val remoteResponse = webClient
								.post()
								.uri { u ->
									u
										.path(startVisit)
										.build()
								}
								.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
								.bodyValue(
									GroupOne(
										dto.aggregateId!!,
										dto.benefitId,
										dto.benefitName,
										dto.memberNumber,
										dto.memberName,
										dto.balanceAmount,
										payerName,
										schemeName,
										provider.data.providerMiddleware,
										firstName,
										lastName,
										savedVisit!!.id,
										"Active",
										"N/A",
										"Family",
										insuranceId,
										"P",
										schemeCode,
										"",
										lastName,
										deviceId = provider.data.providerMiddleware
									)
								)
								.retrieve()
								.bodyToMono(IntegratedClaimResponseDto2::class.java)
								.block()

							response = if (remoteResponse?.control?.error == false) {
								//					SET VISIT TO ACTIVE & SENT
								updateVisitMiddlewareStatus(
									UpdateMiddlewareStatusDTO(
										aggregateId = savedVisit.aggregateId,
										benefitName = savedVisit.benefitName,
										memberNumber = savedVisit.memberNumber,
										visitNumber = savedVisit.id
									)
								)
								true

							} else {
								false
							}

						}
						return if (response) {
							ResultFactory.getSuccessResult(savedVisit)

						} else {
							ResultFactory.getFailResult(
								msg = "Visit could not start , please " +
										"try again"
							)

						}

					} else {

						println(dto)
						val newVisit = Visit(
							memberNumber = dto.memberNumber.trim(),
							memberName = dto.memberName.trim(),
							hospitalProviderId = dto.hospitalProviderId,
							staffId = dto.staffId,
							staffName = dto.staffName,
							status = Status.ACTIVE,
							middlewareStatus = MiddlewareStatus.SENT,
							totalInvoiceAmount = BigDecimal.ZERO,
							aggregateId = dto.aggregateId!!,
							diagnosis = mutableListOf(),
							claimProcessStatus = ClaimProcessStatus.UNPROCESSED,
							beneficiaryType = dto.beneficiaryType,
							benefitName = dto.benefitName,
							categoryId = dto.categoryId,
							invoiceNumber = null,
							payerId = dto.payerId,
							policyNumber = dto.policyNumber,
							balanceAmount = dto.balanceAmount,
							benefitId = dto.benefitId,
							beneficiaryId = dto.beneficiaryId,
							providerMiddleware = MIDDLEWARENAME.NONE,
							visitType = dto.visitType,
							offSystemReason = dto.offSystemReason,
							reimbursementProvider = null,
							reimbursementInvoiceDate = null,
							reimbursementReason = dto.reimbursementReason,
							facilityType = dto.facilityType,
							providerMapping = null,
							benefitMapping = null,
							invoiceDate =  LocalDateTime.now(),
							createdAt = LocalDateTime.now()
						)
						val savedVisit = visitRepo.save(newVisit)
						//					println("save visit tracker---------------")
						//					println(savedVisit)

						return ResultFactory.getSuccessResult(savedVisit)

					}
				} catch (ex: IllegalArgumentException) {
					return ResultFactory.getFailResult(msg = ex.message)
				}
			}
		}


		}

	@Transactional(readOnly = true, rollbackFor = [Exception::class])
	override fun getActiveVisits(
		providerId: Long,
		staffId: String
	): Result<MutableList<VisitDTO?>> {
		val result =
			visitRepo.findAllByHospitalProviderIdAndStatusOrderByCreatedAtDesc(
				providerId,
				Status.ACTIVE
			);

		val visitList = mutableListOf<VisitDTO?>()

		val claimsClient = WebClient.builder()
			.baseUrl(claimsUrl).build()

		val membershipClient = WebClient.builder()
			.baseUrl(memberSearchUrl).build()

		result.forEach {
			val preAuthRes = claimsClient
				.get()
				.uri { u ->
					u
						.path("/api/v1/preauthorization/${it?.id}/preauth")
						.build()
				}
				.retrieve()
				.bodyToMono(PreauthResponseDto::class.java)
				.block()

			val visitInvoices = claimsClient
				.get()
				.uri { u ->
					u
						.path("/api/v1/visit/${it?.id}/invoices")
						.build()
				}
				.retrieve()
				.bodyToMono(InvoiceResponseDto::class.java)
				.block()


			val planRes = membershipClient
				.get()
				.uri { u ->
					u
						.path("/api/v1/membership/category/${it?.categoryId}/plan")
						.build()
				}
				.retrieve()
				.bodyToMono(PlanResponseDto::class.java)
				.block()

			println(it?.payerId)

			val payerRes = membershipClient
				.get()
				.uri { u ->
					u
						.path("/api/v1/membership/payers/${it?.payerId}/payer")
						.build()
				}
				.retrieve()
				.bodyToMono(PayerResponseDto::class.java)
				.block()

			///println(provider)

			val visitdto = VisitDTO(
				id = it!!.id,
				memberNumber = it.memberNumber,
				memberName = it.memberName,
				hospitalProviderId = it.hospitalProviderId!!,
				status = it.status,
				claimProcessStatus = it.claimProcessStatus,
				payerId = it.payerId,
				balanceAmount = it.balanceAmount,
				totalInvoiceAmount = it.totalInvoiceAmount,
				invoiceNumber = it.invoiceNumber,
				aggregateId = it.aggregateId,
				benefitName = it.benefitName,
				benefitId = it.benefitId,
				offSystemReason = it.offSystemReason,
				visitType = it.visitType,
				reimbursementReason = it.reimbursementReason,
				reimbursementInvoiceDate = it.reimbursementInvoiceDate,
				reimbursementProvider = it.reimbursementProvider,
				preAuth = preAuthRes!!.data,
				scheme = planRes!!.data,
				payerName = payerRes!!.data!!.name,
				invoices = visitInvoices!!.data,
				createdAt = it.createdAt?.toString()


			)
			visitList.add(visitdto)
		}

		return ResultFactory.getSuccessResult(visitList)
	}

	override fun getMainAndBranchActiveVisits(
		mainFacilityId: Long,
		status: String
	): Result<MutableList<VisitDTO?>> {
		val result =
			visitRepo.findMainAndBranchActiveVisits(
				mainFacilityId,
				Status.ACTIVE
			);

		val visitList = mutableListOf<VisitDTO?>()

		val claimsClient = WebClient.builder()
			.baseUrl(claimsUrl).build()

		val membershipClient = WebClient.builder()
			.baseUrl(memberSearchUrl).build()
		result.forEach {
			val preAuthRes = claimsClient
				.get()
				.uri { u ->
					u
						.path("/api/v1/preauthorization/${it?.id}/preauth")
						.build()
				}
				.retrieve()
				.bodyToMono(PreauthResponseDto::class.java)
				.block()

			val visitInvoices = claimsClient
				.get()
				.uri { u ->
					u
						.path("/api/v1/visit/${it?.id}/invoices")
						.build()
				}
				.retrieve()
				.bodyToMono(InvoiceResponseDto::class.java)
				.block()


			val planRes = membershipClient
				.get()
				.uri { u ->
					u
						.path("/api/v1/membership/category/${it?.categoryId}/plan")
						.build()
				}
				.retrieve()
				.bodyToMono(PlanResponseDto::class.java)
				.block()

			val payerRes = membershipClient
				.get()
				.uri { u ->
					u
						.path("/api/v1/membership/payers/${it?.payerId}/payer")
						.build()
				}
				.retrieve()
				.bodyToMono(PayerResponseDto::class.java)
				.block()

			///println(provider)

			val visitdto = VisitDTO(
				id = it!!.id,
				memberNumber = it.memberNumber,
				memberName = it.memberName,
				hospitalProviderId = it.hospitalProviderId!!,
				status = it.status,
				claimProcessStatus = it.claimProcessStatus,
				payerId = it.payerId,
				balanceAmount = it.balanceAmount,
				totalInvoiceAmount = it.totalInvoiceAmount,
				invoiceNumber = it.invoiceNumber,
				aggregateId = it.aggregateId,
				benefitName = it.benefitName,
				benefitId = it.benefitId,
				offSystemReason = it.offSystemReason,
				visitType = it.visitType,
				reimbursementReason = it.reimbursementReason,
				reimbursementInvoiceDate = it.reimbursementInvoiceDate,
				reimbursementProvider = it.reimbursementProvider,
				preAuth = preAuthRes!!.data,
				scheme = planRes!!.data,
				payerName = payerRes!!.data!!.name,
				invoices = visitInvoices!!.data,
				createdAt = it.createdAt?.toString()
			)
			visitList.add(visitdto)
		}

		return ResultFactory.getSuccessResult(visitList)
	}


	@Transactional(readOnly = true, rollbackFor = [Exception::class])
	override fun getActiveOffLctVisits(
		payerId: String,
		staffId: Long,
	): Result<MutableList<OffLctVisitDTO?>> {
		val result =
			visitRepo.findAllByPayerIdAndStatusAndVisitTypeOrderByIdDesc(
				payerId,
				Status.ACTIVE,
				VisitType.OFF_LCT
			);

		val claims = mutableListOf<OffLctVisitDTO?>()

		val membershipClient = WebClient.builder()
			.baseUrl(memberSearchUrl).build()

		result.forEach {
			val provider = membershipClient
				.get()
				.uri { u ->
					u
						.path("/api/v1/provider")
						.queryParam("providerId", it!!.hospitalProviderId)
						.build()
				}
				.retrieve()
				.bodyToMono(ProviderResponseDto::class.java)
				.block()

			val offlct = OffLctVisitDTO(
				id = it?.id!!,
				memberNumber = it.memberNumber,
				memberName = it.memberName,
				hospitalProviderId = it.hospitalProviderId!!,
				status = it.status,
				claimProcessStatus = it.claimProcessStatus,
				payerId = it.payerId,
				totalInvoiceAmount = it.totalInvoiceAmount,
				invoiceNumber = it.invoiceNumber,
				benefitName = it.benefitName,
				providerName = provider?.data?.name,
				staffName = it.staffName,
				createdAt = it.createdAt?.toLocalDate()
			)
			claims.add(offlct)
		}

		return ResultFactory.getSuccessResult(claims)
	}

	@Transactional(readOnly = true, rollbackFor = [Exception::class])
	override fun getActiveReimbursementVisits(
		payerId: String,
		staffId: Long,
	): Result<MutableList<ReimbursementDTO?>> {
		val result =
			visitRepo.findAllByPayerIdAndStatusAndVisitTypeOrderByCreatedAtDesc(
				payerId,
				Status.ACTIVE,
				VisitType.REIMBURSEMENT
			);

		val claims = mutableListOf<ReimbursementDTO?>()

		result.forEach {

			val offlct = ReimbursementDTO(
				id = it?.id!!,
				memberNumber = it.memberNumber,
				memberName = it.memberName,
				hospitalProviderId = it.hospitalProviderId!!,
				status = it.status,
				claimProcessStatus = it.claimProcessStatus,
				payerId = it.payerId,
				totalInvoiceAmount = it.totalInvoiceAmount,
				invoiceNumber = it.invoiceNumber,
				benefitName = it.benefitName,
				providerName = it.reimbursementProvider,
			)
			claims.add(offlct)
		}
		return ResultFactory.getSuccessResult(claims)
	}

	@Transactional(readOnly = true, rollbackFor = [Exception::class])
	override fun getPagedActiveVisits(
		providerId: Long,
		staffId: String,
		page: Int,
		size: Int
	): Result<Page<Visit?>> {
		val request = PageRequest.of(page - 1, size)

		val result =

			visitRepo.findAllByHospitalProviderIdAndStatusOrderByCreatedAtDesc(

				providerId,

				Status.ACTIVE,

				request

			)

		return ResultFactory.getSuccessResult(result)
	}

	@Transactional(readOnly = true, rollbackFor = [Exception::class])
	override fun getClosedVisits(providerId: Long, staffId: String): Result<MutableList<Visit?>> {
		val result =
			visitRepo.findClosedVisitsList(
				providerId, Status.ACTIVE
			);
		return ResultFactory.getSuccessResult(result)
	}

	@Transactional(readOnly = true, rollbackFor = [Exception::class])
	override fun getPagedClosedVisits(
		providerId: Long,
		staffId: String,
		page: Int,
		size: Int,
		invoiceNo: String?,
		memberNo: String?,
	): Result<Page<Visit?>?> {
		val request = PageRequest.of(page - 1, size)
		var result: Page<Visit?>?  = null

		if(invoiceNo.isNullOrBlank() && memberNo.isNullOrBlank()){
			result =
				visitRepo.findPagedClosedVisits(
					providerId,
					request
				)
		}else if(invoiceNo.isNullOrBlank() && !memberNo.isNullOrBlank()){
			result =
				visitRepo.findPagedClosedVisitsWithMemberNo(
					providerId,
					memberNo,
					request
				)
		}
		else if(!invoiceNo.isNullOrBlank() && memberNo == null){
			result =
				visitRepo.findPagedClosedVisitsWithInvoiceNo(
					providerId,
					invoiceNo,
					request
				)
		}
		else if(!invoiceNo.isNullOrBlank() && !memberNo.isNullOrBlank()){
			result =
				visitRepo.findPagedClosedVisitsWithInvoiceNoAndMemberNo(
					providerId,
					invoiceNo,
					memberNo,
					request
				)
		}

		return ResultFactory.getSuccessResult(data = result)
	}

	override fun getMainAndBranchPagedClosedVisits(
		mainFacilityId: Long,
		staffId: String,
		page: Int,
		size: Int
	): Result<Page<Visit?>> {
		val request = PageRequest.of(page - 1, size)

		val result = visitRepo.findMainAndBranchClosedVisits(
				mainFacilityId,
				Status.CLOSED,
			request
			)



		return ResultFactory.getSuccessResult(result)
	}

	fun offLctVisit(entity: Visit): OffLctVisitDTO? {
		val offLctVisitDTO = OffLctVisitDTO()
		offLctVisitDTO.id = entity.id
		offLctVisitDTO.benefitName = entity.benefitName
		offLctVisitDTO.memberNumber = entity.memberNumber
		offLctVisitDTO.memberName = entity.memberName
		offLctVisitDTO.hospitalProviderId = entity.hospitalProviderId
		offLctVisitDTO.status = entity.status
		offLctVisitDTO.payerId = entity.payerId
		offLctVisitDTO.totalInvoiceAmount = entity.totalInvoiceAmount
		offLctVisitDTO.invoiceNumber = entity.invoiceNumber
		offLctVisitDTO.staffName = entity.staffName
		offLctVisitDTO.createdAt = entity.createdAt?.toLocalDate()
		val membershipClient = WebClient.builder()
			.baseUrl(memberSearchUrl).build()


		val provider = membershipClient
			.get()
			.uri { u ->
				u
					.path("/api/v1/provider")
					.queryParam("providerId", entity.hospitalProviderId)
					.build()
			}
			.retrieve()
			.bodyToMono(ProviderResponseDto::class.java)
			.block()

		offLctVisitDTO.providerName = provider?.data?.name
		return offLctVisitDTO
	}


	fun reimbursementLctVisit(entity: Visit): ReimbursementDTO? {
		val reimbursementDTO = ReimbursementDTO()
		reimbursementDTO.id = entity.id
		reimbursementDTO.benefitName = entity.benefitName
		reimbursementDTO.memberNumber = entity.memberNumber
		reimbursementDTO.memberName = entity.memberName
		reimbursementDTO.hospitalProviderId = entity.hospitalProviderId
		reimbursementDTO.status = entity.status
		reimbursementDTO.payerId = entity.payerId
		reimbursementDTO.totalInvoiceAmount = entity.totalInvoiceAmount
		reimbursementDTO.invoiceNumber = entity.invoiceNumber
		reimbursementDTO.providerName = entity.reimbursementProvider
		return reimbursementDTO
	}

	@Transactional(readOnly = true, rollbackFor = [Exception::class])
	override fun getPagedClosedOffLctVisits(
		payerId: String,
		staffId: Long,
		page: Int,
		size: Int
	): Result<Page<OffLctVisitDTO?>> {
		val request = PageRequest.of(page - 1, size)
		val result =
			visitRepo.findAllByPayerIdAndStatusAndVisitTypeOrderByIdDesc(
				payerId,
				Status.CLOSED,
				VisitType.OFF_LCT,
				request

			);

		val offLctPage: Page<OffLctVisitDTO?> = result.map { entity ->
			val dto: OffLctVisitDTO? = offLctVisit(entity!!)
			dto
		}



		return ResultFactory.getSuccessResult(offLctPage)
	}

	@Transactional(readOnly = true, rollbackFor = [Exception::class])
	override fun getPagedClosedReimbursementVisits(
		payerId: String,
		staffId: Long,
		page: Int,
		size: Int
	): Result<Page<ReimbursementDTO?>> {
		val request = PageRequest.of(page - 1, size)
		val result =
			visitRepo.findAllByPayerIdAndStatusAndVisitTypeOrderByIdDesc(
				payerId,
				Status.CLOSED,
				VisitType.REIMBURSEMENT,
				request

			);

		val reimbursementPage: Page<ReimbursementDTO?> = result.map { entity ->
			val dto: ReimbursementDTO? = reimbursementLctVisit(entity!!)
			dto
		}
		return ResultFactory.getSuccessResult(reimbursementPage)
	}

	@Transactional(readOnly = true, rollbackFor = [Exception::class])
	override fun getVisitByMemberNumber(memberNumber: String): Result<List<ClaimsDTO?>> {
		val result = visitRepo.findMemberClaimsByMemberNumber(memberNumber)

		val claims = mutableListOf<ClaimsDTO>()

		val membershipClient = WebClient.builder()
			.baseUrl(memberSearchUrl).build()

		result.forEach {

			val claimdto = ClaimsDTO(
				id = it!!.VisitNumber,
				memberNumber = it.memberNumber,
				memberName = it.memberName,
				hospitalProviderId = it.hospitalProviderId!!,
				status = Status.valueOf(it.status),
				claimProcessStatus = ClaimProcessStatus.valueOf(it.claimProcessStatus),
				payerId = it.payerId.toString(),
				balanceAmount = it.balanceAmount,
				totalInvoiceAmount = it.totalInvoiceAmount,
				invoiceNumber = it.invoiceNumber,
				aggregateId = it.aggregateId,
				benefitName = it.benefitName,
				providerName = it.providerName,
				createdAt = it.createdAt,
				updatedAt = it.updatedAt
			)
			claims.add(claimdto)
		}
		return ResultFactory.getSuccessResult(claims)
	}


	@Transactional(readOnly = true, rollbackFor = [Exception::class])
	override fun getVisitByFamilyNumber(familyNumber: String): Result<List<ClaimsDTO?>> {
		val result = visitRepo.findFamilyClaimsByFamilyNumber(familyNumber)
		val claims = mutableListOf<ClaimsDTO>()

		val membershipClient = WebClient.builder()
			.baseUrl(memberSearchUrl).build()

		result.forEach {


			val claimdto = ClaimsDTO(
				id = it!!.VisitNumber,
				memberNumber = it.memberNumber,
				memberName = it.memberName,
				hospitalProviderId = it.hospitalProviderId!!,
				status = Status.valueOf(it.status),
				claimProcessStatus = ClaimProcessStatus.valueOf(it.claimProcessStatus),
				payerId = it.payerId.toString(),
				balanceAmount = it.balanceAmount,
				totalInvoiceAmount = it.totalInvoiceAmount,
				invoiceNumber = it.invoiceNumber,
				aggregateId = it.aggregateId,
				benefitName = it.benefitName,
				providerName = it.providerName,
				createdAt = it.createdAt,
				updatedAt = it.updatedAt
			)
			claims.add(claimdto)
		}
		return ResultFactory.getSuccessResult(claims)
	}

	@Transactional(readOnly = true, rollbackFor = [Exception::class])
	override fun getMemberStatementByMemberNumber(memberNumber: String): MutableList<MembersStatementsDTO> {
		return visitRepo.findMemberStatementByMemberNumber(removeLastChar(memberNumber)!!);
	}

	fun removeLastChar(s: String?): String? {
		return if (s == null || s.isEmpty()) null else s.substring(0, s.length - 1)
	}

	override fun getClaimsByProvider(
		hospitalProviderId: Long,
		planId: String,
		fromDate: String,
		toDate: String
	): ResponseEntity<ByteArray?>? {
		val providerClaims =
				visitRepo.findProviderClaimsStatement(planId, hospitalProviderId, fromDate, toDate)

		val dataSource = JRBeanCollectionDataSource(providerClaims)
		println(providerClaims)

		val input = this.javaClass.getResourceAsStream("/templates/providerStatement.jrxml");
		val jasperReport: JasperReport = JasperCompileManager.compileReport(input)
		val parameters: MutableMap<String, Any> = HashMap()
		parameters["planId"] = planId
		parameters["hospitalProviderId"] = hospitalProviderId
		parameters["fromDate"] = fromDate
		parameters["toDate"] = toDate
		val jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource)
		val data = JasperExportManager.exportReportToPdf(jasperPrint)
		println("------------------Provider claims")

		val headers = HttpHeaders()
		headers.add("Content-Disposition", "inline; filename=providerStatement.pdf")
		return ResponseEntity.ok().headers(headers).contentType(MediaType.APPLICATION_PDF)
			.body<ByteArray>(data)
	}

	override fun getProvidersByPayer(payerId: Long): ResponseEntity<ByteArray?>? {
		val payerProviders =
			visitRepo.findPayerProviders(payerId)

		val dataSource = JRBeanCollectionDataSource(payerProviders)
		///println(providerClaims)

		val input = this.javaClass.getResourceAsStream("/templates/payerproviders.jrxml");
		val jasperReport: JasperReport = JasperCompileManager.compileReport(input)
		val parameters: MutableMap<String, Any> = HashMap()

		val jasperPrint = JasperFillManager.fillReport(jasperReport, null, dataSource)
		val data = JasperExportManager.exportReportToPdf(jasperPrint)
		println("------------------PAYER PROVIDERS")

		val headers = HttpHeaders()
		headers.add("Content-Disposition", "inline; filename=payerproviders.pdf")
		return ResponseEntity.ok().headers(headers).contentType(MediaType.APPLICATION_PDF)
			.body<ByteArray>(data)
	}


	override fun getStatementPlanClaims(
		planId: String,
		fromDate: String,
		toDate: String
	): ResponseEntity<ByteArray?>? {
		val conn = jdbcTemplate.dataSource!!.connection
		val input = this.javaClass.getResourceAsStream("/templates/schemeStatement.jrxml");
		val jasperReport: JasperReport = JasperCompileManager.compileReport(input)
		val parameters: MutableMap<String, Any> = HashMap()
		parameters["planId"] = planId
		parameters["fromDate"] = fromDate
		parameters["toDate"] = toDate

		val jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, conn)
		val data = JasperExportManager.exportReportToPdf(jasperPrint)

		val headers = HttpHeaders()
		headers.add("Content-Disposition", "inline; filename=schemeStatement.pdf")
		return ResponseEntity.ok().headers(headers).contentType(MediaType.APPLICATION_PDF)
			.body<ByteArray>(data)
	}

	override fun getClaimsByProvider1(hospitalProviderId: Long): List<Visit> {
		val result = visitRepo.findByHospitalProviderId(hospitalProviderId)
		return result
	}


	override fun getInvoiceByVisitNumber(visit: Long): Result<Invoice> {
		val visit: Visit = visitRepo.findById(visit).get()
		val result = invoiceRepo.findByVisit(visit);
		return ResultFactory.getSuccessResult(result.get())
	}

	@Transactional(readOnly = true, rollbackFor = [Exception::class])
	override fun getInvoicesByVisitNumber(visitNumber: Long): Result<MutableList<Invoice>?> {
		val visit = visitRepo.findById(visitNumber)
		return if (visit.isPresent) {

			ResultFactory.getSuccessResult(data = invoiceRepo.fetchByVisit(visit.get()))

		} else {
			ResultFactory.getFailResult("No visit with id $visitNumber was found")
		}
	}

	override fun findByBeneficiaryId(beneficiaryId: Long): Result<List<Visit?>> {
		val benefits =
			visitRepo.findByBeneficiaryId(beneficiaryId)
		return ResultFactory.getSuccessResult(benefits)
	}

	override fun getBeneficiaryBenefitByPayerId(payerId: Long, memberNumber: String):
			Result<List<BeneficiaryBenefit?>> {
		val beneficiaryBenefit =
			beneficiaryBenefitRepository.findByPayerIdAndMemberNumber(payerId, memberNumber)
		return ResultFactory.getSuccessResult(beneficiaryBenefit)
	}


	override fun getClaimFromIntegratedFacility(
		dto: GetIntegratedClaimDto
	): Result<IntegratedClaimData?> {
		val response = getProviderRemote(dto.providerId)
		var baseUrl: String? = ""
		if (!response?.data?.baseUrl.isNullOrBlank()) {
			baseUrl = response?.data?.baseUrl ?: ""
		} else if (!response?.data?.mainFacility?.baseUrl.isNullOrBlank()) {
			baseUrl = response?.data?.mainFacility?.baseUrl ?: ""
		}
		if (baseUrl.isNullOrBlank()) {
			return ResultFactory.getFailResult("Provider route has not been configured")
		} else {
			val integratedClaimRequestDto = IntegratedClaimRequestDto(visitNumber = dto.visitNumber)
			val remoteClient = WebClient.builder()
				.baseUrl(baseUrl).build()
			var remoteResponse = remoteClient.post()
				.uri { u -> u.path("/lct/get_kranium_transaction").build() }
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.body(Mono.just(gson.toJson(integratedClaimRequestDto)), String::class.java)
				.retrieve()
				.bodyToMono(IntegratedClaimResponseDto::class.java)
				.block()
			if (remoteResponse?.control?.error == false) {
				return ResultFactory.getSuccessResult(remoteResponse.data)
			}
			return ResultFactory.getFailResult(remoteResponse?.control?.message)
		}
	}

	override fun closeClaimFromIntegratedFacility(dto: IntegratedClaimCloseRequestDto): Result<IntegratedClaimResponseDto?> {
		val visitOptional = visitRepo.findById(dto.visitId)
		if (!visitOptional.isPresent) {
			return ResultFactory.getFailResult(
				"" +
						"Invalid Visit Id."
			)
		}
		val visit = visitOptional.get()
		val response = getProviderRemote(dto.providerId)
		var baseUrl: String? = ""
		if (!response?.data?.baseUrl.isNullOrBlank()) {
			baseUrl = response?.data?.baseUrl ?: ""
		} else if (!response?.data?.mainFacility?.baseUrl.isNullOrBlank()) {
			baseUrl = response?.data?.mainFacility?.baseUrl ?: ""
		}
		if (baseUrl.isNullOrBlank()) {
			return ResultFactory.getFailResult("Provider route has not been configured")
		} else {
			val integratedClaimCloseRequestDto = IntegratedClaimRemoteCloseRequestDto(
				claimRef = dto.claimRef,
				memberNumber = visit.memberNumber,
				visitId = dto.visitId
			)
			val remoteClient = WebClient.builder()
				.baseUrl(baseUrl).build()
			val remoteResponse = remoteClient.post()
				.uri { u -> u.path("/lct/close_kranium_transaction").build() }
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.body(Mono.just(gson.toJson(integratedClaimCloseRequestDto)), String::class.java)
				.retrieve()
				.bodyToMono(IntegratedClaimResponseDto::class.java)
				.block()
			if (remoteResponse?.control?.error == false) {
				return ResultFactory.getSuccessResult(remoteResponse)
			}
			return ResultFactory.getFailResult(msg = remoteResponse!!.control!!.message)
		}
	}


	override fun getVisitTransactions(dto: BenefitsDto): Result<List<VisitTransactionDto?>> {
		val response = getCoverBeneficiaries(dto.beneficiaryId)
		val visitTransactions = mutableListOf<VisitTransactionDto>()
		if (response?.success == true) {
			val benefits = beneficiaryBenefitRepository.findActiveByBeneficiaryId(
				dto.beneficiaryId,
				BeneficiaryBenefit.BenefitStatus.ACTIVE
			)
			val childBenefits = benefits.filter { it.parent?.id == dto.mainBenefitId }
			val benefitIds = mutableListOf<Long>()
			benefitIds.add(dto.mainBenefitId)
			childBenefits.forEach {
				benefitIds.add(it.id)
			}
			val beneficiaries = response.data

			val memberNumbers = mutableListOf<String>()
			beneficiaries?.forEach { beneficiary ->
				memberNumbers.add(beneficiary.memberNumber)
			}

			val invoices = invoiceRepo.findBeneficiaryInvoices(
				benefitIds,
				memberNumbers,
				ClaimProcessStatus.PROCESSED,
				Status.CLOSED,
				Status.LINE_ITEMS_ADDED
			)
			invoices.forEach { invoice ->
				val provider = invoice.hospitalProviderId?.let { getProviderRemote(it) }
				val transaction = VisitTransactionDto(
					visitId = invoice.visit.id,
					visitInvoiceNumber = invoice.visit.invoiceNumber,
					hospitalProviderId = invoice.hospitalProviderId,
					beneficiaryName = invoice.visit.memberName,
					benefitName = invoice.visit.benefitName,
					memberNumber = invoice.visit.memberNumber,
					providerName = provider?.data?.name,
					totalAmount = invoice.totalAmount ?: BigDecimal(0),
					invoiceNumber = invoice.invoiceNumber,
					invoiceLines = invoice.invoiceLines,
					txnDate = invoice.visit.createdAt
				)
				visitTransactions.add(transaction)
			}
		}
		return ResultFactory.getSuccessResult(visitTransactions)
	}

	override fun getVisitTransactions(beneficiaryId: Long, mainBenefitId: Long, page:Int,
	                                  size:Int): Result<List<VisitTxnDto?>> {
		val response = getCoverBeneficiaries(beneficiaryId)
		val visitTransactions = mutableListOf<VisitTxnDto>()
		if (response?.success == true) {
			val benefits = beneficiaryBenefitRepository.findActiveByBeneficiaryId(
				beneficiaryId,
				BeneficiaryBenefit.BenefitStatus.ACTIVE
			)
			val childBenefits = benefits.filter { it.parent?.id == mainBenefitId }
			val benefitIds = mutableListOf<Long>()
			benefitIds.add(mainBenefitId)
			childBenefits.forEach {
				benefitIds.add(it.id)
			}
			val beneficiaries = response.data

			val memberNumbers = mutableListOf<String>()
			beneficiaries?.forEach { beneficiary ->
				memberNumbers.add(beneficiary.memberNumber)
			}

			val policyStartDate = beneficiaries?.get(0)?.category?.policy?.startDate
			val policyEndDate = beneficiaries?.get(0)?.category?.policy?.endDate
			val date1 = LocalDate.parse(policyStartDate, DateTimeFormatter.ISO_DATE)
			val date2 = LocalDate.parse(policyEndDate, DateTimeFormatter.ISO_DATE)
			val dateStart: LocalDateTime = date1.atTime(LocalTime.MIN)
			val dateEnd: LocalDateTime = date2.atTime(LocalTime.MIN)

			val request = PageRequest.of(page - 1, size)
			val visits = visitRepo.findVisitTransactions(
				benefitIds,
				memberNumbers,
				ClaimProcessStatus.PROCESSED,
				Status.CLOSED,
				Status.LINE_ITEMS_ADDED,
				dateStart,
				dateEnd,
				request
			)

			visits.get().forEach { visit ->
				val provider = visit.hospitalProviderId?.let { getProviderRemote(it) }
				val invoices = mutableListOf<VisitInvoiceDto>()
				visit.invoices.forEach { invoice ->
					invoices.add(invoice.toInvoiceDto())
				}
				val transaction = VisitTxnDto(
					visitId = visit.id,
					hospitalProviderId = visit.hospitalProviderId,
					beneficiaryName = visit.memberName,
					benefitName = visit.benefitName,
					memberNumber = visit.memberNumber,
					providerName = provider?.data?.name,
					totalAmount = visit.totalInvoiceAmount ?: BigDecimal(0),
					invoiceNumber = visit.invoiceNumber,
					invoices = invoices.toSet(),
					txnDate = visit.createdAt
				)
				visitTransactions.add(transaction)
			}
		}
		return ResultFactory.getSuccessResult(visitTransactions)
	}


	@Cacheable(value = ["REMOTE"], unless = "#result == null", key = "#providerId")
	private fun getProviderRemote(providerId: Long?): ProviderResponseDto? {
		if(providerId == null){return null}
		///println("Provider Get: $providerId")
		val membershipClient = WebClient.builder()
			.baseUrl(memberSearchUrl).build()
		return membershipClient
			.get()
			.uri { u ->
				u
					.path("/api/v1/provider")
					.queryParam("providerId", providerId)
					.build()
			}
			.retrieve()
			.bodyToMono(ProviderResponseDto::class.java)
			.block()
	}

	private fun getCoverBeneficiaries(beneficiaryId: Long?): CoverBeneficiariesResponse? {
		val membershipClient = WebClient.builder()
			.baseUrl(memberSearchUrl).build()
		return membershipClient
			.get()
			.uri { u ->
				u.path("/api/v1/membership/beneficiaries/${beneficiaryId}").build()
			}
			.retrieve()
			.bodyToMono(CoverBeneficiariesResponse::class.java)
			.block()
	}

	@Transactional(readOnly = false, rollbackFor = [Exception::class])
	override fun saveInvoice(invoiceDTO: InvoiceDTO): Result<Invoice> {
		val visit = visitRepo.findById(invoiceDTO.visitNumber)
		val invoice = invoiceRepo.findByInvoiceNumber(invoiceDTO.invoiceNumber)
		if (invoice.isPresent) {
			return ResultFactory.getFailResult(
				msg = "Invoice Number already exists, add a new " +
						"invoice Number"
			)
		}
		return if (visit.isPresent) {
			var newBill = Invoice(
				hospitalProviderId = invoiceDTO.hospitalProviderId,
				invoiceNumber = invoiceDTO.invoiceNumber,
				visit = visit.get(),
				invoiceLines = mutableSetOf(),
				totalAmount = invoiceDTO.totalInvoiceAmount,
				service = null

			)
			val saved = invoiceRepo.save(newBill)
			ResultFactory.getSuccessResult(saved)
		} else {
			ResultFactory.getFailResult("No Visit with ID ${invoiceDTO.visitNumber} was found")
		}
	}


	@Transactional(readOnly = false, rollbackFor = [Exception::class])
	override fun saveLineItem(lineItemsArray: LineItemsArray): Result<Boolean> {

		val visit = visitRepo.findAllByHospitalProviderIdAndInvoiceNumber(
			lineItemsArray.providerId!!,
			lineItemsArray.invoiceNumber
		)
		val getVisit = visit.get()

		val invoice = invoiceRepo.findByInvoiceNumberAndHospitalProviderId(
			lineItemsArray.invoiceNumber,
			lineItemsArray.providerId
		)
		if (invoice.isPresent) {

			for (it in lineItemsArray.lineItems!!) {
				if(it.lineType.isNullOrBlank()){
					continue
				}
				if (LINE_TYPE.valueOf(it.lineType) == LINE_TYPE.MEDICALPROCEDURE) {
					val invProcedureCode = InvoiceNumberProcedureCode(
						invoiceNumber = it.invoiceNumber,
						procedure_description = it.itemDescription,
						procedure_code = it.itemCode
					)
					invoiceProcedureCodeRepo.save(invProcedureCode)
				} else if (LINE_TYPE.valueOf(it.lineType) == LINE_TYPE.DIAGNOSIS) {

					val diagnosisItem = Diagnosis(
						code = it.itemCode!!,
						title = it.itemDescription,
						visit = getVisit,
						invoiceNumber = it.invoiceNumber
					)
					diagnosisRepository.save(diagnosisItem)
				} else {
					val newInvoiceLine = InvoiceLine(
						description = it.itemDescription,
						invoiceNumber = it.invoiceNumber,
						lineTotal = it.itemAmount,
						lineType = LINE_TYPE.valueOf(it.lineType),
						invoice = invoice.get(),
						quantity = it.itemQuantity!!.toBigDecimal(),
						unitPrice = it.itemUnitPrice!!.toBigDecimal(),
						lineCategory = null
					)
					invoiceLineRepo.save(newInvoiceLine)

				}
			}

			for (it in lineItemsArray.documents!!) {
				val document = Document(
					providerName = it.providerName,
					providerId = it.providerId,
					type = DocumentType.valueOf(it.type.toString()),
					fileUrl = it.fileUrl,
					invoiceNumber = it.invoiceNumber
				)
				documentsRepo.save(document)
			}

			getVisit.apply {
				this.status = Status.LINE_ITEMS_ADDED
			}
			visitRepo.save(getVisit)


			Thread { producerTemplate.sendBody("direct:sendToPayer", mutableListOf(getVisit)) }.start()

			return ResultFactory.getSuccessResult(data = true)

		} else {

			return ResultFactory.getFailResult(
				"Cannot add invoice line item - No invoice found for visit"
			)
		}

	}

	@Transactional(readOnly = false, rollbackFor = [Exception::class])
	override fun saveMultipleStationsLineItem(line: MultipleStationLineItemDTO):
			Result<InvoiceLine> {
		val visit = visitRepo.findById(line.visitId)
		if (visit.isPresent) {
			val invoice = invoiceRepo.findByVisit(visit.get())
			var savedInvoice = if (invoice.isPresent) {
				invoice.get()
			} else {
				val invoice = Invoice(
					hospitalProviderId = line.hospitalProviderId,
					invoiceNumber = line.invoiceNumber,
					visit = visit.get(),
					invoiceLines = mutableSetOf(),
					totalAmount = null,
					service = null
				)
				invoiceRepo.save(invoice)
			}
			if (LINE_TYPE.valueOf(line.lineType) == LINE_TYPE.CONSULTATION) {

				val invoiceFound = invoiceRepo.findByInvoiceNumber(line.invoiceNumber)
				if (invoiceFound.isPresent) {

					return ResultFactory.getFailResult(
						msg = "Invoice Number already exists, add a new " +
								"invoice Number"
					)
				} else {
					val invoice = Invoice(
						hospitalProviderId = line.hospitalProviderId,
						invoiceNumber = line.invoiceNumber,
						visit = visit.get(),
						invoiceLines = mutableSetOf(),
						totalAmount = null,
						service = null
					)
					val savedInvoice = invoiceRepo.save(invoice)
					val invoiceLine = InvoiceLine(
						description = line.itemDescription,
						invoiceNumber = line.invoiceNumber,
						lineTotal = line.itemAmount,
						lineType = LINE_TYPE.valueOf(line.lineType),
						invoice = savedInvoice,
						quantity = BigDecimal.ZERO,
						unitPrice = BigDecimal.ZERO,
						lineCategory = null
					)
					val saved = invoiceLineRepo.save(invoiceLine)
					return ResultFactory.getSuccessResult(
						msg = "Successfully saved Consultation",
						data = saved
					)

				}

			}

			if (LINE_TYPE.valueOf(line.lineType) == LINE_TYPE.DIAGNOSIS) {
				for (d in line.diagnosis!!) {
					val diagnosisItem = Diagnosis(
						code = d.code!!, title = d.title, visit = visit.get()
					)
					diagnosisRepository.save(diagnosisItem)
				}
			} else {
				val interimInvoiceLine = InvoiceLine(
					description = line.itemDescription,
					invoiceNumber = savedInvoice.invoiceNumber,
					lineTotal = line.itemAmount,
					lineType = LINE_TYPE.valueOf(line.lineType),
					invoice = savedInvoice,
					quantity = BigDecimal.ZERO,
					unitPrice = BigDecimal.ZERO,
					lineCategory = null
				)
				invoiceLineRepo.save(interimInvoiceLine)
				return ResultFactory.getSuccessResult(
					msg = "Successfully saved invoice line",
					data = interimInvoiceLine
				)
			}
		}
		return ResultFactory.getSuccessResult(msg = "Successfully saved invoice line")
	}

	//	save line items for multiple stations
	@Transactional(readOnly = false, rollbackFor = [Exception::class])
	override fun saveMultipleBillingLineItemsPortal(multipleLineItemsArray: MultipleLineItemsArray):
			Result<Boolean> {
		val visit = visitRepo.findById(multipleLineItemsArray.visitId)
		val getVisit = visit.get()
		if (visit.isPresent) {
			val invoice = invoiceRepo.findByInvoiceNumberAndHospitalProviderId(
				multipleLineItemsArray.invoiceNumber,
				multipleLineItemsArray.providerId
			)
			if (invoice.isPresent) {
				for (it in multipleLineItemsArray.lineItems!!) {
					val newInvoiceLine = InvoiceLine(
						description = it.itemDescription,
						invoiceNumber = it.invoiceNumber,
						lineTotal = it.itemAmount,
						lineType = LINE_TYPE.valueOf(it.lineType!!),
						invoice = invoice.get(),
						quantity = BigDecimal.valueOf(it.itemQuantity!!),
						unitPrice = BigDecimal.valueOf(it.itemUnitPrice!!.toLong()),
						lineCategory = null
					)
					invoiceLineRepo.save(newInvoiceLine)
				}

//				Update this invoice
				invoice.get().apply {
					this.totalAmount = multipleLineItemsArray.totalAmount
				}
				//					we deduct the balance

				val benefitResponse = benefitService.consumeBenefit(
					ConsumeBenefitDTO(
						amount = multipleLineItemsArray.totalAmount.minus(visit.get().totalInvoiceAmount!!),
						aggregateId = visit.get().aggregateId,
						benefitId = visit.get().benefitId!!,
						memberNumber = visit.get().memberNumber,
						visitNumber = getVisit.id

					)
				)
				if (!benefitResponse.success) {
					return ResultFactory.getFailResult(msg = "Balance Error:Amount more than " +
							"member Balance ")
				}

				println("Res Line items $benefitResponse")

				if (visit.get().invoiceNumber.isNullOrBlank()){
					val theVisit = visit.get().apply {
						this.totalInvoiceAmount = multipleLineItemsArray.totalAmount
						this.invoiceNumber = multipleLineItemsArray.invoiceNumber
						this.claimProcessStatus = ClaimProcessStatus.PROCESSED
						this.status = Status.CLOSED
					}
					visitRepo.save(theVisit)
				}else{
					val theVisit = visit.get().apply {
						this.totalInvoiceAmount = multipleLineItemsArray.totalAmount
						this.claimProcessStatus = ClaimProcessStatus.PROCESSED
						this.status = Status.CLOSED
					}
					visitRepo.save(theVisit)
				}
				return ResultFactory.getSuccessResult(msg = "Successfully saved invoice line ")
			} else {
				return ResultFactory.getFailResult(
					msg = "Invoice Number does not exist, add an existing " +
							"invoice Number to save line item "
				)
			}
		}
		return ResultFactory.getSuccessResult(msg = "No Such visit with id " +
				"${multipleLineItemsArray.visitId} ")

	}

	//	save first invoice for multiple station billing
	@Transactional(readOnly = false, rollbackFor = [Exception::class])
	override fun saveMultipleBillingInvoicePortal(multipleBillingInvoice: MultipleBillingInvoice): Result<Boolean> {
		val visit = visitRepo.findById(multipleBillingInvoice.visitId)
		val getVisit = visit.get()
		if (visit.isPresent) {
			val invoice = invoiceRepo.findByInvoiceNumberAndHospitalProviderId(
				multipleBillingInvoice.invoiceNumber,
				multipleBillingInvoice.providerId
			)
			if (!invoice.isPresent) {

				//					we deduct the balance
				val benefitResponse = benefitService.consumeBenefit(
					ConsumeBenefitDTO(
						amount = multipleBillingInvoice.totalAmount,
						aggregateId = visit.get().aggregateId,
						benefitId = visit.get().benefitId!!,
						memberNumber = visit.get().memberNumber,
						visitNumber = getVisit.id

					)
				)
				if (!benefitResponse.success) {
					return ResultFactory.getFailResult(
						msg = "Balance Error- Amount more than" +
								" " +
								"member Balance "
					)

				}


				val newInvoice = Invoice(
					hospitalProviderId = multipleBillingInvoice.providerId,
					invoiceNumber = multipleBillingInvoice.invoiceNumber,
					visit = visit.get(),
					invoiceLines = mutableSetOf(),
					totalAmount = multipleBillingInvoice.totalAmount,
					service = null
				)
				val savedInvoice = invoiceRepo.save(newInvoice)
				for (it in multipleBillingInvoice.lineItems!!) {
					val newInvoiceLine = InvoiceLine(
						description = it.itemDescription,
						invoiceNumber = it.invoiceNumber,
						lineTotal = it.itemAmount,
						lineType = LINE_TYPE.valueOf(it.lineType!!),
						invoice = savedInvoice,
						quantity = BigDecimal.valueOf(it.itemQuantity!!),
						unitPrice = BigDecimal.valueOf(it.itemUnitPrice!!.toLong()),
						lineCategory = null
					)
					invoiceLineRepo.save(newInvoiceLine)
//					if (LINE_TYPE.valueOf(it.lineType!!) == LINE_TYPE.MEDICALPROCEDURE) {
//						val invProcedureCode = InvoiceNumberProcedureCode(
//							invoiceNumber = it.invoiceNumber,
//							procedure_description = it.itemDescription,
//							procedure_code = it.itemCode
//						)
//						invoiceProcedureCodeRepo.save(invProcedureCode)
//					} else if (LINE_TYPE.valueOf(it.lineType) == LINE_TYPE.DIAGNOSIS) {
//
//						val diagnosisItem = Diagnosis(
//							code = it.itemCode!!,
//							title = it.itemDescription,
//							visit = getVisit,
//							invoiceNumber = it.invoiceNumber
//						)
//						diagnosisRepository.save(diagnosisItem)
//					} else {
//
//
//					}

//					for (it in multipleLineItemsArray.documents!!) {
//						val document = Document(
//							provider_name = it.providerName,
//							provider_id = it.providerId,
//							type = DocumentType.valueOf(it.type.toString()),
//							file_url = it.fileUrl,
//							invoiceNumber = it.invoiceNumber
//						)
//
//						documentsRepo.save(document)
//					}
					//
				}

				if (visit.get().invoiceNumber.isNullOrBlank()){
					val theVisit = visit.get().apply {
						this.totalInvoiceAmount = multipleBillingInvoice.totalAmount
						this.invoiceNumber = multipleBillingInvoice.invoiceNumber
						this.claimProcessStatus = ClaimProcessStatus.PROCESSED
						this.status = Status.CLOSED
					}
					visitRepo.save(theVisit)
				}
			} else {

				return ResultFactory.getFailResult(
					msg = "Invoice Number already exists, add a new " +
							"invoice Number"
				)
			}
		}
		return ResultFactory.getSuccessResult(msg = "Successfully saved invoice")
	}


	@Transactional(readOnly = false, rollbackFor = [Exception::class])
	override fun saveDiagnosisItem(diagnosisItemDTO: DiagnosisItemDTO): Result<Diagnosis> {
		val visitId = visitRepo.findById(diagnosisItemDTO.visitId)
		var diagnosisDetail = Diagnosis(
			title = diagnosisItemDTO.description,
			code = diagnosisItemDTO.icd10code!!,
			visit = visitId.get()
		)
		val saved = diagnosisRepository.save(diagnosisDetail)
		return ResultFactory.getSuccessResult(saved)
	}

	@Transactional(readOnly = false, rollbackFor = [Exception::class])
	override fun saveClinicalInformation(clinicalInfo: ClinicalInformationDTO):
			Result<ClinicalInformation> {
		val visitId = visitRepo.findById(clinicalInfo.visitId)
		var doctorDetail = ClinicalInformation(
			name = clinicalInfo.itemDescription,
			icd10code = clinicalInfo.icd10code,
			visit_number = visitId.get()
		)
		val saved = clinicalInfoRepository.save(doctorDetail)
		return ResultFactory.getSuccessResult(saved)
	}

	@Transactional(readOnly = false, rollbackFor = [Exception::class])
	override fun cancelVisit(cancelVisitDTO: CancelVisitDTO): Result<Visit> {
		val visit = visitRepo.findById(cancelVisitDTO.visitId).get()
		val invoiceOptional = invoiceRepo.findByVisit(visit)
		return if(invoiceOptional.isPresent){
			visit.apply {

				this.status = Status.CLOSED
			}
			visitRepo.save(visit)
			ResultFactory.getSuccessResult(visit)
		}else{
			visit.apply {
				this.cancelReason = cancelVisitDTO.reason
				this.status = Status.CANCELLED
			}
			visitRepo.save(visit)
			ResultFactory.getSuccessResult(visit)
		}

	}

	@Transactional(readOnly = true)
	override fun getClaimsByMemberNumber(memberNumber: String): ResponseEntity<ByteArray?>? {
		val conn = jdbcTemplate.dataSource!!.connection
		val input =  this.javaClass.getResourceAsStream("/templates/memberStatement.jrxml");
		val familyNumber = memberNumber.split("-")
		val jasperReport: JasperReport = JasperCompileManager.compileReport(input)
		val parameters: MutableMap<String, Any> = HashMap()
		parameters["memberNumber"] = memberNumber
		parameters["familyNumber"] = familyNumber[0]
		val jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, conn)
		val data = JasperExportManager.exportReportToPdf(jasperPrint)
		val headers = HttpHeaders()
		headers.add("Content-Disposition", "inline; filename=memberStatement.pdf")
		return ResponseEntity.ok().headers(headers).contentType(MediaType.APPLICATION_PDF).body<ByteArray>(data)
	}

	override fun getProviderClaims(
		planId: String,
		hospitalProviderId: Long,
		dateFrom: String,
		dateTo: String,
		invoiceNo: String?,
		memberNo: String?,
		payerId: String?
	): Result<MutableList<ProviderClaimsDTO>> {
		var providerClaims: MutableList<ProviderClaimsDTO> = mutableListOf()

		if (invoiceNo.isNullOrEmpty() && memberNo.isNullOrEmpty() && !planId.isEmpty() ) {
			providerClaims =
				visitRepo.findProviderClaims(planId, hospitalProviderId, dateFrom, dateTo)
		} else if (!invoiceNo.isNullOrEmpty() && !memberNo.isNullOrEmpty() && !payerId.isNullOrEmpty()
		) {
			providerClaims = visitRepo.findProviderClaimWithPayerMemberNoInvoiceNo(
				planId,
				hospitalProviderId, dateFrom, dateTo, payerId, memberNo, invoiceNo
			)

		} else if (invoiceNo.isNullOrEmpty() && !memberNo.isNullOrEmpty() && !payerId.isNullOrEmpty()
		) {
			providerClaims = visitRepo.findProviderClaimWithPayerMemberNo(
				planId, hospitalProviderId, dateFrom, dateTo, payerId, memberNo
			)
		} else if (!invoiceNo.isNullOrEmpty() && memberNo.isNullOrEmpty() && !payerId.isNullOrEmpty()
		) {
			providerClaims = visitRepo.findProviderClaimWithPayerInvoiceNo(
				planId, hospitalProviderId, dateFrom, dateTo, payerId, invoiceNo
			)
		} else if (!invoiceNo.isNullOrEmpty() && memberNo.isNullOrEmpty() && !payerId
				.isNullOrEmpty()
		) {
			providerClaims = visitRepo.findProviderClaimWithPayerInvoiceNo(
				planId, hospitalProviderId, dateFrom, dateTo, payerId, invoiceNo
			)
		} else if (invoiceNo.isNullOrEmpty() && !memberNo.isNullOrEmpty() && payerId.isNullOrEmpty()
		) {
			providerClaims = visitRepo.findProviderClaimWithMemberNo(
				planId, hospitalProviderId, dateFrom, dateTo, memberNo
			)
		} else if (!invoiceNo.isNullOrEmpty() && memberNo.isNullOrEmpty() && payerId.isNullOrEmpty()
		) {
			providerClaims = visitRepo.findProviderClaimWithInvoiceNo(
				planId, hospitalProviderId, dateFrom, dateTo, invoiceNo
			)
		} else if (invoiceNo.isNullOrEmpty() && memberNo.isNullOrEmpty() && !payerId.isNullOrEmpty()
		) {
			providerClaims = visitRepo.findProviderClaimWithPayer(
				planId, hospitalProviderId, dateFrom, dateTo, payerId
			)
		}

		if (providerClaims.isEmpty()) return ResultFactory.getFailResult("No claims with selected params")

		return if (providerClaims.isNotEmpty()) {
			ResultFactory.getSuccessResult(data = providerClaims, msg = "Claims successfuly found")
		} else {
			ResultFactory.getFailResult("No claims")
		}

	}

	override fun getMainAndBranchClaims(
		hospitalProviderId: Long,
		dateFrom: String,
		dateTo: String
	): Result<MutableList<ProviderClaimsDTO>> {
		var providerClaims: MutableList<ProviderClaimsDTO> = mutableListOf()

		providerClaims =
			visitRepo.findProviderClaimsTest(hospitalProviderId, dateFrom, dateTo)

		if (providerClaims.isEmpty()) return ResultFactory.getFailResult("No claims with selected params")

		return if (providerClaims.isNotEmpty()) {
			ResultFactory.getSuccessResult(data = providerClaims, msg = "Claims successfuly found")
		} else {
			ResultFactory.getFailResult("No claims")
		}
	}



	override fun getProviderClaimsPdf(
		planId: String,
		hospitalProviderId: Long,
		dateFrom: String,
		dateTo: String,
		invoiceNo: String?,
		memberNo: String?,
		payerId: String?
	): ResponseEntity<ByteArray?>? {
		var providerClaims: MutableList<ProviderClaimsDTO> = mutableListOf()

		if (invoiceNo.isNullOrEmpty() && memberNo.isNullOrEmpty() && payerId.isNullOrEmpty()) {
			providerClaims =
				visitRepo.findProviderClaims(planId, hospitalProviderId, dateFrom, dateTo)
		} else if (!invoiceNo.isNullOrEmpty() && !memberNo.isNullOrEmpty() && !payerId.isNullOrEmpty()
		) {
			providerClaims = visitRepo.findProviderClaimWithPayerMemberNoInvoiceNo(
				planId,
				hospitalProviderId, dateFrom, dateTo, payerId, memberNo, invoiceNo
			)

		} else if (invoiceNo.isNullOrEmpty() && !memberNo.isNullOrEmpty() && !payerId.isNullOrEmpty()
		) {
			providerClaims = visitRepo.findProviderClaimWithPayerMemberNo(
				planId, hospitalProviderId, dateFrom, dateTo, payerId, memberNo
			)
		} else if (!invoiceNo.isNullOrEmpty() && memberNo.isNullOrEmpty() && !payerId.isNullOrEmpty()
		) {
			providerClaims = visitRepo.findProviderClaimWithPayerInvoiceNo(
				planId, hospitalProviderId, dateFrom, dateTo, payerId, invoiceNo
			)
		} else if (!invoiceNo.isNullOrEmpty() && memberNo.isNullOrEmpty() && !payerId
				.isNullOrEmpty()
		) {
			providerClaims = visitRepo.findProviderClaimWithPayerInvoiceNo(
				planId, hospitalProviderId, dateFrom, dateTo, payerId, invoiceNo
			)
		} else if (invoiceNo.isNullOrEmpty() && !memberNo.isNullOrEmpty() && payerId.isNullOrEmpty()
		) {
			providerClaims = visitRepo.findProviderClaimWithMemberNo(
				planId, hospitalProviderId, dateFrom, dateTo, memberNo
			)
		} else if (!invoiceNo.isNullOrEmpty() && memberNo.isNullOrEmpty() && payerId.isNullOrEmpty()
		) {
			providerClaims = visitRepo.findProviderClaimWithInvoiceNo(
				planId, hospitalProviderId, dateFrom, dateTo, invoiceNo
			)
		} else if (invoiceNo.isNullOrEmpty() && memberNo.isNullOrEmpty() && !payerId.isNullOrEmpty()
		) {
			providerClaims = visitRepo.findProviderClaimWithPayer(
				planId, hospitalProviderId, dateFrom, dateTo, payerId
			)
		}
		val dataSource = JRBeanCollectionDataSource(providerClaims)
		val input = this.javaClass.getResourceAsStream("/templates/providerStatement.jrxml");
		val jasperReport: JasperReport = JasperCompileManager.compileReport(input)
		val parameters: MutableMap<String, Any> = HashMap()
		parameters["planId"] = planId
		parameters["hospitalProviderId"] = hospitalProviderId
		parameters["fromDate"] = dateFrom
		parameters["toDate"] = dateTo
		val jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource)
		val data = JasperExportManager.exportReportToPdf(jasperPrint)
		//println("------------------Provider claims")
		val headers = HttpHeaders()
		headers.add("Content-Disposition", "inline; filename=providerStatement.pdf")
		return ResponseEntity.ok().headers(headers).contentType(MediaType.APPLICATION_PDF)
			.body<ByteArray>(data)


	}

	override fun getMainAndBranchClaimsPdf(
		hospitalProviderId: Long,
		dateFrom: String,
		dateTo: String
	): ResponseEntity<ByteArray?>? {
		var providerClaims: MutableList<ProviderClaimsDTO> = mutableListOf()

		providerClaims =
			visitRepo.findProviderClaimsTest(hospitalProviderId, dateFrom, dateTo)

		val dataSource = JRBeanCollectionDataSource(providerClaims)
		val input = this.javaClass.getResourceAsStream("/templates/providerStatement.jrxml");
		val jasperReport: JasperReport = JasperCompileManager.compileReport(input)
		val parameters: MutableMap<String, Any> = HashMap()
		parameters["planId"] = 0
		parameters["hospitalProviderId"] = hospitalProviderId
		parameters["fromDate"] = dateFrom
		parameters["toDate"] = dateTo
		val jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource)
		val data = JasperExportManager.exportReportToPdf(jasperPrint)
		//println("------------------Provider claims")
		val headers = HttpHeaders()
		headers.add("Content-Disposition", "inline; filename=providerStatement.pdf")
		return ResponseEntity.ok().headers(headers).contentType(MediaType.APPLICATION_PDF)
			.body<ByteArray>(data)
	}

	override fun getProviderClaimsExcel(
		planId: String,
		hospitalProviderId: Long,
		dateFrom: String?,
		dateTo: String?,
		invoiceNo: String?,
		memberNo: String?,
		payerId: String?
	): ResponseEntity<Resource> {
		var providerClaims: MutableList<ProviderClaimsDTO> = mutableListOf()


		if (invoiceNo.isNullOrEmpty() && memberNo.isNullOrEmpty() && payerId.isNullOrEmpty()) {
			providerClaims =
				visitRepo.findProviderClaims(planId, hospitalProviderId, dateFrom!!, dateTo!!)
		} else if (!invoiceNo.isNullOrEmpty() && !memberNo.isNullOrEmpty() && !payerId.isNullOrEmpty()
		) {
			providerClaims = visitRepo.findProviderClaimWithPayerMemberNoInvoiceNo(
				planId,
				hospitalProviderId, dateFrom!!, dateTo!!, payerId, memberNo, invoiceNo
			)

		} else if (invoiceNo.isNullOrEmpty() && !memberNo.isNullOrEmpty() && !payerId.isNullOrEmpty()
		) {
			providerClaims = visitRepo.findProviderClaimWithPayerMemberNo(
				planId, hospitalProviderId, dateFrom!!, dateTo!!, payerId, memberNo
			)
		} else if (!invoiceNo.isNullOrEmpty() && memberNo.isNullOrEmpty() && !payerId.isNullOrEmpty()
		) {
			providerClaims = visitRepo.findProviderClaimWithPayerInvoiceNo(
				planId, hospitalProviderId, dateFrom!!, dateTo!!, payerId, invoiceNo
			)
		} else if (!invoiceNo.isNullOrEmpty() && memberNo.isNullOrEmpty() && !payerId
				.isNullOrEmpty()
		) {
			providerClaims = visitRepo.findProviderClaimWithPayerInvoiceNo(
				planId, hospitalProviderId, dateFrom!!, dateTo!!, payerId, invoiceNo
			)
		} else if (invoiceNo.isNullOrEmpty() && !memberNo.isNullOrEmpty() && payerId.isNullOrEmpty()
		) {
			providerClaims = visitRepo.findProviderClaimWithMemberNo(
				planId, hospitalProviderId, dateFrom!!, dateTo!!, memberNo
			)
		} else if (!invoiceNo.isNullOrEmpty() && memberNo.isNullOrEmpty() && payerId.isNullOrEmpty()
		) {
			providerClaims = visitRepo.findProviderClaimWithInvoiceNo(
				planId, hospitalProviderId, dateFrom!!, dateTo!!, invoiceNo
			)
		} else if (invoiceNo.isNullOrEmpty() && memberNo.isNullOrEmpty() && !payerId.isNullOrEmpty()
		) {
			providerClaims = visitRepo.findProviderClaimWithPayer(
				planId, hospitalProviderId, dateFrom!!, dateTo!!, payerId
			)
		}
		try {
			XSSFWorkbook().use { workbook ->
				ByteArrayOutputStream().use { out ->
					val sheet = workbook.createSheet("CLAIMS")

					// Header
					val headerRow = sheet.createRow(0)
					for (col in FILEHEADER.indices) {
						val cell = headerRow.createCell(col)
						cell.setCellValue(FILEHEADER[col])
						sheet.autoSizeColumn(col)
					}
					var rowIdx = 1
					for (claim in providerClaims) {
						val row = sheet.createRow(rowIdx++)
						row.createCell(0).setCellValue(claim.memberNumber)
						row.createCell(1).setCellValue(claim.memberName)
						row.createCell(2).setCellValue(claim.benefitName)
						row.createCell(3).setCellValue(claim.invoiceAmount.toString())
						row.createCell(4).setCellValue(claim.createdAt)
						row.createCell(5).setCellValue(claim.invoiceNumber)
						row.createCell(6).setCellValue(claim.schemeName)
						row.createCell(7).setCellValue(claim.providerName)

					}
					workbook.write(out)
					val filename = "ProviderClaims.xlsx"
					val file = InputStreamResource(ByteArrayInputStream(out.toByteArray()))

					return ResponseEntity.ok()
						.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=$filename")
						.contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
						.body(file)
				}
			}
		} catch (e: IOException) {
			throw RuntimeException("fail to export data to Excel file: " + e.message)
		}

	}

	override fun getMainAndBranchClaimsExcel(
		hospitalProviderId: Long,
		dateFrom: String,
		dateTo: String
	): ResponseEntity<Resource> {
		var providerClaims: MutableList<ProviderClaimsDTO> = mutableListOf()

		providerClaims =
			visitRepo.findProviderClaimsTest(hospitalProviderId, dateFrom, dateTo)

		try {
			XSSFWorkbook().use { workbook ->
				ByteArrayOutputStream().use { out ->
					val sheet = workbook.createSheet("CLAIMS")

					// Header
					val headerRow = sheet.createRow(0)
					for (col in FILEHEADER.indices) {
						val cell = headerRow.createCell(col)
						cell.setCellValue(FILEHEADER[col])
						sheet.autoSizeColumn(col)
					}
					var rowIdx = 1
					for (claim in providerClaims) {
						val row = sheet.createRow(rowIdx++)
						row.createCell(0).setCellValue(claim.memberNumber)
						row.createCell(1).setCellValue(claim.memberName)
						row.createCell(2).setCellValue(claim.benefitName)
						row.createCell(3).setCellValue(claim.invoiceAmount.toString())
						row.createCell(4).setCellValue(claim.createdAt)
						row.createCell(5).setCellValue(claim.invoiceNumber)
						row.createCell(6).setCellValue(claim.schemeName)
						row.createCell(7).setCellValue(claim.providerName)

					}
					workbook.write(out)
					val filename = "ProviderClaims.xlsx"
					val file = InputStreamResource(ByteArrayInputStream(out.toByteArray()))

					return ResponseEntity.ok()
						.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=$filename")
						.contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
						.body(file)
				}
			}
		} catch (e: IOException) {
			throw RuntimeException("fail to export data to Excel file: " + e.message)
		}
	}

	override fun getPlanOfflcts(
		planId: String,
		hospitalProviderId: String?,
		dateFrom: String,
		dateTo: String,
		invoiceNo: String?,
		memberNo: String?,
		payerId: String?
	): Result<MutableList<GeneralClaimDTO>> {
		var offlcts: MutableList<GeneralClaimDTO>
		if (!hospitalProviderId.isNullOrEmpty()) {
			offlcts = visitRepo.findPlanOfflctsWithProvider(
				planId,
				hospitalProviderId, dateFrom, dateTo
			)
			return if (offlcts.isNotEmpty()) {
				ResultFactory.getSuccessResult(
					data = offlcts,
					msg = "Offlcts successfuly found"
				)
			} else {
				ResultFactory.getFailResult("No offlcts")
			}
		} else {
			offlcts = visitRepo.findPlanOfflcts(planId, dateFrom, dateTo)
			return if (offlcts.isNotEmpty()) {
				ResultFactory.getSuccessResult(data = offlcts, msg = "Offlcts successfuly found")
			} else {
				ResultFactory.getFailResult("No offlcts")
			}
		}

	}

	override fun getPlanOfflctsPdf(
		planId: String,
		fromDate: String,
		toDate: String,
		hospitalProviderId: String?
	): ResponseEntity<ByteArray?>? {

		val offlcts: MutableList<GeneralClaimDTO> = if (!hospitalProviderId.isNullOrEmpty() && hospitalProviderId.lowercase() != "undefined") {
			visitRepo.findPlanOfflctsWithProvider(
				planId,
				hospitalProviderId, fromDate, toDate
			)

		} else {
			visitRepo.findPlanOfflcts(planId, fromDate, toDate)

		}

		val dataSource = JRBeanCollectionDataSource(offlcts)

		val input = this.javaClass.getResourceAsStream("/templates/offlctStatement.jrxml");
		val jasperReport: JasperReport = JasperCompileManager.compileReport(input)
		val parameters: MutableMap<String, Any> = HashMap()
		parameters["planId"] = planId
		if(!hospitalProviderId.isNullOrEmpty() && hospitalProviderId.lowercase() != "undefined"){
			parameters["hospitalProviderId"] = hospitalProviderId.toLong()
		}
		parameters["fromDate"] = fromDate
		parameters["toDate"] = toDate
		val jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource)
		val data = JasperExportManager.exportReportToPdf(jasperPrint)
		val headers = HttpHeaders()
		headers.add("Content-Disposition", "inline; filename=offlctStatement.pdf")
		return ResponseEntity.ok().headers(headers).contentType(MediaType.APPLICATION_PDF)
			.body<ByteArray>(data)
	}


	override fun getPlanReimbursementsPdf(
		planId: String,
		fromDate: String,
		toDate: String
	): ResponseEntity<ByteArray?>? {

		val reimbursements: MutableList<GeneralClaimDTO> = visitRepo.findPlanReimbursementsPdf(planId,
			fromDate,
			toDate)

		val dataSource = JRBeanCollectionDataSource(reimbursements)

		val input = this.javaClass.getResourceAsStream("/templates/reimbursementStatement.jrxml");
		val jasperReport: JasperReport = JasperCompileManager.compileReport(input)
		val parameters: MutableMap<String, Any> = HashMap()
		parameters["planId"] = planId
		if(!planId.isEmpty() && planId.lowercase() != "undefined"){
			parameters["planId"] = planId.toLong()
		}
		parameters["fromDate"] = fromDate
		parameters["toDate"] = toDate
		val jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource)
		val data = JasperExportManager.exportReportToPdf(jasperPrint)
		val headers = HttpHeaders()
		headers.add("Content-Disposition", "inline; filename=reimbursementStatement.pdf")
		return ResponseEntity.ok().headers(headers).contentType(MediaType.APPLICATION_PDF)
			.body<ByteArray>(data)
	}

	override fun getPagedClosedVisitsPdf(
		providerId: Long, staffId: String, page: Int, size: Int, invoiceNo:
		String?, memberNo: String?
	): ResponseEntity<ByteArray?>? {
		var providerClaims: MutableList<ProviderClaimsDTO> = mutableListOf()

		if (!memberNo.isNullOrEmpty() && !invoiceNo.isNullOrEmpty()){
			 providerClaims = visitRepo
				.findPagedClosedVisitsWithMemberNoAndInvoiceNo(providerId,
					invoiceNo,
					memberNo)
		}else if(invoiceNo.isNullOrEmpty() && !memberNo.isNullOrEmpty()){
			 providerClaims= visitRepo
				.findPagedClosedVisitsWithMemberNo(providerId,
					memberNo)
		}else if(invoiceNo.isNullOrEmpty() && memberNo.isNullOrEmpty()){
			 providerClaims = visitRepo
				.findPagedClosedVisitsNoInvoiceMemberNo(providerId)
		}

		val dataSource = JRBeanCollectionDataSource(providerClaims)

		val input = this.javaClass.getResourceAsStream("/templates/providerHistoryRpt.jrxml");
		val jasperReport: JasperReport = JasperCompileManager.compileReport(input)
		val parameters: MutableMap<String, Any> = HashMap()
		parameters["planId"] = ""
		parameters["fromDate"] = ""
		parameters["toDate"] = ""
		val jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource)
		val data = JasperExportManager.exportReportToPdf(jasperPrint)
		val headers = HttpHeaders()
		headers.add("Content-Disposition", "inline; filename=providerStatement.pdf")
		return ResponseEntity.ok().headers(headers).contentType(MediaType.APPLICATION_PDF)
			.body<ByteArray>(data)
	}

	override fun getPagedClosedVisitsExcel(
		providerId: Long, staffId: String, page: Int, size: Int, invoiceNo:
		String?, memberNo: String?
	): ResponseEntity<Resource>? {
		var providerClaims: MutableList<ProviderClaimsDTO> = mutableListOf()

		if (!memberNo.isNullOrEmpty() && !invoiceNo.isNullOrEmpty()){
			providerClaims = visitRepo
				.findPagedClosedVisitsWithMemberNoAndInvoiceNo(providerId,
					invoiceNo,
					memberNo)
		}else if(invoiceNo.isNullOrEmpty() && !memberNo.isNullOrEmpty()){
			providerClaims= visitRepo
				.findPagedClosedVisitsWithMemberNo(providerId,
					memberNo)
		}else if(invoiceNo.isNullOrEmpty() && memberNo.isNullOrEmpty()){
			providerClaims = visitRepo
				.findPagedClosedVisitsNoInvoiceMemberNo(providerId)
		}
		try {
			XSSFWorkbook().use { workbook ->
				ByteArrayOutputStream().use { out ->
					val sheet = workbook.createSheet("Visits")

					// Header
					val headerRow = sheet.createRow(0)
					for (col in FILEHEADER.indices) {
						val cell = headerRow.createCell(col)
						cell.setCellValue(FILEHEADER[col])
						sheet.autoSizeColumn(col)

					}
					var rowIdx = 1
					for (claim in providerClaims) {
						val row = sheet.createRow(rowIdx++)
						row.createCell(0).setCellValue(claim.memberNumber)
						row.createCell(1).setCellValue(claim.memberName)
						row.createCell(2).setCellValue(claim.benefitName)
						row.createCell(3).setCellValue(claim.invoiceAmount.toString())
						row.createCell(4).setCellValue(claim.createdAt)
						row.createCell(5).setCellValue(claim.invoiceNumber)
						row.createCell(6).setCellValue(claim.schemeName)
						row.createCell(7).setCellValue(claim.providerName)

					}
					workbook.write(out)
					val filename = "ProviderVisits.xlsx"
					val file = InputStreamResource(ByteArrayInputStream(out.toByteArray()))

					return ResponseEntity.ok()
						.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=$filename")
						.contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
						.body(file)
				}
			}
		} catch (e: IOException) {
			throw RuntimeException("fail to export data to Excel file: " + e.message)
		}
	}


	override fun allTransactions(
		planId: String?,
		hospitalProviderId: String?,
		dateFrom: String,
		dateTo: String,
		invoiceNo: String?,
		memberNo: String?,
		payerId: String?,
		status: String
	): Result<MutableList<GeneralClaimDTO>> {
		var allTransactions: MutableList<GeneralClaimDTO> = mutableListOf()
		if (planId.isNullOrEmpty() && hospitalProviderId.isNullOrEmpty() && invoiceNo.isNullOrEmpty()
			&& memberNo.isNullOrEmpty() && payerId.isNullOrEmpty() && !dateFrom.isNullOrEmpty()
			&& !dateTo.isNullOrEmpty()
		) {
			allTransactions = visitRepo.findAllTransactionsDateOnly(dateFrom, dateTo, status)
		} else if (planId.isNullOrEmpty() && !hospitalProviderId.isNullOrEmpty() && invoiceNo
				.isNullOrEmpty()
			&& memberNo.isNullOrEmpty() && payerId.isNullOrEmpty() && !dateFrom.isNullOrEmpty()
			&& !dateTo.isNullOrEmpty()
		) {
			allTransactions = visitRepo.findAllTransactionsDateAndProvider(
				dateFrom, dateTo,
				hospitalProviderId, status
			)

		} else if (!planId.isNullOrEmpty() && !hospitalProviderId.isNullOrEmpty() && invoiceNo
				.isNullOrEmpty()
			&& memberNo.isNullOrEmpty() && payerId.isNullOrEmpty() && !dateFrom.isNullOrEmpty()
			&& !dateTo.isNullOrEmpty()
		) {
			allTransactions = visitRepo.findAllTransactionsWithDateAndProviderAndPlan(
				dateFrom,
				dateTo, hospitalProviderId, planId, status
			)
		} else if (!planId.isNullOrEmpty() && !hospitalProviderId.isNullOrEmpty() && invoiceNo
				.isNullOrEmpty()
			&& memberNo.isNullOrEmpty() && !payerId.isNullOrEmpty() && !dateFrom.isNullOrEmpty()
			&& !dateTo.isNullOrEmpty()
		) {
			allTransactions = visitRepo.findAllTransactionsWithDateAndProviderAndPlanAndPayer(
				dateFrom, dateTo, hospitalProviderId, planId, payerId, status
			)
		} else if (!planId.isNullOrEmpty() && !hospitalProviderId.isNullOrEmpty() && !invoiceNo
				.isNullOrEmpty()
			&& memberNo.isNullOrEmpty() && !payerId.isNullOrEmpty() && !dateFrom.isNullOrEmpty()
			&& !dateTo.isNullOrEmpty()
		) {
			allTransactions = visitRepo
				.findAllTransactionsWithDateAndProviderAndPlanAndPayerAndInvoice(
					dateFrom, dateTo, hospitalProviderId, planId, payerId, invoiceNo, status
				)
		} else if (!planId.isNullOrEmpty() && !hospitalProviderId.isNullOrEmpty()
			&& !payerId.isNullOrEmpty() && !dateFrom.isNullOrEmpty()
			&& !dateTo.isNullOrEmpty() && (invoiceNo
				.isNullOrEmpty() || memberNo.isNullOrEmpty())
		) {
			allTransactions = visitRepo
				.findAllTransactionsWithDateAndProviderAndPlanAndPayerAndInvoiceAndMemberNo(
					dateFrom,
					dateTo,
					hospitalProviderId!!,
					planId!!,
					payerId,
					invoiceNo,
					memberNo,
					status
				)
		}

		if (allTransactions.isEmpty()) return ResultFactory.getFailResult("No claims with selected params")

		return if (allTransactions.isNotEmpty()) {
			ResultFactory.getSuccessResult(data = allTransactions, msg = "Claims successfuly found")
		} else {
			ResultFactory.getFailResult("No claims")
		}
	}


	override fun getClaimBySearch(payerId: String, search: String):
			Result<MutableList<GeneralClaimDTO>> {
		var claim: MutableList<GeneralClaimDTO> = mutableListOf()
		claim = visitRepo.findClaimWithPayerAndSearch(payerId, search)
		if (claim.isNotEmpty()) {
			return ResultFactory.getSuccessResult(
				data = claim, msg = "Claims successfuly " +
						"found"
			)
		} else {
			return ResultFactory.getSuccessResult(data = claim, msg = "No Claims Found")
		}

	}

	override fun getPlanReimbursements(
		planId: String,
		dateFrom: String,
		dateTo: String
	): Result<MutableList<ProviderReimbursementDTO>> {

		val providerClaims = visitRepo.findPlanReimbursements(planId, dateFrom, dateTo)

		if (providerClaims.isEmpty()) return ResultFactory.getFailResult(
			"No Reimbursements with " +
					"selected params"
		)

		return if (providerClaims.isNotEmpty()) {
			ResultFactory.getSuccessResult(
				data = providerClaims,
				msg = "Reimbursements successfuly found"
			)
		} else {
			ResultFactory.getFailResult("No Reimbursements")
		}
	}


	override fun getPlanClaims(
		planId: String,
		dateFrom: String,
		dateTo: String,
		invoiceNo: String?,
		memberNo: String?,
		payerId: String?
	): Result<MutableList<ProviderClaimsDTO>> {
		var providerClaims: MutableList<ProviderClaimsDTO> = mutableListOf()
		if (invoiceNo.isNullOrEmpty() && memberNo.isNullOrEmpty() && payerId.isNullOrEmpty()) {
			providerClaims = visitRepo.findPlanClaims(planId, dateFrom, dateTo)
		} else if (!invoiceNo.isNullOrEmpty() && !memberNo.isNullOrEmpty() && !payerId.isNullOrEmpty
				()
		) {
			providerClaims = visitRepo.findPlanClaimsWithPayerMemberNoInvoiceNo(
				planId, dateFrom,
				dateTo, invoiceNo, memberNo, payerId
			)
		} else if (invoiceNo.isNullOrEmpty() && !memberNo.isNullOrEmpty() && !payerId.isNullOrEmpty
				()
		) {
			providerClaims = visitRepo.findPlanClaimsWithPayerMemberNo(
				planId, dateFrom,
				dateTo, payerId, memberNo
			)
		} else if (!invoiceNo.isNullOrEmpty() && memberNo.isNullOrEmpty() && !payerId.isNullOrEmpty
				()
		) {
			providerClaims = visitRepo.findPlanClaimsWithPayerInvoiceNo(
				planId, dateFrom,
				dateTo, payerId, invoiceNo
			)
		}

		if (providerClaims.isEmpty()) return ResultFactory.getFailResult("No claims with selected params")

		return if (providerClaims.isNotEmpty()) {
			ResultFactory.getSuccessResult(data = providerClaims, msg = "Claims successfuly found")
		} else {
			ResultFactory.getFailResult("No claims")
		}
	}

	@Transactional(readOnly = true)
	override fun getIndividualProviderClaim(
		hospitalProviderId: Long,
		visitNumber: Long,
		invoiceNumber:String?
	): ResponseEntity<ByteArray?>? {

		val visit = visitRepo.findById(visitNumber).get()
		val claim:Optional<SingleClaimDTO>

		val invoices :MutableList<Invoice> = invoiceRepo.findByVisitNumber(visit)
		claim = if (invoices.size>1){

			visitRepo.findSingleProviderClaimByInvoiceNumber(visitNumber,
				invoiceNumber!!
			)

		}else{
			visitRepo.findSingleProviderClaim(visitNumber, hospitalProviderId)
		}

		val theClaim = claim.stream().collect(Collectors.toList())
		val dataSource = JRBeanCollectionDataSource(theClaim)

		val templateStream = this.javaClass.getResourceAsStream("/templates/singleClaimStatement" + ".jrxml");

		val jasperReport: JasperReport = JasperCompileManager.compileReport(templateStream)
		val parameters: MutableMap<String, Any> = HashMap()
		parameters["hospitalProviderId"] = hospitalProviderId
		parameters["visitNumber"] = visitNumber
//
		val jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource)
		val data = JasperExportManager.exportReportToPdf(jasperPrint)

		val headers = HttpHeaders()
		headers.add("Content-Disposition", "inline; filename=claimId_$visitNumber.pdf")
		return ResponseEntity.ok().headers(headers).contentType(MediaType.APPLICATION_PDF)
			.body<ByteArray>(data)
	}

	@Transactional(readOnly = true)
	override fun getIndividualReimbursementClaim(
		visitNumber: Long
	): ResponseEntity<ByteArray?>? {

		val claim = visitRepo.findSingleReimbursementClaim(visitNumber)
		val theClaim = claim.stream().collect(Collectors.toList())
		val dataSource = JRBeanCollectionDataSource(theClaim)

		val templateStream = this.javaClass.getResourceAsStream("/templates/singleClaimStatement" + ".jrxml");

		val jasperReport: JasperReport = JasperCompileManager.compileReport(templateStream)
		val parameters: MutableMap<String, Any> = HashMap()
		parameters["visitNumber"] = visitNumber
//
		val jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource)
		val data = JasperExportManager.exportReportToPdf(jasperPrint)

		val headers = HttpHeaders()
		headers.add("Content-Disposition", "inline; filename=reimbursementId_$visitNumber.pdf")
		return ResponseEntity.ok().headers(headers).contentType(MediaType.APPLICATION_PDF)
			.body<ByteArray>(data)
	}


	override fun getClientActiveVisit(
		memberNumber: String,
		hospitalProviderId: Long
	): Result<Visit> {
		val active = visitRepo.findActiveVisitsForMember(
			memberNumber = memberNumber.trim(),
			hospitalProviderId = hospitalProviderId,
			status = Status.ACTIVE
		)
		if (active.isPresent) {
			return ResultFactory.getSuccessResult(active.get())
		}
		return ResultFactory.getFailResult("Client has no active visit")
	}

	override fun previewProviderInvoice(invoiceNumber: String, hospitalProviderId: Long): ResponseEntity<ByteArray?>? {
		val document = documentsRepo.findByInvoiceNumberAndProviderIdAndType(invoiceNumber, hospitalProviderId, DocumentType.INVOICE)
		val documentClient = WebClient.builder().baseUrl(documentUrl).build()
		val invoiceImage = documentClient
			.get()
			.uri {
				u ->
				u.queryParam("name", document.get().fileUrl)
					.build()
			}
			.retrieve()
			.bodyToMono(String::class.java)
			.block()

		val invoiceRes = gson.fromJson(invoiceImage.toString(),  InvoiceRes::class.java)
		println(invoiceRes.data)

		val url = URL(invoiceRes.data)

		url.openStream().use { inp ->
			BufferedInputStream(inp).use{ bis ->
				FileOutputStream("invoice.pdf").use { fos ->
					val data = ByteArray(1024)
					var count: Int
					while (bis.read(data,0,1024).also { count = it } != -1){
						fos.write(data, 0, count)
					}
				}
			}
		}

		val file: File = ResourceUtils.getFile("invoice.pdf")
		val fl = FileInputStream(file)
		val arr = ByteArray(file.length().toInt())
		fl.read(arr);
		fl.close();

		val headers = HttpHeaders()
		headers.add("Content-Disposition", "inline; filename=$invoiceNumber.pdf")
		return ResponseEntity.ok().headers(headers).contentType(MediaType.APPLICATION_PDF)
			.body<ByteArray>(arr)
	}

	override fun getMemberStatementData(memberNumber: String): MutableList<MemberStatement>? {
		val statements = mutableListOf<MemberStatement>()
		val membershipClient = WebClient.builder().baseUrl(memberSearchUrl).build()
		visitRepo.findByFamilyNumber(memberNumber)?.forEach { visits ->
			val mappingResult = membershipClient
				.get()
				.uri { u ->
					u
						.path("/api/v1/membership/payer/mappings")
						.queryParam("providerId", visits.providerId)
						.queryParam("payerId", visits.payerId)
						.queryParam("benefitId", visits.benefitId)
						.queryParam("categoryId", visits.categoryId)
						.build()
				}
				.retrieve()
				.bodyToMono(String::class.java)
				.block()
			val mapRes = gson.fromJson(mappingResult.toString(), ProviderMappingRes::class.java)

			val statement = MemberStatement(
				memberName = visits.memberName,
				memberNumber = visits.memberNumber,
				benefitName = visits.benefitName,
				schemeName = mapRes.data.schemeName!!,
				subBenefit = visits.subBenefit,
				payerName = mapRes.data.payerName,
				invoiceAmount = visits.totalAmount,
				invoiceNumber = visits.invoiceNumber,
				createdAt = visits.createdAt,
				providerName = mapRes.data.providerName,
				initialLimit = visits.initialLimit,
				fromDate = visits.fromDate,
				toDate = visits.toDate
			)
			statements.add(statement)
		}
		return statements
	}

	override fun generateMemberStatement(memberNumber: String): ByteArray? {
		val conn = jdbcTemplate.dataSource!!.connection
		val input =  this.javaClass.getResourceAsStream("/templates/memberStatement.jrxml");
		val jasperReport: JasperReport = JasperCompileManager.compileReport(input)
		val parameters: MutableMap<String, Any> = HashMap()
		parameters["memberNumber"] = memberNumber
		val jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, conn)
		val data = JasperExportManager.exportReportToPdf(jasperPrint)
		return data
	}

	@Scheduled(cron = "0 30 3 8 * *")
	@SchedulerLock(name = "sendKengenEmails")
	override fun emailMemberStatement(): MutableList<EmailBeneficiaryDto>? {
//		val members = beneficiaryBenefitRepository.getPrincipalInCategory(56)
		val members = beneficiaryBenefitRepository.getPrincipalInfor("7")
		val notificationClient = WebClient.builder().baseUrl(notificationBaseUrl+emailUrl).build()
		members?.forEach { member ->
			val statement = generateMemberStatement(member.memberNumber)
			val emailSubject = "MEMBER STATEMENT FOR POLICY PERIOD: " + member.startDate + " " + member.endDate
			val emailBody = "Dear ${member.memberName}\n\nThe email version of your statement is attached and is ready for viewing (in pdf format).\n\n" +
					"As this is an automatically generated mail, please do not reply to this address.\n\n For any clarification, please contact LCT Africa on" +
					" the following:\n\n Phone: 0703071333\n Email: contactcentre@lctafrica.net; info@lctafrica.net\n\nThank You\nLCT Africa"
			val path: Path = Paths.get("statement.pdf")
			val file = Files.write(path, statement)
			val builder = MultipartBodyBuilder()
			val fileSystemResource = FileSystemResource(file)
			builder.part("file", fileSystemResource);
			val emailStatement = notificationClient
				.post()
				.uri { u ->
					u
						.queryParam("message", emailBody)
						.queryParam("subject", emailSubject)
						.queryParam("recipient", member.email)
						.build()
				}
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.body(BodyInserters.fromMultipartData(builder.build()))
				.retrieve()
				.bodyToMono(String::class.java)
				.block()
		}

		return members
	}


	@Transactional(readOnly = true)
	override fun getSchemeFinancialUtilizations(filters:ReportFilters): ResponseEntity<Resource> {
		val headers = HttpHeaders()
		var plan_name: String? =null;

		if(filters.startDate == null && filters.endDate != null)
		{
			//return ResultFactory.getFailResult("Please specify start date.")
			//headers.add("Content-Disposition", "inline; filename=Error_No_start_Date_Specified.xlsx")
			//return ResponseEntity.ok().headers(headers).contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
			//		.body(null)
			plan_name="No Data Found";
		}

		if(filters.startDate != null && filters.endDate == null)
		{
			filters.endDate= LocalDate.now();
		}

		if(filters.startDate == null && filters.endDate == null)
		{
			filters.endDate= LocalDate.now();
			filters.startDate= LocalDate.now().minusDays(30)
		}


		val useList: MutableList<Financial> = mutableListOf();
		val listVisit:List<SchemeStatementDTO> = visitRepo.findSchemeFinancialUtilizations(filters.planId, filters.startDate!!, filters.endDate!!, filters.payerId);
		val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
		val timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss")


		if(listVisit.isEmpty())
		{
			//return ResultFactory.getFailResult("No records available.")
			//headers.add("Content-Disposition", "inline; filename=No Data Found_${filters.startDate}_${filters.endDate}.xlsx")
			//return ResponseEntity.ok().headers(headers).contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
			//		.body(null)
			plan_name="No Data Found";
		}

		for (list in listVisit)
		{
			useList.add(Financial(list.visit_number,list.member_number,list.member_name,if(list.created_at == null) "N/A" else dateFormat.format(list.created_at),list.invoice_number,list.total_invoice_amount,list.provider_name,list.benefit_name,list.category,list.categoryDesc,list.status,list.visit_type, "N/A" ,(if(list.payer_status == null) "N/A" else list.payer_status),list.payer))
			plan_name=list.plan_name;
		}


		//load file and compile it
		val file = this.javaClass.getResourceAsStream("/templates/FinancialStatement.jrxml");
		val jasperReport: JasperReport = JasperCompileManager.compileReport(file)
		val dataSource = JRBeanCollectionDataSource(useList)
		val parameters: MutableMap<String, Any> = HashMap()
		parameters["scheme"]= plan_name.toString();
		parameters["time"] = timeFormat.format(LocalDateTime.now());
		parameters["fromdate"] = dateFormat.format(filters.startDate);
		parameters["todate"] = dateFormat.format(filters.endDate);
		val jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource)

		var excel: ByteArray? = null
		val exporter = JRXlsxExporter()
        val xlsReport = ByteArrayOutputStream()
		exporter.setExporterInput(SimpleExporterInput(jasperPrint))
		exporter.exporterOutput = SimpleOutputStreamExporterOutput(xlsReport)
		val configuration = SimpleXlsxReportConfiguration()
		configuration.isDetectCellType = true //Set configuration as you like it!!
		configuration.isCollapseRowSpan=true
		configuration.isForcePageBreaks=true
		configuration.isCellHidden=true
		configuration.isRemoveEmptySpaceBetweenRows=true
		configuration.isWrapText=true
		configuration.isRemoveEmptySpaceBetweenColumns=true
		configuration.isOnePagePerSheet=false
		configuration.isShowGridLines=true
		configuration.isShrinkToFit=false
		configuration.isIgnorePageMargins=false
		configuration.isWhitePageBackground=false
		configuration.setColumnWidthRatio(1.4f);


		exporter.setConfiguration(configuration)
		exporter.exportReport()
		excel = xlsReport.toByteArray();
		val generated_file = InputStreamResource(ByteArrayInputStream(excel))

		headers.add("Content-Disposition", "inline; filename=${plan_name.toString()}_${filters.startDate}_${filters.endDate}.xlsx")
		return ResponseEntity.ok().headers(headers).contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
				.body(generated_file)
	}


	@Transactional(readOnly = true)
	override fun getSchemeClinicalUtilizations(filters:ReportFilters): ResponseEntity<Resource> {
		val headers = HttpHeaders()
		var plan_name: String? =null;

		if(filters.startDate == null && filters.endDate != null)
		{
			//return ResultFactory.getFailResult("Please specify start date.")
			//headers.add("Content-Disposition", "inline; filename=Error_No_start_Date_Specified.xlsx")
			//return ResponseEntity.ok().headers(headers).contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
			//		.body(null)
			plan_name="No Data Found";
		}

		if(filters.startDate != null && filters.endDate == null)
		{
			filters.endDate= LocalDate.now();
		}

		if(filters.startDate == null && filters.endDate == null)
		{
			filters.endDate= LocalDate.now();
			filters.startDate= LocalDate.now().minusDays(30)
		}


		val useList: MutableList<Clinical> = mutableListOf();
		val listVisit:List<SchemeClinicalStatementDTO> = visitRepo.findSchemeClinicalUtilizations(filters.planId, filters.startDate!!, filters.endDate!!, filters.payerId);
		val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
		val timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss")


		if(listVisit.isEmpty())
		{
			//return ResultFactory.getFailResult("No records available.")
			//headers.add("Content-Disposition", "inline; filename=No Data Found_${filters.startDate}_${filters.endDate}.xlsx")
			//return ResponseEntity.ok().headers(headers).contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
			//		.body(null)
			plan_name="No Data Found";
		}

		for (list in listVisit)
		{	useList.add(Clinical(list.visit_number,list.member_number,list.member_name,if(list.created_at == null) "N/A" else dateFormat.format(list.created_at),list.invoice_number,if(list.icd10_code == null) "N/A" else list.icd10_code,if(list.icd10_title == null) "N/A" else list.icd10_title,((if(list.line_type == null) "" else list.line_type)+" "+(if(list.line_category == null) "" else list.line_category)+" "+list.description),list.quantity,list.unit_price,list.line_total,list.benefit_name,list.provider_name,list.payer))
			plan_name=list.plan_name;
		}


		//load file and compile it
		val file = this.javaClass.getResourceAsStream("/templates/ClinicalStatement.jrxml");
		//val file: File = ResourceUtils.getFile("classpath:templates\\ClinicalStatement.jrxml")
		val jasperReport: JasperReport = JasperCompileManager.compileReport(file)
		val dataSource = JRBeanCollectionDataSource(useList)
		val parameters: MutableMap<String, Any> = HashMap()
		parameters["scheme"]= plan_name.toString();
		parameters["time"] = timeFormat.format(LocalDateTime.now());
		parameters["fromdate"] = dateFormat.format(filters.startDate);
		parameters["todate"] = dateFormat.format(filters.endDate);
		val jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource)

		var excel: ByteArray? = null
		val exporter = JRXlsxExporter()
		val xlsReport = ByteArrayOutputStream()
		exporter.setExporterInput(SimpleExporterInput(jasperPrint))
		exporter.exporterOutput = SimpleOutputStreamExporterOutput(xlsReport)
		val configuration = SimpleXlsxReportConfiguration()
		configuration.isDetectCellType = true //Set configuration as you like it!!
		configuration.isCollapseRowSpan=true
		configuration.isForcePageBreaks=true
		configuration.isCellHidden=true
		configuration.isRemoveEmptySpaceBetweenRows=true
		configuration.isWrapText=true
		configuration.isRemoveEmptySpaceBetweenColumns=true
		configuration.isOnePagePerSheet=false
		configuration.isShowGridLines=true
		configuration.isShrinkToFit=false
		configuration.isIgnorePageMargins=false
		configuration.isWhitePageBackground=false
		configuration.setColumnWidthRatio(1.4f);

		exporter.setConfiguration(configuration)
		exporter.exportReport()
		excel = xlsReport.toByteArray();
		val generated_file = InputStreamResource(ByteArrayInputStream(excel))

		headers.add("Content-Disposition", "inline; filename=${plan_name.toString()}_${filters.startDate}_${filters.endDate}.xlsx")
		return ResponseEntity.ok().headers(headers).contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
				.body(generated_file)
	}


	@Transactional(readOnly = true)
	override fun getInvoiceProvider(invoiceDetails:InvoiceDetails): Result<ProviderDTO> {
		var provider = visitRepo.findProviderByInvoice(invoiceDetails.invoiceNumber!!,invoiceDetails.mainProvider!!)
		if(provider != null) {
			return ResultFactory.getSuccessResult(provider,"Provider obtained successfully")
		}
		else {
			return ResultFactory.getFailResult("Could not get provider")
		}
	}

	@Transactional(readOnly = false, rollbackFor = [Exception::class])
	override fun saveBillAndCloseVisit(billDTO: SaveAndCloseVisitDTO): Result<Visit>? {
		val visit = visitRepo.findById(billDTO.id)
		if (visit.isPresent) {
			val visitData = visit.get()
			val beneficiaryBenefit = beneficiaryBenefitRepository.findByAggregateIdAndBenefitIdAndMemberNumber(
				visitData.aggregateId,
				visitData.benefitId ?:0,
				visitData.memberNumber)
			if (beneficiaryBenefit?.benefitType == BenefitType.INSURED && visitData.balanceAmount < billDTO.totalInvoiceAmount) {
				return ResultFactory.getFailResult("You cannot bill more than ${visitData.balanceAmount}")
			}else if(beneficiaryBenefit?.benefitType == BenefitType.CAPITATION){
				if(beneficiaryBenefit.initialLimit > BigDecimal.ZERO && visitData.balanceAmount < billDTO.totalInvoiceAmount){
					return ResultFactory.getFailResult("You cannot bill more than ${visitData.balanceAmount}")
				}
			}
			if (billDTO.billingStation !== BillingStation.MULTIPLE) {
				for (d in billDTO.diagnosis!!) {
					val diagnosisItem = Diagnosis(
						code = d.code!!, title = d.title, visit = visit.get()
					)
					diagnosisRepository.save(diagnosisItem)
				}
			}
		}
		return visit.map {
			it.totalInvoiceAmount = billDTO.totalInvoiceAmount
			it.invoiceNumber = billDTO.invoiceNumber
			it.status = Status.CLOSED
			it.visitEnd = LocalDateTime.now()
			visitRepo.save(it)
			//Trigger apache camel
			producerTemplate.sendBody("direct:standalone-deduct-balance", mutableListOf(it))
			ResultFactory.getSuccessResult(it)
		}.orElseGet {
			ResultFactory.getFailResult("No such visit exists")
		}
	}

	@Transactional(rollbackFor = [Exception::class])
	override fun saveBillAndCloseVisitPortal(billDTO: SaveAndCloseVisitDTO): Result<Visit>? {
		val visit = visitRepo.findById(billDTO.id)
		val preauth = preAuthRepo.findByVisitNumber(billDTO.id)
		if (visit.isPresent) {
			val theVisit = visit.get()
//			check if preauth exists
			val billPreauth =preAuthService.consumePreauth(
				ConsumePreAuthDTO(
				amount = billDTO.totalInvoiceAmount,
				aggregateId = theVisit.aggregateId,
				benefitId = theVisit.benefitId!!,
				memberNumber = theVisit.memberNumber,
				visitNumber = theVisit.id

			))
			if(billPreauth.success){
				theVisit.apply {
					this.totalInvoiceAmount = billDTO.totalInvoiceAmount
					this.invoiceNumber = billDTO.invoiceNumber
					this.status = Status.CLOSED
					this.visitEnd = LocalDateTime.now()
				}
				visitRepo.save(theVisit)
				return ResultFactory.getSuccessResult(visit.get())
			}else if(!billPreauth.success && preauth!= null){
				return ResultFactory.getFailResult(billPreauth.msg)

			}else if (preauth == null){
				val beneficiaryBenefit = beneficiaryBenefitRepository.findByAggregateIdAndBenefitIdAndMemberNumber(
					theVisit.aggregateId,
					theVisit.benefitId,
					theVisit.memberNumber)

				if (beneficiaryBenefit?.benefitType == BenefitType.INSURED && theVisit.balanceAmount < billDTO.totalInvoiceAmount) {
					return ResultFactory.getFailResult("You cannot bill more than ${theVisit.balanceAmount}")
				}else if(beneficiaryBenefit?.benefitType == BenefitType.CAPITATION){
					if(beneficiaryBenefit.initialLimit > BigDecimal.ZERO && theVisit.balanceAmount < billDTO.totalInvoiceAmount){
						return ResultFactory.getFailResult("You cannot bill more than ${theVisit.balanceAmount}")
					}
				}
				val invoice = findByInvoiceNumberAndProvider(billDTO)
				if (b(
						invoice,
						billDTO,
						visit
					)
				) return ResultFactory.getFailResult("Invoice Number already exists, add a new invoice Number")

				return visit.map {
					it.totalInvoiceAmount = billDTO.totalInvoiceAmount
					it.invoiceNumber = billDTO.invoiceNumber
					it.status = Status.CLOSED
					it.visitEnd = LocalDateTime.now()
					visitRepo.save(it)
					producerTemplate.sendBody("direct:standalone-deduct-balance", mutableListOf(it))
					ResultFactory.getSuccessResult(it)
				}.orElseGet {
					ResultFactory.getFailResult("No such visit exists")
				}
			}else{
				return ResultFactory.getFailResult("")
			}
		}else{
			return ResultFactory.getFailResult("No such visit exists")
		}
	}

	private fun findByInvoiceNumberAndProvider(billDTO: SaveAndCloseVisitDTO): Optional<Invoice> {
		return invoiceRepo.findByInvoiceNumberAndHospitalProviderId(
			billDTO.invoiceNumber,
			billDTO.hospitalProviderId
		)
	}

	private fun b(
		invoice: Optional<Invoice>,
		billDTO: SaveAndCloseVisitDTO,
		visit: Optional<Visit>
	): Boolean {
		if (invoice.isPresent) {
			return true
		} else {

			if (billDTO.invoices!!.isNotEmpty()) {
				for (i in billDTO.invoices!!) {
					val newInvoice = Invoice(
						hospitalProviderId = billDTO.hospitalProviderId,
						invoiceNumber = i.invoiceNumber,
						totalAmount = BigDecimal.valueOf(i.amount!!),
						visit = visit.get(),
						invoiceLines = mutableSetOf(),
						service = i.service
					)
					invoiceRepo.save(newInvoice)
				}
			} else {
				val newBill = Invoice(
					hospitalProviderId = billDTO.hospitalProviderId,
					invoiceNumber = billDTO.invoiceNumber,
					visit = visit.get(),
					invoiceLines = mutableSetOf(),
					totalAmount = billDTO.totalInvoiceAmount,
					service = null
				)
				invoiceRepo.save(newBill)
			}
		}
		return false
	}

	override fun saveBillAndCloseMultipleStationsVisit(billDTO: SaveAndCloseVisitDTO): Result<Visit>? {
		val visit = visitRepo.findById(billDTO.id)
		if (visit.isPresent) {
			val visitData = visit.get()
			val beneficiaryBenefit = beneficiaryBenefitRepository.findByAggregateIdAndBenefitIdAndMemberNumber(
				visitData.aggregateId,
				visitData.benefitId ?:0,
				visitData.memberNumber)
			if (beneficiaryBenefit?.benefitType == BenefitType.INSURED && visit.get().balanceAmount < billDTO.totalInvoiceAmount) {
				return ResultFactory.getFailResult("You cannot bill more than ${visitData.balanceAmount}")
			}else if(beneficiaryBenefit?.benefitType == BenefitType.CAPITATION){
				if(beneficiaryBenefit.initialLimit > BigDecimal.ZERO && visitData.balanceAmount < billDTO.totalInvoiceAmount){
					return ResultFactory.getFailResult("You cannot bill more than ${visitData.balanceAmount}")
				}
			}

			if (billDTO.billingStation !== BillingStation.MULTIPLE) {
				for (d in billDTO.diagnosis!!) {
					val diagnosisItem = Diagnosis(
						code = d.code!!, title = d.title, visit = visit.get()
					)
					diagnosisRepository.save(diagnosisItem)
				}
			}
		}
		return visit.map {
			it.totalInvoiceAmount = billDTO.totalInvoiceAmount
			it.invoiceNumber = billDTO.invoiceNumber
			it.status = Status.CLOSED
			it.visitEnd = LocalDateTime.now()
			visitRepo.save(it)
			producerTemplate.sendBody("direct:standalone-deduct-balance", mutableListOf(it))
			ResultFactory.getSuccessResult(it)
		}.orElseGet {
			ResultFactory.getFailResult("No such visit exists")
		}
	}


	override fun saveIcd10CompleteFile(file: MultipartFile): Result<List<Icd10code>> {

		val icd10codes: List<Icd10code>? = excelHelper.excelToicd10codes(file.inputStream)
		if (icd10codes != null) {
			icD10Repository.saveAll(icd10codes.toList())
		}
		return ResultFactory.getSuccessResult(msg = "Successfully saved codes")
	}

	override fun saveMedicalProcedureFile(file: MultipartFile): Result<List<MedicalProcedure>> {
		val medicalProcedures: List<MedicalProcedure>? =
			excelHelper.excelToMedicalProcedure(file.inputStream)
		if (medicalProcedures != null) {
			medicalProcedureRepository.saveAll(medicalProcedures.toList())
		}
		return ResultFactory.getSuccessResult(msg = "Successfully saved medical procedures")
	}

	override fun searchMedicalProcedure(
		search: String?,
		page: Int,
		size: Int
	): Result<Page<MedicalProcedure>> {
		val request = PageRequest.of(page - 1, size)
		val results = medicalProcedureRepository.findByProcedureCodeORProcedureDescriptionLike(
			search = "%${search}%",
			request
		)
		return ResultFactory.getSuccessResult(results)
	}

	override fun saveMedicalDrugFile(file: MultipartFile): Result<List<MedicalDrug>> {
		val medicalDrugs: List<MedicalDrug>? = excelHelper.excelToMedicalDrugs(file.inputStream)
		if (medicalDrugs != null) {
			medicalDrugRepository.saveAll(medicalDrugs.toList())
		}
		return ResultFactory.getSuccessResult(msg = "Successfully saved medical Drugs")
	}

	override fun saveLaboratoryFile(file: MultipartFile): Result<List<Laboratory>> {
		val laboratory: List<Laboratory>? = excelHelper.excelToLaboratory(file.inputStream)
		if (laboratory != null) {
			laboratoryRepository.saveAll(laboratory.toList())
		}
		return ResultFactory.getSuccessResult(msg = "Successfully saved Laboratory items")
	}

	override fun saveOtherBenefitDetailFile(file: MultipartFile): Result<List<OtherBenefitDetail>> {
		val otherBenefitDetail: List<OtherBenefitDetail>? = excelHelper.excelToOtherBenefitDetail(
			file
				.inputStream
		)
		if (otherBenefitDetail != null) {
			otherBenefitDetailRepository.saveAll(otherBenefitDetail.toList())
		}
		return ResultFactory.getSuccessResult(msg = "Successfully saved Other Benefit Items")
	}

	override fun searchMedicalDrugs(
		search: String,
		page: Int,
		size: Int
	): Result<Page<MedicalDrug>> {
		val request = PageRequest.of(page - 1, size)
		val results = medicalDrugRepository.findByNameLike(search = "%${search}%", request)
		return ResultFactory.getSuccessResult(results)
	}

	override fun searchLaboratory(search: String, page: Int, size: Int): Result<Page<Laboratory>> {
		val request = PageRequest.of(page - 1, size)
		val results = laboratoryRepository.findByNameLike(search = "%${search}%", request)
		return ResultFactory.getSuccessResult(results)
	}


	override fun saveRadiologyFile(file: MultipartFile): Result<List<Radiology>> {
		val radiologyList: List<Radiology>? = excelHelper.excelToRadiology(file.inputStream)
		if (radiologyList != null) {
			radiologyRepository.saveAll(radiologyList.toList())
		}
		return ResultFactory.getSuccessResult(msg = "Successfully saved Radiology")
	}

	override fun searchRadiology(search: String, page: Int, size: Int): Result<Page<Radiology>> {
		val request = PageRequest.of(page - 1, size)
		val results = radiologyRepository.findByDetailLike(search = "%${search}%", request)
		return ResultFactory.getSuccessResult(results)
	}

	override fun searchOtherBenefitDetail(
		search: String?,
		page: Int,
		size: Int
	): Result<Page<OtherBenefitDetail>> {
		val request = PageRequest.of(page - 1, size)
		val results =
			otherBenefitDetailRepository.findByBenefitDetailLike(search = "%${search}%", request)
		return ResultFactory.getSuccessResult(results)
	}

	@Transactional(readOnly = true)
	override fun searchIcd10CodeByTitle(
		search: String,
		page: Int,
		size: Int
	): Result<Page<Icd10code>> {
		val request = PageRequest.of(page - 1, size)
		val results = icD10Repository.findByTitleLike(search = "%${search}%", request)
		return ResultFactory.getSuccessResult(results)
	}

	@Transactional(readOnly = true)
	override fun searchIcd10CodeByCode(
		search: String,
		page: Int,
		size: Int
	): Result<Page<Icd10code>> {
		val request = PageRequest.of(page - 1, size)
		val results = icD10Repository.findByCodeLike(search = "%${search}%", request)
		return ResultFactory.getSuccessResult(results)
	}


	@Transactional(readOnly = true, rollbackFor = [Exception::class])
	override fun getVisitById(id: Long): Result<Visit> {
		val visit: Visit = visitRepo.findById(id).get()

		return ResultFactory.getSuccessResult(visit)
	}

	@Transactional(readOnly = true, rollbackFor = [Exception::class])
	override fun getLineItemsByInvoiceNumber(invoiceNumber: String,providerId: Long):
			Result<MutableList<InvoiceLine>?> {
		val invoice: Invoice =
			invoiceRepo.findByInvoiceNumberAndHospitalProviderId(invoiceNumber.trim(), providerId)
				.get()
		val invoiceLines = mutableListOf<InvoiceLine>()
		val invoiceLinesList = invoice.invoiceLines
		for (line in invoiceLinesList) {
			val invoiceLine = InvoiceLine(
				description = line.description.toString(),
				quantity = line.quantity,
				invoice = invoice,
				invoiceNumber = invoiceNumber,
				lineCategory = null,
				lineTotal = line.lineTotal,
				lineType = line.lineType,
				unitPrice = line.unitPrice
			)
			invoiceLines.add(invoiceLine)
		}
		return ResultFactory.getSuccessResult(invoiceLines)
	}

	override fun getInvoiceProcedureCodeByInvoiceNumber(invoiceNumber: String): Result<MutableList<InvoiceNumberProcedureCode>?> {
		val invoice: Invoice =
			invoiceRepo.findByInvoiceNumber(invoiceNumber.trim()).get()
		return if(!invoice.invoiceNumber.isNullOrBlank()){
			val InvoiceNumberProcedureCodeList = mutableListOf<InvoiceNumberProcedureCode>()
			val invoiceProcedureCodes: MutableList<InvoiceNumberProcedureCode> = invoiceProcedureCodeRepo.findByInvoiceNumber(invoiceNumber.trim())
			ResultFactory.getSuccessResult(invoiceProcedureCodes)

		}else{
			ResultFactory.getFailResult("No Procedures for $invoiceNumber.Try with another " +
					"invoice number!")
		}
	}

	override fun getLineItemsByVisitNumber(visitNumber: Long): Result<MutableList<InvoiceLine>?> {

		val theVisit: Visit = visitRepo.findById(visitNumber).get()

		val invoiceLines = mutableListOf<InvoiceLine>()
		if (!theVisit.invoiceNumber.isNullOrBlank()) {
			val invoiceList: MutableList<Invoice> = invoiceRepo.findByVisitNumber(
				theVisit
			)
			for (invoice in invoiceList) {
				val invoicing = invoice.invoiceLines
				for (line in invoicing) {
					val invoiceLine = InvoiceLine(
						description = line.description.toString(),
						quantity = line.quantity,
						invoice = invoice,
						unitPrice = line.unitPrice,
						lineType = line.lineType,
						lineTotal = line.lineTotal,
						lineCategory = null,
						invoiceNumber = line.invoiceNumber
					)
					invoiceLines.add(invoiceLine)
				}
			}


		}

		return ResultFactory.getSuccessResult(invoiceLines)

	}

	override fun getLineItemsByInvoiceNumberAndVisit(
		invoiceNumber: String,
		visitNumber: Long
	): Result<MutableList<InvoiceLine>?> {
		val theVisit: Visit = visitRepo.findById(visitNumber).get()
		val invoiceOptional = invoiceRepo.findByInvoiceNumberAndVisit(invoiceNumber,theVisit)
		val invoiceLines = mutableListOf<InvoiceLine>()
		if(invoiceOptional.isPresent){
			return ResultFactory.getSuccessResult(data = invoiceOptional.get().invoiceLines.toMutableList())

		}
		return ResultFactory.getSuccessResult(invoiceLines)
	}

	@Transactional(readOnly = true, rollbackFor = [Exception::class])
	override fun getDiagnosisByVisitId(id: Long): Result<MutableList<Diagnosis>?> {
		val visit = visitRepo.findById(id)
		return if (visit.isPresent) {

			val diagnosis: MutableList<Diagnosis> = diagnosisRepository
				.findByVisit(visit.get())

			ResultFactory.getSuccessResult(diagnosis)
		} else {
			ResultFactory.getFailResult("No visit with id $id was found")
		}
	}

	override fun updateVisitMiddlewareStatus(dto: UpdateMiddlewareStatusDTO): Result<Boolean> {
		val visit = visitRepo.findByAggregateIdAndBenefitNameAndMemberNumberAndId(
			dto.aggregateId,
			dto.benefitName, dto.memberNumber, dto.visitNumber
		)
		visit.ifPresent {
			it.apply {
				middlewareStatus = MiddlewareStatus.SENT
				status = Status.ACTIVE
			}
			visitRepo.save(it)
		}
		return ResultFactory.getSuccessResult(data = true)
	}

	@Transactional(rollbackFor = [Exception::class])
	override fun saveUnSuccessfulCloseClaimRef(dto: IntegratedUnsuccessfulClaimCloseDTO): Result<Boolean> {
		val unsuccessfulCloseClaimRef = claimCloseFailedRepo.findByClaimRef(dto.claimRef)

		return if(!unsuccessfulCloseClaimRef.isPresent){
			val claim = ClaimCloseFailed(
				claimRef = dto.claimRef, provider = dto.provider.toString(), closed = false
					.toString(), reason = dto.reason, memberNumber = dto.memberNumber
			)
			claimCloseFailedRepo.save(claim)
			ResultFactory.getSuccessResult(true)
		}else{
			ResultFactory.getSuccessResult(false)
		}


	}

	fun reopenStandaloneMultipleProviderVisitSetToActive(dto: ExistingTodayVisitDTO): Visit? {
		var theVisit: Visit? = null
		val visit = visitRepo.findByAggregateIdAndBenefitNameAndMemberNumberAndId(
			dto.aggregateId,
			dto.benefitName, dto.memberNumber, dto.visitNumber
		)
		visit.ifPresent {
			it.apply {
				middlewareStatus = MiddlewareStatus.SENT
				status = Status.ACTIVE
			}
			theVisit = visitRepo.save(it)
		}

		return theVisit
	}

	fun reopenIntergratedProviderVisitSetToInactive(dto: ExistingTodayVisitDTO): Visit? {
		var theVisit: Visit? = null
		val visit = visitRepo.findByAggregateIdAndBenefitNameAndMemberNumberAndId(
			dto.aggregateId,
			dto.benefitName, dto.memberNumber, dto.visitNumber
		)
		visit.ifPresent {
			it.apply {
				middlewareStatus = MiddlewareStatus.SENT
				status = Status.INACTIVE
			}
			theVisit = visitRepo.save(it)
		}

		return theVisit
	}

	@Scheduled(cron = "0 0 1 * * *")
	@SchedulerLock(name = "setMultipleTypeVisitToClosed")
	fun shortRunningTask() {
		visitRepo.updateMultipleTypeVisitToClosed()

	}



}
