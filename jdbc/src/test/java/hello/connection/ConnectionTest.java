package hello.connection;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static hello.connection.ConnectionConst.*;

/**
 * DataSource
 * -> Connection 을 추상화하기 위해서 만들어진 Java 인터페이스
 * -> .getConnection() 메서드를 통해 Connection 을 획득할 수 있다
 * -> DBCP2, HikariCP 와 같은 대부분의 커넥션 풀들은 대부분 이 인터페이스를 구현해두었다.
 * -> DriverManager 의 경우 DataSource 인터페이스를 사용하지 않는 데 Spring 에서 이를 위해 DriverManagerDataSource 클래스 를 제공한다.
 */
@Slf4j
public class ConnectionTest {

    @Test
    @DisplayName("DriverManager 를 통해 Connection 을 획득하는 예시 코드")
    void driver_manager_test() throws SQLException {
        Connection conn1 = DriverManager.getConnection(URL, USERNAME, PASSWORD);
        Connection conn2 = DriverManager.getConnection(URL, USERNAME, PASSWORD);
        log.info("By DriverManager -> conn1 = {}, class = {}", conn1, conn1.getClass());
        log.info("By DriverManager -> conn2 = {}, class = {}", conn2, conn2.getClass());
    }

    @Test
    @DisplayName("DataSourceDriverManager 를 통해 Connection 을 획득하는 예시 코드")
    void data_source_driver_manager_test() throws SQLException {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(URL, USERNAME, PASSWORD);
        useDataSource(dataSource);
    }

    /**
     * DataSource 를 사용하여 Connection 을 획득하고 이를 로그로 남김
     * @param dataSource
     */
    private void useDataSource(DataSource dataSource) throws SQLException {
        Connection conn1 = dataSource.getConnection();
        Connection conn2 = dataSource.getConnection();
        log.info("By DriverManagerDataSource -> conn1 = {}, class = {}", conn1, conn1.getClass());
        log.info("By DriverManagerDataSource -> conn2 = {}, class = {}", conn2, conn2.getClass());
    }

}
