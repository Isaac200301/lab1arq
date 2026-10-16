package com.udea.lab1arq.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Habilita CORS para que el frontend de React pueda consumir la API.
 *
 * Sin esto el navegador bloquea las peticiones: al ser origenes distintos
 * (puertos o dominios diferentes), el navegador exige que el servidor declare
 * explicitamente que acepta peticiones desde ese origen.
 *
 * Los origenes no estan escritos en el codigo porque cambian segun el entorno:
 * en desarrollo es el servidor de Vite en el puerto 5173, y una vez desplegada
 * la aplicacion es el dominio publico del frontend. Se leen de la propiedad
 * app.cors.allowed-origins, que a su vez se alimenta de una variable de
 * entorno. Asi el mismo artefacto sirve para ambos entornos.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String[] allowedOrigins;

    public WebConfig(@Value("${app.cors.allowed-origins}") String[] allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}
