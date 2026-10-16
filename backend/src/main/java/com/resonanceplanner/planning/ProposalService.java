package com.resonanceplanner.planning;

import com.resonanceplanner.catalog.FarmingSourceRepository;
import com.resonanceplanner.common.config.Today;
import com.resonanceplanner.common.error.ConflictException;
import com.resonanceplanner.common.error.NotFoundException;
import com.resonanceplanner.common.error.UnauthorizedException;
import com.resonanceplanner.common.error.UniqueViolation;
import com.resonanceplanner.event.EventService;
import com.resonanceplanner.planner.PlanInputs;
import com.resonanceplanner.planner.PlannerService;
import com.resonanceplanner.planner.ScheduleDraft;
import com.resonanceplanner.planner.ScheduleDraft.DayPlan;
import com.resonanceplanner.planner.ScheduleDraft.DraftTask;
import com.resonanceplanner.planner.ScheduleRequest;
import com.resonanceplanner.planning.dto.ProposalResponse;
import com.resonanceplanner.user.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The proposals of the user of the token (D22, D64, D124): a plan of the planner is saved as the pending proposal, and
 * the user confirms or rejects it. Every write locks the user first (D118), so two requests at the same moment run one
 * after the other; the unique index of D37 is the last guard. It answers 401 if the user does not exist anymore (D102).
 */
@Service
@Transactional
public class ProposalService {

    private static final String NOT_FOUND = "Proposta non trovata.";
    private static final List<ProposalStatus> LAST_PLAN = List.of(ProposalStatus.PENDING, ProposalStatus.CONFIRMED);
    private static final String NOTHING_TO_PLAN = "Il pianificatore non ha trovato niente da fare in questi giorni.";

    private final PlanProposalRepository proposalRepository;
    private final PlannedTaskRepository taskRepository;
    private final FarmingSourceRepository farmingSourceRepository;
    private final UserRepository userRepository;
    private final PlannerService plannerService;
    private final PlanInputs planInputs;
    private final Today today;
    private final Clock clock;

    public ProposalService(PlanProposalRepository proposalRepository, PlannedTaskRepository taskRepository,
            FarmingSourceRepository farmingSourceRepository, UserRepository userRepository,
            PlannerService plannerService, PlanInputs planInputs, Today today, Clock clock) {
        this.proposalRepository = proposalRepository;
        this.taskRepository = taskRepository;
        this.farmingSourceRepository = farmingSourceRepository;
        this.userRepository = userRepository;
        this.plannerService = plannerService;
        this.planInputs = planInputs;
        this.today = today;
        this.clock = clock;
    }

    /** The basic plan without AI (D64): the planner with its defaults and a standard text with its warnings. */
    public ProposalResponse createBasic(Long userId) {
        lockUser(userId);
        ScheduleDraft draft = plannerService.draft(userId, ScheduleRequest.defaults());
        return save(userId, draft, basicSummary(draft));
    }

    /**
     * Saves a plan of the planner as the pending proposal, in place of the one before (D22, D56); 409 with the
     * warnings of the planner if the plan has no task (D124, point 1). The agent uses it too (T33): the caller should
     * lock the user before it calculates the draft, like {@link #createBasic}, so the draft sees the latest state.
     */
    public ProposalResponse save(Long userId, ScheduleDraft draft, String summary) {
        lockUser(userId);
        if (taskCount(draft) == 0) {
            throw new ConflictException(
                    draft.warnings().isEmpty() ? NOTHING_TO_PLAN : String.join(" ", draft.warnings()));
        }
        Instant now = clock.instant();
        proposalRepository.findByUserIdAndStatus(userId, ProposalStatus.PENDING)
                .ifPresent(pending -> close(pending, ProposalStatus.REPLACED, now));
        LocalDate firstDay = draft.days().getFirst().date();
        LocalDate lastDay = draft.days().getLast().date();
        PlanProposal proposal = insert(PlanProposal.pending(userId, summary, now, firstDay, lastDay,
                planInputs.fingerprint(userId, firstDay, lastDay)));
        List<PlannedTask> tasks = new ArrayList<>();
        for (DayPlan day : draft.days()) {
            for (DraftTask task : day.tasks()) {
                tasks.add(taskRepository.save(PlannedTask.proposed(userId, proposal.getId(), day.date(), task,
                        task.farmingSourceId() == null
                                ? null
                                : farmingSourceRepository.getReferenceById(task.farmingSourceId()),
                        now)));
            }
        }
        return ProposalResponse.from(proposal, tasks);
    }

    /** The pending proposal with its tasks, if there is one. */
    @Transactional(readOnly = true)
    public Optional<ProposalResponse> pending(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new UnauthorizedException();
        }
        return pendingOf(userId);
    }

    /** The same, for a caller that has already checked the user, like the calendar. */
    @Transactional(readOnly = true)
    Optional<ProposalResponse> pendingOf(Long userId) {
        return proposalRepository.findByUserIdAndStatus(userId, ProposalStatus.PENDING)
                .map(proposal -> ProposalResponse.from(proposal, tasksOf(proposal)));
    }

    /**
     * True when roster, goals or events of the days of the last plan changed after it was made (D23, D125); false
     * without a plan, or for a plan saved before the fingerprint existed.
     */
    @Transactional(readOnly = true)
    boolean isPlanOutdated(Long userId) {
        return proposalRepository.findFirstByUserIdAndStatusInOrderByIdDesc(userId, LAST_PLAN)
                .filter(plan -> plan.getInputsHash() != null)
                // The planner makes plans of at most 14 days; a longer one could not be read with the events, whose
                // range is limited, and would stop the whole calendar: it is never called out of date
                .filter(plan -> ChronoUnit.DAYS.between(plan.getFirstDay(), plan.getLastDay())
                        < EventService.MAX_RANGE_DAYS)
                .map(plan -> !plan.getInputsHash().equals(
                        planInputs.fingerprint(userId, plan.getFirstDay(), plan.getLastDay())))
                .orElse(false);
    }

    /**
     * The tasks enter the calendar and replace the confirmed tasks of the agent from today on (D22, D124, point 4).
     * Only on the day the proposal was made: it was calculated with the Waveplate and the tasks of that day.
     */
    public ProposalResponse confirm(Long userId, Long proposalId) {
        lockUser(userId);
        PlanProposal proposal = requirePending(userId, proposalId);
        if (!today.dateOf(proposal.getCreatedAt()).equals(today.date())) {
            throw new ConflictException("La proposta è di un altro giorno: generane una nuova.");
        }
        taskRepository.findReplaceable(userId, today.date()).forEach(PlannedTask::replace);
        List<PlannedTask> tasks = tasksOf(proposal);
        tasks.forEach(PlannedTask::confirm);
        proposal.decide(ProposalStatus.CONFIRMED, clock.instant());
        proposalRepository.saveAndFlush(proposal);
        return ProposalResponse.from(proposal, tasks);
    }

    /** The proposal stays as history, its tasks are deleted (D124, point 5). */
    public ProposalResponse reject(Long userId, Long proposalId) {
        lockUser(userId);
        PlanProposal proposal = requirePending(userId, proposalId);
        close(proposal, ProposalStatus.REJECTED, clock.instant());
        return ProposalResponse.from(proposal, List.of());
    }

    // Written at once: Hibernate runs the inserts before the updates, and the new pending proposal would meet the
    // index of D37 while the old one is still pending
    private void close(PlanProposal proposal, ProposalStatus decision, Instant now) {
        taskRepository.deleteAll(tasksOf(proposal));
        proposal.decide(decision, now);
        proposalRepository.saveAndFlush(proposal);
    }

    private PlanProposal insert(PlanProposal proposal) {
        try {
            return proposalRepository.saveAndFlush(proposal);
        } catch (DataIntegrityViolationException e) {
            if (!UniqueViolation.isIn(e)) {
                throw e;
            }
            throw new ConflictException("C'è già un'altra proposta in attesa: riprova.");
        }
    }

    private List<PlannedTask> tasksOf(PlanProposal proposal) {
        return taskRepository.findAllByUserIdAndProposalIdOrderByTaskDateAscIdAsc(proposal.getUserId(),
                proposal.getId());
    }

    private PlanProposal requirePending(Long userId, Long proposalId) {
        PlanProposal proposal = proposalRepository.findByIdAndUserId(proposalId, userId)
                .orElseThrow(() -> new NotFoundException(NOT_FOUND));
        if (!proposal.isPending()) {
            throw new ConflictException("Questa proposta non è più in attesa.");
        }
        return proposal;
    }

    // D124, point 2: the length of the plan and its tasks, then what the planner warns about
    private static String basicSummary(ScheduleDraft draft) {
        StringBuilder summary = new StringBuilder("Piano base di ").append(draft.days().size())
                .append(" giorni, ").append(taskCount(draft)).append(" attività.");
        draft.warnings().forEach(warning -> summary.append(' ').append(warning));
        return summary.toString();
    }

    private static long taskCount(ScheduleDraft draft) {
        return draft.days().stream().mapToLong(day -> day.tasks().size()).sum();
    }

    private void lockUser(Long userId) {
        if (userRepository.lockById(userId).isEmpty()) {
            throw new UnauthorizedException();
        }
    }
}
