package interview.guide.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "app.resume")
@Getter
@Setter
public class AppConfigProperties {
    private String uploadDir;
    private List<String> allowedTypes;
}
