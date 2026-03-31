package com.lshlabs.prompthubspring.transaction;

import com.lshlabs.prompthubspring.auth.AuthService;
import com.lshlabs.prompthubspring.post.PostService;
import com.lshlabs.prompthubspring.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceTransactionBoundaryTest {

    @Test
    void writePaths_mustBeTransactional() {
        assertTransactionalMethods(AuthService.class, List.of(
                "register",
                "login",
                "loginWithGoogle",
                "refresh",
                "logout",
                "rotateTokenPair"
        ));

        assertTransactionalMethods(UserService.class, List.of(
                "updateProfile",
                "changePassword",
                "updateSettings",
                "endSession",
                "endOtherSessions",
                "regenerateAvatar",
                "deleteAccount"
        ));

        assertTransactionalMethods(PostService.class, List.of(
                "detail",
                "create",
                "update",
                "delete",
                "toggleLike",
                "toggleBookmark"
        ));
    }

    private void assertTransactionalMethods(Class<?> serviceClass, List<String> expectedMethods) {
        Map<String, Boolean> transactionalByName = java.util.Arrays.stream(serviceClass.getDeclaredMethods())
                .collect(java.util.stream.Collectors.toMap(
                        Method::getName,
                        method -> method.isAnnotationPresent(Transactional.class),
                        (left, right) -> left || right
                ));

        for (String methodName : expectedMethods) {
            boolean transactional = transactionalByName.getOrDefault(methodName, false);
            assertTrue(transactional, serviceClass.getSimpleName() + "." + methodName + " must be @Transactional");
        }
    }
}
