package hello.connection;

import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DriverManager
 * -> JDBC API 에 포함된 클래스
 * 주요 기능
 * 1. JDBC 드라이버를 자동으로 로드 및 관리
 * 2. .getConnection() 메서드를 통해 DB와 연결
 * 3. 드라이버 관리 -> 여러 데이터베이스 드라이버를 관리할 수 있으며, url 패턴과 일치하는 드라이버를 찾는 방식으로 매핑
 */
@Slf4j
public class DBConnectionUtil {

    public static Connection getConnection(String url, String username, String password) {
        try {
            Connection conn = DriverManager.getConnection(url, username, password);
            log.info("connection = {}, class = {}", conn, conn.getClass());
            return conn;
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

}
