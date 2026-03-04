package org.example.contract.controller;

import org.example.contract.dto.ContractStatsDto;
import org.example.contract.dto.PdfExportRequest;
import org.example.contract.dto.SignatureRequest;
import org.example.contract.entity.Contract;
import org.example.contract.service.ContractPdfService;
import org.example.contract.service.ContractService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"},
        allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST,
        RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.DELETE, RequestMethod.OPTIONS})
@RestController
@RequestMapping("/api/contracts")
public class ContractController {

    private final ContractService contractService;
    private final ContractPdfService contractPdfService;

    @Value("${welcome.message}")
    private String welcomeMessage;

    @Autowired
    public ContractController(ContractService contractService, ContractPdfService contractPdfService) {
        this.contractService = contractService;
        this.contractPdfService = contractPdfService;
    }

    @GetMapping("/welcome")
    public String welcome() {
        return welcomeMessage;
    }

    @PostMapping
    public ResponseEntity<Contract> createContract(@RequestBody Contract contract) {
        return ResponseEntity.ok(contractService.createContract(contract));
    }

    @GetMapping
    public ResponseEntity<List<Contract>> getAllContracts() {
        return ResponseEntity.ok(contractService.getAllContracts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Contract> getContractById(@PathVariable Long id) {
        return contractService.getContractById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Contract> updateContract(@PathVariable Long id, @RequestBody Contract contract) {
        try {
            return ResponseEntity.ok(contractService.updateContract(id, contract));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteContract(@PathVariable Long id) {
        contractService.deleteContract(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<Contract>> getContractsByClient(@PathVariable Long clientId) {
        return ResponseEntity.ok(contractService.getContractsByClientId(clientId));
    }

    @GetMapping("/freelancer/{freelancerId}")
    public ResponseEntity<List<Contract>> getContractsByFreelancer(@PathVariable Long freelancerId) {
        return ResponseEntity.ok(contractService.getContractsByFreelancerId(freelancerId));
    }

    /**
     * Upload a signature image for the contract.
     * Accepts a JSON body: { "role": "CLIENT"|"FREELANCER", "signatureData": "data:image/png;base64,..." }
     * Stores the Base64 image data in the DB (MEDIUMTEXT, up to 16 MB).
     * Auto-activates the contract when both parties have signed.
     */
    @PatchMapping("/{id}/sign")
    public ResponseEntity<Contract> signContract(
            @PathVariable Long id,
            @RequestBody SignatureRequest request) {
        try {
            return ResponseEntity.ok(contractService.signContract(id, request.getRole(), request.getSignatureData()));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Export contract as PDF.
     * Body (optional): { "includeAttachments": true } – when true, conflicts are included.
     * Returns application/pdf with filename "contract-{id}.pdf".
     */
    @PostMapping(value = "/{id}/export-pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportPdf(
            @PathVariable Long id,
            @RequestBody(required = false) PdfExportRequest request) {
        return contractService.getContractById(id)
                .map(contract -> {
                    boolean includeAttachments = request != null && request.isIncludeAttachments();
                    String clientName     = request != null ? request.getClientName()     : null;
                    String freelancerName = request != null ? request.getFreelancerName() : null;
                    byte[] pdf = contractPdfService.generateContractPdf(contract, includeAttachments, clientName, freelancerName);
                    return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_PDF)
                            .header(HttpHeaders.CONTENT_DISPOSITION,
                                    "inline; filename=\"contract-" + id + ".pdf\"")
                            .body(pdf);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/stats")
    public ResponseEntity<ContractStatsDto> getStats() {
        return ResponseEntity.ok(contractService.getContractStats());
    }

    /** Standalone status update (submit for signature, mark conflict, complete, cancel). */
    @PatchMapping("/{id}/status")
    public ResponseEntity<Contract> updateStatus(
            @PathVariable Long id,
            @RequestParam org.example.contract.entity.ContractStatus status) {
        try {
            return ResponseEntity.ok(contractService.updateStatus(id, status));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
