package com.reservas.api.usuario;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
public class UsuarioService {

    // Nombre del índice único creado en la migración V1
    private static final String RESTRICCION_EMAIL = "uq_usuario_email";

    private final UsuarioRepository repositorio;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository repositorio, PasswordEncoder passwordEncoder) {
        this.repositorio = repositorio;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UsuarioResponse registrar(RegistroRequest datos) {
        String email = datos.email().trim().toLowerCase(Locale.ROOT);
        if (repositorio.existsByEmailIgnoreCase(email)) {
            throw emailYaRegistrado();
        }

        String passwordHash = passwordEncoder.encode(datos.password());
        Rol rol = datos.tipoDeCuenta() == null ? Rol.CLIENTE : datos.tipoDeCuenta().rol();

        Usuario usuario = new Usuario(email, passwordHash, datos.nombre(), rol);
        try {
            return UsuarioResponse.de(repositorio.saveAndFlush(usuario));
        } catch (DataIntegrityViolationException e) {
            if (!violaIndiceEmail(e)) {
                throw e;
            }
            throw emailYaRegistrado();
        }
    }

    private boolean violaIndiceEmail(DataIntegrityViolationException e) {
        return e.getMessage() != null && e.getMessage().contains(RESTRICCION_EMAIL);
    }

    private ResponseStatusException emailYaRegistrado() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un usuario con ese email");
    }

}
