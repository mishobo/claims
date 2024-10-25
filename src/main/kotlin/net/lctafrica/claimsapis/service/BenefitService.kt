package net.lctafrica.claimsapis.service

import net.lctafrica.claimsapis.dto.*
import net.lctafrica.claimsapis.dto.InvoiceLine
import net.lctafrica.claimsapis.repository.*
import net.lctafrica.claimsapis.util.*
import org.apache.camel.ProducerTemplate
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*


@Service
@Transactional
class BenefitService(
	val repo: BeneficiaryBenefitRepository,
	val visitRepo: VisitRepository,
	val diagnosisRepository: DiagnosisRepository,
	val doctorRepository: DoctorRepository,
	val invoiceRepo: InvoiceRepository,
	val invoiceLineRepository: InvoiceLineRepository,
	val invoiceProcedureCodeRepo: InvoiceProcedureCodeRepo,
	val producerTemplate: ProducerTemplate
) :
	IBenefitService {


	var logger: Logger = LoggerFactory.getLogger(this.javaClass)

	@Value("\${lct-africa.notification.base-url}")
	lateinit var notificationUrl: String

	@Value("\${lct-africa.notification.sms-endpoint}")
	lateinit var smsEndPoint: String

	@Value("\${lct-africa.member.search.url}")
	lateinit var memberSearchUrl: String

	@Value("\${lct-africa.claims.url}")
	lateinit var claimsUrl: String

	@Value("\${lct-africa.claims.get_open_transactions.url}")
	lateinit var getOpenTransactions: String

	@Value("\${lct-africa.claims.close_open_transaction.url}")
	lateinit var closeOpenTransaction: String


	@Transactional(rollbackFor = [Exception::class])
	override fun addNew(dto: CreateBenefitDTO): Result<Boolean> {

		if (dto.manualSubBenefitParentId !== null) {
			val benBenefitOptional = repo.findMainBenefit(
				dto.categoryId, dto
					.manualSubBenefitParentId!!, dto.aggregateId
			)

			dto.beneficiaries.map {
				val parent = BeneficiaryBenefit(
					aggregateId = dto.aggregateId,
					benefitId = dto.benefitId,
					benefitName = dto.benefitName,
					memberNumber = it.memberNumber,
					status = BeneficiaryBenefit.BenefitStatus.valueOf(dto.status),
					balance = dto.balance,
					initialLimit = dto.balance,
					suspensionThreshold = dto.suspensionThreshold,
					categoryId = dto.categoryId,
					startDate = dto.startDate,
					endDate = dto.endDate,
					beneficiaryId = it.id,
					utilization = BigDecimal.ZERO,
					payerId = dto.payer.payerId,
					parent = benBenefitOptional[0],
					memberName = it.name,
					subBenefits = mutableSetOf(),
					gender = it.gender,
					memberType = it.beneficiaryType,
					catalogId = dto.catalogId,
					jicEntityId = dto.jicEntityId,
					apaEntityId = dto.apaEntityId,
					benefitType = dto.benefitType,
					capitationType = dto.capitationType,
					capitationPeriod = dto.capitationPeriod,
					capitationMaxVisitCount = dto.capitationMaxVisitCount,
					requireBeneficiaryToSelectProvider=dto.requireBeneficiaryToSelectProvider,
					daysOfAdmissionLimit = dto.daysOfAdmissionLimit,
					amountPerDayLimit = dto.amountPerDayLimit

					)
				repo.save(parent)
			}

		} else {
			val all = mutableSetOf<BeneficiaryBenefit>()
			dto.beneficiaries.map {
				val parent = BeneficiaryBenefit(
					aggregateId = dto.aggregateId,
					benefitId = dto.benefitId,
					benefitName = dto.benefitName,
					memberNumber = it.memberNumber,
					status = BeneficiaryBenefit.BenefitStatus.valueOf(dto.status),
					balance = dto.balance,
					initialLimit = dto.balance,
					suspensionThreshold = dto.suspensionThreshold,
					categoryId = dto.categoryId,
					startDate = dto.startDate,
					endDate = dto.endDate,
					beneficiaryId = it.id,
					utilization = BigDecimal.ZERO,
					payerId = dto.payer.payerId,
					parent = null,
					memberName = it.name,
					subBenefits = mutableSetOf(),
					gender = it.gender,
					memberType = it.beneficiaryType,
					catalogId = dto.catalogId,
					jicEntityId = dto.jicEntityId,
					apaEntityId = dto.apaEntityId,
					benefitType = dto.benefitType,
					capitationType = dto.capitationType,
					capitationPeriod = dto.capitationPeriod,
					capitationMaxVisitCount = dto.capitationMaxVisitCount,
					requireBeneficiaryToSelectProvider=dto.requireBeneficiaryToSelectProvider,
					daysOfAdmissionLimit = dto.daysOfAdmissionLimit,
					amountPerDayLimit = dto.amountPerDayLimit
					)
				repo.save(parent)
				all.add(parent)
				return@map parent
			}.map { parent ->
				val subs = mutableSetOf<BeneficiaryBenefit>()

				dto.subBenefits
					.filter { genderPasses(parent.gender, it.gender) }
					.filter { typePasses(parent.memberType, it.memberType) }
					.forEach {
						val sub = BeneficiaryBenefit(
							parent = parent,
							aggregateId = parent.aggregateId,
							benefitId = it.benefitId,
							benefitName = it.name,
							memberNumber = parent.memberNumber,
							status = BeneficiaryBenefit.BenefitStatus.valueOf(dto.status),
							balance = it.balance,
							initialLimit = it.balance,
							suspensionThreshold = it.suspensionThreshold,
							categoryId = parent.categoryId,
							startDate = it.startDate,
							endDate = it.endDate,
							beneficiaryId = parent.beneficiaryId,
							utilization = BigDecimal.ZERO,
							payerId = parent.payerId,
							memberName = parent.memberName,
							subBenefits = mutableSetOf(),
							gender = parent.gender,
							memberType = parent.memberType,
							catalogId = it.catalogId,
							jicEntityId = dto.jicEntityId,
							apaEntityId = dto.apaEntityId,
							benefitType = dto.benefitType,
							capitationType = dto.capitationType,
							capitationPeriod = dto.capitationPeriod,
							capitationMaxVisitCount = dto.capitationMaxVisitCount,
							requireBeneficiaryToSelectProvider=dto.requireBeneficiaryToSelectProvider,
							daysOfAdmissionLimit = dto.daysOfAdmissionLimit,
							amountPerDayLimit = dto.amountPerDayLimit
						)
						subs.add(sub)
					}
				repo.saveAll(subs)
				all.addAll(subs)
			}
			logger.info(all.toString())
		}


		return ResultFactory.getSuccessResult(true)
	}

	private fun genderPasses(memberGender: Gender, benefitGender: ApplicableGender): Boolean {
		if (benefitGender == ApplicableGender.ALL) return true
		if (benefitGender.name == memberGender.name) return true
		return false
	}

	private fun typePasses(memberType: MemberType, applicableMember: ApplicableMember): Boolean {

		return when (applicableMember) {
			ApplicableMember.ALL -> true
			ApplicableMember.PRINCIPAL -> memberType == MemberType.PRINCIPAL
			ApplicableMember.SPOUSE -> memberType == MemberType.SPOUSE
			ApplicableMember.CHILD -> memberType == MemberType.CHILD
			ApplicableMember.PARENT -> memberType == MemberType.PARENT
			ApplicableMember.PRINCIPAL_AND_SPOUSE -> {
				(memberType == MemberType.PRINCIPAL || memberType == MemberType.SPOUSE)
			}
		}

	}

	@Transactional(readOnly = true)
	override fun findActiveByMemberNumber(memberNumber: String): Result<List<BeneficiaryBenefit>> {
		val benefits =
			repo.findActiveByMemberNumber(memberNumber, BeneficiaryBenefit.BenefitStatus.ACTIVE)
		return ResultFactory.getSuccessResult(benefits)
	}

	override fun findActiveByBeneficiaryIdProviderId(
		beneficiaryId: Long,
		providerId: Long
	): Result<List<BeneficiaryBenefit>> {

		val benefits =
			repo.findActiveByBeneficiaryId(beneficiaryId, BeneficiaryBenefit.BenefitStatus.ACTIVE)

		val membershipClient = WebClient.builder()
			.baseUrl(memberSearchUrl).build()
		val filteredBenefits:MutableList<BeneficiaryBenefit> =  mutableListOf<BeneficiaryBenefit>()

		for (benefit in benefits) {

			val mappingResult = membershipClient
				.get()
				.uri { u ->
					u
						.path("/api/v1/membership/mapping")
						.queryParam("providerId", providerId)
						.queryParam("payerId", benefit.payerId)
						.build()
				}
				.retrieve()
				.bodyToMono(String::class.java)
				.block()

			val mapRes = gson.fromJson(mappingResult.toString(), PayerProviderMapRes::class.java)
			println(mapRes)

			if(mapRes.success){
				filteredBenefits.add(benefit)
			}
		}
		return if(filteredBenefits.size<1){
			ResultFactory.getFailResult(msg = "Member is not eligible to get services in this " +
					"facility, Contact the Insurer")
		}else{
			ResultFactory.getSuccessResult(filteredBenefits)
		}
	}

	override fun findActiveByBeneficiaryId(beneficiaryId: Long): Result<List<BeneficiaryBenefit>> {

		val benefits =
			repo.findActiveByBeneficiaryId(beneficiaryId, BeneficiaryBenefit.BenefitStatus.ACTIVE)

		return ResultFactory.getSuccessResult(benefits)
	}

	override fun findByBeneficiaryId(beneficiaryId: Long): Result<List<BeneficiaryBenefit>> {
		val benefits =
			repo.findAllByBeneficiaryId(beneficiaryId)
		return ResultFactory.getSuccessResult(benefits)
	}

	@Transactional(rollbackFor = [Exception::class])
	override fun consumeBenefit(dto: ConsumeBenefitDTO): Result<Boolean> {
		val visit = dto.visitNumber?.let { visitRepo.findById(it) }?.get()
		val benefits = repo.findAllByAggregateId(dto.aggregateId, dto.benefitId)
		if (benefits.size < 1) {
			return ResultFactory.getFailResult("No benefit with aggregate id and benefit id")
		}
		val others = repo.findMainByAggregateWithoutId(dto.aggregateId, dto.benefitId)
		val tracker = mutableListOf<Long>()

		val result = computeBalance(benefits, dto.amount)
		if (result.success) {
			suspendBenefit(benefits)
			for (benefit in benefits) {
				if (benefit.parent != null) {
					tracker.add(benefit.parent.id)
					alignChildBalances(benefit.parent)
				} else {
					alignChildBalances(benefit)
				}
			}

			val finalList = others.filterNot { b -> tracker.contains(b.id) }.toMutableSet()
			val resultTwo = computeBalance(finalList, dto.amount)
			if (resultTwo.success) {
				suspendBenefit(finalList)
				finalList.map { o -> alignChildBalances(o) }
				repo.saveAll(benefits)
				repo.saveAll(finalList)

			}
			//removal of zero check will send multiple sms to client!
			if (visit?.visitType == VisitType.ONLINE && dto.amount !== BigDecimal.ZERO) {
				//send utilization SMS
				Thread { sendUtilizationSMS(dto) }.start()
			}
		} else {

			return ResultFactory.getFailResult("Amount of ${dto.amount} more than Member Balance ")
		}

		return ResultFactory.getSuccessResult(true)
	}

	override fun reverseBenefit(dto: ReverseBenefitDTO): Result<Boolean> {
		val benefits = repo.findAllByAggregateId(dto.aggregateId, dto.benefitId)
		if (benefits.size < 1) {
			return ResultFactory.getFailResult("No benefit with aggregate id and benefit id")
		}
		val others = repo.findMainByAggregateWithoutId(dto.aggregateId, dto.benefitId)
		val tracker = mutableListOf<Long>()

		computeReverseBalance(benefits, dto.amount)
		for (benefit in benefits) {
			if (benefit.parent != null) {
				tracker.add(benefit.parent.id)
				alignChildBalances(benefit.parent)
			} else {
				alignChildBalances(benefit)
			}
		}

		val finalList = others.filterNot { b -> tracker.contains(b.id) }.toMutableSet()
		computeReverseBalance(finalList, dto.amount)
		finalList.map { o -> alignChildBalances(o) }
		repo.saveAll(benefits)
		repo.saveAll(finalList)

//		send Reverse sms
		return ResultFactory.getSuccessResult(true)
	}


	private fun computeBalance(b: MutableSet<BeneficiaryBenefit>, amount: BigDecimal): Result<
			MutableSet<BeneficiaryBenefit>> {
		var response = false;

		b.map {

			if (amount > it.balance) {
				response = false
			} else {
				it.balance = it.balance.subtract(amount)
				it.utilization += amount
				it.parent?.let { p ->
					p.balance = p.balance.subtract(amount)
					p.utilization += amount
				}
				response = true
			}


		}
		return if (response) {
			ResultFactory.getSuccessResult(data = b, msg = "")
		} else {
			ResultFactory.getFailResult(data = b, msg = "")
		}


	}

	private fun computeReverseBalance(b: MutableSet<BeneficiaryBenefit>, amount: BigDecimal) {
		b.map {
			it.balance = it.balance.add(amount)
			it.utilization -= amount
			it.parent?.let { p ->
				p.balance = p.balance.add(amount)
				p.utilization -= amount
			}
		}
	}

	private fun suspendBenefit(b: MutableSet<BeneficiaryBenefit>) {
		b.filter {
			it.suspensionThreshold >= it.balance
		}.map {
			it.status = BeneficiaryBenefit.BenefitStatus.SUSPENDED
			it.subBenefits.forEach { s ->
				s.status = BeneficiaryBenefit.BenefitStatus.SUSPENDED
			}
		}
	}

	private fun alignChildBalances(p: BeneficiaryBenefit) {

		p.subBenefits.filter { c ->
			c.balance > p.balance
		}.map {
			it.balance = p.balance
		}

	}

	private fun sendUtilizationSMS(dto: ConsumeBenefitDTO) {
		val membershipClient = WebClient.builder()
			.baseUrl(memberSearchUrl).build()

		val notificationClient = WebClient.builder()
			.baseUrl(notificationUrl).build()

		val benBen = repo.findByAggregateIdAndBenefitIdAndMemberNumber(
			aggregateId = dto.aggregateId,
			benefitId = dto.benefitId,
			memberNumber = dto.memberNumber
		)

		val date = LocalDate.now()
		val localTime = LocalTime.now(ZoneId.of("Africa/Nairobi"))
		val dateTimeFormatter = DateTimeFormatter.ofPattern("hh:mm a")

		val payerResponse = membershipClient
			.get()
			.uri { u ->
				u
					.path("/api/v1/membership/payers/${benBen!!.payerId}/payer")
					.build()
			}
			.retrieve()
			.bodyToMono(String::class.java)
			.block()
		val payer = gson.fromJson(payerResponse, PayerRes::class.java)

		val visit = dto.visitNumber?.let { visitRepo.findById(it) }

		val providerRes = membershipClient
			.get()
			.uri { u ->
				u
					.path("/api/v1/provider")
					.queryParam("providerId", visit?.get()?.hospitalProviderId)
					.build()
			}
			.retrieve()
			.bodyToMono(String::class.java)
			.block()
		val provider = gson.fromJson(providerRes, ProviderRes::class.java)
		val providerName = provider.data.name.toString().trim()
		val benefitName = benBen!!.benefitName.trim()
		val msg =
			"""Dear ${benBen.memberName}, you have utilized KES ${dto.amount} at $providerName on $date  at ${
				localTime.format(dateTimeFormatter)
			} for $benefitName.
			Queries ${payer.data.contact}. To view your balance, download LCT Africa app on Android (https://shorturl.at/rGKV5) or iOS (https://shorturl.at/DFOW0)""".trimIndent()
		val response = membershipClient
			.get()
			.uri { u ->
				u
					.path("/api/v1/membership/beneficiaries/UtilizationSms")
					.queryParam("search", dto.memberNumber)
					.build()
			}
			.retrieve()
			.bodyToMono(String::class.java)
			.block()

		val member = gson.fromJson(response, MemRes::class.java)

		if (member.success) {
			//todo make phone number available in the visit to avoid this reckless danger
			val tel = member.data[0].phoneNumber

			if (visit?.get()?.visitType == VisitType.ONLINE) {
				if (!tel.isNullOrEmpty() && !tel.isNullOrBlank()) {
					notificationClient
						.post()
						.uri { u ->
							u
								.path(smsEndPoint)
								.build()
						}
						.header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.body(
							Mono.just(gson.toJson(SmsDTO(phone = tel, msg = msg))),
							String::class.java
						)
						.exchange()
						.doOnError { res ->

						}
						.doOnSuccess { res ->

						}
						.block()!!.toString()
				}
			}

		}

	}

	override fun closeClaimFromIntegratedFacilityWeb(dto: CloseIntergratedWebClaimDTO):
			Result<Visit> {
		val visit =
			visitRepo.findById(dto.encounterId!!)
		return if (visit.isEmpty) {
			ResultFactory.getFailResult(
				"No such visit with visit id ${dto.encounterId}"
			)
		} else {

			visit.get().apply {
				this.claimProcessStatus = ClaimProcessStatus.PROCESSED
				this.status = Status.CLOSED
				this.totalInvoiceAmount = dto.amount
				this.invoiceNumber = dto.invoiceNumber
				this.visitEnd = LocalDateTime.now()
			}
			return ResultFactory.getSuccessResult(visitRepo.save(visit.get()))


		}
	}

	override fun checkVisitClaimStatus(dto: CloseIntergratedWebClaimDTO): Result<Visit?> {
		val claimsClient = WebClient.builder()
			.baseUrl(claimsUrl).build()
		val visit = visitRepo.findByIdAndMemberNumber(dto.encounterId!!, dto.memberNumber!!)

		return if (visit.isPresent) {
			ResultFactory.getSuccessResult(visit.get())
		} else {
			/*println("No visit found with id  ${dto.encounterId} and member number " +dto
				.memberNumber)*/
			claimsClient
				.post()
				.uri { u ->
					u
						.path("/api/v1/visit/integrated/saveUnSuccessfulCloseClaimRef")
						.build()
				}.header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.bodyValue(
					IntegratedUnsuccessfulClaimCloseDTO
						(
						claimRef = dto.claimRef!!,
						provider = dto.providerName.toString(),
						memberNumber = dto.memberNumber!!,
						reason = "No visit found with id  ${dto.encounterId} and member number " +
								dto.memberNumber
					)
				)
				.retrieve()
				.bodyToMono(String::class.java)
				.block()
			ResultFactory.getFailResult(
				msg = "No visit found with id  ${dto.encounterId} and " +
						"member number " + dto.memberNumber
			)


		}

	}

	override fun checkIntegratedProviderInvoice(dto: IntergratedInvoiceProviderDTO):
			Result<Boolean> {

		val visit = visitRepo.findById(dto.encounterId)
		val existingInvoice = invoiceRepo.findByInvoiceNumberAndHospitalProviderId(
			dto
				.invoiceNumber, visit.get().hospitalProviderId!!
		)
		if (existingInvoice.isPresent && visit.get().hospitalProviderId!! == existingInvoice.get().visit.hospitalProviderId) {
			return ResultFactory.getSuccessResult(
				data = true, msg = "Provider already has an " +
						"existing invoice with invoice Number ${dto.invoiceNumber}"
			)
		} else {
			ResultFactory.getFailResult(data = false, msg = "No such invoice for provider ")
		}
		return ResultFactory.getFailResult(data = false, msg = "")

	}


	override fun saveUnsuccessfulClaim(dto: IntegratedUnsuccessfulClaimCloseDTO): Result<Boolean> {
		val claimsClient = WebClient.builder()
			.baseUrl(claimsUrl).build()
		claimsClient
			.post()
			.uri { u ->
				u
					.path("/api/v1/visit/integrated/saveUnSuccessfulCloseClaimRef")
					.build()
			}.header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
			.bodyValue(
				IntegratedUnsuccessfulClaimCloseDTO
					(
					claimRef = dto.claimRef,
					provider = dto.provider.toString(),
					memberNumber = dto.memberNumber,
					reason = dto.reason
				)
			)
			.exchange()
			.doOnSuccess { res ->

			}
			.doOnError { res ->
			}

			.block()

		return ResultFactory.getSuccessResult(data = true)

	}

	override fun saveClaimFromMiddleware(dto: ClaimRes): Result<Boolean> {


		var responseResult: Boolean = false;
		var responseError: String? = "";

		val claimRef: String? = dto.claimRef

		val memberNumber: String = dto.memberNumber
		val aggregateId: String = dto.aggregateId
		val benefitId: Long = dto.benefitId
		val invoiceDetail: List<InvoiceDetail>? = dto.invoiceDetails
		val doctorDetail: List<DoctorDetail>? = dto.doctorDetails
		val diagnosisDetail: List<DiagnosisDetail>? = dto.diagnosisDetails
		val procedureCodes: List<ProcedureCode>? = dto.procedureCodes

		val invoiceNumber: String? = dto.invoiceDetails!![0].invoiceNumber

		val invoiceAmount = invoiceDetail!![0].invoiceAmount

		if (invoiceAmount!! <= BigDecimal.ZERO) {
			return ResultFactory.getFailResult(
				msg = "$invoiceNumber Invoice amount cannot be " +
						"less than zero " +
						"${dto.deviceId} " + dto.claimRef
			)
		}


		if (!dto.deviceId.equals("MATER")) {
			val invoiceExists = checkIntegratedProviderInvoice(
				IntergratedInvoiceProviderDTO(
					encounterId = dto.encounterId,
					invoiceNumber = invoiceNumber!!,
					providerName = dto.deviceId!!
				)
			)

			if (invoiceExists.data!!) {
				return ResultFactory.getFailResult(
					msg = "Invoice $invoiceNumber already exists for this provider " +
							"${dto.deviceId} " + dto.claimRef
				)
			}
		}


		val claimStatus = checkVisitClaimStatus(
			CloseIntergratedWebClaimDTO(
				encounterId = dto.encounterId,
				invoiceAmount!!,
				invoiceNumber = invoiceNumber!!,
				providerName = dto.deviceId!!,
				claimRef = dto.claimRef!!,
				memberNumber = dto.memberNumber

			)
		)


		if (claimStatus.data!!.status!! == Status.ACTIVE || claimStatus.data.status!! == Status.CLOSED
		) {
			val visit = visitRepo.findById(dto.encounterId)
			val getVisit = visit.get()

			val invoicePresent = invoiceRepo.findByClaimRef(claimRef!!)

			if (invoicePresent.isEmpty()) {
//
				val benefitResponse = consumeBenefit(
					ConsumeBenefitDTO(
						amount = invoiceAmount!!,
						aggregateId = aggregateId,
						benefitId = benefitId,
						memberNumber = memberNumber,
						visitNumber = getVisit.id

					)
				)

				if (benefitResponse.success) {

					//save invoice
					val newInvoice = Invoice(
						hospitalProviderId = null,
						invoiceNumber = invoiceNumber,
						visit = visit.get(),
						invoiceLines = mutableSetOf(),
						totalAmount = invoiceAmount,
						claimRef = claimRef,
						service = null

					)
					val savedInvoice = invoiceRepo.save(newInvoice)
					//										Save diagnosis
					if (diagnosisDetail !== null) {
						for (d in diagnosisDetail) {
							val diagnosisItem = Diagnosis(
								code = d.code.toString(),
								title = d.name.toString(),
								invoiceNumber = invoiceNumber,
								visit = getVisit
							)
							diagnosisRepository.save(diagnosisItem)
						}
					}


//					save procedure codes if any
					if (procedureCodes !== null) {
						for (d in procedureCodes) {
							val procedure = InvoiceNumberProcedureCode(
								procedure_code = d.code,
								procedure_description = d.name,
								invoiceNumber = invoiceNumber

							)
							invoiceProcedureCodeRepo.save(procedure)
						}

					}

					//	                                    Save Invoice lines
					if (invoiceDetail !== null) {
						for (d in invoiceDetail) {
							val invoicelines = d.invoiceLines
							for (line in invoicelines) {
								val invoiceline = InvoiceLine(
									description = line.billName,
									lineTotal = line.unitPrice!! * line.quantity!!,
									invoiceNumber = invoiceNumber,
									invoice = savedInvoice,
									lineType = null,
									claimRef = claimRef,
									lineCategory = line.billCategory,
									unitPrice = line.unitPrice!!,
									quantity = line.quantity!!

								)
								invoiceLineRepository.save(invoiceline)
							}
						}
					}

					//	                                    Save doctorDetails
					if (doctorDetail !== null) {
						for (d in doctorDetail) {
							val doctorItem = Doctor(
								code = d.code.toString(), name = d.name.toString
									(), speciality = d.speciality.toString(),
								visit_number = getVisit
							)
							doctorRepository.save(doctorItem)
						}

					}


					//								Set the visit to closed

					if (claimStatus.data.status!! !== Status.CLOSED) {
						val closedVisit = closeClaimFromIntegratedFacilityWeb(
							CloseIntergratedWebClaimDTO(
								encounterId = dto.encounterId,
								amount = invoiceAmount,
								invoiceNumber = invoiceNumber,
								claimRef = dto.claimRef!!,
								memberNumber = dto.memberNumber,
								providerName = dto.deviceId!!
							)
						)
					} else {
						val closedVisit = closeClaimFromIntegratedFacilityWeb(
							CloseIntergratedWebClaimDTO(
								encounterId = dto.encounterId,
								amount = invoiceAmount,
								invoiceNumber = invoiceNumber,
								claimRef = dto.claimRef!!,
								memberNumber = dto.memberNumber,
								providerName = dto.deviceId!!
							)
						)
					}

					responseResult = true

				} else {
					responseResult = false
					responseError = dto.claimRef + "  Member Balance Error, amount more than " +
							"available Balance"

				}

			} else {

				saveUnsuccessfulClaim(
					IntegratedUnsuccessfulClaimCloseDTO
						(
						claimRef = dto.claimRef!!,
						provider = dto.deviceId.toString(),
						memberNumber = dto.memberNumber,
						reason = "ClaimRef already Exists " + dto.claimRef
					)
				)

				return ResultFactory.getSuccessResult(
					data = true, msg = "ClaimRef already Exists" + dto.claimRef
				)


			}

		} else {
			responseResult = false
			responseError = dto.claimRef + "No Visit Found, claim cannot be closed"
			///println(dto.claimRef + "No Visit Found,  claim cannot be closed")

		}
		return if (responseResult) {

			ResultFactory.getSuccessResult(
				data = true, msg = "Claim Successfully Received"
			)

		} else {
			saveUnsuccessfulClaim(
				IntegratedUnsuccessfulClaimCloseDTO
					(
					claimRef = dto.claimRef!!,
					provider = dto.deviceId!!.toString(),
					memberNumber = dto.memberNumber,
					reason = responseError.toString()
				)
			)
			ResultFactory.getFailResult(
				data = false, msg = responseError
			)

		}


	}

	override fun saveCompleteDiagnosisClaimFromMiddleware(dto: DiagnosisClaimRes): Result<Boolean> {

		val diagnosisDetail: List<DiagnosisDetail>? = dto.diagnosisDetails

		val invoiceNumber: String? = dto.invoiceDetails!![0].invoiceNumber
		val visit = visitRepo.findById(dto.encounterId)

		val getVisit = visit.get()

		//Save diagnosis
		if (diagnosisDetail !== null && dto.claimStatus!!.lowercase() == "complete" && (dto
				.deviceId.equals("MATER") || dto.deviceId.equals("AGAKHANNAIROBI"))
		) {
			val currentDiagnosis = diagnosisRepository.findByVisit(getVisit)
			if (currentDiagnosis.isNotEmpty()) {
				currentDiagnosis.forEach {
					diagnosisRepository.deleteById(it.id)
				}
			}
			for (d in diagnosisDetail) {
				val diagnosisItem = Diagnosis(
					code = d.code.toString(),
					title = d.name.toString(),
					invoiceNumber = invoiceNumber,
					visit = getVisit
				)
				diagnosisRepository.save(diagnosisItem)
			}
			return ResultFactory.getSuccessResult(data = true, msg = "Successfully Saved Diagnosis")
		}
		return ResultFactory.getFailResult("Could not save Diagnosis")


	}


	@Transactional(rollbackFor = [Exception::class])
	override fun deactivateBenefits(dto: DeactivateBenefitDTO): Result<Boolean> {

		val benefits = repo.findByCategoryIdAndBeneficiaryId(dto.categoryId, dto.beneficiaryId)
		return if (benefits!!.isEmpty()) {
			ResultFactory.getFailResult(data = true, msg = "No benefits Found")
		} else {
			benefits.forEach {
				it.apply {
					status = BeneficiaryBenefit.BenefitStatus.SUSPENDED
				}
				repo.save(it)
			}
			ResultFactory.getSuccessResult(
				data = true, msg = "Successfully Deactivated " +
						"Benefits"
			)
		}

	}

	@Transactional(rollbackFor = [Exception::class])
	override fun activateBenefits(dto: activateBenefitDTO): Result<Boolean> {

		val benefits = repo.findByCategoryIdAndBeneficiaryId(dto.categoryId, dto.beneficiaryId)
		return if (benefits!!.isEmpty()) {
			ResultFactory.getFailResult(data = true, msg = "No benefits Found")
		} else {
			benefits.forEach {
				it.apply {
					status = BeneficiaryBenefit.BenefitStatus.ACTIVE
				}
				repo.save(it)
			}
			ResultFactory.getSuccessResult(
				data = true, msg = "Successfully activated " +
						"Benefits"
			)
		}

	}

	private fun transferBalance(b: MutableSet<BeneficiaryBenefit>, amount: BigDecimal) {

		b.map {
			if (it.parent == null) {
				it.balance = it.balance.subtract(amount)
			}
		}
	}


	private fun topupBalance(b: MutableSet<BeneficiaryBenefit>, amount: BigDecimal) {
		b.map {
			if (it.parent == null) {
				it.balance = it.balance.add(amount)
			}
		}
	}


	@Transactional(rollbackFor = [Exception::class])
	override fun topUpBenefit(dto: TopUpBenefitDTO): Result<Boolean> {
		val benefit = repo.findById(dto.id)
		if (benefit.isEmpty) return ResultFactory.getFailResult(msg = "Benefit not found")
		val visit = visitRepo.findByAggregateIdAndStatus(
			dto.aggregateId,
			Status.ACTIVE
		)
		if (benefit.isPresent) {
			val benefits = repo.findAllByAggregateId(dto.aggregateId, dto.benefitId)
			if (benefits.size < 1) {
				return ResultFactory.getFailResult("No benefit with aggregate id and benefit id")
			}
			topupBalance(benefits, dto.amount)
//			for (benefit in benefits) {
//				alignTopUpChildBalances(benefit)
//			}
			repo.saveAll(benefits)

			if (visit.isPresent) {
				visit.get().apply {
					balanceAmount += dto.amount
				}
				visitRepo.save(visit.get())
			}
			return ResultFactory.getSuccessResult(data = true, msg = "Top up Successful")

		} else {
			return ResultFactory.getFailResult(msg = "No Benefit Found")
		}

	}

	@Transactional(rollbackFor = [Exception::class])
	override fun transferBenefit(dto: TransferBenefitDTO): Result<Boolean> {

		val fromVisit = visitRepo.findByAggregateIdAndStatus(
			dto.fromAggregate!!,
			Status.ACTIVE
		)

		val toVisit = visitRepo.findByAggregateIdAndStatus(
			dto.toAggregate!!,
			Status.ACTIVE
		)
		val frombenefitByAggregateId = repo.findAllByAggregateIdOnly(dto.fromAggregate)
		if (frombenefitByAggregateId.size < 1) {
			return ResultFactory.getFailResult(
				"No benefit to transfer from  with aggregate " +
						"id and benefit" +
						" id"
			)
		}
		transferBalance(frombenefitByAggregateId, dto.amount!!)

		val toBenefitByAggregateId = repo.findAllByAggregateIdOnly(dto.toAggregate)
		if (toBenefitByAggregateId.size < 1) {
			return ResultFactory.getFailResult(
				"No benefit to transfer to  with aggregate id " +
						"and " +
						"benefit id"
			)
		}
		topupBalance(toBenefitByAggregateId, dto.amount)

		repo.saveAll(toBenefitByAggregateId)
		repo.saveAll(frombenefitByAggregateId)


//          Apply to current active visit if any
		if (fromVisit.isPresent) {
			fromVisit.get().apply {
				balanceAmount -= dto.amount
			}
			visitRepo.save(fromVisit.get())
		}

		if (toVisit.isPresent) {
			toVisit.get().apply {
				balanceAmount += dto.amount
			}
			visitRepo.save(toVisit.get())
		}

		return ResultFactory.getSuccessResult(data = true, msg = "Benefit Transfer Successful")

	}

	override fun findBeneficiaryBenefitByMemberNumberAndBenefit(
		memberNumber: String,
		benefit: String,
		period: String
	): Result<BeneficiaryBenefit?>? {
		println(memberNumber)
		println(benefit)
		var ben: MutableList<BeneficiaryBenefit?>? = null
		if (period == "previous") {
			ben = repo.findAllByMemberNumberAndBenefitNamePreviousPeriod(memberNumber, benefit)

			if (ben.size > 1) {
				return ResultFactory.getFailResult(
					msg = "User $memberNumber has more than one benefit " +
							"of $benefit  Cancelled "
				)
			}
			return if (ben.size > 0) {
				ResultFactory.getSuccessResult(data = ben[0])
			} else {
				ResultFactory.getFailResult(
					msg = "No benefit found for $memberNumber and " +
							benefit
				)
			}
		} else if (period == "current") {
			ben = repo.findAllByMemberNumberAndBenefitNameCurrentPeriod(memberNumber, benefit)
			if (ben.size > 1) {
				return ResultFactory.getFailResult(
					msg = "User $memberNumber has more than one benefit " +
							"of $benefit  Active "
				)
			}
			return if (ben.size > 0) {
				ResultFactory.getSuccessResult(data = ben[0])
			} else {
				ResultFactory.getFailResult(
					msg = "No benefit found for $memberNumber and " +
							benefit
				)
			}
		}
		return ResultFactory.getFailResult(
			"Could not find " +
					"member $memberNumber and benefit $benefit"
		)

	}
}


