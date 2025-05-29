package com.vimainsurance.vimaadmin.util;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.vimainsurance.vimaadmin.entity.AdminUser;
import com.vimainsurance.vimaadmin.entity.Agents;
import com.vimainsurance.vimaadmin.repository.IAdminUserRepository;

@Service
public class AdminUserDetailsService implements UserDetailsService {

    @Autowired
    private  IAdminUserRepository adminUserRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<AdminUser> adminUser = adminUserRepository.findByUsername(username);

        // if (!adminUser.getIsActive()) {
        //     throw new DisabledException("User is inactive");
        // }

        // SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + adminUser.getRole());

        return adminUser.map(Agents::new).orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

    }
}

