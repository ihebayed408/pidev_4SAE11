# Planning Microservice – Advanced Method Signatures

Short list of advanced methods (statistics/reporting) for the Planning microservice.

---

1. **`Map<String, Object> getProgressStatisticsByFreelancer(Long freelancerId);`**  
   Stats for one freelancer: total updates, average progress %, last update date.

2. **`Map<String, Object> getProgressStatisticsByProject(Long projectId);`**  
   Stats for one project: number of updates, current progress %, comment count.

3. **`List<ProgressTrendDto> getProgressTrendByProject(Long projectId, LocalDate from, LocalDate to);`**  
   Progress over time for a project in a date range (for charts).

4. **`List<FreelancerActivityDto> getFreelancersByActivity(int limit);`**  
   Top N freelancers by number of progress updates (activity ranking).

5. **`List<Long> getProjectIdsWithStalledProgress(int daysWithoutUpdate);`**  
   Project IDs that have had no progress update in the last X days.
