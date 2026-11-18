package org.tkit.onecx.ai.bff.rs.headers;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

@ApplicationScoped
public class UserAuthorizationClientHeadersFactory implements ClientHeadersFactory {

    @ConfigProperty(name = "tkit.rs.context.token.header-param")
    String principalTokenHeader;

    @ConfigProperty(name = "onecx.bff.user-authorization-header")
    String userAuthorizationHeader;

    @Override
    public MultivaluedMap<String, String> update(MultivaluedMap<String, String> incomingHeaders,
            MultivaluedMap<String, String> clientOutgoingHeaders) {
        MultivaluedMap<String, String> updatedHeaders = new MultivaluedHashMap<>();
        if (clientOutgoingHeaders != null) {
            updatedHeaders.putAll(clientOutgoingHeaders);
        }

        propagateHeader(incomingHeaders, updatedHeaders, principalTokenHeader);

        String incomingAuthorization = getFirstIgnoreCase(incomingHeaders, HttpHeaders.AUTHORIZATION);
        if (incomingAuthorization != null && !incomingAuthorization.isBlank()) {
            updatedHeaders.putSingle(userAuthorizationHeader, incomingAuthorization);
        }

        return updatedHeaders;
    }

    private static void propagateHeader(MultivaluedMap<String, String> incomingHeaders,
            MultivaluedMap<String, String> updatedHeaders, String headerName) {
        String value = getFirstIgnoreCase(incomingHeaders, headerName);
        if (value != null && !value.isBlank()) {
            updatedHeaders.putSingle(headerName, value);
        }
    }

    private static String getFirstIgnoreCase(MultivaluedMap<String, String> headers, String headerName) {
        if (headers == null || headers.isEmpty()) {
            return null;
        }

        for (var entry : headers.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(headerName)
                    && entry.getValue() != null && !entry.getValue().isEmpty()) {
                return entry.getValue().get(0);
            }
        }
        return null;
    }
}
