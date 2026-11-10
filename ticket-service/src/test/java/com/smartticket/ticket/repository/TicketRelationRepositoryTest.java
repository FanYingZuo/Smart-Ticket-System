package com.smartticket.ticket.repository;

import com.smartticket.ticket.entity.PauseHistory;
import com.smartticket.ticket.entity.Priority;
import com.smartticket.ticket.entity.StatusTransition;
import com.smartticket.ticket.entity.Ticket;
import com.smartticket.ticket.entity.TicketStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TicketRelationRepositoryTest {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private StatusTransitionRepository statusTransitionRepository;

    @Autowired
    private EntityManager em;

    private Ticket newTicket(TicketStatus status) {
        Ticket ticket = new Ticket();
        ticket.setTitle("測試工單");
        ticket.setPriority(Priority.P2);
        ticket.setStatus(status);
        return ticket;
    }

    private StatusTransition transition(Ticket ticket, TicketStatus from, TicketStatus to, LocalDateTime at) {
        StatusTransition t = new StatusTransition();
        t.setTicket(ticket);
        t.setFromStatus(from);
        t.setToStatus(to);
        t.setChangedBy("tester");
        t.setChangedAt(at);
        ticket.getStatusTransitions().add(t);
        return t;
    }

    @Test
    void statusTransitions_areLoadedInChangedAtOrder_andLinkedBackToTicket() {
        LocalDateTime base = LocalDateTime.of(2026, 11, 10, 9, 0);
        Ticket ticket = newTicket(TicketStatus.RESOLVED);
        // 刻意以非時間順序加入，驗證讀回時由資料庫依 changedAt 排序
        transition(ticket, TicketStatus.ASSIGNED, TicketStatus.RESOLVED, base.plusHours(2));
        transition(ticket, TicketStatus.OPEN, TicketStatus.ASSIGNED, base.plusMinutes(5));

        Long ticketId = ticketRepository.save(ticket).getId();
        em.flush();
        em.clear();

        Ticket reloaded = ticketRepository.findById(ticketId).orElseThrow();
        List<StatusTransition> viaTicket = reloaded.getStatusTransitions();
        assertThat(viaTicket).hasSize(2);
        assertThat(viaTicket).extracting(StatusTransition::getToStatus)
                .containsExactly(TicketStatus.ASSIGNED, TicketStatus.RESOLVED);
        assertThat(viaTicket).extracting(StatusTransition::getChangedAt).isSorted();

        List<StatusTransition> viaRepo = statusTransitionRepository.findByTicketIdOrderByChangedAtAsc(ticketId);
        assertThat(viaRepo).hasSize(2);
        assertThat(viaRepo).extracting(StatusTransition::getId)
                .containsExactlyElementsOf(viaTicket.stream().map(StatusTransition::getId).toList());

        assertThat(viaRepo).allSatisfy(t -> assertThat(t.getTicket().getId()).isEqualTo(ticketId));
    }

    @Test
    void pauseHistories_includeOpenEndedRecord_inEnteredAtOrder() {
        LocalDateTime base = LocalDateTime.of(2026, 11, 10, 10, 0);
        Ticket ticket = newTicket(TicketStatus.PENDING);

        PauseHistory open = new PauseHistory();
        open.setTicket(ticket);
        open.setEnteredAt(base.plusHours(3));
        open.setExitedAt(null);
        open.setReason("仍在暫停中");

        PauseHistory closed = new PauseHistory();
        closed.setTicket(ticket);
        closed.setEnteredAt(base);
        closed.setExitedAt(base.plusMinutes(30));
        closed.setReason("已結束的暫停");

        ticket.getPauseHistories().add(open);
        ticket.getPauseHistories().add(closed);

        Long ticketId = ticketRepository.save(ticket).getId();
        em.flush();
        em.clear();

        Ticket reloaded = ticketRepository.findById(ticketId).orElseThrow();
        List<PauseHistory> histories = reloaded.getPauseHistories();
        assertThat(histories).hasSize(2);
        assertThat(histories).extracting(PauseHistory::getEnteredAt)
                .containsExactly(base, base.plusHours(3));
        assertThat(histories.get(0).getExitedAt()).isEqualTo(base.plusMinutes(30));
        assertThat(histories.get(1).getExitedAt()).isNull();
        assertThat(histories).allSatisfy(h -> assertThat(h.getTicket().getId()).isEqualTo(ticketId));
    }
}
