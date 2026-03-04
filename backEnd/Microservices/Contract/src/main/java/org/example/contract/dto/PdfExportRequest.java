package org.example.contract.dto;

public class PdfExportRequest {

    private boolean includeAttachments = false;
    private String clientName;
    private String freelancerName;

    public PdfExportRequest() {}

    public boolean isIncludeAttachments() { return includeAttachments; }
    public void setIncludeAttachments(boolean includeAttachments) { this.includeAttachments = includeAttachments; }

    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }

    public String getFreelancerName() { return freelancerName; }
    public void setFreelancerName(String freelancerName) { this.freelancerName = freelancerName; }
}
