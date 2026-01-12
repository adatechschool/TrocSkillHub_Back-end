package RNCP.TrocSkillHub.DTOs;

import java.io.Serial;

public record UserDTO (
    Serial id,
    String firstName,
    String lastName,
    String email,
    String address,
    String city,
    String country,
    String phoneNumber,
    String description
) {}
