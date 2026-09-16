package com.udea.lab1arq.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Habilita CORS para que el frontend de React (servido por Vite en el puerto
 * 5173) pueda consumir la API, que corre en el puerto 8088.
 *
 * Sin esto el navegador bloquea las peticiones: al ser origenes distintos
 * (puertos diferentes), el navegador exige que el servidor declare
 * explicitamente que acepta peticiones desde ese origen.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(
                        "http://localhost:5173",
                        "http://127.0.0.1:5173"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}
