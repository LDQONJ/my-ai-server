package work.daqian.myai.config;

import org.springframework.stereotype.Component;
import work.daqian.myai.enums.Provider;

import java.util.Map;
import java.util.Set;

@Component
public class ModelPermissionConfig {

    private static final Map<String, Set<Provider>> ROLE_MODEL_PERMISSION = Map.of(
            "ROLE_USER", Set.of(Provider.OLLAMA),
            "ROLE_ADMIN", Set.of(Provider.OLLAMA, Provider.ALIBABA, Provider.RESTRICT, Provider.GOOGLE)
    );

    public static boolean hasPermission(String role, Provider provider) {
        Set<Provider> providers = ROLE_MODEL_PERMISSION.get(role);
        if (!providers.isEmpty()) {
            for (Provider p : providers) {
                if (p.equals(provider))
                    return true;
            }
        }
        return false;
    }
}
