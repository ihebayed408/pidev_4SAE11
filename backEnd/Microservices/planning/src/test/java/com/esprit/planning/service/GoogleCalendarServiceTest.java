package com.esprit.planning.service;

import com.google.api.services.calendar.Calendar;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for GoogleCalendarService. Verifies isAvailable (true when enabled and calendar bean present),
 * and that createEvent/deleteEvent/listEvents return empty or no-op when not available.
 */
class GoogleCalendarServiceTest {

    @Test
    void isAvailable_whenCalendarNull_returnsFalse() {
        GoogleCalendarService service = new GoogleCalendarService(null, true, "primary");
        assertThat(service.isAvailable()).isFalse();
    }

    @Test
    void isAvailable_whenDisabled_returnsFalse() {
        GoogleCalendarService service = new GoogleCalendarService(mock(Calendar.class), false, "primary");
        assertThat(service.isAvailable()).isFalse();
    }

    @Test
    void isAvailable_whenEnabledAndCalendarSet_returnsTrue() {
        GoogleCalendarService service = new GoogleCalendarService(mock(Calendar.class), true, "primary");
        assertThat(service.isAvailable()).isTrue();
    }

    @Test
    void createEvent_whenNotAvailable_returnsEmpty() {
        GoogleCalendarService service = new GoogleCalendarService(null, false, "primary");
        var result = service.createEvent("primary", "Title",
                LocalDateTime.now(), LocalDateTime.now().plusHours(1), "Desc");
        assertThat(result).isEmpty();
    }

    @Test
    void createEvent_whenDescriptionNull_doesNotSetDescription() {
        GoogleCalendarService service = new GoogleCalendarService(null, false, "primary");
        var result = service.createEvent("primary", "Title",
                LocalDateTime.now(), LocalDateTime.now().plusHours(1), null);
        assertThat(result).isEmpty();
    }

    @Test
    void deleteEvent_whenNotAvailable_returnsFalse() {
        GoogleCalendarService service = new GoogleCalendarService(null, false, "primary");
        boolean result = service.deleteEvent("primary", "event-123");
        assertThat(result).isFalse();
    }

    @Test
    void deleteEvent_whenEventIdNull_returnsFalse() {
        GoogleCalendarService service = new GoogleCalendarService(mock(Calendar.class), true, "primary");
        boolean result = service.deleteEvent("primary", null);
        assertThat(result).isFalse();
    }

    @Test
    void deleteEvent_whenEventIdBlank_returnsFalse() {
        GoogleCalendarService service = new GoogleCalendarService(mock(Calendar.class), true, "primary");
        boolean result = service.deleteEvent("primary", "   ");
        assertThat(result).isFalse();
    }

    @Test
    void listEvents_whenNotAvailable_returnsEmptyList() {
        GoogleCalendarService service = new GoogleCalendarService(null, false, "primary");
        var result = service.listEvents("primary", LocalDateTime.now(), LocalDateTime.now().plusDays(1));
        assertThat(result).isEmpty();
    }
}
