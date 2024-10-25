package net.lctafrica.claimsapis.model.reports

import java.math.BigDecimal

data class Clinical
(
        val visitNumber:Long?,
        val memberNumber:String?,
        val memberName:String?,
        val invoiceDate:String?,
        val invoiceNumber:String?,
        val icd10Code:String?,
        val diagnosisDesc:String?,
        val serviceDescription:String?,
        val quantity: BigDecimal?,
        val unitPrice:BigDecimal?,
        val lineTotal:BigDecimal?,
        val benefitName:String?,
        val providerName:String?,
        val payer:String?,
        )


