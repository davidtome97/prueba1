package com.sistemagestionapp.demojava.service;

import com.sistemagestionapp.demojava.model.Usuario;
import com.sistemagestionapp.demojava.model.mongo.UsuarioMongo;
import com.sistemagestionapp.demojava.repository.UsuarioRepository;
import com.sistemagestionapp.demojava.repository.mongo.UsuarioMongoRepository;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UsuarioService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;               // null si mongo
    private final UsuarioMongoRepository usuarioMongoRepository;     // null si sql
    private final String dbEngine;

    public UsuarioService(
            ObjectProvider<UsuarioRepository> usuarioRepository,
            ObjectProvider<UsuarioMongoRepository> usuarioMongoRepository,
            @Value("${app.db.engine:h2}") String dbEngine
    ) {
        this.usuarioRepository = usuarioRepository.getIfAvailable();
        this.usuarioMongoRepository = usuarioMongoRepository.getIfAvailable();
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
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        if (isMongo()) {
            if (usuarioMongoRepository == null) {
                throw new IllegalStateException("UsuarioMongoRepository no disponible (perfil mongo mal configurado)");
            }

            UsuarioMongo usuario = usuarioMongoRepository
                    .findByCorreo(username)
                    .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado en Mongo: " + username));

            return new User(
                    usuario.getCorreo(),
                    usuario.getPassword(),
                    List.of(new SimpleGrantedAuthority("ROLE_USER"))
            );
        }

        if (usuarioRepository == null) {
            throw new IllegalStateException("UsuarioRepository no disponible (perfil sql mal configurado)");
        }

        Usuario usuario = usuarioRepository
                .findByCorreo(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado en SQL: " + username));

        return new User(
                usuario.getCorreo(),
                usuario.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    // =========================================================
    // 2) USADO POR CONTROLADORES
    // =========================================================
    @Transactional(readOnly = true)
    public Usuario buscarPorCorreo(String correo) {

        if (isMongo()) {
            if (usuarioMongoRepository == null) {
                throw new IllegalStateException("UsuarioMongoRepository no disponible (perfil mongo mal configurado)");
            }

            return usuarioMongoRepository.findByCorreo(correo)
                    .map(um -> {
                        Usuario u = new Usuario();
                        u.setNombre(um.getNombre());
                        u.setCorreo(um.getCorreo());
                        u.setPassword(um.getPassword());
                        return u;
                    })
                    .orElse(null);
        }

        if (usuarioRepository == null) {
            throw new IllegalStateException("UsuarioRepository no disponible (perfil sql mal configurado)");
        }

        return usuarioRepository.findByCorreo(correo).orElse(null);
    }

    @Transactional(readOnly = true)
    public boolean existePorCorreo(String correo) {

        if (isMongo()) {
            if (usuarioMongoRepository == null) {
                throw new IllegalStateException("UsuarioMongoRepository no disponible (perfil mongo mal configurado)");
            }
            return usuarioMongoRepository.existsByCorreo(correo);
        }

        if (usuarioRepository == null) {
            throw new IllegalStateException("UsuarioRepository no disponible (perfil sql mal configurado)");
        }
        return usuarioRepository.existsByCorreo(correo);
    }

    @Transactional
    public void registrarUsuario(Usuario usuario) {

        if (isMongo()) {
            if (usuarioMongoRepository == null) {
                throw new IllegalStateException("UsuarioMongoRepository no disponible (perfil mongo mal configurado)");
            }

            if (usuarioMongoRepository.existsByCorreo(usuario.getCorreo())) return;

            UsuarioMongo um = new UsuarioMongo();
            um.setNombre(usuario.getNombre());
            um.setCorreo(usuario.getCorreo());
            um.setPassword(usuario.getPassword());

            usuarioMongoRepository.save(um);
            return;
        }

        if (usuarioRepository == null) {
            throw new IllegalStateException("UsuarioRepository no disponible (perfil sql mal configurado)");
        }

        if (usuarioRepository.existsByCorreo(usuario.getCorreo())) return;
        usuarioRepository.save(usuario);
    }
}