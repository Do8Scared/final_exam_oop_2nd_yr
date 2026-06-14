package com.garahe.database;

import com.garahe.models.User;

public interface UserDAO {
    boolean registerUser(User user);
    User authenticateUser(String email, String password);
}
