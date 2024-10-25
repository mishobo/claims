package net.lctafrica.claimsapis.service

import java.math.BigDecimal
import java.time.LocalDateTime
import net.lctafrica.claimsapis.dto.BeneficiaryBenefit
import net.lctafrica.claimsapis.dto.ClaimError
import net.lctafrica.claimsapis.dto.ClaimImport
import net.lctafrica.claimsapis.dto.ClaimMigrationDTO
import net.lctafrica.claimsapis.dto.ClaimProcessStatus
import net.lctafrica.claimsapis.dto.ConsumeBenefitDTO
import net.lctafrica.claimsapis.dto.Invoice
import net.lctafrica.claimsapis.dto.InvoiceLine
import net.lctafrica.claimsapis.dto.LINE_TYPE
import net.lctafrica.claimsapis.dto.LineItemError
import net.lctafrica.claimsapis.dto.Status
import net.lctafrica.claimsapis.dto.TransactionError
import net.lctafrica.claimsapis.dto.TransactionInvoiceDto
import net.lctafrica.claimsapis.dto.TransactionMigrationDto
import net.lctafrica.claimsapis.helper.ExcelHelper
import net.lctafrica.claimsapis.repository.BeneficiaryBenefitRepository
import net.lctafrica.claimsapis.repository.ClaimErrorRepo
import net.lctafrica.claimsapis.repository.InvoiceLineRepository
import net.lctafrica.claimsapis.repository.InvoiceRepository
import net.lctafrica.claimsapis.repository.LineItemErrorRepo
import net.lctafrica.claimsapis.repository.TransactionErrorRepo
import net.lctafrica.claimsapis.repository.VisitRepository
import net.lctafrica.claimsapis.util.Result
import net.lctafrica.claimsapis.util.ResultFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile


@Service
@Transactional
class DataMigrationService(
    val repo: BeneficiaryBenefitRepository,
    val benefitService: BenefitService,
    val claimErrorRepo: ClaimErrorRepo,
    val visitRepository: VisitRepository,
    val invoiceRepo: InvoiceRepository,
    val claimsService: IClaimsService,
    val transactionErrorRepo: TransactionErrorRepo,
    val lineItemErrorRepo: LineItemErrorRepo,
    val invoiceLineRepo: InvoiceLineRepository,
    @Lazy @Autowired val excelHelper: ExcelHelper,
) :
    IDataMigrationService {

    @Transactional(rollbackFor = [Exception::class])
    override fun saveClaims(dto: ClaimImport): Result<Boolean> {
        var errCount = 0
        dto.claims.forEach {
            val ben = aggregateFromBenefitAndMemberNumber(it.benefit, it.memberNumber)
            ben?.let { bb ->
                val consumeBenefitDTO = ConsumeBenefitDTO(
                    amount = it.amount,
                    aggregateId = bb.aggregateId,
                    benefitId = bb.benefitId,
                    memberNumber = bb.memberNumber,
                    visitNumber = null

                )
                if (!benefitService.consumeBenefit(consumeBenefitDTO).success) {
                    saveFailed(it)
                    errCount++
                }

            } ?: kotlin.run {
                saveFailed(it)
                errCount++
            }

        }
        return if (errCount > 0) {
            ResultFactory.getFailResult("There were $errCount errors while saving claims")
        } else {
            ResultFactory.getSuccessResult("Successfully migrated the claims")
        }

    }

    fun aggregateFromBenefitAndMemberNumber(benefitName: String, memberNumber: String): BeneficiaryBenefit? {
        return repo.findAggregateFromMemberAndBenefit(benefitName, memberNumber)
    }

    fun saveFailed(input: ClaimMigrationDTO) {
        claimErrorRepo.save(
            ClaimError(
                amount = input.amount,
                benefit = input.benefit,
                date = input.date,
                memberNumber = input.memberNumber
            )
        )
    }

    override fun getErrors(): Result<MutableList<ClaimError>> {
        val errors = claimErrorRepo.findAll()
        return ResultFactory.getSuccessResult(errors)
    }

    @Transactional(rollbackFor = [Exception::class])
    override fun migrateTransactions(dto: TransactionMigrationDto): Result<Boolean> {
        var errorCount = 0
        dto.invoices.forEach { data ->
            val invoice = data.invoice
            val visitOpt = visitRepository.findById(invoice.visitNumber)
            if (visitOpt.isPresent) {
                val visit = visitOpt.get()
                if (visit.status == Status.CLOSED) {
                    errorCount++
                    visit.invoiceNumber = invoice.items[0].invoiceNumber
                    visit.totalInvoiceAmount = invoice.invoiceTotal
                    visitRepository.save(visit)
                    errorSavingTrans(invoice, "Visit is already closed")
                } else {
                    invoice.items.forEach { item ->
                        val exist = invoiceRepo.findByInvoiceNumberAndHospitalProviderId(
                            invoiceNumber = item.invoiceNumber,
                            hospital_provider_id = visit.hospitalProviderId ?: 0
                        )
                        if (exist.isPresent) {
                            errorCount++
                            errorSavingTrans(invoice, "Invoice number exists already")
                        } else {
                            var saveInvoice = Invoice(
                                hospitalProviderId = visit.hospitalProviderId,
                                invoiceNumber = item.invoiceNumber,
                                visit = visit,
                                invoiceLines = mutableSetOf(),
                                totalAmount = item.totalAmount,
                                service = null
                            )
                            val invoiceSaveResult = invoiceRepo.save(saveInvoice)

                            item.lineItems.forEach { line ->
                                val newInvoiceLine = InvoiceLine(
                                    description = line.description,
                                    invoiceNumber = item.invoiceNumber,
                                    lineTotal = line.amount,
                                    lineType = LINE_TYPE.OTHER,
                                    invoice = invoiceSaveResult,
                                    quantity = line.quantity,
                                    unitPrice = line.unitPrice,
                                    lineCategory = null
                                )
                                invoiceLineRepo.save(newInvoiceLine)
                            }
                        }
                    }
                    visit.claimProcessStatus = ClaimProcessStatus.PROCESSED
                    visit.status = Status.CLOSED
                    visit.invoiceNumber = invoice.items[0].invoiceNumber
                    visit.totalInvoiceAmount = invoice.invoiceTotal

                    visit.createdAt = LocalDateTime.now()
                    invoice.invoiceDate?.let {
                        visit.createdAt = it
                    }
                    visit.createdAt= LocalDateTime.now()
                    invoice.invoiceDate?.let {
                        visit.createdAt=it
                    }
                    visitRepository.save(visit)
                }
            } else {
                errorCount++
                errorSavingTrans(invoice, "visit number doesn't exist")
            }
        }
        return ResultFactory.getSuccessResult("Successfully migrated transactions with $errorCount errors")
    }

    override fun savePreviousPeriodVisitsFromFile(file: MultipartFile): Result<Boolean?> {

        val processedClaims: Result<Boolean>? = excelHelper.excelToVisit(file.inputStream,"previous")

        if (processedClaims!!.success){
            return ResultFactory.getSuccessResult(msg = "Successfully saved claims", data =
            processedClaims.data)
        }else{
            return ResultFactory.getFailResult(msg = processedClaims.msg, data =
            processedClaims!!.success)
        }
    }

    override fun saveCurrentPeriodVisitsFromFile(file: MultipartFile): Result<Boolean?> {
        val processedClaims:  Result<Boolean>? = excelHelper.excelToVisit(file.inputStream,"current")

        if (processedClaims!!.success){
            return ResultFactory.getSuccessResult(msg = "Successfully saved claims", data =
            processedClaims.data)
        }else{
            return ResultFactory.getFailResult(msg = processedClaims.msg, data =
            processedClaims!!.success)
        }


    }

    @Transactional(readOnly = true)
     override fun getTransactionErrors(page:Int, size:Int): Result<Page<TransactionError>> {
         val pageable = PageRequest.of(page - 1, size)
        val errors = transactionErrorRepo.findAllByOrderByIdDesc(pageable)
        return ResultFactory.getSuccessResult(errors)
    }

    private fun errorSavingTrans(invoice: TransactionInvoiceDto, note: String?) {
        val check = transactionErrorRepo.findByVisitNumber(invoice.visitNumber)
        if (check.isNotEmpty()) {
            return
        }
        invoice.items.forEach { item ->
            var invoiceDate = LocalDateTime.now()
            invoice.invoiceDate?.let {
                invoiceDate = it
            }
            val transactionError = TransactionError(
                totalAmount = item.totalAmount,
                invoiceNumber = item.invoiceNumber,
                memberNumber = invoice.memberNumber,
                visitNumber = invoice.visitNumber,
                invoiceDate = invoiceDate,
                note = note
            )
            transactionErrorRepo.save(transactionError)
            item.lineItems.forEach { line ->
                val lineError = LineItemError(
                    amount = line.amount,
                    description = line.description,
                    quantity = line.quantity,
                    unitPrice = line.unitPrice,
                    note = note,
                    transactionError = transactionError
                )
                lineItemErrorRepo.save(lineError)
            }
        }
    }
}