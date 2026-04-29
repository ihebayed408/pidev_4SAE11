package tn.esprit.freelanciajob.listener;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.freelanciajob.Client.NotificationClient;
import tn.esprit.freelanciajob.Client.UserClient;
import tn.esprit.freelanciajob.Dto.request.NotificationCreateRequest;
import tn.esprit.freelanciajob.Dto.response.UserDto;
import tn.esprit.freelanciajob.Entity.Job;
import tn.esprit.freelanciajob.Entity.JobApplication;
import tn.esprit.freelanciajob.Entity.Enums.ApplicationStatus;
import tn.esprit.freelanciajob.Entity.Enums.JobStatus;
import tn.esprit.freelanciajob.Entity.Enums.LocationType;
import tn.esprit.freelanciajob.Event.ApplicationAcceptedEvent;
import tn.esprit.freelanciajob.Event.ApplicationSubmittedEvent;
import tn.esprit.freelanciajob.Event.JobCreatedEvent;
import tn.esprit.freelanciajob.Listener.JobEventListener;
import tn.esprit.freelanciajob.Service.EmailService;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JobEventListener – Unit Tests")
class JobEventListenerTest {

    @Mock private EmailService emailService;
    @Mock private UserClient userClient;
    @Mock private NotificationClient notificationClient;

    @InjectMocks
    private JobEventListener jobEventListener;

    private Job buildJob() {
        return Job.builder()
                .id(1L).clientId(100L).title("Backend Dev")
                .description("Java role").category("Engineering")
                .locationType(LocationType.REMOTE).status(JobStatus.OPEN)
                .budgetMin(BigDecimal.valueOf(1000)).budgetMax(BigDecimal.valueOf(3000))
                .currency("USD")
                .build();
    }

    private JobApplication buildApplication() {
        return JobApplication.builder()
                .id(50L).job(buildJob()).freelancerId(200L)
                .proposalMessage("I'm interested").status(ApplicationStatus.PENDING)
                .build();
    }

    private UserDto buildUser(String firstName, String email) {
        UserDto u = new UserDto();
        u.setFirstName(firstName);
        u.setLastName("Test");
        u.setEmail(email);
        return u;
    }

    @Nested
    @DisplayName("onJobCreated()")
    class JobCreatedTests {

        @Test
        @DisplayName("should send HTML emails to all freelancers")
        void freelancersExist_sendsEmails() {
            JobCreatedEvent event = new JobCreatedEvent(this, buildJob(), "Alice");
            List<UserDto> freelancers = List.of(
                    buildUser("Bob", "bob@test.com"),
                    buildUser("Carol", "carol@test.com")
            );
            when(userClient.getUsersByRole("FREELANCER")).thenReturn(freelancers);

            jobEventListener.onJobCreated(event);

            verify(emailService, times(2)).sendHtmlEmail(anyString(), anyString(), eq("email/job-posted"), anyMap());
        }

        @Test
        @DisplayName("should skip freelancers with null email")
        void nullEmail_skipped() {
            JobCreatedEvent event = new JobCreatedEvent(this, buildJob(), "Alice");
            UserDto noEmail = buildUser("Dave", null);
            when(userClient.getUsersByRole("FREELANCER")).thenReturn(List.of(noEmail));

            jobEventListener.onJobCreated(event);

            verify(emailService, never()).sendHtmlEmail(anyString(), anyString(), anyString(), anyMap());
        }

        @Test
        @DisplayName("should return early when no freelancers found")
        void noFreelancers_returnsEarly() {
            JobCreatedEvent event = new JobCreatedEvent(this, buildJob(), "Alice");
            when(userClient.getUsersByRole("FREELANCER")).thenReturn(Collections.emptyList());

            jobEventListener.onJobCreated(event);

            verify(emailService, never()).sendHtmlEmail(anyString(), anyString(), anyString(), anyMap());
        }
    }

    @Nested
    @DisplayName("onApplicationSubmitted()")
    class ApplicationSubmittedTests {

        @Test
        @DisplayName("should send confirmation to freelancer and notify client")
        void bothUsersExist_sendsEmailsAndNotification() {
            ApplicationSubmittedEvent event = new ApplicationSubmittedEvent(this, buildApplication(), 200L);
            UserDto freelancer = buildUser("Bob", "bob@test.com");
            UserDto client = buildUser("Alice", "alice@test.com");

            when(userClient.getUserById(200L)).thenReturn(freelancer);
            when(userClient.getUserById(100L)).thenReturn(client);

            jobEventListener.onApplicationSubmitted(event);

            verify(emailService).sendHtmlEmail(eq("bob@test.com"), anyString(),
                    eq("email/application-submitted"), anyMap());
            verify(emailService).sendHtmlEmail(eq("alice@test.com"), anyString(),
                    eq("email/client-application-received"), anyMap());
            verify(notificationClient).create(any(NotificationCreateRequest.class));
        }

        @Test
        @DisplayName("should skip freelancer email when user not found")
        void freelancerNotFound_skipsFreelancerEmail() {
            ApplicationSubmittedEvent event = new ApplicationSubmittedEvent(this, buildApplication(), 200L);
            when(userClient.getUserById(200L)).thenReturn(null);

            UserDto client = buildUser("Alice", "alice@test.com");
            when(userClient.getUserById(100L)).thenReturn(client);

            jobEventListener.onApplicationSubmitted(event);

            verify(emailService, never()).sendHtmlEmail(eq("bob@test.com"), anyString(),
                    eq("email/application-submitted"), anyMap());
        }

        @Test
        @DisplayName("should handle notification client failure gracefully")
        void notificationFails_doesNotThrow() {
            ApplicationSubmittedEvent event = new ApplicationSubmittedEvent(this, buildApplication(), 200L);
            UserDto freelancer = buildUser("Bob", "bob@test.com");
            when(userClient.getUserById(200L)).thenReturn(freelancer);
            when(userClient.getUserById(100L)).thenReturn(buildUser("Alice", "alice@test.com"));
            doThrow(new RuntimeException("Service down")).when(notificationClient).create(any());

            assertThatCode(() -> jobEventListener.onApplicationSubmitted(event))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should skip client notification when job has no clientId")
        void noClientId_skipsClientNotification() {
            Job jobNoClient = buildJob();
            jobNoClient.setClientId(null);
            JobApplication app = JobApplication.builder()
                    .id(50L).job(jobNoClient).freelancerId(200L)
                    .proposalMessage("Interested").status(ApplicationStatus.PENDING)
                    .build();
            ApplicationSubmittedEvent event = new ApplicationSubmittedEvent(this, app, 200L);
            when(userClient.getUserById(200L)).thenReturn(buildUser("Bob", "bob@test.com"));

            jobEventListener.onApplicationSubmitted(event);

            verify(notificationClient, never()).create(any());
        }
    }

    @Nested
    @DisplayName("onApplicationAccepted()")
    class ApplicationAcceptedTests {

        @Test
        @DisplayName("should send acceptance email to freelancer")
        void freelancerExists_sendsEmail() {
            ApplicationAcceptedEvent event = new ApplicationAcceptedEvent(this, buildApplication(), 200L);
            when(userClient.getUserById(200L)).thenReturn(buildUser("Bob", "bob@test.com"));

            jobEventListener.onApplicationAccepted(event);

            verify(emailService).sendHtmlEmail(eq("bob@test.com"), anyString(),
                    eq("email/application-accepted"), anyMap());
        }

        @Test
        @DisplayName("should return early when freelancer not found")
        void freelancerNotFound_returnsEarly() {
            ApplicationAcceptedEvent event = new ApplicationAcceptedEvent(this, buildApplication(), 200L);
            when(userClient.getUserById(200L)).thenReturn(null);

            jobEventListener.onApplicationAccepted(event);

            verify(emailService, never()).sendHtmlEmail(anyString(), anyString(), anyString(), anyMap());
        }

        @Test
        @DisplayName("should return early when freelancer has no email")
        void freelancerNoEmail_returnsEarly() {
            ApplicationAcceptedEvent event = new ApplicationAcceptedEvent(this, buildApplication(), 200L);
            when(userClient.getUserById(200L)).thenReturn(buildUser("Bob", null));

            jobEventListener.onApplicationAccepted(event);

            verify(emailService, never()).sendHtmlEmail(anyString(), anyString(), anyString(), anyMap());
        }
    }
}
