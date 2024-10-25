package net.lctafrica.claimsapis.util

import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.time.LocalDate
import java.time.format.DateTimeFormatter

val gson = GsonBuilder()
    .setPrettyPrinting()
    .registerTypeAdapter(LocalDate::class.java, LocalDateAdapter().nullSafe())
    .create()

class LocalDateAdapter : TypeAdapter<LocalDate>() {

    override fun write(out: JsonWriter, value: LocalDate) {
        out.value(DateTimeFormatter.ISO_LOCAL_DATE.format(value))
    }

    override fun read(input: JsonReader): LocalDate {
        return LocalDate.parse(input.nextString())
    }
}