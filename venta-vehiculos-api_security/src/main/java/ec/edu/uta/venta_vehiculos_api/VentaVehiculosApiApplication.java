// Define el paquete al que pertenece la clase en la estructura del proyecto
package ec.edu.uta.venta_vehiculos_api;

// Importa la clase SpringApplication para permitir el arranque de la aplicación
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Habilita la autoconfiguración, el escaneo de paquetes (@ComponentScan) y la configuración de Spring Boot (@Configuration)
@SpringBootApplication
// Declaración de la clase principal de la aplicación
public class VentaVehiculosApiApplication {

    // Método principal (entry point) que se ejecuta al iniciar la aplicación en Java
    public static void main(String[] args) {
        // Inicializa y arranca el contexto de Spring Boot pasando la clase actual y los argumentos de consola
        SpringApplication.run(VentaVehiculosApiApplication.class, args);
    }

}