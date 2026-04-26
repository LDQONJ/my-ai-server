package work.daqian.myai.util;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import work.daqian.myai.config.ModelPermissionConfig;
import work.daqian.myai.enums.Provider;

import java.util.Collection;

public class SecurityAssert {
    public static void canAccessModel(Provider provider) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) return;
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        String authority = authorities.iterator().next().getAuthority();
        boolean authorized = ModelPermissionConfig.hasPermission(authority, provider);
        if (!authorized) throw new AccessDeniedException("没有使用该模型的权限");
    }
}
