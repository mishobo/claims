package net.lctafrica.claimsapis.util

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.math.BigDecimal


class MiddlewareClaim {
	var control: Control? = null
	var data: ClaimRes? = null
}
@JsonIgnoreProperties(ignoreUnknown = true)
class InvoiceLine {
	var billCode: String? = null
	var billName: String? = null
	var billCategory: String? = null
	var billDate: String? = null
	var quantity: BigDecimal? = null
	var unitPrice: BigDecimal? = null
}

@JsonIgnoreProperties(ignoreUnknown = true)
class InvoiceDetail {
	var invoiceNumber: String? = null
	var invoiceState: String? = null
	var invoicePicked: Int? = null
	var invoiceAmount: BigDecimal? = null
	var invoiceLines: List<InvoiceLine> = ArrayList()
}

@JsonIgnoreProperties(ignoreUnknown = true)
class ProcedureCode {
	var code: String? = null
	var name: String? = ""

}

@JsonIgnoreProperties(ignoreUnknown = true)
class DoctorDetail {
	var code: String? = null
	var name: String? = null
	var speciality: Any? = null
}

@JsonIgnoreProperties(ignoreUnknown = true)
class DiagnosisDetail {
	var code: Any? = null
	var name: Any? = null
	var codingStandard: String? = null
	var codingStandardVersion: Any? = null
	var isPrimary: String? = null
}

@JsonIgnoreProperties(ignoreUnknown = true)
class DiagnosisClaimRes {
	var claimRef: String? = null
	var aggregateId: String = ""
	var benefitId: Long = 0
	var encounterId: Long = 0
	var memberNumber: String = ""
	var memberName: String? = null
	var memberLimit: String? = null
	var deviceId: String? = null
	var lctStartDate: String? = null
	var lctEndDate: String? = null
	var providerStartDate: String? = null
	var providerEndDate: String? = null
	var statusId: Int? = null
	var statusName: String? = null
	var patientNumber: String? = null
	var payerCode: String? = null
	var payerName: String? = null
	var schemeCode: String? = null
	var schemeName: String? = null
	var visitNumber: String? = null
	var visitType: String? = null
	var branchCode: String? = null
	var branchName: String? = null
	var claimStatus: String? = null
	var diagnosisDetails: List<DiagnosisDetail>? = null
	var doctorDetails: List<DoctorDetail>? = null
	var invoiceDetails: List<InvoiceDetail>? = ArrayList()
	var procedureCodes: List<ProcedureCode>? = ArrayList()
}

@JsonIgnoreProperties(ignoreUnknown = true)
class ClaimRes {
	var claimRef: String? = null
	var aggregateId: String = ""
	var benefitId: Long = 0
	var encounterId: Long = 0
	var memberNumber: String = ""
	var memberName: String? = null
	var memberLimit: String? = null
	var deviceId: String? = null
	var lctStartDate: String? = null
	var lctEndDate: String? = null
	var providerStartDate: String? = null
	var providerEndDate: String? = null
	var statusId: Int? = null
	var statusName: String? = null
	var patientNumber: String? = null
	var payerCode: String? = null
	var payerName: String? = null
	var schemeCode: String? = null
	var schemeName: String? = null
	var visitNumber: String? = null
	var visitType: String? = null
	var branchCode: String? = null
	var branchName: String? = null
	var diagnosisDetails: List<DiagnosisDetail>? = null
	var doctorDetails: List<DoctorDetail>? = null
	var invoiceDetails: List<InvoiceDetail>? = ArrayList()
	var procedureCodes: List<ProcedureCode>? = ArrayList()
}

class Control {
	var error: Boolean? = null
	var customError: Boolean? = null
	var message: String? = null
}