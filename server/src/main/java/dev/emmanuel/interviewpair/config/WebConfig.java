package dev.emmanuel.interviewpair.config;

import java.io.IOException;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * Serves the built React app from the classpath. A path that is not a real file and does not look
 * like an API call gets index.html, so refreshing a page inside the single-page app works.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new SpaFallbackResolver());
    }

    static class SpaFallbackResolver extends PathResourceResolver {
        @Override
        protected Resource getResource(String resourcePath, Resource location) throws IOException {
            Resource requested = location.createRelative(resourcePath);
            if (requested.exists() && requested.isReadable()) {
                return requested;
            }
            if (isApiPath(resourcePath) || resourcePath.contains(".")) {
                return null;
            }
            Resource index = new ClassPathResource("static/index.html");
            return index.exists() ? index : null;
        }

        private static boolean isApiPath(String path) {
            return path.equals("api") || path.startsWith("api/")
                    || path.equals("actuator") || path.startsWith("actuator/")
                    || path.equals("ws") || path.startsWith("ws/");
        }
    }
}
