package net.lctafrica.claimsapis.controller

import io.swagger.v3.oas.annotations.Operation
import net.lctafrica.claimsapis.dto.ClaimImport
import net.lctafrica.claimsapis.dto.ErrorResponse
import net.lctafrica.claimsapis.dto.TransactionMigrationDto
import net.lctafrica.claimsapis.service.IDataMigrationService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.*
import org.springframework.web.context.request.WebRequest
import org.springframework.web.multipart.MultipartFile


@RestController
@RequestMapping("/api/v1/migration")
class MigrationController(val service: IDataMigrationService) {

    @PostMapping(value=["/claims"])
    fun migrateClaims(@RequestBody dto: ClaimImport) = service.saveClaims(dto)

    @GetMapping(value=["/errors"])
    fun getErrors() = service.getErrors()

    @PostMapping(value=["/transactions"])
    fun migrateTransactions(@RequestBody dto: TransactionMigrationDto) = service.migrateTransactions(dto)

    @GetMapping(value = ["transactionsErrors/{page}/{size}"], produces = ["application/json"])
    @Operation(
        summary = "Get paged result of migrated transaction errors"
    )
    fun transactionsErrors(
        @PathVariable("page") page: Int, @PathVariable("size") size: Int
    ) = service.getTransactionErrors(page, size)

    @PostMapping(value = ["/massUpload/previousPeriod/visits"], produces = ["application/json"],
        consumes =
    ["multipart/form-data"])
    @Operation(summary = "Save Previous Visits from excel config file ")
    fun savePreviousPeriodVisitsFromFile(@RequestParam("file") file: MultipartFile) = service
        .savePreviousPeriodVisitsFromFile(file)

    @PostMapping(value = ["/massUpload/currentPeriod/visits"], produces = ["application/json"],
        consumes =
        ["multipart/form-data"])
    @Operation(summary = "Save Current Period Visits from excel config file ")
    fun saveCurrentPeriodVisitsFromFile(@RequestParam("file") file: MultipartFile) = service
        .saveCurrentPeriodVisitsFromFile(file)

}