package com.inspire.educlasses.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HealthService {

    private final JdbcTemplate jdbcTemplate;

    public String getHealth(){
        String dbStatus = "DOWN";

        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);

            if(Integer.valueOf(1).equals(result)){
                dbStatus = "UP";
            }
        } catch (DataAccessException e) {
            throw new RuntimeException(e);
        }

        return dbStatus;
    }
}
