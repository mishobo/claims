//package net.lctafrica.claimsapis.kafka
//
//import com.fasterxml.jackson.core.JsonProcessingException
//import com.fasterxml.jackson.core.json.JsonReadFeature
//import com.fasterxml.jackson.databind.ObjectMapper
//import com.fasterxml.jackson.databind.SerializationFeature
//import net.lctafrica.claimsapis.dto.CreateBenefitDTO
//import net.lctafrica.claimsapis.repository.BeneficiaryBenefitRepository
//import net.lctafrica.claimsapis.service.IBenefitService
//import org.apache.kafka.clients.consumer.ConsumerRecord
//import org.springframework.beans.factory.annotation.Autowired
//import org.springframework.kafka.annotation.KafkaListener
//import org.springframework.stereotype.Service
//
//
//@Service
//class KafkaConsumer(
//	val repo: BeneficiaryBenefitRepository,
//	private val dataRepository: BeneficiaryBenefitRepository,
//	@Autowired val objectMapper: ObjectMapper,
//	val service: IBenefitService
//) {
//
//	@KafkaListener(topics = ["BENEFIT"], groupId = "benefit-topic-consumer1")
//	@Throws(JsonProcessingException::class)
//	fun onMessage(consumerRecord: ConsumerRecord<String?, String?>?) {
//		objectMapper.configure(
//			JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature(),
//			true
//		);
//		val retValue: String =
//			consumerRecord?.value().toString().replace("[\\\\p{Cntrl}^\\r\\n\\t]+", "")
//
//		objectMapper.disable(SerializationFeature.INDENT_OUTPUT);
//		val s1: String = retValue.substring(retValue.indexOf("{") + 1)
//		val _sb = StringBuilder(s1)
//		_sb.insert(0, "{");
//		println(_sb)
//
//		val crecord: CreateBenefitDTO = objectMapper.readValue(
//			_sb.toString(),
//			CreateBenefitDTO::class.java
//		)
//		service.addNew(crecord)
//		println(crecord)
//	}
//
//}