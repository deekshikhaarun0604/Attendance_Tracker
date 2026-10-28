package com.smartattend.security;

import com.smartattend.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class UserDetailsImpl implements UserDetails {

    private final String id;
    private final String username;
    private final String password;
    private final String fullName;
    private final String email;
    private final String role;
    private final String linkedStudentId;
    private final String linkedFacultyId;
    private final boolean active;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserDetailsImpl(String id, String username, String password, String fullName,
                           String email, String role, String linkedStudentId, String linkedFacultyId,
                           boolean active, Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.linkedStudentId = linkedStudentId;
        this.linkedFacultyId = linkedFacultyId;
        this.active = active;
        this.authorities = authorities;
    }

    public static UserDetailsImpl build(User user) {
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(user.getRole().name()));
        return new UserDetailsImpl(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                user.getFullName(),
                user.getEmail(),
                user.getRole().name(),
                user.getLinkedStudentId(),
                user.getLinkedFacultyId(),
                user.isActive(),
                authorities
        );
    }

    public String getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public String getLinkedStudentId() {
        return linkedStudentId;
    }

    public String getLinkedFacultyId() {
        return linkedFacultyId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
