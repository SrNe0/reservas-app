package com.reservas.api.auth;

import com.reservas.api.usuario.Usuario;
import com.reservas.api.usuario.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Optional;

@Service
public class AuthService {

    private static final String CREDENCIALES_INCORRECTAS = "Credenciales incorrectas";

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwt;
    // Hash de una contraseña que nadie usa: sirve para gastar el mismo tiempo de BCrypt cuando el email no existe
    private final String hashFalso;

    public AuthService(UsuarioRepository usuarios, PasswordEncoder passwordEncoder, JwtService jwt) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.jwt = jwt;
        this.hashFalso = passwordEncoder.encode("contrasena-que-no-pertenece-a-nadie");
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest datos) {
        String email = datos.email().toLowerCase(Locale.ROOT);
        Optional<Usuario> usuario = usuarios.findByEmailIgnoreCase(email);

        // Se ejecuta BCrypt siempre, exista o no el email: si no, la respuesta sería mucho más rápida
        // para los emails que no existen y el tiempo delataría qué cuentas están registradas
        String hash = usuario.map(Usuario::getPasswordHash).orElse(hashFalso);
        boolean coincide = passwordEncoder.matches(datos.password(), hash);

        if (usuario.isEmpty() || !coincide) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, CREDENCIALES_INCORRECTAS);
        }
        return LoginResponse.bearer(jwt.emitir(usuario.get()), jwt.segundosDeVida());
    }

}
