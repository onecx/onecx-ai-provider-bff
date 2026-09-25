package org.tkit.onecx.ai.bff.rs.headers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class UserAuthorizationClientHeadersFactoryTest {

    private static final String APM_PRINCIPAL_TOKEN = "apm-principal-token";
    private static final String USER_AUTHORIZATION = "UserAuthorization";

    @Test
    void updateShouldCopyOutgoingAndPropagateHeaders() {
        UserAuthorizationClientHeadersFactory factory = new UserAuthorizationClientHeadersFactory();

        MultivaluedMap<String, String> incoming = new MultivaluedHashMap<>();
        incoming.putSingle("APM-PRINCIPAL-TOKEN", "id-token");
        incoming.putSingle("authorization", "Bearer user-token");

        MultivaluedMap<String, String> outgoing = new MultivaluedHashMap<>();
        outgoing.putSingle(HttpHeaders.AUTHORIZATION, "Bearer app-token");
        outgoing.putSingle("x-trace-id", "trace-1");

        MultivaluedMap<String, String> updated = factory.update(incoming, outgoing);

        assertEquals("Bearer app-token", updated.getFirst(HttpHeaders.AUTHORIZATION));
        assertEquals("trace-1", updated.getFirst("x-trace-id"));
        assertEquals("id-token", updated.getFirst(APM_PRINCIPAL_TOKEN));
        assertEquals("Bearer user-token", updated.getFirst(USER_AUTHORIZATION));
    }

    @Test
    void updateShouldHandleNullOutgoingAndSkipBlankHeaders() {
        UserAuthorizationClientHeadersFactory factory = new UserAuthorizationClientHeadersFactory();

        MultivaluedMap<String, String> incoming = new MultivaluedHashMap<>();
        incoming.putSingle(APM_PRINCIPAL_TOKEN, " ");
        incoming.putSingle(HttpHeaders.AUTHORIZATION, "  ");

        MultivaluedMap<String, String> updated = factory.update(incoming, null);

        assertTrue(updated.isEmpty());
    }

    @Test
    void updateShouldHandleNullIncomingHeaders() {
        UserAuthorizationClientHeadersFactory factory = new UserAuthorizationClientHeadersFactory();

        MultivaluedMap<String, String> outgoing = new MultivaluedHashMap<>();
        outgoing.putSingle(HttpHeaders.AUTHORIZATION, "Bearer app-token");

        MultivaluedMap<String, String> updated = factory.update(null, outgoing);

        assertEquals("Bearer app-token", updated.getFirst(HttpHeaders.AUTHORIZATION));
        assertFalse(updated.containsKey(USER_AUTHORIZATION));
        assertFalse(updated.containsKey(APM_PRINCIPAL_TOKEN));
    }

    @Test
    void getFirstIgnoreCaseShouldReturnNullForEmptyOrInvalidEntries() throws Exception {
        Method method = UserAuthorizationClientHeadersFactory.class
                .getDeclaredMethod("getFirstIgnoreCase", MultivaluedMap.class, String.class);
        method.setAccessible(true);

        assertNull(method.invoke(null, null, HttpHeaders.AUTHORIZATION));

        MultivaluedMap<String, String> empty = new MultivaluedHashMap<>();
        assertNull(method.invoke(null, empty, HttpHeaders.AUTHORIZATION));

        MultivaluedMap<String, String> invalid = new MultivaluedHashMap<>();
        invalid.put(null, new ArrayList<>());
        invalid.put("x-other", new ArrayList<>());
        invalid.put(HttpHeaders.AUTHORIZATION, null);
        invalid.put(APM_PRINCIPAL_TOKEN, new ArrayList<>());

        assertNull(method.invoke(null, invalid, HttpHeaders.AUTHORIZATION));
    }

    @Test
    void getFirstIgnoreCaseShouldSkipMatchingKeyWithEmptyValueList() throws Exception {
        Method method = UserAuthorizationClientHeadersFactory.class
                .getDeclaredMethod("getFirstIgnoreCase", MultivaluedMap.class, String.class);
        method.setAccessible(true);

        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.put(HttpHeaders.AUTHORIZATION, new ArrayList<>());
        assertNull(method.invoke(null, headers, HttpHeaders.AUTHORIZATION));

        headers.put(HttpHeaders.AUTHORIZATION, List.of("Bearer token"));
        assertEquals("Bearer token", method.invoke(null, headers, HttpHeaders.AUTHORIZATION));
    }
}