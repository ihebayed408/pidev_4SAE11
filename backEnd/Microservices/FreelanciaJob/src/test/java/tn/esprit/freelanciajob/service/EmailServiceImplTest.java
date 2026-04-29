package tn.esprit.freelanciajob.service;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import tn.esprit.freelanciajob.Service.EmailServiceImpl;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailServiceImpl – Unit Tests")
class EmailServiceImplTest {

    @Mock private JavaMailSender mailSender;
    @Mock private TemplateEngine templateEngine;

    @InjectMocks
    private EmailServiceImpl emailService;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(emailService, "fromAddress", "test@example.com");
        ReflectionTestUtils.setField(emailService, "fromName", "Test Platform");
    }

    @Nested
    @DisplayName("sendSimpleEmail()")
    class SimpleEmailTests {

        @Test
        @DisplayName("should send simple email successfully")
        void validInput_sendsEmail() {
            emailService.sendSimpleEmail("user@example.com", "Subject", "Body text");

            verify(mailSender).send(any(SimpleMailMessage.class));
        }

        @Test
        @DisplayName("should catch MailException without rethrowing")
        void mailFails_loggedNotRethrown() {
            doThrow(new MailSendException("SMTP error"))
                    .when(mailSender).send(any(SimpleMailMessage.class));

            assertThatCode(() -> emailService.sendSimpleEmail("user@example.com", "Subject", "Body"))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("sendHtmlEmail()")
    class HtmlEmailTests {

        @Test
        @DisplayName("should render template and send HTML email")
        void validInput_sendsHtmlEmail() {
            MimeMessage mimeMessage = mock(MimeMessage.class);
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
            when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<h1>Hello</h1>");

            emailService.sendHtmlEmail("user@example.com", "Subject", "email/template",
                    Map.of("name", "Alice"));

            verify(mailSender).send(any(MimeMessage.class));
            verify(templateEngine).process(eq("email/template"), any(Context.class));
        }

        @Test
        @DisplayName("should catch MailException without rethrowing")
        void mailFails_loggedNotRethrown() {
            MimeMessage mimeMessage = mock(MimeMessage.class);
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
            when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<h1>Hi</h1>");
            doThrow(new MailSendException("SMTP error")).when(mailSender).send(any(MimeMessage.class));

            assertThatCode(() -> emailService.sendHtmlEmail("user@example.com", "Subject",
                    "email/template", Map.of()))
                    .doesNotThrowAnyException();
        }
    }
}
