package net.lctafrica.claimsapis.helper

import com.sun.xml.txw2.output.ResultFactory
import net.lctafrica.claimsapis.dto.*
import net.lctafrica.claimsapis.repository.InvoiceLineRepository
import net.lctafrica.claimsapis.repository.InvoiceRepository
import net.lctafrica.claimsapis.repository.VisitRepository
import net.lctafrica.claimsapis.service.IBenefitService
import net.lctafrica.claimsapis.util.Result
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.io.InputStream
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter




@Component
class ExcelHelper(
	val benefitService: IBenefitService,
	private val visitRepository: VisitRepository,
	private val invoiceRepository: InvoiceRepository,
	private val invoiceLineRepository: InvoiceLineRepository,
) {
	var TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
	var HEADERs = arrayOf("Code", "Title")
	var SHEET = "ICD10CODES"
	var SHEET_DRUGS = "medical_drugs"
	var SHEET_LAB = "lab"
	var SHEET_RADIOLOGY = "radiology"
	var SHEET_PROCEDURES = "medical_procedures"
	var SHEET_OTHER_BENEFIT_DETAIL = "OTHER"
	var BILLS = "BILLS"

	fun hasExcelFormat(file: MultipartFile): Boolean {
		return TYPE == file.contentType
	}

//	 fun icd10codesToExcel(icd10codes: List<Icd10code?>): ByteArrayInputStream? {
//		try {
//			XSSFWorkbook().use { workbook ->
//				ByteArrayOutputStream().use { out ->
//					val sheet = workbook.createSheet(SHEET)
//
//					// Header
//					val headerRow: Row = sheet.createRow(0)
//					for (col in HEADERs.indices) {
//						val cell: Cell = headerRow.createCell(col)
//						cell.setCellValue(HEADERs[col])
//					}
//					var rowIdx = 1
//					for (icd10code in icd10codes) {
//						val row: Row = sheet.createRow(rowIdx++)
//						if (icd10code != null) {
//							row.createCell(0).setCellValue(icd10code.code.toString())
//							row.createCell(1).setCellValue(icd10code.title.toString())
//						}
//					}
//					workbook.write(out)
//					return ByteArrayInputStream(out.toByteArray())
//				}
//			}
//		} catch (e: IOException) {
//			throw RuntimeException("fail to import data to Excel file: " + e.message)
//		}
//	}

	fun excelToicd10codes(`is`: InputStream?): List<Icd10code>? {
		return try {
			val workbook: Workbook = XSSFWorkbook(`is`)
			val sheet = workbook.getSheet(SHEET)
			val rows: Iterator<Row> = sheet.iterator()
			val icd10codes: MutableList<Icd10code> = ArrayList<Icd10code>()
			var rowNumber = 0
			while (rows.hasNext()) {
				val currentRow: Row = rows.next()

				// skip header
				if (rowNumber == 0) {
					rowNumber++
					continue
				}
				val cellsInRow: Iterator<Cell> = currentRow.iterator()
				val icd10code = Icd10code()
				var cellIdx = 0
				while (cellsInRow.hasNext()) {
					val currentCell: Cell = cellsInRow.next()
					when (cellIdx) {
						0 -> icd10code.code = currentCell.stringCellValue
						1 -> icd10code.title = currentCell.stringCellValue

						else -> {}
					}
					cellIdx++
				}
				icd10codes.add(icd10code)
			}
			workbook.close()
			icd10codes
		} catch (e: IOException) {
			throw RuntimeException("fail to parse Excel file: " + e.message)
		}
	}

	fun excelToMedicalProcedure(`is`: InputStream?): List<MedicalProcedure>? {
		return try {
			val workbook: Workbook = XSSFWorkbook(`is`)
			val sheet = workbook.getSheet(SHEET_PROCEDURES)
			val rows: Iterator<Row> = sheet.iterator()
			val medicalProcedures: MutableList<MedicalProcedure> = ArrayList<MedicalProcedure>()
			var rowNumber = 0
			while (rows.hasNext()) {
				val currentRow: Row = rows.next()

				// skip header
				if (rowNumber == 0) {
					rowNumber++
					continue
				}
				val cellsInRow: Iterator<Cell> = currentRow.iterator()
				val medicalProcedure = MedicalProcedure()
				var cellIdx = 0
				while (cellsInRow.hasNext()) {
					val currentCell: Cell = cellsInRow.next()
					when (cellIdx) {

						0 -> medicalProcedure.procedure_code = currentCell.stringCellValue
						1 -> medicalProcedure.procedure_description = currentCell.stringCellValue
						else -> {}
					}
					cellIdx++
				}
				medicalProcedures.add(medicalProcedure)
			}
			workbook.close()
			medicalProcedures
		} catch (e: IOException) {
			throw RuntimeException("fail to parse Excel file: " + e.message)
		}
	}

	fun excelToMedicalDrugs(`is`: InputStream?): List<MedicalDrug>? {
		return try {
			val workbook: Workbook = XSSFWorkbook(`is`)
			val sheet = workbook.getSheet(SHEET_DRUGS)
			val rows: Iterator<Row> = sheet.iterator()
			val medicalDrugs: MutableList<MedicalDrug> = ArrayList<MedicalDrug>()
			var rowNumber = 0
			while (rows.hasNext()) {
				val currentRow: Row = rows.next()

				// skip header
				if (rowNumber == 0) {
					rowNumber++
					continue
				}
				val cellsInRow: Iterator<Cell> = currentRow.iterator()
				val medicalDrug = MedicalDrug()
				var cellIdx = 0
				while (cellsInRow.hasNext()) {
					val currentCell: Cell = cellsInRow.next()
					when (cellIdx) {
						0 -> medicalDrug.name = currentCell.stringCellValue

						else -> {}
					}
					cellIdx++
				}
				medicalDrugs.add(medicalDrug)
			}
			workbook.close()
			medicalDrugs
		} catch (e: IOException) {
			throw RuntimeException("fail to parse Excel file: " + e.message)
		}
	}

	fun excelToLaboratory(`is`: InputStream?): List<Laboratory>? {
		return try {
			val workbook: Workbook = XSSFWorkbook(`is`)
			val sheet = workbook.getSheet(SHEET_LAB)
			val rows: Iterator<Row> = sheet.iterator()
			val laboratoryList: MutableList<Laboratory> = ArrayList<Laboratory>()
			var rowNumber = 0
			while (rows.hasNext()) {
				val currentRow: Row = rows.next()

				// skip header
				if (rowNumber == 0) {
					rowNumber++
					continue
				}
				val cellsInRow: Iterator<Cell> = currentRow.iterator()
				val laboratory = Laboratory()
				var cellIdx = 0
				while (cellsInRow.hasNext()) {
					val currentCell: Cell = cellsInRow.next()
					when (cellIdx) {
						0 -> laboratory.name = currentCell.stringCellValue

						else -> {}
					}
					cellIdx++
				}
				laboratoryList.add(laboratory)
			}
			workbook.close()
			laboratoryList
		} catch (e: IOException) {
			throw RuntimeException("fail to parse Excel file: " + e.message)
		}
	}

	fun excelToOtherBenefitDetail(`is`: InputStream?): List<OtherBenefitDetail>? {
		return try {
			val workbook: Workbook = XSSFWorkbook(`is`)
			val sheet = workbook.getSheet(SHEET_OTHER_BENEFIT_DETAIL)
			val rows: Iterator<Row> = sheet.iterator()
			val otherBenefitDetailList: MutableList<OtherBenefitDetail> =
				ArrayList<OtherBenefitDetail>()
			var rowNumber = 0
			while (rows.hasNext()) {
				val currentRow: Row = rows.next()

				// skip header
				if (rowNumber == 0) {
					rowNumber++
					continue
				}
				val cellsInRow: Iterator<Cell> = currentRow.iterator()
				val otherBenefitDetail = OtherBenefitDetail()
				var cellIdx = 0
				while (cellsInRow.hasNext()) {
					val currentCell: Cell = cellsInRow.next()
					when (cellIdx) {
						0 -> otherBenefitDetail.benefit_detail = currentCell.stringCellValue

						else -> {}
					}
					cellIdx++
				}
				otherBenefitDetailList.add(otherBenefitDetail)
			}
			workbook.close()
			otherBenefitDetailList
		} catch (e: IOException) {
			throw RuntimeException("fail to parse Excel file: " + e.message)
		}
	}

	fun excelToRadiology(`is`: InputStream?): List<Radiology>? {
		return try {
			val workbook: Workbook = XSSFWorkbook(`is`)
			val sheet = workbook.getSheet(SHEET_RADIOLOGY)
			val rows: Iterator<Row> = sheet.iterator()
			val radiologies: MutableList<Radiology> = ArrayList<Radiology>()
			var rowNumber = 0
			while (rows.hasNext()) {
				val currentRow: Row = rows.next()

				// skip header
				if (rowNumber == 0) {
					rowNumber++
					continue
				}
				val cellsInRow: Iterator<Cell> = currentRow.iterator()
				val radiology = Radiology()
				var cellIdx = 0
				while (cellsInRow.hasNext()) {
					val currentCell: Cell = cellsInRow.next()
					when (cellIdx) {
						0 -> radiology.detail = currentCell.stringCellValue

						else -> {}
					}
					cellIdx++
				}
				radiologies.add(radiology)
			}
			workbook.close()
			radiologies
		} catch (e: IOException) {
			throw RuntimeException("fail to parse Excel file: " + e.message)
		}
	}


	fun findMemberByMemberNo(
		memberNumber: String
	): List<BeneficiaryBenefit>? {
		//println(providerId)
		val beneficiaryBen =
			benefitService.findActiveByMemberNumber(memberNumber)
		///return beneficiaryBen.data;

		return beneficiaryBen.data
	}

	fun findVisitByMemberNoAndBenefit(
		memberNumber: String,
		benefit: String,
		period: String,
	): Result<BenBenefitDTO?>? {
		//println(providerId)
		val beneficiaryBen =
			benefitService.findBeneficiaryBenefitByMemberNumberAndBenefit(memberNumber, benefit,period)
		if(!beneficiaryBen!!.success){
			return net.lctafrica.claimsapis.util.ResultFactory.getFailResult(msg = beneficiaryBen.msg)
		}

		if(beneficiaryBen!!.data!== null) {

			val data =  BenBenefitDTO(
				aggregateId = beneficiaryBen!!.data!!.aggregateId,
				benefitId = beneficiaryBen.data!!.benefitId,
				balance = beneficiaryBen.data.balance,
				categoryId = beneficiaryBen.data.categoryId,
				beneficiaryId = beneficiaryBen.data.beneficiaryId,

				)
			return  net.lctafrica.claimsapis.util.ResultFactory.getSuccessResult(data = data)
		}
		return net.lctafrica.claimsapis.util.ResultFactory.getFailResult(msg = "Failed")
	}

	fun excelToVisit(`is`: InputStream?, period:String): Result<Boolean> {

		val workbook: Workbook = XSSFWorkbook(`is`)
		var row: Row
		var cell: Cell?
		val sheet = workbook.getSheet(BILLS)
//			val sheet = workbook.getSheet(DEVICE)
		val rows: Iterator<Row> = sheet.iterator()

		var rowNumber = 0
		val formatter = DataFormatter()

		//precision only up to 15 significant
		// digits

		while (rows.hasNext()) {
			val currentRow: Row = rows.next()

			// skip header
			if (rowNumber == 0) {
				rowNumber++
				continue
			}
			val cellsInRow: Iterator<Cell> = currentRow.iterator()
			val dTF = DateTimeFormatter.ofPattern("d-MMM-yyyy")
			val date:LocalDate = LocalDate.parse(currentRow.getCell(0).toString(),dTF)
			val invoiceNo = currentRow.getCell(1).toString()
			val memeberName = currentRow.getCell(2).toString()
			val memberNo = currentRow.getCell(3).toString()
			val itemDescription = currentRow.getCell(4).toString()
			val benefitName = currentRow.getCell(5).toString()
			val itemQuantity = currentRow.getCell(6).toString()
			val total:Double = currentRow.getCell(7).toString().toDouble()
			val payerId = currentRow.getCell(8).toString().toDouble()

			var cellIdx = 0
			val ben = findVisitByMemberNoAndBenefit(memberNo, benefitName,period)!!

			if (ben.data== null){
				return net.lctafrica.claimsapis.util.ResultFactory.getFailResult(msg = ben.msg)
			}

			val aggregateId = ben.data!!.aggregateId
			val balance = ben.data.balance
			val categoryId = ben.data.categoryId
			val beneficiaryId = ben.data.beneficiaryId
			val benefitId = ben.data.benefitId


			val theVisit = Visit(
				beneficiaryId = beneficiaryId,
				memberNumber = memberNo,
				payerId = payerId.toLong().toString(),
				hospitalProviderId = 1,
				invoiceNumber = invoiceNo,
				invoiceDate = date.atStartOfDay(),
				benefitId = benefitId,
				benefitName = benefitName,
				categoryId = categoryId.toString(),
				staffName = "irene",
				memberName = memeberName,
				visitType = VisitType.OFF_LCT,
				claimProcessStatus = ClaimProcessStatus.PROCESSED,
				middlewareStatus = MiddlewareStatus.SENT,
				offSystemReason = OffSystemReason.SYSTEM_DOWNTIME,
				status = Status.CLOSED,
				balanceAmount = balance!!,
				totalInvoiceAmount = BigDecimal.valueOf(total),
				aggregateId = aggregateId!!,
				providerMiddleware = MIDDLEWARENAME.AGAKHANNAIROBI,
				reimbursementProvider = null,
				reimbursementInvoiceDate = null,
				reimbursementReason = null,
				cancelReason = null,
				staffId = "0"
			)

			val theVisit2 = visitRepository.save(theVisit)
			var saveInvoice = Invoice(
				hospitalProviderId = theVisit2.hospitalProviderId,
				invoiceNumber = invoiceNo,
				visit = theVisit2,
				invoiceLines = mutableSetOf(),
				totalAmount = BigDecimal.valueOf(total),
				service = null
			)
			val invoiceSaveResult = invoiceRepository.save(saveInvoice)


				val newInvoiceLine = InvoiceLine(
					description = itemDescription,
					invoiceNumber = invoiceNo,
					lineTotal = BigDecimal.valueOf(total),
					lineType = LINE_TYPE.OTHER,
					invoice = invoiceSaveResult,
					quantity = BigDecimal.valueOf(itemQuantity.toDouble().toLong()),
					unitPrice = BigDecimal.valueOf(total),
					lineCategory = null
				)
				invoiceLineRepository.save(newInvoiceLine)


		}
//				while (cellsInRow.hasNext()) {
//					val currentCell: Cell = cellsInRow.next()
//
//
//					when (cellIdx) {
////						KENGEN
//						3 -> beneficiaryBenefit.forEach {
//							it.balance
//						} = findMemberByMemberNo (currentCell.numericCellValue.toLong()
//							.toString())
//						5 ->  payerProviderMapping.payer = findVisitByMemberNoAndBenefit(currentCell.numericCellValue.toLong())!!
//
//						else -> {}
//					}
//					cellIdx++
//				}
//				payerProviderMappingList.add(payerProviderMapping)
//		}
//		workbook.close()
		return net.lctafrica.claimsapis.util.ResultFactory.getSuccessResult(data=true)


	}
}