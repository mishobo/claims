package net.lctafrica.claimsapis.controller

import net.lctafrica.claimsapis.dto.SaveDocumentDTO
import net.lctafrica.claimsapis.service.IDocumentService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/document")
class DocumentController(val service: IDocumentService) {

	@PostMapping(value = ["/save"], produces = ["application/json"])
	fun saveDocument(@RequestBody dto: SaveDocumentDTO) = service.saveDocument(dto)

}