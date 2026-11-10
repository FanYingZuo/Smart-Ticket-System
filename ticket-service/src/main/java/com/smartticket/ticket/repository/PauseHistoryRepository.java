package com.smartticket.ticket.repository;

import com.smartticket.ticket.entity.PauseHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PauseHistoryRepository extends JpaRepository<PauseHistory, Long> {

    List<PauseHistory> findByTicketIdOrderByEnteredAtAsc(Long ticketId);

    Optional<PauseHistory> findByTicketIdAndExitedAtIsNull(Long ticketId);
}
