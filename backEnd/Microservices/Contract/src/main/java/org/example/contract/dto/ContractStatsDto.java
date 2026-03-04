package org.example.contract.dto;

public class ContractStatsDto {

    private long total;
    private long draft;
    private long pendingSignature;
    private long active;
    private long completed;
    private long cancelled;
    private long inConflict;

    public ContractStatsDto() {}

    public long getTotal()            { return total; }
    public void setTotal(long v)      { this.total = v; }

    public long getDraft()            { return draft; }
    public void setDraft(long v)      { this.draft = v; }

    public long getPendingSignature()       { return pendingSignature; }
    public void setPendingSignature(long v) { this.pendingSignature = v; }

    public long getActive()           { return active; }
    public void setActive(long v)     { this.active = v; }

    public long getCompleted()        { return completed; }
    public void setCompleted(long v)  { this.completed = v; }

    public long getCancelled()        { return cancelled; }
    public void setCancelled(long v)  { this.cancelled = v; }

    public long getInConflict()       { return inConflict; }
    public void setInConflict(long v) { this.inConflict = v; }
}
