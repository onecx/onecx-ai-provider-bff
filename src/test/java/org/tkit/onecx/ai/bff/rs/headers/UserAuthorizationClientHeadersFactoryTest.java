package org.tkit.onecx.ai.bff.rs.headers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

import org.junit.jupiter.api.Test;

class UserAuthorizationClientHeadersFactoryTest {

    private final UserAuthorizationClientHeadersFactory factory = new UserAuthorizationClientHeadersFactory();

    @Test
    void mapsIncomingAuthorizationToUserAuthorizationAndPreservesServiceAuthorization() {
        MultivaluedMap<String, String> incoming = new MultivaluedHashMap<>();
        incoming.putSingle("Authorization", "Bearer user-token");
        incoming.putSingle("apm-principal-token", "tenant-user");

        MultivaluedMap<String, String> outgoing = new MultivaluedHashMap<>();
        outgoing.putSingle("Authorization", "Bearer service-token");

        MultivaluedMap<String, String> result = factory.update(incoming, outgoing);

        assertEquals("Bearer service-token", result.getFirst("Authorization"));
        assertEquals("Bearer user-token", result.getFirst("UserAuthorization"));
        assertEquals("tenant-user", result.getFirst("apm-principal-token"));
    }
}
