package org.tkit.onecx.ai.bff.rs.headers;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

@ApplicationScoped
public class UserAuthorizationClientHeadersFactory implements ClientHeadersFactory {

    private static final String APM_PRINCIPAL_TOKEN = "apm-principal-token";
    private static final String USER_AUTHORIZATION = "UserAuthorization";

    @Override
    public MultivaluedMap<String, String> update(MultivaluedMap<String, String> incomingHeaders,
            MultivaluedMap<String, String> clientOutgoingHeaders) {
        MultivaluedMap<String, String> updatedHeaders = new MultivaluedHashMap<>();
        if (clientOutgoingHeaders != null) {
            updatedHeaders.putAll(clientOutgoingHeaders);
        }

        propagateHeader(incomingHeaders, updatedHeaders, APM_PRINCIPAL_TOKEN);

        String incomingAuthorization = getFirstIgnoreCase(incomingHeaders, HttpHeaders.AUTHORIZATION);
        if (incomingAuthorization != null && !incomingAuthorization.isBlank()) {
            updatedHeaders.putSingle(USER_AUTHORIZATION, incomingAuthorization);
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
