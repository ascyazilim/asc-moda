package com.asc.userservice.dto;
import com.asc.userservice.entity.Role;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Data
public class UserResponseDTO {

    private Long id;
    private String username;
    private String email;
    private Role role;
}