package com.userPresence1.userPresence1;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Set;

@RestController
@RequestMapping("/api/sse")
@CrossOrigin(origins = "*")
public class PresenceSSEController {

    private final Sinks.Many<Set<String>> sink = Sinks.many().multicast().onBackpressureBuffer();

    @Autowired
    private AuthController authController;

    @GetMapping(value = "/active-users", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Set<String>> streamActiveUsers() {
        return sink.asFlux();
    }

    // ✅ FIXED: Add parameter

}
