package work.daqian.myai.util;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import work.daqian.myai.common.SecurityUserDetails;

public class SecurityUtils {
    
    /**
     * 获取当前登录用户ID
     * 替代你之前的 UserContext.getUserId()
     */
    public static Long getCurrentUserId() {
        SecurityUserDetails userDetails = getCurrentUser();
        return userDetails != null ? userDetails.getId() : null;
    }
    
    /**
     * 获取当前登录用户名
     */
    public static String getCurrentUsername() {
        SecurityUserDetails userDetails = getCurrentUser();
        return userDetails != null ? userDetails.getUsername() : null;
    }

    /**
     * 获取完整的当前用户信息
     */
    public static SecurityUserDetails getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() 
                && authentication.getPrincipal() instanceof SecurityUserDetails) {
            return (SecurityUserDetails) authentication.getPrincipal();
        }
        return null;
    }
    
    /**
     * 检查是否已登录
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}