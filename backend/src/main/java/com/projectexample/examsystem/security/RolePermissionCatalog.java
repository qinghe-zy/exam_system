package com.projectexample.examsystem.security;

import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class RolePermissionCatalog {

    public List<String> permissionsForRole(String roleCode) {
        Set<String> permissions = new LinkedHashSet<>();
        if (roleCode == null) {
            return List.of();
        }
        switch (roleCode.toUpperCase(Locale.ROOT)) {
            case "ADMIN" -> permissions.addAll(List.of(
                    "auth:register:view", "auth:password:reset",
                    "sys:user:create", "sys:user:update", "sys:user:import",
                    "exam:question:create", "exam:question:update", "exam:question:delete", "exam:question:import", "exam:question:knowledge:auto-group",
                    "exam:paper:create", "exam:paper:update", "exam:paper:delete",
                    "notice:create", "notice:update", "notice:delete"
            ));
            case "ORG_ADMIN" -> permissions.addAll(List.of(
                    "auth:register:view", "auth:password:reset",
                    "sys:user:create", "sys:user:update", "sys:user:import",
                    "exam:question:create", "exam:question:update", "exam:question:delete", "exam:question:import", "exam:question:knowledge:auto-group",
                    "exam:paper:create", "exam:paper:update", "exam:paper:delete",
                    "notice:create", "notice:update", "notice:delete"
            ));
            case "TEACHER" -> permissions.addAll(List.of(
                    "auth:register:view", "auth:password:reset",
                    "sys:user:assignable:view",
                    "exam:question:create", "exam:question:update", "exam:question:delete", "exam:question:import", "exam:question:knowledge:auto-group",
                    "exam:paper:create", "exam:paper:update", "exam:paper:delete",
                    "notice:create", "notice:update"
            ));
            case "STUDENT" -> permissions.addAll(List.of(
                    "auth:register:view", "auth:password:reset",
                    "candidate:score:view", "candidate:score:detail"
            ));
            case "GRADER" -> permissions.addAll(List.of(
                    "auth:password:reset"
            ));
            case "PROCTOR" -> permissions.addAll(List.of(
                    "auth:password:reset"
            ));
            default -> {
            }
        }
        return permissions.stream().toList();
    }
}
