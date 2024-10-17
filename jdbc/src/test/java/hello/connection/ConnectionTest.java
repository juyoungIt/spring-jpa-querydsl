package hello.connection;

import com.zaxxer.hikari.HikariDataSource;
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

    @Test
    @DisplayName("DataSource 를 통해 Connection Pool 의 사용을 추상화하여 Connection 을 획득하는 예시 코드")
    void data_source_connection_pool() throws SQLException, InterruptedException {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(URL);
        dataSource.setUsername(USERNAME);
        dataSource.setPassword(PASSWORD);
        dataSource.setMaximumPoolSize(10); // 다음과 같이 Connection Pool 의 사이즈를 조정할 수 있다.
        dataSource.setPoolName("myTestPool");
        // 생성한 connection pool 을 통한 connection 획득
        useDataSource(dataSource);

        /*
         * Thread.sleep() 메서드를 여기서 사용한 이유?
         * -> Connection Pool 에 Connection 생성하는 작업은 애플리케이션 실행 속도에 영향을 주지 않도록 별도의 쓰레드에서 수행
         * -> 별도의 쓰레드에서 동작하므로 테스트가 먼저 종료되어 버리면서 Connection Pool 이 구성될 충분한 시간을 얻지 못하게 됨
         * -> 아래와 같이 Thread.sleep() 으로 대기 시간을 주어야 쓰레드 풀에 커넥션이 생성되는 로그를 확인할 수 있다.
         */
        Thread.sleep(1000); // 커넥션 풀에서 커넥션 생성 시간 대기
    }

    /**
     * DataSource 를 사용하여 Connection 을 획득하고 이를 로그로 남김
     * @param dataSource
     */
    private void useDataSource(DataSource dataSource) throws SQLException {
        Connection conn1 = dataSource.getConnection();
        Connection conn2 = dataSource.getConnection();
        log.info("By Connection Pool -> conn1 = {}, class = {}", conn1, conn1.getClass());
        log.info("By Connection Pool -> conn2 = {}, class = {}", conn2, conn2.getClass());
    }

}
