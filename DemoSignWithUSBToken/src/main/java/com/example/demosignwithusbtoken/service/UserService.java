package com.example.demosignwithusbtoken.service;

import com.example.demosignwithusbtoken.model.Role;
import com.example.demosignwithusbtoken.model.User;

import java.util.ArrayList;
import java.util.List;

import static java.lang.System.out;

public class UserService {
    private volatile static UserService userService;

    private UserService() {}

    public static UserService getInstance() {
        UserService userService = UserService.userService;
        if (userService == null) {
            synchronized (UserService.class) {
                userService = UserService.userService;
                if (userService == null) {
                    UserService.userService = userService = new UserService();
                }
            }
        }
        return userService;
    }

    public List<User> getUsers() {
        return UserService.getInstance().getUsers();
    }

    public User getUser(String username) {
        if (username.equals("Conghiale")) {
            List<String> roles = new ArrayList<>();
            roles.add(Role.ROLE_CUSTOMER);
            return new User("Conghiale", "52000691", roles);

        } else if (username.equals("Bigboss")) {
            List<String> roles = new ArrayList<>();
            roles.add(Role.ROLE_ADMIN);
            return new User("Bigboss", "28102002", roles);

        }else
            return null;

//        return UserService.getInstance().getUser(username);
    }

    public boolean createUser(User user) {
        return UserService.getInstance().createUser(user);
    }

    public boolean updateUser(User user) {
        return UserService.getInstance().updateUser(user);
    }

    public boolean deleteUser(String id) {
        return UserService.getInstance().deleteUser(id);
    }

    public String getUserName(User user) {
        return UserService.getInstance().getUserName(user);
    }

    public void setUserName(User user, String userName) {
        UserService.getInstance().setUserName(user, userName);
    }

    public String getPassword(User user) {
        return UserService.getInstance().getPassword(user);
    }

    public void setPassword(User user, String password) {
        UserService.getInstance().setPassword(user, password);
    }

    public String getRole(User user) {
        return UserService.getInstance().getRole(user);
    }

    public void setRole(User user, String role) {
        UserService.getInstance().setRole(user, role);
    }
}
