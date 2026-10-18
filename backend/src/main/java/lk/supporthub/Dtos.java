package lk.supporthub;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;

final class Dtos {
    record Register(@NotBlank @Size(max=100) String name, @NotBlank @Email @Size(max=190) String email,
                    @NotBlank @Size(min=12,max=64) String password) {}
    record Login(@NotBlank @Email @Size(max=190) String email, @NotBlank @Size(max=64) String password) {}
    record NewTicket(@NotBlank @Size(max=160) String title, @NotBlank @Size(max=8000) String description,
                     @NotNull Category category, @NotNull Priority priority) {}
    record UpdateTicket(@NotNull @PositiveOrZero Long version, @NotNull Priority priority,
                        @NotNull Status status, @Positive Long assigneeId) {}
    record NewComment(@NotBlank @Size(max=4000) String body) {}
    record UpdateAccount(@NotNull Role role, @NotNull Boolean enabled) {}
    record Person(Long id, String name, String email, Role role, boolean enabled) {
        static Person of(Account a) { return a==null ? null : new Person(a.id,a.name,a.email,a.role,a.enabled); }
    }
    record TicketView(Long id, String title, String description, Category category, Priority priority,
                      Status status, Person requester, Person assignee, Instant createdAt, Instant updatedAt, long version) {
        static TicketView of(Ticket t) { return new TicketView(t.id,t.title,t.description,t.category,t.priority,t.status,
            Person.of(t.requester),Person.of(t.assignee),t.createdAt,t.updatedAt,t.version); }
    }
    record CommentView(Long id, Person author, String body, Instant createdAt) {}
    record ActivityView(Long id, String actor, String action, Instant createdAt) {}
    record Detail(TicketView ticket, List<CommentView> comments, List<ActivityView> activities) {}
    record PageResult(List<TicketView> content, long totalElements, int totalPages, int number) {}
    record Dashboard(long total, long open, long inProgress, long waiting, long resolved, long closed, long urgent) {}
}
