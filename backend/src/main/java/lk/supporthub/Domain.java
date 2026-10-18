package lk.supporthub;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

enum Role { REQUESTER, AGENT, ADMIN }
enum Priority { LOW, MEDIUM, HIGH, URGENT }
enum Status { OPEN, IN_PROGRESS, WAITING, RESOLVED, CLOSED }
enum Category { GENERAL, SOFTWARE, HARDWARE, NETWORK, ACCESS }

@Entity @Table(name="accounts") @Getter @Setter
class Account {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @Column(nullable=false, length=100) String name;
    @Column(nullable=false, unique=true, length=190) String email;
    @Column(nullable=false) String passwordHash;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) Role role;
    @Column(nullable=false) boolean enabled=true;
}
@Entity @Table(name="tickets") @Getter @Setter
class Ticket {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @Column(nullable=false, length=160) String title;
    @Column(nullable=false, length=8000) String description;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) Category category;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) Priority priority;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) Status status=Status.OPEN;
    @ManyToOne(optional=false) @JoinColumn(name="requester_id") Account requester;
    @ManyToOne @JoinColumn(name="assignee_id") Account assignee;
    @Column(nullable=false) Instant createdAt=Instant.now();
    @Column(nullable=false) Instant updatedAt=Instant.now();
    @Version long version;
}
@Entity @Table(name="comments") @Getter @Setter
class Comment {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @ManyToOne(optional=false) @JoinColumn(name="ticket_id") Ticket ticket;
    @ManyToOne(optional=false) @JoinColumn(name="author_id") Account author;
    @Column(nullable=false, length=4000) String body;
    @Column(nullable=false) Instant createdAt=Instant.now();
}
@Entity @Table(name="activities") @Getter @Setter
class Activity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @ManyToOne(optional=false) @JoinColumn(name="ticket_id") Ticket ticket;
    @ManyToOne(optional=false) @JoinColumn(name="actor_id") Account actor;
    @Column(nullable=false, length=500) String action;
    @Column(nullable=false) Instant createdAt=Instant.now();
}
