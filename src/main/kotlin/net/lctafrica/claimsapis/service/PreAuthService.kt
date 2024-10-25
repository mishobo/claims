package net.lctafrica.claimsapis.service

import net.lctafrica.claimsapis.dto.*
import net.lctafrica.claimsapis.repository.PreAuthRepo
import net.lctafrica.claimsapis.util.Result
import net.lctafrica.claimsapis.util.ResultFactory
import org.springframework.data.jpa.domain.AbstractPersistable_.id
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

@Service("pre-authService")
@Transactional
class PreAuthService(val repo: PreAuthRepo,
                     val benefitService: BenefitService) : IPreAuthService {

	@Transactional(rollbackFor = [Exception::class])
	override fun add(dto: PreAuthDTO): Result<Preauthorization?> {
		if (dto.id == null) {

			val preAuth = Preauthorization(
				time = LocalDateTime.now(),
				status = Preauthorization.PreauthStatus.PENDING,
				requestAmount = dto.requestAmount,
				aggregateId = dto.aggregateId,
				benefitId = dto.benefitId,
				payerId = dto.payerId,
				providerId = dto.providerId,
				requester = dto.requester,
				//authorizer = dto.authorizer,
				notes = dto.notes,
				visitNumber = dto.visitNumber,
				memberNumber = dto.memberNumber,
				benefitName = dto.benefitName,
				memberName = dto.memberName,
				authorizationNotes = null,
				service = dto.service,
				requestType = dto.requestType,
				reference = dto.reference,
				diagnosis = dto.diagnosisDescriptionValue,
				medProcedure = dto.medProcedureDescriptionValue,
				payerName = dto.payer,
				schemeName = dto.scheme

			)
			//TODO resolve authoriser
			repo.save(preAuth)
			return ResultFactory.getSuccessResult(preAuth)


		} else {
			val auth = repo.findById(dto.id!!).orElseGet { null }
			var preAuthRes: Preauthorization? = null


			if (dto.draft == true && auth !== null) {

				auth.apply {
					status = Preauthorization.PreauthStatus.INACTIVE
					requestAmount = dto.requestAmount
					aggregateId = dto.aggregateId
					benefitId = dto.benefitId
					payerId = dto.payerId
					providerId = dto.providerId
					requester = dto.requester
					//authorizer = dto.authorizer,
					notes = dto.notes
					visitNumber = dto.visitNumber
					memberNumber = dto.memberNumber
					benefitName = dto.benefitName
					memberName = dto.memberName
					authorizationNotes = null
					service = dto.service
					requestType = dto.requestType
					reference = dto.reference
					diagnosis = dto.diagnosisDescriptionValue
					medProcedure = dto.medProcedureDescriptionValue
					time = LocalDateTime.now()
					schemeName = dto.scheme
					payerName = dto.payer
				}

				//TODO resolve authoriser

				return ResultFactory.getSuccessResult(repo.save(auth))

			} else if (dto.draft == true) {
				val preAuth = Preauthorization(
					time = LocalDateTime.now(),
					status = Preauthorization.PreauthStatus.INACTIVE,
					requestAmount = dto.requestAmount,
					aggregateId = dto.aggregateId,
					benefitId = dto.benefitId,
					payerId = dto.payerId,
					providerId = dto.providerId,
					requester = dto.requester,
					//authorizer = dto.authorizer,
					notes = dto.notes,
					visitNumber = dto.visitNumber,
					memberNumber = dto.memberNumber,
					benefitName = dto.benefitName,
					memberName = dto.memberName,
					authorizationNotes = null,
					service = dto.service,
					requestType = dto.requestType,
					reference = dto.reference,
					diagnosis = dto.diagnosisDescriptionValue,
					medProcedure = dto.medProcedureDescriptionValue,
					payerName = dto.payer,
					schemeName = dto.scheme,


					)
				//TODO resolve authoriser
				return ResultFactory.getSuccessResult(repo.save(preAuth))


			} else {
				val memberAuths = repo.findByAggregateIdAndBenefitId(dto.aggregateId, dto.benefitId)

				memberAuths.forEach {
					if (isOpen(it))
						return ResultFactory.getFailResult(
							"A pending pre-authorization,  REF-  ${it.reference} " +
									"was found for this member"
						)
				}

				if (auth.id == dto.id) {
					auth.apply {
						status = Preauthorization.PreauthStatus.PENDING
						requestAmount = dto.requestAmount
						aggregateId = dto.aggregateId
						benefitId = dto.benefitId
						payerId = dto.payerId
						providerId = dto.providerId
						requester = dto.requester
						//authorizer = dto.authorizer,
						notes = dto.notes
						visitNumber = dto.visitNumber
						memberNumber = dto.memberNumber
						benefitName = dto.benefitName
						memberName = dto.memberName
						authorizationNotes = null
						service = dto.service
						requestType = dto.requestType
						reference = dto.reference
						diagnosis = dto.diagnosisDescriptionValue
						medProcedure = dto.medProcedureDescriptionValue
						schemeName = dto.scheme
						payerName = dto.payer
						time = LocalDateTime.now()
					}

					 preAuthRes  = repo.save(auth)


				}


			}

			return ResultFactory.getSuccessResult(preAuthRes)
		}
	}




	fun isOpen(preauthorization: Preauthorization): Boolean {
		return when (preauthorization.status) {
			Preauthorization.PreauthStatus.AUTHORIZED -> true
			else -> false
		}
	}

	override fun findByAggregate(aggregateId: String): Result<List<Preauthorization>> {
		val auths = repo.findByAggregateId(aggregateId)
		return ResultFactory.getSuccessResult(auths)
	}

	override fun findByVisitNumber(visitNumber: Long): Result<Preauthorization?> {
		val auths = repo.findByVisitNumber(visitNumber)
		return ResultFactory.getSuccessResult(auths)
	}

	override fun findById(id: Long): Result<Preauthorization?> {
		val auths = repo.findById(id).get()
		return ResultFactory.getSuccessResult(auths)
	}

	override fun findByMemberNumber(memberNumber: String): Result<List<Preauthorization>> {
		val auths = repo.findByMemberNumber(memberNumber)
		return ResultFactory.getSuccessResult(auths)
	}

	override fun decline(dto: AuthorizePreAuthDTO): Result<Preauthorization> {
		val auth = repo.findById(dto.id)

		if (auth.isPresent) {
			val auth = auth.get()
			auth.apply {
				authorizedAmount = dto.amount
				status = Preauthorization.PreauthStatus.DECLINED
				authorizationNotes = dto.notes
			}
			return ResultFactory.getSuccessResult(repo.save(auth))
		}

		return ResultFactory.getFailResult("No such pre-auth was found")

	}


	override fun authorize(dto: AuthorizePreAuthDTO): Result<Preauthorization> {
		val auth = repo.findById(dto.id)


		if (auth.isPresent) {
			val auth = auth.get()

//			NB:Will send utilisation sms
			val benefitResponse = benefitService.consumeBenefit(
				ConsumeBenefitDTO(
					amount = dto.amount,
					aggregateId = auth.aggregateId,
					benefitId = auth.benefitId,
					memberNumber = auth.memberNumber!!,
					visitNumber = auth.visitNumber

				)
			)

			if(benefitResponse.success){
				auth.apply {
					authorizedAmount = dto.amount
					status = Preauthorization.PreauthStatus.AUTHORIZED
					authorizationNotes = dto.notes
				}
				return ResultFactory.getSuccessResult(repo.save(auth))
			}else{
				return ResultFactory.getFailResult(msg = "Insufficient Balance")
			}
		}

		return ResultFactory.getFailResult("No such pre-auth was found")

	}


	override fun release(dto: AuthorizePreAuthDTO): Result<Preauthorization> {
		val auth = repo.findById(dto.id)

		if (auth.isPresent) {
			val auth = auth.get()
			val benefitResponse = benefitService.reverseBenefit(
				ReverseBenefitDTO(
					amount = dto.amount,
					aggregateId = auth.aggregateId,
					benefitId = auth.benefitId,
					memberNumber = auth.memberNumber!!,
					visitNumber = auth.visitNumber
				)
			)

			return if(benefitResponse.success){

				ResultFactory.getSuccessResult(msg = "Release Successful")
			}else{
				ResultFactory.getFailResult(msg = "Insufficient Balance")
			}

		}

		return ResultFactory.getFailResult("No such pre-auth was found")

	}

	override fun consumePreauth(dto: ConsumePreAuthDTO): Result<Preauthorization?> {

		val auth = repo.findByVisitNumber(dto.visitNumber!!)
		if(auth!== null){
			if (auth!!.authorizedAmount!! <dto.amount) {
				return ResultFactory.getFailResult(msg = "Amount More than authorized amount", data =
				auth)

			}else{
				val theauth = auth.apply {
					this.authorizedAmount = this.authorizedAmount!!.subtract(dto.amount)
					this.utilization = this.utilization!!.add(dto.amount)
				}
				return ResultFactory.getSuccessResult(msg = "Success", data = repo.save(theauth))

			}
		}else{
			return ResultFactory.getFailResult(msg = "No preauth", data = auth)
		}


	}

	override fun findPendingByProviderId(providerId: Long): Result<MutableList<Preauthorization?>> {
		val result =
			repo.findAllByProviderIdAndStatusOrderByCreatedAtDesc(
				providerId,
				Preauthorization.PreauthStatus.PENDING
			);
		return ResultFactory.getSuccessResult(result)
	}

	override fun findAllByProviderId(providerId: Long): Result<MutableList<Preauthorization?>> {
		val result =
			repo.findAllByProviderIdAndStatusNotOrderByCreatedAtDesc(
				providerId,
				Preauthorization.PreauthStatus.PENDING
			);
		return ResultFactory.getSuccessResult(result)
	}

	override fun findPendingByPayerId(payerId: Long): Result<MutableList<Preauthorization?>> {
		val result =
			repo.findAllByPayerIdAndStatusOrderByCreatedAtDesc(
				payerId,
				Preauthorization.PreauthStatus.PENDING
			);
		return ResultFactory.getSuccessResult(result)
	}

	override fun findAllByPayerId(payerId: Long): Result<MutableList<Preauthorization?>> {
		val result =
			repo.findAllByPayerIdAndStatusNotOrderByCreatedAtDesc(
				payerId,
				Preauthorization.PreauthStatus.PENDING
			);
		return ResultFactory.getSuccessResult(result)
	}




}