package work.daqian.myai.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import work.daqian.myai.common.SecurityUserDetails;
import work.daqian.myai.domain.dto.LoginUserDTO;
import work.daqian.myai.util.BeanUtils;
import work.daqian.myai.util.JwtUtil;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = request.getHeader("Authorization");
        if (token != null) {
            LoginUserDTO loginUserDTO = jwtUtil.parseToken(token);
            SecurityUserDetails userDetails = BeanUtils.copyBean(loginUserDTO, SecurityUserDetails.class);
            List<SimpleGrantedAuthority> authorities = loginUserDTO.getRoles()
                    .stream().map(SimpleGrantedAuthority::new).toList();
            userDetails.setAuthorities(authorities);
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        }
        filterChain.doFilter(request, response);
    }
}
