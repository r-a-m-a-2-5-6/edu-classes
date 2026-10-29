package com.inspire.educlasses.service;

import com.inspire.educlasses.entity.CustomUserDetails;
import com.inspire.educlasses.entity.User;
import com.inspire.educlasses.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CustomUserDetailsServiceTests {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userService;

    private User buildUser() {
        User user = new User();
        user.setId(5L);
        user.setName("siva");
        user.setEmail("siva@example.com");
        return user;
    }

    @Test
    void loadUserByUsername_whenUserExists_returnsCustomUserDetails() {
        // Arrange
        User user = buildUser();
        when(userRepository.findByEmail("siva@example.com")).thenReturn(Optional.of(user));

        // Act
        UserDetails result = userService.loadUserByUsername("siva@example.com");

        // Assert
        assertInstanceOf(CustomUserDetails.class, result);
        assertEquals("siva@example.com", result.getUsername());
        verify(userRepository).findByEmail("siva@example.com");
    }


    @Test
    void loadByUsername_whenUserNotFound_throwsException(){
        //Arrange
        when(userRepository.findByEmail("nouser@gmail.com"))
                .thenReturn(Optional.empty());

        //Act + Assert
        UsernameNotFoundException exception =assertThrows(UsernameNotFoundException.class,
                () -> userService.loadUserByUsername("nouser@gmail.com"));

        assertEquals("User not found",exception.getMessage());
    }

    @Test
    void loadByUsername_whenEmptyEmailPassed_thenUserNotFound_throwsException(){
        //Arrange
        when(userRepository.findByEmail(null))
                .thenReturn(Optional.empty());

        //Act + Assert
        assertThrows(UsernameNotFoundException.class,
                () -> userService.loadUserByUsername(null));


    }


}
