package RNCP.TrocSkillHub.DTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PasswordResetDto {
    @NotBlank
    @Size(max = 64)
    private String resetToken;
    @NotBlank
    @Size(min = 8, max = 72)
    private String newPassword;
    @NotBlank
    @Size(min = 8, max = 72)
    private String confirmPassword;

    public PasswordResetDto() {
    }

    public PasswordResetDto(String resetToken, String newPassword, String confirmPassword) {
        this.resetToken = resetToken;
        this.newPassword = newPassword;
        this.confirmPassword = confirmPassword;
    }

    public String getResetToken() {
        return resetToken;
    }

    public void setResetToken(String resetToken) {
        this.resetToken = resetToken;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
