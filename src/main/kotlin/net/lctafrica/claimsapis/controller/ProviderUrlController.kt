package net.lctafrica.claimsapis.controller

import net.lctafrica.claimsapis.dto.MIDDLWARE_URL_TYPE
import net.lctafrica.claimsapis.dto.ProviderUrlDTO
import net.lctafrica.claimsapis.dto.SaveDocumentDTO
import net.lctafrica.claimsapis.service.IDocumentService
import net.lctafrica.claimsapis.service.IProviderUrlService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/visit/providerUrl")
class ProviderUrlController(val service: IProviderUrlService) {

	@GetMapping(value = ["/getConfig"], produces = ["application/json"])
	fun findByProvider(@RequestParam providerName:String, @RequestParam urlType:MIDDLWARE_URL_TYPE) = service
		.findByProviderAndUrlType(providerName,urlType)

	@GetMapping(value = ["/getProviderUrlByType"], produces = ["application/json"])
	fun findByUrlType(@RequestParam urlType:MIDDLWARE_URL_TYPE) = service
		.getAllByUrlType(urlType)

}