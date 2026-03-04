package org.example.contract.dto;

public class ConflictStatsDto {

    private long total;
    private long open;
    private long inReview;
    private long resolved;

    public ConflictStatsDto() {}

    public long getTotal()          { return total; }
    public void setTotal(long v)    { this.total = v; }

    public long getOpen()           { return open; }
    public void setOpen(long v)     { this.open = v; }

    public long getInReview()       { return inReview; }
    public void setInReview(long v) { this.inReview = v; }

    public long getResolved()       { return resolved; }
    public void setResolved(long v) { this.resolved = v; }
}
