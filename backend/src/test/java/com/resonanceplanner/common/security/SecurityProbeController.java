package com.resonanceplanner.common.security;

import com.resonanceplanner.auth.AuthenticatedUser;
import com.resonanceplanner.common.api.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exists only in the tests: two protected paths that show who the JWT filter authenticated,
 * so the security rules can be checked before the real /api/me and /api/admin controllers exist.
 */
@RestController
class SecurityProbeController {

    @GetMapping("/api/probe/me")
    ApiResponse<AuthenticatedUser> me(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success(user);
    }

    @GetMapping("/api/admin/probe")
    ApiResponse<String> adminOnly() {
        return ApiResponse.success("admin area");
    }
}
