package RNCP.TrocSkillHub.DTOs;


public record UserDTO (
    Long id,
    String firstName,
    String lastName,
    String email,
    String address,
    String city,
    String country,
    String phoneNumber,
    String description
) {}
