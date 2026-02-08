package com.esprit.user.dto;

import com.esprit.user.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request body for creating or updating a user")
public class UserRequest {

    @NotBlank
    @Email
    @Schema(description = "User email address", example = "john.doe@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @Schema(description = "Plain password (only for create; optional for update)")
    private String password;

    @NotBlank
    @Schema(description = "First name", example = "John", requiredMode = Schema.RequiredMode.REQUIRED)
    private String firstName;

    @NotBlank
    @Schema(description = "Last name", example = "Doe", requiredMode = Schema.RequiredMode.REQUIRED)
    private String lastName;

    @NotNull
    @Schema(description = "User role", requiredMode = Schema.RequiredMode.REQUIRED)
    private Role role;

    @Schema(description = "Phone number", example = "+1234567890")
    private String phone;

    @Schema(description = "URL of the user's avatar image")
    private String avatarUrl;

    @Schema(description = "Whether the user account is active", example = "true")
    private Boolean isActive;
}
