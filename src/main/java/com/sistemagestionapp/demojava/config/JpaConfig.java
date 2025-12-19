package com.sistemagestionapp.demojava.config;

package com.sistemagestionapp.demojava.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@Profile("!mongo")
@EnableJpaRepositories(
        basePackages = "com.sistemagestionapp.demojava.repository",
        entityManagerFactoryRef = "entityManagerFactory",
        transactionManagerRef = "transactionManager"
)
public class JpaConfig {

    // 👉 Usamos tus variables DB_* (con defaults)
    @Value("${DB_ENGINE:mysql}")
    private String engine;

    @Value("${DB_HOST:localhost}")
    private String host;

    @Value("${DB_PORT:3306}")
    private int port;

    @Value("${DB_NAME:demo}")
    private String dbName;

    @Value("${DB_USER:demo}")
    private String user;

    @Value("${DB_PASSWORD:demo}")
    private String password;

    // ddl-auto se queda en properties por perfil (mysql/postgres)
    @Value("${spring.jpa.hibernate.ddl-auto:create}")
    private String ddlAuto;

    @Value("${spring.jpa.show-sql:false}")
    private boolean showSql;

    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();

        String normalized = (engine == null ? "mysql" : engine.trim().toLowerCase());

        final String url;
        final String driverClassName;

        if ("postgres".equals(normalized) || "postgresql".equals(normalized)) {
            url = String.format("jdbc:postgresql://%s:%d/%s", host, port, dbName);
            driverClassName = "org.postgresql.Driver";
        } else {
            // default: mysql
            url = String.format("jdbc:mysql://%s:%d/%s", host, port, dbName);
            driverClassName = "com.mysql.cj.jdbc.Driver";
        }

        config.setJdbcUrl(url);
        config.setDriverClassName(driverClassName);
        config.setUsername(user);
        config.setPassword(password);

        // Ajustes recomendables
        config.setMaximumPoolSize(10);
        config.setPoolName("demo-hikari");

        return new HikariDataSource(config);
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();

        emf.setDataSource(dataSource);
        emf.setPackagesToScan("com.sistemagestionapp.demojava.model");
        emf.setPersistenceUnitName("default");

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        vendorAdapter.setShowSql(showSql);
        emf.setJpaVendorAdapter(vendorAdapter);

        Map<String, Object> props = new HashMap<>();
        props.put("hibernate.hbm2ddl.auto", ddlAuto);

        // Si quieres dejar que Hibernate detecte el dialect automáticamente,
        // puedes borrar este bloque entero.
        String normalized = (engine == null ? "mysql" : engine.trim().toLowerCase());
        if ("postgres".equals(normalized) || "postgresql".equals(normalized)) {
            props.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        } else {
            props.put("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        }

        // Opcional pero útil
        props.put("hibernate.jdbc.time_zone", "UTC");

        emf.setJpaPropertyMap(props);
        return emf;
    }

    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}