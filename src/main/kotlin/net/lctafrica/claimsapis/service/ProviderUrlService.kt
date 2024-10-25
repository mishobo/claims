package net.lctafrica.claimsapis.service

import net.lctafrica.claimsapis.dto.*
import net.lctafrica.claimsapis.repository.DocumentsRepo
import net.lctafrica.claimsapis.repository.ProviderUrlRepo
import net.lctafrica.claimsapis.util.Result
import net.lctafrica.claimsapis.util.ResultFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service("providerUrlService")
@Transactional
class ProviderUrlService(val repo:ProviderUrlRepo) : IProviderUrlService {

	@Transactional(readOnly = true)
	override fun findByProviderAndUrlType(
		providerName: String,
		urlType: MIDDLWARE_URL_TYPE
	): Result<ProviderUrl> {
		val providerUrl = repo.findByProviderAndUrlType(providerName,urlType)
		return if (providerUrl.isEmpty) ResultFactory.getFailResult(msg = "No Config for Provider!")
		else
			return ResultFactory.getSuccessResult(data = providerUrl.get())
	}

	override fun getAllByUrlType(urlType: MIDDLWARE_URL_TYPE): Result<MutableList<ProviderUrl>> {
		val providerUrl:MutableList<ProviderUrl> = repo.findAllByUrlType(urlType)
		return ResultFactory.getSuccessResult(data = providerUrl)
	}

}