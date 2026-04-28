package com.nutricook.security;

import com.nutricook.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("security")
public class SecurityExpressionUtils {

  public boolean isOwner(Authentication auth, Long userId) {
    if (auth == null || !auth.isAuthenticated()) return false;
    Object principal = auth.getPrincipal();
    if (!(principal instanceof User user)) return false;
    return user.getId().equals(userId);
  }
}
