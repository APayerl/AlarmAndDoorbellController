package se.payerl.alarmanddoorbellcontroller.datatypes

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies

class Jackson {
    companion object {
        fun get(failOnUnknowns: Boolean): ObjectMapper {
            val om = ObjectMapper()
            om.propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
            om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, failOnUnknowns)
            return om
        }
    }
}