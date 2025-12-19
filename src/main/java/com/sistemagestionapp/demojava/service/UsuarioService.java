package com.sistemagestionapp.demojava.service;

import com.sistemagestionapp.demojava.model.Usuario;
import com.sistemagestionapp.demojava.model.mongo.UsuarioMongo;
import com.sistemagestionapp.demojava.repository.UsuarioRepository;
import com.sistemagestionapp.demojava.repository.mongo.UsuarioMongoRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;            // null si mongo
    private final UsuarioMongoRepository usuarioMongoRepository;  // null si sql
    private final PasswordEncoder passwordEncoder;
    private final String dbEngine;

    public UsuarioService(
            ObjectProvider<UsuarioRepository> usuarioRepository,
            ObjectProvider<UsuarioMongoRepository> usuarioMongoRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.db.engine:h2}") String dbEngine
    ) {
        this.usuarioRepository = usuarioRepository.getIfAvailable();
        this.usuarioMongoRepository = usuarioMongoRepository.getIfAvailable();
        this.passwordEncoder = passwordEncoder;
        this.dbEngine = (dbEngine == null ? "h2" : dbEngine.toLowerCase());
    }

    private boolean isMongo() {
        return "mongo".equalsIgnoreCase(dbEngine);
    }

    // =========================================================
    // 1) SPRING SECURITY LOGIN
    // =========================================================
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {

        if (isMongo()) {
            if (usuarioMongoRepository == null) {
                throw new IllegalStateException("UsuarioMongoRepository no disponible (revisa dependencias/perfil mongo)");
            }

            UsuarioMongo u = usuarioMongoRepository.findByCorreo(correo)
                    .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado en Mongo: " + correo));

            return User.withUsername(u.getCorreo())
                    .password(u.getPassword())
                    .roles("USER")
                    .build();
        }

        if (usuarioRepository == null) {
            throw new IllegalStateException("UsuarioRepository no disponible (revisa dependencias/perfil sql)");
        }

        Usuario u = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado en SQL: " + correo));

        return User.withUsername(u.getCorreo())
                .password(u.getPassword())
                .roles("USER")
                .build();
    }

    // =========================================================
    // 2) USADO POR CONTROLADORES
    // =========================================================
    @Transactional(readOnly = true)
    public Usuario buscarPorCorreo(String correo) {
        if (isMongo()) {
            if (usuarioMongoRepository == null) {
                throw new IllegalStateException("UsuarioMongoRepository no disponible (revisa dependencias/perfil mongo)");
            }
            return usuarioMongoRepository.findByCorreo(correo)
                    .map(um -> {
                        Usuario u = new Usuario();
                        u.setNombre(um.getNombre());
                        u.setCorreo(um.getCorreo());
                        u.setPassword(um.getPassword()); // OJO: esto es el hash
                        return u;
                    })
                    .orElse(null);
        }

        if (usuarioRepository == null) {
            throw new IllegalStateException("UsuarioRepository no disponible (revisa dependencias/perfil sql)");
        }
        return usuarioRepository.findByCorreo(correo).orElse(null);
    }

    @Transactional(readOnly = true)
    public boolean existePorCorreo(String correo) {
        if (isMongo()) {
            if (usuarioMongoRepository == null) {
                throw new IllegalStateException("UsuarioMongoRepository no disponible (revisa dependencias/perfil mongo)");
            }
            return usuarioMongoRepository.existsByCorreo(correo);
        }

        if (usuarioRepository == null) {
            throw new IllegalStateException("UsuarioRepository no disponible (revisa dependencias/perfil sql)");
        }
        return usuarioRepository.existsByCorreo(correo);
    }

    @Transactional
    public void registrarUsuario(Usuario usuario) {

        // ✅ SIEMPRE guardamos en BCrypt (si no, login falla)
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));

        if (isMongo()) {
            if (usuarioMongoRepository == null) {
                throw new IllegalStateException("UsuarioMongoRepository no disponible (revisa dependencias/perfil mongo)");
            }

            if (usuarioMongoRepository.existsByCorreo(usuario.getCorreo())) return;

            UsuarioMongo um = new UsuarioMongo();
            um.setNombre(usuario.getNombre());
            um.setCorreo(usuario.getCorreo());
            um.setPassword(usuario.getPassword()); // ya va hasheada

            usuarioMongoRepository.save(um);
            return;
        }

        if (usuarioRepository == null) {
            throw new IllegalStateException("UsuarioRepository no disponible (revisa dependencias/perfil sql)");
        }

        if (usuarioRepository.existsByCorreo(usuario.getCorreo())) return;
        usuarioRepository.save(usuario);
    }
}