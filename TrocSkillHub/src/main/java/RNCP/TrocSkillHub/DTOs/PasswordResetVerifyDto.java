package RNCP.TrocSkillHub.DTOs;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PasswordResetVerifyDto {
    @NotBlank
    @Email
    @Size(max = 120)
    private String email;
    @NotBlank
    @Size(min = 4, max = 16)
    private String code;

    public PasswordResetVerifyDto() {
    }

    public PasswordResetVerifyDto(String email, String code) {
        this.email = email;
        this.code = code;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
