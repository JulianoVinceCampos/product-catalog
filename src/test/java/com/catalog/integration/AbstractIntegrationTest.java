package com.catalog.integration;

import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("integration-test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractIntegrationTest {

    @Autowired
    protected TestRestTemplate restTemplate;

    static final MySQLContainer<?> MYSQL =
        new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("catalog_it").withUsername("it_user").withPassword("it_pass").withReuse(true);

    @SuppressWarnings("rawtypes")
    static final GenericContainer REDIS =
        new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .waitingFor(Wait.forLogMessage(".*Ready to accept connections.*", 1))
            .withReuse(true);

    static final ElasticsearchContainer ELASTICSEARCH =
        new ElasticsearchContainer(DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:8.13.0"))
            .withEnv("xpack.security.enabled", "false")
            .withEnv("discovery.type", "single-node")
            .withEnv("ES_JAVA_OPTS", "-Xms256m -Xmx256m")
            .withStartupTimeout(Duration.ofMinutes(2))
            .withReuse(true);

    static {
        MYSQL.start();
        REDIS.start();
        ELASTICSEARCH.start();
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",      MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username",  MYSQL::getUsername);
        registry.add("spring.datasource.password",  MYSQL::getPassword);
        registry.add("spring.data.redis.host",  REDIS::getHost);
        registry.add("spring.data.redis.port",  () -> REDIS.getMappedPort(6379));
        registry.add("spring.elasticsearch.uris", () -> "http://" + ELASTICSEARCH.getHttpHostAddress());
    }
}
