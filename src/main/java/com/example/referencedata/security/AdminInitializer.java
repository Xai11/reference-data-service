package com.example.referencedata.security;

import com.example.referencedata.model.AppUser;
import com.example.referencedata.model.Role;
import com.example.referencedata.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.HexFormat;

/** Создаёт первого администратора, если в системе ещё нет ни одного пользователя. */
@Component
public class AdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final AppUserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String password;

    public AdminInitializer(AppUserRepository repository, PasswordEncoder passwordEncoder,
                            @Value("${app.admin.username:admin}") String username,
                            @Value("${app.admin.password:}") String password) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) {
            return;
        }
        String rawPassword = password;
        boolean generated = rawPassword == null || rawPassword.isBlank();
        if (generated) {
            byte[] bytes = new byte[12];
            new SecureRandom().nextBytes(bytes);
            rawPassword = HexFormat.of().formatHex(bytes);
        }
        AppUser admin = new AppUser();
        admin.setUsername(username);
        admin.setPassword(passwordEncoder.encode(rawPassword));
        admin.setRole(Role.EDITOR);
        repository.save(admin);
        if (generated) {
            log.warn("Создан администратор '{}' со сгенерированным паролем: {}", username, rawPassword);
        } else {
            log.info("Создан администратор '{}'", username);
        }
    }
}
