package net.lctafrica.claimsapis.apacheCamel.route.benefitMngt

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.lctafrica.claimsapis.dto.*
import net.lctafrica.claimsapis.repository.DiagnosisRepository
import net.lctafrica.claimsapis.repository.InvoiceRepository
import net.lctafrica.claimsapis.repository.VisitRepository
import net.lctafrica.claimsapis.service.BenefitService
import net.lctafrica.claimsapis.util.Constants.BRITAM_PAYER_ID
import net.lctafrica.claimsapis.util.Constants.LIAISON_PAYER_ID
import net.lctafrica.claimsapis.util.Constants.PACIS_PAYER_ID
import net.lctafrica.claimsapis.util.Constants.STAGING_PAYERS
import net.lctafrica.claimsapis.util.Result
import net.lctafrica.claimsapis.util.ResultFactory
import net.lctafrica.claimsapis.util.gson
import org.apache.camel.CamelContext
import org.apache.camel.Exchange
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.component.jackson.JacksonDataFormat
import org.apache.camel.component.jackson.ListJacksonDataFormat
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.math.BigDecimal


/*
* Avenue----------------------------------AVENUE
* Mater-----------------------------------MATER
* Nairobi Hospital------------------------NAIROBIHOSPITAL
* Getrudes--------------------------------GETRUDES
* MP SHAH---------------------------------MPSHAH
* METROPOLITAN----------------------------METROPOLITAN
* AGA KHAN KISUMU-------------------------AGAKHANKISUMU
* AGA KHAN MSA----------------------------AGAKHANMOMBASA
* AGA KHAN NAIROBI------------------------AGAKHANNAIROBI
*
* */

@Component
class VisitManagement(
    context: CamelContext,
    @Autowired
    val objectMapper: ObjectMapper,
    val unProcessedVisits: UnProcessedVisits,
    val integratedUnProcessedVisits: IntegratedUnProcessedVisits,
    val bodyTransformer: BodyTransformer,
    val processBalance: ProcessBalance,
    val processClaim: ProcessClaim,
    val pushToPayer: PushToPayer,
    val setVisitToInactive: SetVisitToInactive,
    val setVisitToClosedIfAmountMoreThanZero: SetVisitToClosedIfAmountMoreThanZero,
    val payerIntegrations: PayerIntegrations


) : RouteBuilder(context) {

    var formatToAggregateRecord: JacksonDataFormat = ListJacksonDataFormat(AggregateRecord::class.java)
    override fun configure() {
        from("direct:standalone-deduct-balance").process { exchange ->
            val e = exchange.getIn().body
            val body: String = objectMapper.writeValueAsString(exchange.getIn().body)
            exchange.getIn().body = JsonParser().parse(body).asJsonArray
        }
            .bean(bodyTransformer)
            .process { exchange ->
                print("-----------------")
                if (exchange.getIn().body == null) {
                    exchange.isRouteStop = true
                } else {
                    val item: JsonNode = objectMapper.readTree(exchange.getIn().body.toString())
                    val visitNumber = item["id"].asLong()
                    val aggregateId = item["aggregateId"].asText()!!
                    val amount = item["amount"].asText()!!
                    val benefitId = item["benefitId"].asLong()
                    val benefitName = item["benefitName"].asText()!!
                    val memberNumber = item["memberNumber"].asText()!!
                    exchange.getIn().setHeader("aggregateId", aggregateId)
                    exchange.getIn().body =
                        AggregateRecord(
                            aggregateId, amount, benefitName,
                            memberNumber, visitNumber, benefitId
                        )
                }
            }.marshal(formatToAggregateRecord)
            .to("direct:pickClaim")

        from("direct:pickClaim")
            .multicast().to("direct:sendtoBenefitManagement", "direct:processClaim")

        from("direct:sendtoBenefitManagement")
            .setHeader(Exchange.HTTP_METHOD, constant("POST"))
            .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
            .to("log:DEBUG?showBody=true&showHeaders=true")
            .log("benefit management process")
            .bean(method(processBalance))

        from("direct:processClaim")
            .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
            .to("log:DEBUG?showBody=true&showHeaders=true")
            .bean(method(processClaim))

//        Update visit which has been active more than 24 hours to inactive
        from("timer:setVisitToInactive?period=8hour")
            .bean(method(setVisitToInactive))

        //        Update visit which has been active more than 24 hours to closed if amount more
        //        than zero
        from("timer:setVisitWithBillToClosed?period=8hour")
            .bean(method(setVisitToClosedIfAmountMoreThanZero))

        from("direct:sendToPayer")
            .routeId("payerClaims")
            .bean(method(payerIntegrations))

    }
}

@Component
class PayerIntegrations(
    @Autowired
    val pacisIntegration: PacisIntegration,
    @Autowired
    val britamIntegration: BritamIntegration,
    @Autowired
    val liaisonIntegration: LiaisonIntegration,
    @Autowired
    val pushToPayer: PushToPayer
    ){

    fun payerIntegrations(exchange: Exchange) {
        val visit: Visit = exchange.`in`.getBody(Visit::class.java)
        println(visit)

        if (visit.payerId == PACIS_PAYER_ID) {
            pacisIntegration.pacis(visit)
        } else if (visit.payerId == BRITAM_PAYER_ID) {
            britamIntegration.britamPayload(visit)
        } else if (visit.payerId == LIAISON_PAYER_ID) {
            liaisonIntegration.liaisonPayload(visit)
        } else {
            for( staging in STAGING_PAYERS){
                if(visit.payerId == staging){
                    pushToPayer.sendClaims(visit)
                }
            }
        }

    }
}

@Component
class LiaisonIntegration(
    private val visitRepo: VisitRepository,
    private val invoiceRepository: InvoiceRepository,
    private val diagRepo: DiagnosisRepository
) {

    @Value("\${lct-africa.liaison.url}")
    lateinit var liaisonUrl: String

    @Value("\${lct-africa.member.search.url}")
    lateinit var memberSearchUrl: String

    fun liaisonPayload(visit: Visit) {
        val diagnoses = diagRepo.fetchByVisit(visit)
        val liaisonDiagnosis = mutableListOf<LiaisonDiagnosisDto>()
        val invoices = invoiceRepository.fetchByVisit(visit)
        val liaisonInvoices = mutableListOf<LiaisonInvoicesDto>()
        val documents = mutableListOf<LiaisonDocumentDto>()

        invoices.forEach { it ->
            val liaisonInvoiceLines = mutableListOf<LiaisonInvoiceLinesDto>()
            val invoiceLines = it.invoiceLines
            for (line in invoiceLines) {
                val invoiceLine = LiaisonInvoiceLinesDto(
                    unit = line.unitPrice!!,
                    quantity = line.quantity!!,
                    amount = line.lineTotal!!,
                    description = line.description!!,
                    claimService = 1
                )
                liaisonInvoiceLines.add(invoiceLine)
            }

            val invoice = LiaisonInvoicesDto(
                invoiceNo = it.invoiceNumber!!,
                claimAmount = it.totalAmount!!,
                lineItems = liaisonInvoiceLines
            )
            liaisonInvoices.add(invoice)
        }

        diagnoses.forEach { it ->
            val diagnosis = LiaisonDiagnosisDto(
                icd10 = it.code.toString(),
                description = it.title!!
            )
            liaisonDiagnosis.add(diagnosis)
        }

        val document = LiaisonDocumentDto(
            type = "INVOICE",
            path = "/path/to/claim_form"
        )
        documents.add(document)

        val membershipClient = WebClient.builder()
            .baseUrl(memberSearchUrl).build()

        var mappings = Mapping(
            payerId = visit.payerId.toLong(),
            providerCode = visit.hospitalProviderId.toString(),
            benefitCode = visit.providerMapping?.toLong() ?: 0,
            name = null,
            providerName = null,
            schemeName = null,
            serviceId = 0,
            serviceGroup = null,
            payerName = null
        )

        if (mappings.benefitCode!!<1) {
            val mappingResult = membershipClient
                .get()
                .uri { u ->
                    u
                        .path("/api/v1/membership/payer/mappings")
                        .queryParam("providerId", visit.hospitalProviderId)
                        .queryParam("payerId", visit.payerId)
                        .queryParam("benefitId", visit.benefitId)
                        .queryParam("categoryId", visit.categoryId)
                        .build()
                }
                .retrieve()
                .bodyToMono(String::class.java)
                .block()
            val mapRes = gson.fromJson(mappingResult.toString(), ProviderMappingRes::class.java)
            println(mapRes)
            mappings = mapRes.data
        }


        val claim = LiaisonDTO(
            claimId = visit.id,
            memberNo = visit.memberNumber,
            benefit = mappings.benefitCode.toString(),
            invoiceDate = visit.createdAt!!.toLocalDate(),
            dateReceived = visit.updatedAt!!.toLocalDate(),
            provider = mappings.providerCode.toString(),
            invoices = liaisonInvoices,
            diagnosis = liaisonDiagnosis,
            document = documents
        )
        val jsonClaim = gson.toJson(claim)
        println("liaison claim payload to payer $jsonClaim")

        val stagingClient = WebClient.builder().baseUrl(liaisonUrl).build()

        val remoteResponse = stagingClient.post()
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body(Mono.just(jsonClaim), String::class.java)
            .retrieve()
            .bodyToMono(String::class.java)
            .block()
    }

}

@Component
class BritamIntegration(
    private val visitRepo: VisitRepository,
    private val invoiceRepository: InvoiceRepository,
    private val diagRepo: DiagnosisRepository
) {

    @Value("\${lct-africa.britam.url}")
    lateinit var britamUrl: String

    @Value("\${lct-africa.member.search.url}")
    lateinit var memberSearchUrl: String


    fun britamPayload(visit: Visit) {
        val invoices = invoiceRepository.fetchByVisit(visit)
        val britamInvoiceLines = mutableListOf<BritamInvoiceLines>()
        val diagnoses = diagRepo.fetchByVisit(visit)
        val britamDiagnosis = mutableListOf<BritamDiagnosisDTO>()

        val membershipClient = WebClient.builder()
            .baseUrl(memberSearchUrl).build()

        var mappings = Mapping(
            payerId = visit.payerId.toLong(),
            providerCode = visit.hospitalProviderId?.toString(),
            benefitCode = visit.providerMapping?.toLong() ?: 0,
            name = null,
            providerName = null,
            schemeName = null,
            serviceId = 0,
            serviceGroup = null,
            payerName = null
        )

        if (mappings.benefitCode!!<1) {
            val mappingResult = membershipClient
                .get()
                .uri { u ->
                    u
                        .path("/api/v1/membership/payer/mappings")
                        .queryParam("providerId", visit.hospitalProviderId)
                        .queryParam("payerId", visit.payerId)
                        .queryParam("benefitId", visit.benefitId)
                        .queryParam("categoryId", visit.categoryId)
                        .build()
                }
                .retrieve()
                .bodyToMono(String::class.java)
                .block()
            val mapRes = gson.fromJson(mappingResult.toString(), ProviderMappingRes::class.java)
            println(mapRes)
            mappings = mapRes.data
        }

        invoices.forEach { it ->
            val invoiceLine = it.invoiceLines
            for (line in invoiceLine) {
                val invoice = BritamInvoiceLines(
                    activityCode = "Code68",
                    requestedPrice = line.lineTotal!!,
                    requestedCurrency = "Kes",
                    activityName = line.description,
                    quantity = line.quantity.toString(),
                    GUID = ""
                )
                britamInvoiceLines.add(invoice)
            }
        }

        diagnoses.forEach { it ->
            val diagnosis = BritamDiagnosisDTO(
                diagnosisCode = it.code!!,
                description = it.title!!,
                GUID = ""
            )
            britamDiagnosis.add(diagnosis)
        }

        val claim = BritamDTO(
            invoiceNumber = visit.invoiceNumber!!,
            invoiceDate = visit.updatedAt!!.toLocalDate(),
            eventType = "Illness",
            providerName = mappings.providerName!!,
            providerCode = mappings.providerCode?.toLong()!!,
            invoiceAmount = visit.totalInvoiceAmount!!,
            currency = "Kes",
            patientName = visit.memberName,
            memberShipNo = visit.memberNumber,
            servicePlace = mappings.serviceGroup,
            networkCode = 2,
            systemType = "LCT",
            GUID = "",
            schemeName = mappings.schemeName!!,
            invoiceLines = britamInvoiceLines,
            diagnosis = britamDiagnosis
        )

        val req = Req(claim)
        val claimJson = gson.toJson(req).toString()
        println("britam claim $claimJson")

        val stagingClient = WebClient.builder().baseUrl(britamUrl).build()

        val remoteResponse = stagingClient.post()
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body(Mono.just(claimJson), String::class.java)
            .retrieve()
            .bodyToMono(String::class.java)
            .block()

        val britamResponse = gson.fromJson(remoteResponse.toString(), BritamClaimResponse::class.java)
        println("britam claim response :: ${britamResponse.res.invoiceId}")

        visitRepo.updateVisitInvoiceIdSentToPayer(britamResponse.res.invoiceId, visit.id)
    }
}


@Component
class PacisIntegration(
    private val visitRepo: VisitRepository,
    private val invoiceRepository: InvoiceRepository,
    private val diagRepo: DiagnosisRepository
) {

    @Value("\${lct-africa.member.search.url}")
    lateinit var memberSearchUrl: String


    fun pacis(visit: Visit): PacisDTO {
        val invoices = invoiceRepository.fetchByVisit(visit)
        val pacisInvoiceLines = mutableListOf<PacisInvoiceLines>()
        val diagnoses = diagRepo.fetchByVisit(visit)
        val pacisDiagnosis = mutableListOf<PacisDiagnosisDTO>()

        println("visit: $visit")

        val membershipClient = WebClient.builder()
            .baseUrl(memberSearchUrl).build()

        val mappingResult = membershipClient
            .get()
            .uri { u ->
                u
                    .path("/api/v1/membership/payer/mappings")
                    .queryParam("providerId", visit.hospitalProviderId)
                    .queryParam("payerId", visit.payerId)
                    .queryParam("benefitId", visit.benefitId)
                    .build()
            }
            .retrieve()
            .bodyToMono(String::class.java)
            .block()

        val mapRes = gson.fromJson(mappingResult.toString(), PayerMappingRes::class.java)

        diagnoses.forEach { it ->
            val diagnosis = PacisDiagnosisDTO(
                codingStandard = "icd11",
                diagnosisName = it.title,
                diagnosisCode = it.code
            )
            pacisDiagnosis.add(diagnosis)
        }

        invoices.forEach { it ->
            val invoiceLine = it.invoiceLines
            for (line in invoiceLine) {
                val invoice = PacisInvoiceLines(
                    invoiceNumber = line.invoiceNumber,
                    service = 1,
                    hasDiagnosis = 1,
                    quantity = line.quantity,
                    cost = line.unitPrice,
                    apiDiagnosis = pacisDiagnosis
                )
                pacisInvoiceLines.add(invoice)
            }
        }

        val claim = PacisDTO(
            memberNumber = visit.memberNumber,
            billId = visit.aggregateId,
            providerCode = mapRes.data.providerCode,
            benefitCode = mapRes.data.benefitCode,
            totalAmount = visit.totalInvoiceAmount,
            invoiceDate = visit.updatedAt!!.toLocalDate(),
            patientNumber = null,
            patientSigned = "0",
            doctorSigned = "0",
            documentLink = null,
            preAuthNumber = null,
            apiBillDetails = pacisInvoiceLines
        )

        val claimJson = gson.toJson(claim)
        println("pacis claim $claimJson")
        return claim
    }


}

@Component
class ProcessBalance(
    private val visitRepo: VisitRepository,
    @Autowired val objectMapper: ObjectMapper,
    @Autowired val benefitService: BenefitService
) {

    fun processMessage(exchange: Exchange): Unit {

        val body = exchange.getIn().getBody(
            String::class.java
        )

        val item: JsonNode = objectMapper.readTree(body)
        //println(item)

        try {
            val aggregateId = item["aggregateId"].asText()!!
            val amount = item["amount"].asLong()
            val benefitName = item["benefitName"].asText()!!
            val memberNumber = item["memberNumber"].asText()!!
            val visitNumber = item["visitNumber"].asLong()
            val benefitId = item["benefitId"].asLong()

            val response = benefitService.consumeBenefit(
                ConsumeBenefitDTO(
                    amount = BigDecimal.valueOf
                        (amount),
                    aggregateId = aggregateId,
                    benefitId = benefitId,
                    memberNumber = memberNumber,
                    visitNumber = visitNumber,

                    )
            )
            //println(response)
        } catch (e: Exception) {
            //println(e)
        }


    }
}

@Component
class BodyTransformer {

    fun processMessage(jsonArray: JsonArray): JsonObject? {
        //println(jsonArray)
        if (jsonArray.size() > 0) {
            for (claim in jsonArray) {
                val jsonObject: JsonObject = JsonObject()
                jsonObject.addProperty("id", claim.asJsonObject.get("id").asString)
                jsonObject.addProperty(
                    "aggregateId", claim.asJsonObject.get("aggregateId")
                        .asString
                )
                jsonObject.addProperty(
                    "amount",
                    claim.asJsonObject.get("totalInvoiceAmount").asString
                )
                jsonObject.addProperty(
                    "benefitName",
                    claim.asJsonObject.get("benefitName").asString
                )
                jsonObject.addProperty(
                    "memberNumber",
                    claim.asJsonObject.get("memberNumber").asString
                )
                jsonObject.addProperty("benefitId", claim.asJsonObject.get("benefitId").asString)

                return jsonObject
            }
        }
        return null

    }
}

@Component
class ProcessClaim(
    private val visitRepo: VisitRepository,
    @Autowired val objectMapper: ObjectMapper
) {

    fun processMessage(exchange: Exchange): Result<Visit> {

        val body = exchange.getIn().getBody(String::class.java)
        val item: JsonNode = objectMapper.readTree(body)
        val visitNumber = item["visitNumber"].asLong()

        val visit = visitRepo.findById(visitNumber)
        visit.ifPresent {
            it.apply {
                claimProcessStatus = ClaimProcessStatus.PROCESSED
            }
            visitRepo.save(it)
        }
        return ResultFactory.getSuccessResult(visit.get())
    }
}

@Component
class SetVisitToInactive(private val visitRepo: VisitRepository) {

    fun setVisitToInactiveAfter24hours() {
        return visitRepo.updateVisitToInactiveAfter24Hours()
    }
}

@Component
class SetVisitToClosedIfAmountMoreThanZero(private val visitRepo: VisitRepository) {

    fun setVisitToInactiveAfter24hours() {
        return visitRepo.updateVisitWithBillToClosedAfter24Hours()
    }
}

@Component
class UnProcessedVisits(private val visitRepo: VisitRepository) {

    fun getUnprocessedVisits(): MutableList<Visit?> {
        return visitRepo
            .findAllByStatusAndClaimProcessStatusAndProviderMiddleware(
                Status.CLOSED,
                ClaimProcessStatus.UNPROCESSED,
                MIDDLEWARENAME.NONE
            )
    }
}

@Component
class IntegratedUnProcessedVisits(private val visitRepo: VisitRepository) {

    fun getUnprocessedVisits(): MutableList<Visit?> {
        return visitRepo
            .findAllByStatusAndClaimProcessStatusAndProviderMiddlewareNot(
                Status.CLOSED,
                ClaimProcessStatus.UNPROCESSED,
                MIDDLEWARENAME.NONE
            )
    }
}

@Component
class PushToPayer(
    private val visitRepo: VisitRepository,
    private val invoiceRepository: InvoiceRepository,
    private val diagRepo: DiagnosisRepository
) {
    @Value("\${lct-africa.staging.url}")
    lateinit var stagingUrl: String

    @Value("\${lct-africa.member.search.url}")
    lateinit var memberSearchUrl: String

    fun sendInBatch(): Result<MutableList<Visit>> {
        val batch = mutableListOf<Visit>()

        val integrated =
            visitRepo.findTop200ByPayerIdInAndStatusAndPayerStatusIsNullAndProviderMiddlewareNot(
                payerId = STAGING_PAYERS,
                status = Status.CLOSED
            )
        val standAlone =
            visitRepo.findTop200ByPayerIdInAndStatusAndPayerStatusIsNullAndInvoiceNumberIsNotNullAndProviderMiddleware(
                payerId = STAGING_PAYERS, status = Status.LINE_ITEMS_ADDED
            )
        batch.addAll(integrated)
        batch.addAll(standAlone)
        batch.forEach {
            sendClaims(it)
        }
        return ResultFactory.getSuccessResult(batch)

        /*try {
            batch.forEach {
                sendClaims(it)
            }
            return ResultFactory.getSuccessResult(batch)
        } catch (e: Exception) {
            println("****************************************************************************")
            println(e.stackTrace)
            println("****************************************************************************")
            println(e.stackTrace)
            return ResultFactory.getFailResult(msg = e.message)
        }*/
    }

    fun sendClaims(visit: Visit) {
        // push KenGen and all Staging-based integrations
        println("push KenGen and all Staging-based integrations")

        val membershipClient = WebClient.builder()
            .baseUrl(memberSearchUrl).build()

        val mappingResult = membershipClient
            .get()
            .uri { u ->
                u
                    .path("/api/v1/membership/mapping")
                    .queryParam("providerId", visit.hospitalProviderId)
                    .queryParam("payerId", visit.payerId)
                    .build()
            }
            .retrieve()
            .bodyToMono(String::class.java)
            .block()

        val mapRes = gson.fromJson(mappingResult.toString(), PayerProviderMapRes::class.java)
        val claim = buildPayload(visit, mapRes.data.code)
        claim?.let {
            sendToStaging(it)
            updatePayerStatus(visit = visit, repository = visitRepo, status = PayerStatus.SENT)
        }
    }

    fun buildPayload(visit: Visit, mappingCode: String): OutBoundClaimDTO? {
        val diags = diagRepo.fetchByVisit(visit)
        val diagList = mutableListOf<OutBoundDiagnosisDTO>()
        val outboundInvoices = mutableListOf<OutBoundInvoiceDTO>()
        val invoices = invoiceRepository.fetchByVisit(visit)
        invoices.forEach { invoice ->
            val invoiceLines = mutableListOf<OutBoundInvoiceLineDTO>()
            val invoiceLineSet = invoice.invoiceLines
            for (line in invoiceLineSet) {
                val outboundInvoiceLine = OutBoundInvoiceLineDTO(
                    description = line.description.toString(),
                    quantity = line.quantity?.toInt(),
                    amount = line.lineTotal!!,
                    unit = line.unitPrice,
                    itemName = line.description.toString()
                )
                invoiceLines.add(outboundInvoiceLine)
            }
            val outboundInvoice = OutBoundInvoiceDTO(
                invoiceNumber = invoice.invoiceNumber ?: visit.invoiceNumber!!,
                total = invoice.totalAmount!!,
                items = invoiceLines
            )
            outboundInvoices.add(outboundInvoice)
        }
        diags.forEach {
            val diag = OutBoundDiagnosisDTO(
                icd10code = it.code!!,
                description = it.title
            )
            diagList.add(diag)
        }

        val claim = OutBoundClaimDTO(
            benefit = visit.benefitName,
            payerCode = visit.payerId.toInt(),
            diagnoses = diagList,
            invoices = outboundInvoices,
            claimDate = visit.updatedAt!!.toLocalDate(),
            providerName = visit.hospitalProviderId!!.toString(),
            providerCode = mappingCode,
            memberName = visit.memberName,
            memberNumber = visit.memberNumber
        )
        return claim
    }

    fun sendToStaging(claim: OutBoundClaimDTO) {
        println("sendToStaging :"+gson.toJson(claim))
        val stagingClient = WebClient.builder()
            .baseUrl(stagingUrl).build()
        //println(gson.toJson(claim))
        val remoteResponse = stagingClient.post()
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body(Mono.just(gson.toJson(claim)), String::class.java)
            .retrieve()
            .bodyToMono(String::class.java)
            .block()
    }

}

fun updatePayerStatus(visit: Visit, status: PayerStatus, repository: VisitRepository) {
    visit.payerStatus = status
    repository.save(visit)
}


fun callPayerAPI(claim: String, api: String) {
    println("payload to payer $claim")
    val jsonClaim = gson.toJson(claim)
    println("payload to payer $jsonClaim")

    val stagingClient = WebClient.builder().baseUrl(api).build()

    val remoteResponse = stagingClient.post()
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .body(Mono.just(gson.toJson(claim)), String::class.java)
        .retrieve()
        .bodyToMono(String::class.java)
        .block()

}

