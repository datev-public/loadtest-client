package de.datev.loadtest.client.entity;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

@WritingConverter
public class LogEventToByteConverter implements Converter<LogEvent, byte[]> {

    private final Jackson2JsonRedisSerializer<LogEvent> serializer;

    public LogEventToByteConverter() {

        serializer = new Jackson2JsonRedisSerializer<>(LogEvent.class);
        final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json()
                .serializationInclusion(JsonInclude.Include.NON_NULL) // Don’t include null values
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS) //ISODate
                .build();
        serializer.setObjectMapper(objectMapper);
    }

    @Override
    public byte[] convert(LogEvent value) {
        return serializer.serialize(value);
    }
}