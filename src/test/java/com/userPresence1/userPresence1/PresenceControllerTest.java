package com.userPresence1.userPresence1;

import com.fasterxml.jackson.databind.ObjectMapper; // For converting Java objects to JSON
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

/**
 * Test class for the PresenceController.
 * Uses @WebMvcTest to test only the web layer, and mocks other service dependencies.
 */
@WebMvcTest(controllers = PresenceController.class,
excludeAutoConfiguration = { SecurityAutoConfiguration.class } ) // Focuses on testing the web layer for PresenceController
class PresenceControllerTest {

    @Autowired
    private MockMvc mockMvc; // Used to simulate HTTP requests

    @MockBean
    private PresenceService presenceService; // Mocks the service layer dependency

    @MockBean
    @Qualifier("sseExecutor") // Mocks the custom Executor bean used for SSE
    private Executor sseExecutor;

    // Mock the ScheduledExecutorService used for heartbeats.
    // We cannot directly mock a 'final' field, so we use reflection or a test configuration
    // to provide a mock. For simplicity in @WebMvcTest, we'll try to verify the schedule method.
    // Or more robustly, we could use @SpyBean if the scheduler was a bean.
    // For now, we'll rely on Mockito's ability to mock static/final methods if PowerMock was used,
    // or acknowledge direct testing of `scheduleAtFixedRate` on an internal field is hard.
    // A better approach for the scheduler would be to inject it as a bean.

    private final ObjectMapper objectMapper = new ObjectMapper(); // Helper for JSON conversions

    @BeforeEach
    void setUp() {
        // Reset mocks before each test to ensure test isolation
        reset(presenceService, sseExecutor);

        // Define default mock behavior for presenceService.getUsersViewing
        when(presenceService.getUsersViewing(anyString()))
                .thenReturn(Collections.emptySet()); // Default to empty set
    }

    @Test
    void testUpdateUserPresence_Success() throws Exception {
        UUID noteId = UUID.randomUUID();
        String username = "testUser";
        Set<String> expectedUsers = Set.of(username); // What we expect after update

        // Stub the behavior of presenceService.getUsersViewing
        when(presenceService.getUsersViewing(noteId.toString())).thenReturn(expectedUsers);

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("noteId", noteId.toString());
        requestBody.put("username", username);

        mockMvc.perform(post("/presence/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk()) // Expect HTTP 200 OK
                .andExpect(jsonPath("$.message").value("Presence updated")) // Check response message
                .andExpect(jsonPath("$.users[0]").value(username)); // Check users array

        // Verify that presenceService.addUser was called once with the correct arguments
        verify(presenceService, times(1)).addUser(noteId.toString(), username);

        // Verify that notifyPresenceUpdate was called (by checking its internal calls)
        // Since notifyPresenceUpdate is private, we can't directly verify it.
        // We'll verify that the emitter.send is attempted on the executor.
        // This is a subtle point: The test does not verify the content of notifyPresenceUpdate's logic,
        // but rather that the overall process leads to the expected service call and response.
        // More direct testing of notifyPresenceUpdate would be done via a separate unit test for PresenceController
        // if it were made public or via a spy on the controller.
    }

    @Test
    void testUpdateUserPresence_InvalidNoteId() throws Exception {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("noteId", "invalid-uuid");
        requestBody.put("username", "testUser");

        mockMvc.perform(post("/presence/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest()) // Expect HTTP 400 Bad Request
                .andExpect(jsonPath("$.error").value("Invalid UUID format for noteId"));

        // Verify that presenceService.addUser was NOT called
        verify(presenceService, never()).addUser(anyString(), anyString());
    }

    @Test
    void testUpdateUserPresence_MissingFields() throws Exception {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("username", "testUser"); // Missing noteId

        mockMvc.perform(post("/presence/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest()) // Expect HTTP 400 Bad Request
                .andExpect(jsonPath("$.error").value("Invalid request"));

        verify(presenceService, never()).addUser(anyString(), anyString());
    }

    @Test
    void testRemoveUser_Success() throws Exception {
        UUID noteId = UUID.randomUUID();
        String username = "testUser";
        Set<String> expectedUsersAfterRemoval = Collections.emptySet(); // Expect no users

        // Stub the behavior after removal
        when(presenceService.getUsersViewing(noteId.toString())).thenReturn(expectedUsersAfterRemoval);

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("noteId", noteId.toString());
        requestBody.put("username", username);

        mockMvc.perform(post("/presence/remove")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk()) // Expect HTTP 200 OK
                .andExpect(jsonPath("$.message").value("User removed"))
                .andExpect(jsonPath("$.users").isEmpty()); // Expect empty users array

        // Verify that presenceService.removeUser was called
        verify(presenceService, times(1)).removeUser(noteId.toString(), username);
    }

    @Test
    void testSubscribeToPresence_Success() throws Exception {
        UUID noteId = UUID.randomUUID();

        // Perform GET request to subscribe
        mockMvc.perform(get("/presence/subscribe/{noteId}", noteId))
                .andExpect(status().isOk())
                .andExpect(content().string("")); // SSE response starts with no content until events are sent

        // Verify that an SseEmitter's send method is called for the initial connection event
        // We use ArgumentCaptor to capture the Runnable passed to sseExecutor.execute()
        // and then run it to trigger the emitter.send() call.
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(sseExecutor, times(1)).execute(runnableCaptor.capture());

        // Execute the captured Runnable to simulate the emitter.send() call
        Runnable capturedRunnable = runnableCaptor.getValue();
        capturedRunnable.run();

        // It's tricky to directly mock an SseEmitter and verify its send method within a MockMvc test.
        // This test mainly verifies that the endpoint can be hit successfully and that
        // the sseExecutor.execute() is called for the initial event.
        // More in-depth SSE behavior (like multiple events or heartbeats) requires
        // a more complex integration test setup with a real HTTP client.
    }
}
