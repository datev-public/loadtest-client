package de.datev.loadtest.client.config.request;

import java.util.UUID;

public class DefaultRequestIdGenerator implements RequestIdGenerator {

    @Override
    public String generateNewId() {

        return UUID.randomUUID().toString();
    }
}
