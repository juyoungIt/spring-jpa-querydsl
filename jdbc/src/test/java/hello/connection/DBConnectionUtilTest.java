package hello.connection;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;

import static hello.connection.ConnectionConst.*;
import static org.assertj.core.api.Assertions.*;


@Slf4j
class DBConnectionUtilTest {

    @Test
    @DisplayName("DriverManager.getConnection() 에 URL, USERNAME, PASSWORD 를 전달하여 Connection 을 얻어온다")
    void get_connection_success() {
        Connection conn = DBConnectionUtil.getConnection(URL, USERNAME, PASSWORD);
        assertThat(conn).isNotNull().isInstanceOf(Connection.class);
    }

    @Test
    @DisplayName("DriverManager.getConnection() 에 전달한 URL, USERNAME, PASSWORD 가 올바르지 않은 경우 예외를 반환한다")
    void get_connection_failed() {
        // 의도적으로 틀린 비밀번호를 사용하여 실패하도록 처리
        assertThatThrownBy(() -> DBConnectionUtil.getConnection(URL, USERNAME, PASSWORD + "?"))
                .isInstanceOf(IllegalStateException.class);

    }

}