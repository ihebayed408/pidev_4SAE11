package tn.esprit.freelanciajob.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import tn.esprit.freelanciajob.Controller.JobApplicationController;
import tn.esprit.freelanciajob.Dto.request.JobApplicationRequest;
import tn.esprit.freelanciajob.Dto.response.ApplyJobResponse;
import tn.esprit.freelanciajob.Dto.response.AttachmentResponse;
import tn.esprit.freelanciajob.Dto.response.JobApplicationResponse;
import tn.esprit.freelanciajob.Entity.Enums.ApplicationStatus;
import tn.esprit.freelanciajob.Repository.ApplicationAttachmentRepository;
import tn.esprit.freelanciajob.Service.IJobApplicationService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JobApplicationController – Unit Tests")
class JobApplicationControllerTest {

    @Mock private IJobApplicationService applicationService;
    @Mock private ApplicationAttachmentRepository attachmentRepository;

    @InjectMocks
    private JobApplicationController controller;

    private JobApplicationResponse buildResponse() {
        JobApplicationResponse r = new JobApplicationResponse();
        r.setId(1L);
        r.setJobId(10L);
        r.setFreelancerId(20L);
        r.setStatus("PENDING");
        r.setProposalMessage("Test proposal message for the role");
        return r;
    }

    @Nested
    @DisplayName("CRUD endpoints")
    class CrudTests {

        @Test
        @DisplayName("POST /add should return 201 with created application")
        void addApplication_returns201() {
            JobApplicationRequest request = new JobApplicationRequest();
            when(applicationService.addApplication(any())).thenReturn(buildResponse());

            ResponseEntity<JobApplicationResponse> result = controller.addApplication(request);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().getStatus()).isEqualTo("PENDING");
        }

        @Test
        @DisplayName("PUT /update/{id} should return 200 with updated application")
        void updateApplication_returns200() {
            JobApplicationRequest request = new JobApplicationRequest();
            when(applicationService.updateApplication(eq(1L), any())).thenReturn(buildResponse());

            ResponseEntity<JobApplicationResponse> result = controller.updateApplication(1L, request);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("DELETE /{id} should return 204")
        void deleteApplication_returns204() {
            ResponseEntity<Void> result = controller.deleteApplication(1L);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(applicationService).deleteApplication(1L);
        }

        @Test
        @DisplayName("GET /{id} should return 200 with application")
        void getById_returns200() {
            when(applicationService.getApplicationById(1L)).thenReturn(buildResponse());

            ResponseEntity<JobApplicationResponse> result = controller.getApplicationById(1L);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody().getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("GET /list should return 200 with all applications")
        void getAllApplications_returns200() {
            when(applicationService.getAllApplications()).thenReturn(List.of(buildResponse()));

            ResponseEntity<List<JobApplicationResponse>> result = controller.getAllApplications();

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Query endpoints")
    class QueryTests {

        @Test
        @DisplayName("GET /job/{jobId} should return applications for job")
        void getByJob_returnsApplications() {
            when(applicationService.getApplicationsByJob(10L)).thenReturn(List.of(buildResponse()));

            ResponseEntity<List<JobApplicationResponse>> result = controller.getByJob(10L);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).hasSize(1);
        }

        @Test
        @DisplayName("GET /freelancer/{id} should return applications for freelancer")
        void getByFreelancer_returnsApplications() {
            when(applicationService.getApplicationsByFreelancer(20L)).thenReturn(List.of(buildResponse()));

            ResponseEntity<List<JobApplicationResponse>> result = controller.getByFreelancer(20L);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("PATCH /{id}/status should update and return application")
        void updateStatus_returns200() {
            when(applicationService.updateStatus(eq(1L), eq(ApplicationStatus.ACCEPTED)))
                    .thenReturn(buildResponse());

            ResponseEntity<JobApplicationResponse> result = controller.updateStatus(1L, "ACCEPTED");

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    @Nested
    @DisplayName("Apply with files endpoint")
    class ApplyTests {

        @Test
        @DisplayName("POST /{jobId}/apply should return 201 with response")
        void applyToJob_returns201() {
            ApplyJobResponse applyResponse = new ApplyJobResponse();
            applyResponse.setId(1L);
            applyResponse.setAttachments(Collections.emptyList());
            when(applicationService.applyToJob(eq(10L), eq(20L), anyString(), any(), any(), anyList()))
                    .thenReturn(applyResponse);

            ResponseEntity<ApplyJobResponse> result = controller.applyToJob(
                    10L, 20L, "My proposal message", BigDecimal.valueOf(50),
                    LocalDate.now(), Collections.emptyList());

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(result.getBody()).isNotNull();
        }

        @Test
        @DisplayName("POST /{jobId}/apply with null files passes empty list")
        void nullFiles_passesEmptyList() {
            ApplyJobResponse applyResponse = new ApplyJobResponse();
            applyResponse.setAttachments(Collections.emptyList());
            when(applicationService.applyToJob(eq(10L), eq(20L), anyString(), any(), any(), eq(Collections.emptyList())))
                    .thenReturn(applyResponse);

            ResponseEntity<ApplyJobResponse> result = controller.applyToJob(
                    10L, 20L, "My proposal message", null, null, null);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        }
    }

    @Nested
    @DisplayName("Attachment endpoints")
    class AttachmentTests {

        @Test
        @DisplayName("GET /{applicationId}/attachments should return attachment list")
        void getAttachments_returnsMetadata() {
            AttachmentResponse att = new AttachmentResponse();
            att.setId(1L);
            att.setFileName("cv.pdf");
            att.setFileType("application/pdf");
            when(applicationService.getAttachments(1L)).thenReturn(List.of(att));

            ResponseEntity<List<AttachmentResponse>> result = controller.getAttachments(1L);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).hasSize(1);
        }
    }
}
