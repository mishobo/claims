package net.lctafrica.claimsapis.util

import net.lctafrica.claimsapis.dto.Invoice
import net.lctafrica.claimsapis.dto.VisitInvoiceDto

fun Invoice.toInvoiceDto() = VisitInvoiceDto(
    id = id,
    number = invoiceNumber,
    amount = totalAmount,
    invoiceLines = invoiceLines
)