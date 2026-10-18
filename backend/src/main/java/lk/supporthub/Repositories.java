package lk.supporthub;
import java.util.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

interface AccountRepository extends JpaRepository<Account,Long> {
    Optional<Account> findByEmail(String email);
    List<Account> findAllByOrderByNameAsc();
    List<Account> findByEnabledTrueAndRoleInOrderByNameAsc(Collection<Role> roles);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select a from Account a where a.id=:id")
    Optional<Account> lock(@Param("id") Long id);
}
interface TicketRepository extends JpaRepository<Ticket,Long>, JpaSpecificationExecutor<Ticket> {
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select t from Ticket t where t.id=:id")
    Optional<Ticket> lock(@Param("id") Long id);
    boolean existsByAssigneeIdAndStatusNotIn(Long id, Collection<Status> statuses);
}
interface CommentRepository extends JpaRepository<Comment,Long> {
    List<Comment> findByTicketIdOrderByCreatedAtAscIdAsc(Long id);
}
interface ActivityRepository extends JpaRepository<Activity,Long> {
    List<Activity> findByTicketIdOrderByCreatedAtAscIdAsc(Long id);
}
