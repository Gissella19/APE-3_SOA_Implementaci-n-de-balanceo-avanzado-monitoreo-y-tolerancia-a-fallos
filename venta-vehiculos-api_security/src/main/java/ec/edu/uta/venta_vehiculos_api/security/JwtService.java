package ec.edu.uta.venta_vehiculos_api.security;


// Importa Claims, que representa los datos contenidos dentro del JWT.
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;


// Registra esta clase como un servicio de Spring.
// Gracias a esto podemos inyectarla "pasar el dato" en AuthController
// o en JwtAuthenticationFilter. 
@Service
public class JwtService {


    // Lee desde application.properties:
    // app.security.jwt-secret=....
    //
    // Este valor será la clave secreta utilizada
    // para FIRMAR y posteriormente VALIDAR los JWT.
    @Value("${app.security.jwt-secret}")
    private String jwtSecret;


    // Lee la duración del Access Token desde application.properties.
    //
    // Ejemplo:
    // app.security.access-token-minutes=600
    @Value("${app.security.access-token-minutes}")
    private long accessTokenMinutes;


    // Lee la duración del Refresh Token.
    //
    // Ejemplo:
    // app.security.refresh-token-minutes=1440
    @Value("${app.security.refresh-token-minutes}")
    private long refreshTokenMinutes;


    // Método privado que genera la clave criptográfica
    // que utilizará JJWT para firmar y validar los tokens.
    private SecretKey getSigningKey() {

        // Keys.hmacShaKeyFor() crea una clave HMAC
        // utilizando los bytes de nuestro secreto.
        return Keys.hmacShaKeyFor(

                // Convierte el String jwtSecret en un arreglo de bytes
                // utilizando codificación UTF-8.
                jwtSecret.getBytes(StandardCharsets.UTF_8)
        );
    }


    // Método público utilizado para generar un ACCESS TOKEN.
    //
    // Recibe:
    // username -> usuario autenticado.
    // idapli   -> identificador de la aplicación.
    // scope    -> permiso que tendrá el token.
    public String generarAccessToken(
            String username,
            String idapli,
            String scope,
            String rol) {

        // No genera directamente todo el JWT.
        // Reutiliza el método privado generarToken().
        return generarToken(

                // Usuario que aparecerá como subject del JWT.
                username,

                // Identificador de aplicación que irá dentro del token.
                idapli,

                // Permiso asociado al token.
                scope,
                rol,

                // Tiempo de duración configurado para el Access Token.
                accessTokenMinutes,

                // Tipo de token.
                // Este claim permitirá identificar que es un ACCESS TOKEN.
                "access"
        );
    }


    // Método público utilizado para generar un REFRESH TOKEN.
    //
    // Recibe los mismos datos que el Access Token.
    public String generarRefreshToken(
            String username,
            String idapli,
            String scope,
            String rol) {

        // También reutiliza generarToken().
        return generarToken(

                // Usuario.
                username,

                // Identificador de aplicación.
                idapli,

                // Scope o permiso.
                scope,
                rol,

                // En este caso utiliza el tiempo configurado
                // específicamente para el Refresh Token.
                refreshTokenMinutes,

                // Indica que este JWT será de tipo refresh.
                "refresh"
        );
    }


    // Método privado reutilizable encargado realmente
    // de construir cualquier JWT.
    //
    // Se utiliza tanto para Access Token como para Refresh Token.
    private String generarToken(
            String username, // Usuario propietario del token.
            String idapli,   // Identificador de la aplicación.
            String scope,    // Permiso del usuario/token.
            String rol,
            long minutos,    // Tiempo de duración.
            String tipo) {   // access o refresh.


        // Obtiene la fecha y hora actual.
        //
        // Ejemplo conceptual:
        // 03/09/2026 07:40
        Date ahora = new Date();


        // Calcula cuándo debe expirar el token.
        Date expiracion = new Date(

                // getTime() devuelve la fecha actual en milisegundos.
                //
                // minutos * 60     -> convierte minutos a segundos.
                // * 1000           -> convierte segundos a milisegundos.
                ahora.getTime() + minutos * 60 * 1000
        );


        // Comienza la construcción del JWT.
        return Jwts.builder()

                // Define el SUBJECT del token.
                //
                // El subject normalmente representa
                // al usuario propietario del token.
                //
                // En el JWT aparecerá como:
                //
                // "sub": "9999999999"
                .subject(username)


                // Agrega claims personalizados.
                //
                // Claims = información almacenada
                // dentro del payload del JWT.
                .claims(Map.of(

                        // Guarda el identificador de aplicación.
                        //
                        // Ejemplo:
                        // "idapli": "92"
                        "idapli", idapli,

                        // Guarda el scope o permiso.
                        //
                        // Ejemplo:
                        // "scope": "secret"
                        "scope", scope,
                       // Rol de validadicion
                        "rol", rol,

                        // Guarda el tipo de token.
                        //
                        // Puede ser:
                        // "access"
                        // o
                        // "refresh"
                        "tipo", tipo
                ))


                // Agrega la fecha en la cual fue creado el token.
                //
                // En JWT corresponde al claim estándar:
                // iat = Issued At
                .issuedAt(ahora)


                // Define hasta cuándo será válido el JWT.
                //
                // Corresponde al claim:
                // exp = Expiration Time
                .expiration(expiracion)


                // Firma digitalmente el JWT.
                //
                // Utiliza la clave obtenida desde jwt-secret.
                //
                // Esta firma permite detectar
                // si alguien modificó el contenido del token.
                .signWith(getSigningKey())


                // Convierte todo lo construido a la representación
                // compacta del JWT:
                //
                // HEADER.PAYLOAD.SIGNATURE
                //
                // Ejemplo:
                // eyJhbGciOiJIUzI1NiJ9...
                .compact();
    }


    // Método que recibe un JWT enviado por el cliente
    // y comprueba que sea válido.
    public Claims validarToken(String token) {

        // Comienza a construir el parser
        // encargado de leer y verificar JWT.
        return Jwts.parser()

                // Indica qué clave debe utilizarse
                // para comprobar la firma del JWT.
                //
                // Debe ser la MISMA clave utilizada
                // cuando se generó el token.
                .verifyWith(getSigningKey())

                // Construye el parser.
                .build()

                // Intenta analizar el JWT recibido.
                //
                // Aquí JJWT verifica, entre otras cosas:
                // - estructura del JWT
                // - firma
                // - expiración
                //
                // Si algo está mal, lanza una excepción.
                .parseSignedClaims(token)

                // Si todo fue correcto,
                // devuelve el payload del JWT.
                //
                // Es decir, devuelve sus Claims.
                .getPayload();
    }


    // Método auxiliar que devuelve cuánto dura
    // el Access Token, pero expresado en SEGUNDOS.
    public long getAccessTokenExpiresInSeconds() {

        // La configuración originalmente está en minutos.
        //
        // Por ejemplo:
        // 600 minutos * 60 = 36 000 segundos.
        //
        // Este valor normalmente se utiliza
        // para devolver el campo expires_in.
        return accessTokenMinutes * 60;
    }
}
