package tn.esprit.freelanciajob.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.freelanciajob.Service.FileStorageServiceImpl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileStorageServiceImpl – Unit Tests")
class FileStorageServiceImplTest {

    @InjectMocks
    private FileStorageServiceImpl fileStorageService;

    private Path tempDir;

    @BeforeEach
    void setup() throws IOException {
        tempDir = Files.createTempDirectory("freelancia-test-uploads");
        ReflectionTestUtils.setField(fileStorageService, "uploadBaseDir", tempDir.toString());
    }

    @AfterEach
    void cleanup() throws IOException {
        if (Files.exists(tempDir)) {
            try (var stream = Files.walk(tempDir)) {
                stream.sorted(java.util.Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(java.io.File::delete);
            }
        }
    }

    private MultipartFile mockFile(String name, String contentType, long size) {
        MultipartFile file = mock(MultipartFile.class);
        lenient().when(file.getOriginalFilename()).thenReturn(name);
        lenient().when(file.getContentType()).thenReturn(contentType);
        lenient().when(file.getSize()).thenReturn(size);
        return file;
    }

    @Nested
    @DisplayName("validateFiles()")
    class ValidateTests {

        @Test
        @DisplayName("should pass validation for valid files")
        void validFiles_passes() {
            List<MultipartFile> files = List.of(
                    mockFile("doc.pdf", "application/pdf", 1024),
                    mockFile("img.png", "image/png", 2048)
            );

            assertThatCode(() -> fileStorageService.validateFiles(files))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should reject when too many files")
        void tooManyFiles_throws() {
            List<MultipartFile> files = IntStream.range(0, 6)
                    .mapToObj(i -> mockFile("file" + i + ".pdf", "application/pdf", 1024))
                    .toList();

            assertThatThrownBy(() -> fileStorageService.validateFiles(files))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Too many files");
        }

        @Test
        @DisplayName("should reject file exceeding size limit")
        void oversizedFile_throws() {
            List<MultipartFile> files = List.of(
                    mockFile("big.pdf", "application/pdf", 11 * 1024 * 1024)
            );

            assertThatThrownBy(() -> fileStorageService.validateFiles(files))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("10 MB");
        }

        @Test
        @DisplayName("should reject invalid MIME type")
        void invalidMimeType_throws() {
            List<MultipartFile> files = List.of(
                    mockFile("script.exe", "application/octet-stream", 1024)
            );

            assertThatThrownBy(() -> fileStorageService.validateFiles(files))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("not allowed");
        }

        @Test
        @DisplayName("should reject null content type")
        void nullContentType_throws() {
            List<MultipartFile> files = List.of(
                    mockFile("noext", null, 1024)
            );

            assertThatThrownBy(() -> fileStorageService.validateFiles(files))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("not allowed");
        }

        @Test
        @DisplayName("should accept all valid MIME types")
        void allValidTypes_pass() {
            List<MultipartFile> files = List.of(
                    mockFile("a.pdf", "application/pdf", 100),
                    mockFile("b.png", "image/png", 100),
                    mockFile("c.jpg", "image/jpeg", 100),
                    mockFile("d.doc", "application/msword", 100),
                    mockFile("e.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", 100)
            );

            assertThatCode(() -> fileStorageService.validateFiles(files))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("storeFile()")
    class StoreTests {

        @Test
        @DisplayName("should store file and return relative URL")
        void validFile_storesAndReturnsUrl() throws IOException {
            MultipartFile file = mockFile("resume.pdf", "application/pdf", 100);
            when(file.getInputStream()).thenReturn(new ByteArrayInputStream("test content".getBytes()));

            String url = fileStorageService.storeFile(file, 42L);

            assertThat(url).startsWith("/uploads/applications/42/");
            assertThat(url).endsWith(".pdf");

            // Verify file was actually created on disk
            Path uploadDir = tempDir.resolve("applications/42");
            assertThat(Files.exists(uploadDir)).isTrue();
            assertThat(Files.list(uploadDir).count()).isEqualTo(1);
        }

        @Test
        @DisplayName("should handle file with no extension")
        void noExtension_storesSuccessfully() throws IOException {
            MultipartFile file = mockFile("noext", "application/pdf", 100);
            when(file.getInputStream()).thenReturn(new ByteArrayInputStream("data".getBytes()));

            String url = fileStorageService.storeFile(file, 1L);

            assertThat(url).startsWith("/uploads/applications/1/");
        }

        @Test
        @DisplayName("should handle null original filename")
        void nullFilename_storesSuccessfully() throws IOException {
            MultipartFile file = mockFile(null, "application/pdf", 100);
            when(file.getOriginalFilename()).thenReturn(null);
            when(file.getInputStream()).thenReturn(new ByteArrayInputStream("data".getBytes()));

            String url = fileStorageService.storeFile(file, 1L);

            assertThat(url).startsWith("/uploads/applications/1/");
        }

        @Test
        @DisplayName("should throw when IO error occurs")
        void ioError_throwsRuntime() throws IOException {
            MultipartFile file = mockFile("fail.pdf", "application/pdf", 100);
            when(file.getInputStream()).thenThrow(new IOException("Disk full"));

            assertThatThrownBy(() -> fileStorageService.storeFile(file, 1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Could not save file");
        }
    }

    @Nested
    @DisplayName("deleteApplicationFiles()")
    class DeleteTests {

        @Test
        @DisplayName("should delete existing directory and files")
        void existingDir_deletedSuccessfully() throws IOException {
            Path dir = tempDir.resolve("applications/99");
            Files.createDirectories(dir);
            Files.writeString(dir.resolve("file1.pdf"), "content1");
            Files.writeString(dir.resolve("file2.pdf"), "content2");

            fileStorageService.deleteApplicationFiles(99L);

            assertThat(Files.exists(dir)).isFalse();
        }

        @Test
        @DisplayName("should not throw when directory does not exist")
        void nonExistentDir_noOp() {
            assertThatCode(() -> fileStorageService.deleteApplicationFiles(999L))
                    .doesNotThrowAnyException();
        }
    }
}
