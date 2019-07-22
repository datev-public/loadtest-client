package de.datev.loadtest.client.entity;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

@ReadingConverter
public class BytesToLogEventConverter implements Converter<byte[], LogEvent> {

    private final Jackson2JsonRedisSerializer<LogEvent> serializer;

    public BytesToLogEventConverter() {

        this.serializer = new Jackson2JsonRedisSerializer<>(LogEvent.class);
        final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json()
                .serializationInclusion(JsonInclude.Include.NON_NULL) // Don’t include null values
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS) //ISODate
                .build();
        this.serializer.setObjectMapper(objectMapper);
    }

    @Override
    public LogEvent convert(byte[] value) {
        return serializer.deserialize(value);
    }
}