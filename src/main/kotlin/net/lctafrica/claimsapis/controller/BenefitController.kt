package net.lctafrica.claimsapis.controller

import net.lctafrica.claimsapis.dto.ConsumeBenefitDTO
import net.lctafrica.claimsapis.dto.CreateBenefitDTO
import net.lctafrica.claimsapis.service.IBenefitService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/benefit")
class BenefitController(val service: IBenefitService) {

    @PostMapping(value = ["/register"], produces = ["application/json"])
    fun registerBenefits(@RequestBody dto: CreateBenefitDTO) = service.addNew(dto)

    @GetMapping(value = ["/search/{beneficiaryId}"], produces = ["application/json"])
    fun search(
        @PathVariable(value = "beneficiaryId") memberNumber: Long,
    ) = service.findActiveByBeneficiaryId(memberNumber)

    @PostMapping(value=["/consume"], produces = ["application/json"])
    fun consumeBenefit(@RequestBody dto: ConsumeBenefitDTO) = service.consumeBenefit(dto)
}