package net.lctafrica.claimsapis.model.intergratedDocuments

import com.fasterxml.jackson.annotation.JsonFormat
import org.springframework.format.annotation.DateTimeFormat
import java.time.LocalDate

data class InvoiceDetails (
        val invoiceNumber:String?,
        val mainProvider:String?
        )