package com.smartticket.ticket.repository;

import com.smartticket.ticket.entity.Ticket;
import com.smartticket.ticket.entity.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByStatus(TicketStatus status);

    List<Ticket> findByAssigneeId(Long assigneeId);

    List<Ticket> findByAssignedTeam(String assignedTeam);

    long countByAssigneeIdAndStatusNot(Long assigneeId, TicketStatus status);
}
