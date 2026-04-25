package work.daqian.myai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import work.daqian.myai.common.SecurityUserDetails;
import work.daqian.myai.domain.po.User;
import work.daqian.myai.mapper.UserMapper;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) throw new UsernameNotFoundException("用户不存在");

        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(user.getRoles()));
        return new SecurityUserDetails(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                authorities,
                true
        );
    }
}
