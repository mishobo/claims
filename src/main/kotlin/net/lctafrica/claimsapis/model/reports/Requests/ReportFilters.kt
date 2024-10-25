package net.lctafrica.claimsapis.model.reports.Requests

import com.fasterxml.jackson.annotation.JsonFormat
import org.springframework.format.annotation.DateTimeFormat
import java.time.LocalDate

data class ReportFilters (
        val planId:Int,
        val payerId:Int,
        @field:DateTimeFormat(pattern = "yyyy-MM-dd")
        var startDate: LocalDate?,
        @field:DateTimeFormat(pattern = "yyyy-MM-dd")
        var endDate: LocalDate?
        )