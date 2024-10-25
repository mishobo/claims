package net.lctafrica.claimsapis.util

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.modelmapper.ModelMapper
import org.modelmapper.convention.MatchingStrategies


object MyModelMapper : ModelMapper() {

    fun init(): MyModelMapper {
        configuration.isSkipNullEnabled = true
        configuration.propertyCondition
        configuration.matchingStrategy = MatchingStrategies.STRICT



        return this
    }

}