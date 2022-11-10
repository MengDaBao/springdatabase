package com.yby;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@EnableConfigurationProperties(DataBasePropeties.class)
public class DataBaseConfiguration {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Bean
    public ExecuteSql initExecuteSql(DataBasePropeties ybyDataBasePropeties){
        ExecuteSql executeSql = new ExecuteSql();
        executeSql.setPath(ybyDataBasePropeties.getPath());
        executeSql.setFileName(ybyDataBasePropeties.getFileName());
        executeSql.setJdbcTemplate(jdbcTemplate);
        return executeSql;
    }
}
