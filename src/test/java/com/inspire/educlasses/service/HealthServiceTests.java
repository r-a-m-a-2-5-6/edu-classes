package com.inspire.educlasses.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class HealthServiceTests {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private HealthService healthService;

    @Nested
    @DisplayName("getHealth")
    class GetHealth{

        @Test
        void getHealth_whenDBStatusIsUp(){
            //Arrange
            when(jdbcTemplate.queryForObject("SELECT 1", Integer.class))
                    .thenReturn(1);

            //Act
            String result = healthService.getHealth();

            assertEquals("UP",result);
            verify(jdbcTemplate).queryForObject("SELECT 1",Integer.class);
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(ints = {0,-1,2})
        void getHealth_whenDBReturnUnexpectedValue_returnDown(Integer dbResult){
            //Arrange
            when(jdbcTemplate.queryForObject("SELECT 1", Integer.class))
                    .thenReturn(dbResult);

            //Act
            String result = healthService.getHealth();

            assertEquals("DOWN",result);

        }

        @Test
        void getHealth_whenDBIsDown_throwsExceptionWithCause(){

            //Arrange
            DataAccessResourceFailureException dbException =
                    new DataAccessResourceFailureException("The system is temporarily unable to reach required backend services");

            when(jdbcTemplate.queryForObject("SELECT 1", Integer.class))
                    .thenThrow(dbException);

            //Act

            Exception exception = assertThrows(Exception.class,
                    () -> healthService.getHealth());

            assertSame(dbException,exception.getCause(),"Original DB exception should be preserved as the cause");


        }
    }
}
