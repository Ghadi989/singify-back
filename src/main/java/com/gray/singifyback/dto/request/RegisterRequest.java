package com.gray.singifyback.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(min = 3, max = 30) String username,
    @NotBlank @Size(min = 6) String password
) {}
