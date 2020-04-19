package service;

import common.ServerResponse;
import pojo.User;

public interface UserService {

	public ServerResponse checkUser(User user);

	public ServerResponse retrieveUserByUsername(String username);

	public ServerResponse checkEmail(User user);

	public ServerResponse addUser(User user);

	public ServerResponse updateUserRoleById(User user);

	public ServerResponse deleteUserById(Integer id);

    public ServerResponse checkUsernameIsExist(String name);

	public ServerResponse findUsersByRole(Integer role);

    public ServerResponse updatePasswordById(User newUser);

    public ServerResponse findAllUsersExcludeRole0(int pageNum, int pageSize);

	public ServerResponse checkUserIsExist(String username, String email);

    public ServerResponse findUserQuantity();
}
