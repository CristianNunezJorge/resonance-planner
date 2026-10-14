package com.resonanceplanner.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import com.resonanceplanner.inventory.dto.UpdateQuantityRequest;
import com.resonanceplanner.user.Role;
import com.resonanceplanner.user.User;
import com.resonanceplanner.user.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

// No @Transactional on the test: every call of the service opens and commits its own transaction, like in production.
// In the other tests the service joins the transaction of the test, which never commits, so they could not notice a
// write that answers 200 and saves nothing, nor two requests that really run at the same moment.
@SpringBootTest
@ActiveProfiles("test")
@Timeout(60)
class InventoryCommitTest {

    private static final String MATERIAL = "shell-credit";

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void writesAndCorrectsTheQuantityInTheDatabaseWhenEachCallEnds() {
        User user = newUser("inventory.commit");
        try {
            inventoryService.setQuantity(user.getId(), MATERIAL, new UpdateQuantityRequest(120));
            assertThat(savedQuantity(user)).isEqualTo(120);

            inventoryService.setQuantity(user.getId(), MATERIAL, new UpdateQuantityRequest(30));
            assertThat(savedQuantity(user)).isEqualTo(30);
            assertThat(rowsOf(user)).isEqualTo(1);
        } finally {
            cleanUp(user);
        }
    }

    @Test
    void savesTheSameNewMaterialFromManyRequestsAtTheSameMomentWithoutAnError() throws Exception {
        User user = newUser("inventory.race");
        int requests = 8;
        ExecutorService pool = Executors.newFixedThreadPool(requests);
        try {
            // The first call is slow (connections, query plans): done before, so the 8 requests really overlap
            User warmUp = newUser("inventory.warm");
            try {
                inventoryService.setQuantity(warmUp.getId(), MATERIAL, new UpdateQuantityRequest(1));
            } finally {
                cleanUp(warmUp);
            }
            CountDownLatch start = new CountDownLatch(1);
            List<Future<?>> results = new ArrayList<>();
            for (int i = 1; i <= requests; i++) {
                int quantity = i * 10;
                results.add(pool.submit(() -> {
                    start.await();
                    inventoryService.setQuantity(user.getId(), MATERIAL, new UpdateQuantityRequest(quantity));
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> result : results) {
                // A get() that throws means one of the requests failed: the second insert must not hit the unique index
                result.get(30, TimeUnit.SECONDS);
            }

            assertThat(rowsOf(user)).isEqualTo(1);
            // The last one to write wins: the quantity is one of those sent, never a mix
            assertThat(savedQuantity(user)).isIn(10, 20, 30, 40, 50, 60, 70, 80);
        } finally {
            pool.shutdownNow();
            pool.awaitTermination(10, TimeUnit.SECONDS);
            cleanUp(user);
        }
    }

    // A name and an email that no earlier run can have left behind in the test database
    private User newUser(String prefix) {
        String unique = prefix + "." + UUID.randomUUID().toString().substring(0, 8);
        return userRepository.saveAndFlush(User.create(unique, unique + "@example.com", "hash", Role.USER));
    }

    // A query of its own, outside the persistence context: it sees only what was really committed
    private Integer savedQuantity(User user) {
        return jdbc.queryForObject("select quantity from inventory_item where user_id = ? and material_id = ?",
                Integer.class, user.getId(), MATERIAL);
    }

    private Integer rowsOf(User user) {
        return jdbc.queryForObject("select count(*) from inventory_item where user_id = ?", Integer.class, user.getId());
    }

    // These tests commit, so they clean up after themselves (ON DELETE CASCADE removes the inventory with the user)
    private void cleanUp(User user) {
        jdbc.update("delete from users where id = ?", user.getId());
    }
}
