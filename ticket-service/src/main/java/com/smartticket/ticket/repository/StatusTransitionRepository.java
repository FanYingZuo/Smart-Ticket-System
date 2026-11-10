package com.smartticket.ticket.repository;

import com.smartticket.ticket.entity.StatusTransition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StatusTransitionRepository extends JpaRepository<StatusTransition, Long> {

    List<StatusTransition> findByTicketIdOrderByChangedAtAsc(Long ticketId);
}
