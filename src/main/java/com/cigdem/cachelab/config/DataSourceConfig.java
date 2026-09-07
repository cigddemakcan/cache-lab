package com.cigdem.cachelab.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Bean(destroyMethod = "close")
    public DataSource cacheLabDataSource(
            @Value("${cachelab.datasource.url}") String url,
            @Value("${cachelab.datasource.username}") String username,
            @Value("${cachelab.datasource.password}") String password
    ) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setPoolName("CacheLabPool");
        return dataSource;
    }
}
