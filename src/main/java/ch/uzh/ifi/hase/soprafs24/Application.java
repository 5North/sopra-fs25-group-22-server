package ch.uzh.ifi.hase.soprafs24;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@RestController
@SpringBootApplication
public class Application {

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

  @GetMapping(value = "/", produces = MediaType.TEXT_PLAIN_VALUE)
  @ResponseStatus(HttpStatus.OK)
  public String helloWorld() {
    return """
               ███████╗ ██████╗ ██████╗ ██████╗  █████╗     ███████╗ ██████╗ ██████╗  \s
               ██╔════╝██╔════╝██╔═══██╗██╔══██╗██╔══██╗    ██╔════╝██╔═══██╗██╔══██╗ \s
               ███████╗██║     ██║   ██║██████╔╝███████║    █████╗  ██║   ██║██████╔╝ \s
               ╚════██║██║     ██║   ██║██╔═══╝ ██╔══██║    ██╔══╝  ██║   ██║██╔══██╗ \s
               ███████║╚██████╗╚██████╔╝██║     ██║  ██║    ██║     ╚██████╔╝██║  ██║ \s
               ╚══════╝ ╚═════╝ ╚═════╝ ╚═╝     ╚═╝  ╚═╝    ╚═╝      ╚═════╝ ╚═╝  ╚═╝ \s
           \s
               ██████╗ ███████╗ ██████╗ ██╗███╗   ██╗███╗   ██╗███████╗██████╗ ███████╗
               ██╔══██╗██╔════╝██╔════╝ ██║████╗  ██║████╗  ██║██╔════╝██╔══██╗██╔════╝
               ██████╔╝█████╗  ██║  ███╗██║██╔██╗ ██║██╔██╗ ██║█████╗  ██████╔╝███████╗
               ██╔══██╗██╔══╝  ██║   ██║██║██║╚██╗██║██║╚██╗██║██╔══╝  ██╔══██╗╚════██║
               ██████╔╝███████╗╚██████╔╝██║██║ ╚████║██║ ╚████║███████╗██║  ██║███████║
               ╚═════╝ ╚══════╝ ╚═════╝ ╚═╝╚═╝  ╚═══╝╚═╝  ╚═══╝╚══════╝╚═╝  ╚═╝╚══════╝
              \s
            The application is running.
           \s""";
  }

  @Bean
  public WebMvcConfigurer corsConfigurer() {
    return new WebMvcConfigurer() {
      @Override
      public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:3000", "https://sopra-fs25-group-22-client.vercel.app")
                .allowedMethods("*")
                .exposedHeaders("Token");
      }
    };
  }
}
