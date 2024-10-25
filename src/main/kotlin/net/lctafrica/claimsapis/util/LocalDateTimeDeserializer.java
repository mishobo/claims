package net.lctafrica.claimsapis.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class LocalDateTimeDeserializer extends StdDeserializer<LocalDateTime> {

	protected LocalDateTimeDeserializer() {
		super(LocalDate.class);
	}

	@Override
	public LocalDateTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
		try {
			return LocalDateTime.parse(parser.readValueAs(String.class));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

}
