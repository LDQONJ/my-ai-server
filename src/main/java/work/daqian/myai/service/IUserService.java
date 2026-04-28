package work.daqian.myai.service;

import com.baomidou.mybatisplus.extension.service.IService;
import work.daqian.myai.common.PageDTO;
import work.daqian.myai.common.R;
import work.daqian.myai.domain.dto.UserDTO;
import work.daqian.myai.domain.dto.UserPageQuery;
import work.daqian.myai.domain.po.User;
import work.daqian.myai.domain.vo.UserVO;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 李达千
 * @since 2026-04-17
 */
public interface IUserService extends IService<User> {

    R<String> register(UserDTO registerForm);

    R<String> me();

    R<Void> updateUserInfo(UserDTO updateForm);

    R<PageDTO<UserVO>> queryUserPage(UserPageQuery pageQuery);
}
