package lk.supporthub;

import static lk.supporthub.Dtos.*;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service @Transactional
class HelpdeskService {
    private final AccountRepository accounts; private final TicketRepository tickets;
    private final CommentRepository comments; private final ActivityRepository activities; private final PasswordEncoder encoder;
    HelpdeskService(AccountRepository a, TicketRepository t, CommentRepository c, ActivityRepository h, PasswordEncoder e) {
        accounts=a;tickets=t;comments=c;activities=h;encoder=e;
    }
    static ResponseStatusException error(HttpStatus status,String message) { return new ResponseStatusException(status,message); }
    Account actor() {
        var auth=SecurityContextHolder.getContext().getAuthentication();
        if(auth==null) throw error(HttpStatus.UNAUTHORIZED,"Please sign in");
        return accounts.findByEmail(auth.getName()).filter(a->a.enabled)
            .orElseThrow(()->error(HttpStatus.UNAUTHORIZED,"Account unavailable. Please sign in again."));
    }
    void staff(Account a) { if(a.role==Role.REQUESTER) throw error(HttpStatus.FORBIDDEN,"A support role is required"); }
    void admin(Account a) { if(a.role!=Role.ADMIN) throw error(HttpStatus.FORBIDDEN,"Administrator access required"); }
    void visible(Account a,Ticket t) {
        if(a.role==Role.REQUESTER&&!t.requester.id.equals(a.id)) throw error(HttpStatus.NOT_FOUND,"Ticket not found");
    }
    public Person me() { return Person.of(actor()); }
    public Person register(Register input) {
        String email=input.email().trim().toLowerCase(Locale.ROOT);
        if(accounts.findByEmail(email).isPresent()) throw error(HttpStatus.CONFLICT,"An account already exists with this email");
        var a=new Account();a.name=input.name().trim();a.email=email;a.passwordHash=encoder.encode(input.password());a.role=Role.REQUESTER;
        return Person.of(accounts.saveAndFlush(a));
    }
    Specification<Ticket> scope(Account a) {
        return (root,q,cb)-> a.role==Role.REQUESTER?cb.equal(root.get("requester").get("id"),a.id):cb.conjunction();
    }
    @Transactional(readOnly=true)
    public PageResult list(String query, Status status, Priority priority, boolean mine, int page) {
        Account a=actor();Specification<Ticket> spec=scope(a);
        if(page<0) throw error(HttpStatus.BAD_REQUEST,"Page must be zero or greater");
        if(query!=null&&!query.isBlank()) {
            if(query.length()>160) throw error(HttpStatus.BAD_REQUEST,"Search is too long");
            String term="%"+query.trim().toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_")+"%";
            spec=spec.and((root,q,cb)->cb.or(cb.like(cb.lower(root.get("title")),term,'!'),cb.like(cb.lower(root.get("description")),term,'!')));
        }
        if(status!=null) spec=spec.and((r,q,cb)->cb.equal(r.get("status"),status));
        if(priority!=null) spec=spec.and((r,q,cb)->cb.equal(r.get("priority"),priority));
        if(mine) spec=spec.and((r,q,cb)->cb.equal(r.get("assignee").get("id"),a.id));
        var result=tickets.findAll(spec,PageRequest.of(page,12,Sort.by(Sort.Order.desc("updatedAt"),Sort.Order.desc("id"))));
        return new PageResult(result.getContent().stream().map(TicketView::of).toList(),result.getTotalElements(),result.getTotalPages(),page);
    }
    @Transactional(readOnly=true)
    public Dashboard dashboard() {
        var s=scope(actor());
        long open=count(s,Status.OPEN), progress=count(s,Status.IN_PROGRESS), waiting=count(s,Status.WAITING), resolved=count(s,Status.RESOLVED), closed=count(s,Status.CLOSED);
        long urgent=tickets.count(s.and((r,q,c)->c.and(c.equal(r.get("priority"),Priority.URGENT),r.get("status").in(Status.OPEN,Status.IN_PROGRESS,Status.WAITING))));
        return new Dashboard(open+progress+waiting+resolved+closed,open,progress,waiting,resolved,closed,urgent);
    }
    private long count(Specification<Ticket> s, Status status) {return tickets.count(s.and((r,q,c)->c.equal(r.get("status"),status)));}
    public TicketView create(NewTicket input) {
        var a=actor();var t=new Ticket();t.title=input.title().trim();t.description=input.description().trim();
        t.category=input.category();t.priority=input.priority();t.requester=a;tickets.saveAndFlush(t);record(t,a,"Created ticket");
        return TicketView.of(t);
    }
    @Transactional(readOnly=true)
    public Detail detail(Long id) {
        var a=actor();var t=tickets.findById(id).orElseThrow(()->error(HttpStatus.NOT_FOUND,"Ticket not found"));visible(a,t);
        return new Detail(TicketView.of(t),comments.findByTicketIdOrderByCreatedAtAscIdAsc(id).stream()
            .map(c->new CommentView(c.id,Person.of(c.author),c.body,c.createdAt)).toList(),
            activities.findByTicketIdOrderByCreatedAtAscIdAsc(id).stream().map(h->new ActivityView(h.id,h.actor.name,h.action,h.createdAt)).toList());
    }
    public TicketView update(Long id,UpdateTicket input) {
        var a=actor();staff(a);
        // Account locks are acquired before ticket locks, matching team-management order.
        Account assignee=input.assigneeId()==null?null:accounts.lock(input.assigneeId()).orElseThrow(()->error(HttpStatus.BAD_REQUEST,"Assignee not found"));
        if(assignee!=null&&(!assignee.enabled||assignee.role==Role.REQUESTER)) throw error(HttpStatus.BAD_REQUEST,"Choose an active support agent");
        var t=tickets.lock(id).orElseThrow(()->error(HttpStatus.NOT_FOUND,"Ticket not found"));
        if(t.version!=input.version()) throw error(HttpStatus.CONFLICT,"This ticket changed. Refresh it before saving.");
        if(!allowed(t.status,input.status())) throw error(HttpStatus.BAD_REQUEST,"That status transition is not allowed");
        if(input.status()==Status.IN_PROGRESS&&assignee==null) throw error(HttpStatus.BAD_REQUEST,"Assign an agent before starting work");
        if(t.status!=input.status()) record(t,a,"Status: "+t.status+" → "+input.status());
        if(t.priority!=input.priority()) record(t,a,"Priority: "+t.priority+" → "+input.priority());
        if(!Objects.equals(t.assignee==null?null:t.assignee.id,input.assigneeId())) record(t,a,"Assigned to "+(assignee==null?"Unassigned":assignee.name));
        t.status=input.status();t.priority=input.priority();t.assignee=assignee;t.updatedAt=Instant.now();tickets.flush();return TicketView.of(t);
    }
    static boolean allowed(Status from, Status to) {
        if(from==to) return true;
        return switch(from) {
            case OPEN -> to==Status.IN_PROGRESS||to==Status.RESOLVED;
            case IN_PROGRESS -> to==Status.WAITING||to==Status.RESOLVED;
            case WAITING -> to==Status.IN_PROGRESS||to==Status.RESOLVED;
            case RESOLVED -> to==Status.CLOSED||to==Status.OPEN;
            case CLOSED -> to==Status.OPEN;
        };
    }
    public CommentView comment(Long id,NewComment input) {
        var a=actor();var t=tickets.lock(id).orElseThrow(()->error(HttpStatus.NOT_FOUND,"Ticket not found"));visible(a,t);
        if(t.status==Status.CLOSED) throw error(HttpStatus.CONFLICT,"Reopen the ticket before adding a comment");
        var c=new Comment();c.ticket=t;c.author=a;c.body=input.body().trim();comments.saveAndFlush(c);
        t.updatedAt=Instant.now();record(t,a,"Added a comment");return new CommentView(c.id,Person.of(a),c.body,c.createdAt);
    }
    private void record(Ticket t,Account a,String action) { var h=new Activity();h.ticket=t;h.actor=a;h.action=action;activities.save(h); }
    @Transactional(readOnly=true)
    public List<Person> agents() { staff(actor());return accounts.findByEnabledTrueAndRoleInOrderByNameAsc(List.of(Role.AGENT,Role.ADMIN)).stream().map(Person::of).toList(); }
    @Transactional(readOnly=true)
    public List<Person> team() { admin(actor());return accounts.findAllByOrderByNameAsc().stream().map(Person::of).toList(); }
    public Person updateAccount(Long id,UpdateAccount input) {
        var a=actor();admin(a);
        if(a.id.equals(id)) throw error(HttpStatus.BAD_REQUEST,"You cannot change your own role or disable your account");
        var target=accounts.lock(id).orElseThrow(()->error(HttpStatus.NOT_FOUND,"Account not found"));
        if((!input.enabled()||input.role()==Role.REQUESTER)&&tickets.existsByAssigneeIdAndStatusNotIn(id,List.of(Status.RESOLVED,Status.CLOSED)))
            throw error(HttpStatus.CONFLICT,"Reassign this person's active tickets first");
        target.role=input.role();target.enabled=input.enabled();return Person.of(target);
    }
}
