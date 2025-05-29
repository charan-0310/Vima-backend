package com.vimainsurance.vimaadmin.entity;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class Agents implements UserDetails{

    private String username; 
    private String password;
    private List<GrantedAuthority> authorities;

    public Agents(AdminUser adminUser){
        this.username = adminUser.getUsername();
        this.password = adminUser.getPasswordHash();
        this.authorities = List.of(adminUser.getRole().split(","))
                .stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
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

}
