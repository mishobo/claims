package net.lctafrica.claimsapis.service

import net.lctafrica.claimsapis.dto.*
import net.lctafrica.claimsapis.repository.DocumentsRepo
import net.lctafrica.claimsapis.util.Result
import net.lctafrica.claimsapis.util.ResultFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service("documentsService")
@Transactional
class DocumentService(val repo: DocumentsRepo) : IDocumentService {

	@Transactional(rollbackFor = [Exception::class])
	override fun saveDocument(dto: SaveDocumentDTO): Result<Boolean> {
		val document = Document(
			providerName = dto.providerName,
			providerId = dto.providerId,
			type = DocumentType.valueOf(dto.type.toString()),
			fileUrl = dto.fileUrl,
			invoiceNumber = dto.invoiceNumber
		)

		repo.save(document)
		return ResultFactory.getSuccessResult(true)
	}

}