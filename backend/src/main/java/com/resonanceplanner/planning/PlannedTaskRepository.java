package com.resonanceplanner.planning;

import com.resonanceplanner.catalog.FarmingSourceType;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * The tasks of a user. It extends the bare Repository on purpose: JpaRepository would bring findAll, findById and
 * deleteById, which take no user, and a later card could call them by mistake (D44). Every query here takes the user.
 */
public interface PlannedTaskRepository extends Repository<PlannedTask, Long> {

    PlannedTask save(PlannedTask task);

    void delete(PlannedTask task);

    Optional<PlannedTask> findByIdAndUserId(Long id, Long userId);

    long countByUserIdAndOrigin(Long userId, TaskOrigin origin);

    /** The tasks of a range with the given statuses, by day and then in order of creation, with their sources. */
    @EntityGraph(attributePaths = "farmingSource")
    @Query("""
            select t from PlannedTask t
            where t.userId = :userId and t.taskDate between :from and :to and t.status in :statuses
            order by t.taskDate, t.id
            """)
    List<PlannedTask> findInRange(@Param("userId") Long userId, @Param("from") LocalDate from,
            @Param("to") LocalDate to, @Param("statuses") Collection<TaskStatus> statuses);

    /** How many runs of a type of source the user has in tasks with that status on that day (D94, D118). */
    @Query("""
            select coalesce(sum(t.runs), 0L) from PlannedTask t
            where t.userId = :userId and t.taskDate = :day and t.status = :status and t.farmingSource.type = :type
            """)
    long sumRuns(@Param("userId") Long userId, @Param("day") LocalDate day, @Param("status") TaskStatus status,
            @Param("type") FarmingSourceType type);
}
