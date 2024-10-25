package net.lctafrica.claimsapis.model.reports

import java.math.BigDecimal
import java.time.LocalDateTime

data class Financial (
        val visitNumber: Long?,
        val memberNumber:String?,
        val memberName: String?,
        val invoiceDate: String?,
        val invoiceNumber:String?,
        val totalInvoiceAmount: BigDecimal?,
        val providerName:String?,
        val benefitName:String?,
        val category: String?,
        val categoryDesc: String?,
        val status:String?,
        val claimType: String?,
        val reimbursementInvoiceDate:String?,
        val sentToPayer:String?,
        val payer:String?
    )