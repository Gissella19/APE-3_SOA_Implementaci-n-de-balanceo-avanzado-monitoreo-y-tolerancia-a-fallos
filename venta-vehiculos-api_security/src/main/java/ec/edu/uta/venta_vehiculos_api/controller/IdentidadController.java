package ec.edu.uta.venta_vehiculos_api.controller;

import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
public class IdentidadController {

 @GetMapping("/api/identidad")

 public Map<String,Object> identidad(Authentication a) {

  return Map.of("usuario",a.getName(),"roles",
   a.getAuthorities().stream().map(x -> x.getAuthority()).toList());
 }
}
