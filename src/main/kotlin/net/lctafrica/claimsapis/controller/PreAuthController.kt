package net.lctafrica.claimsapis.controller

import io.swagger.v3.oas.annotations.Operation
import net.lctafrica.claimsapis.dto.AuthorizePreAuthDTO
import net.lctafrica.claimsapis.dto.PreAuthDTO
import net.lctafrica.claimsapis.service.IPreAuthService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/preauthorization")
class PreAuthController(val service: IPreAuthService) {

    @PostMapping(value = ["/new"], produces = ["application/json"])
    fun createNew(@RequestBody dto: PreAuthDTO) = service.add(dto)

    @PutMapping(value = ["/authorize"], produces = ["application/json"])
    fun authorize(@RequestBody dto: AuthorizePreAuthDTO) = service.authorize(dto)

    @PutMapping(value = ["/decline"], produces = ["application/json"])
    fun decline(@RequestBody dto: AuthorizePreAuthDTO) = service.decline(dto)

    @GetMapping(value = ["/find"], produces = ["application/json"])
    fun findByAggregate(aggregateId: String) = service.findByAggregate(aggregateId)

    @GetMapping(value = ["/{providerId}/pending"], produces = ["application/json"])
    @Operation(
        summary = "Get Pending Preauths by provider and status at integration/service " +
                "provider"
    )
    fun getPendingPreAuths(@PathVariable("providerId") providerId: Long) =
        service.findPendingByProviderId(providerId)


    @GetMapping(value = ["/{visitNumber}/preauth"], produces = ["application/json"])
    @Operation(
        summary = "Get All Preauths by visitNumber"
    )
    fun getPreAuthByVisitNumber(@PathVariable("visitNumber") visitNumber: Long) =
        service.findByVisitNumber(visitNumber)

    @GetMapping(value = ["/id/{id}/preauth"], produces = ["application/json"])
    @Operation(
        summary = "Get Preauth by id"
    )
    fun getPreAuthById(@PathVariable("id") id: Long) =
        service.findById(id)

    @GetMapping(value = ["/{providerId}/provider"], produces = ["application/json"])
    @Operation(
        summary = "Get All Preauths by provider and status at integration/service " +
                "provider"
    )
    fun getAllPreAuths(@PathVariable("providerId") providerId: Long) =
        service.findAllByProviderId(providerId)


    @GetMapping(value = ["/{payerId}/pending/payer"], produces = ["application/json"])
    @Operation(summary = "Get Pending Preauths by payer and status at Payer")
    fun getPayerPendingPreAuths(@PathVariable("payerId") payerId: Long) =
        service.findPendingByPayerId(payerId)

    @GetMapping(value = ["/{payerId}/payer"], produces = ["application/json"])
    @Operation(summary = "Get All Preauths by payer at Payer")
    fun getAllPayerPreAuths(@PathVariable("payerId") payerId: Long) =
        service.findAllByPayerId(payerId)

}