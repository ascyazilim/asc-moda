package com.asc.userservice.controller;

import com.asc.userservice.dto.*;
import com.asc.userservice.security.JwtUtil;
import com.asc.userservice.service.AuthenticationService;
import com.asc.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")


public class UserController {

    private final UserService userService;
    private final AuthenticationService authenticationService;

    private final JwtUtil jwtUtil;

    public UserController(UserService userService, AuthenticationService authenticationService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.authenticationService = authenticationService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponseDTO> registerUser(@RequestBody @Valid RegisterUserDTO registerUserDTO){
        UserResponseDTO userResponseDTO = userService.saveUser(registerUserDTO);
        return ResponseEntity.ok(userResponseDTO);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@RequestBody AuthRequestDTO authRequestDTO){
        AuthResponseDTO response = authenticationService.authenticate(authRequestDTO);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{username}")
    public ResponseEntity<UserResponseDTO> getUserByUsername(@PathVariable String username) {
        Optional<UserResponseDTO> user = userService.findByUsername(username);
        return user.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/update")
    public ResponseEntity<UserResponseDTO> updateUser(@RequestBody @Valid UpdateUserDTO updateUserDTO,
                                                      @RequestHeader("Authorization") String token){
        String username = jwtUtil.extractUsername(token.substring(7));
        UserResponseDTO updatedUser = userService.updateUser(username, updateUserDTO);
        return ResponseEntity.ok(updatedUser);
    }

    @PutMapping("/change-password")
    public ResponseEntity<String> changePassword(@RequestBody @Valid ChangePasswordDTO changePasswordDTO,
                                                 @RequestHeader("Authorization") String token){
        String username = jwtUtil.extractUsername(token.substring(7));
        userService.changePassword(username,changePasswordDTO);
        return ResponseEntity.ok("Password changed successfully");
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponseDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok("User deleted successfully");
    }
}