package com.resonanceplanner.agent.client;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * The calls to the model, day by day and user by user (D129). Each method has its own transaction: the agent loop has
 * none (D128), and a call must count even when the turn fails afterwards.
 */
public interface ModelCallDayRepository extends Repository<ModelCallDay, ModelCallDay.Key> {

    /**
     * Adds one call of the user to the day with a single statement, so two calls at the same moment never lose one:
     * the first creates the row, the other waits for it and adds to it.
     */
    @Transactional
    @Modifying
    @Query(value = """
            insert into agent_model_calls (call_date, user_id, calls) values (:day, :userId, 1)
            on conflict (call_date, user_id) do update set calls = agent_model_calls.calls + 1""", nativeQuery = true)
    int addCall(@Param("day") LocalDate day, @Param("userId") Long userId);

    /** The calls of the whole app in the day. */
    @Transactional(readOnly = true)
    @Query("select coalesce(sum(d.calls), 0) from ModelCallDay d where d.id.callDate = :day")
    long callsOn(@Param("day") LocalDate day);

    /** The calls of one user in the day. */
    @Transactional(readOnly = true)
    @Query("select d.calls from ModelCallDay d where d.id.callDate = :day and d.id.userId = :userId")
    Optional<Integer> callsOn(@Param("day") LocalDate day, @Param("userId") Long userId);
}
