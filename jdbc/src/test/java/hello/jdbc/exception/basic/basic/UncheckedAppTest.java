package hello.jdbc.exception.basic.basic;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.ConnectException;
import java.sql.SQLException;

/**
 * UncheckedException 을 도입하여 체크 예외 사용 시 발생했던 다음 두 가지 문제를 해결했다
 * -
 * 1. 복구 불가능한 예외
 *    -> 시스템에서 발생하는 예외는 대부분 복구가 불가능한 예외에 해당함
 *    -> 언체크 예외를 사용함으로써 서비스나 컨트롤러가 복구 불가능한 예외에 대하여 신경쓸 필요가 없어짐
 *    -> 물론, 이렇게 복구 불가능한 예외들은 일관성 있는 방법으로 처리하여야 한다.
 * 2. 의존 관계 문제
 *    -> 런타임 예외는 해당 객체가 처리할수 없는 예외라면 무시하면 된다.
 *    -> 덕분에 체크 예외처럼 처리할 수 없는 예외에 강제를 의존할 필요가 없어지게 된다.
 * -
 * 대신 언체크예외는 문서화를 잘 해둬야 한다.
 * 또는 코드에 throws 언체크예외 를 남겨서 중요한 예외임을 다른 개발자들이 인지하도록 하는 것도 방법이다
 */
@Slf4j
public class UncheckedAppTest {

    /**
     * Service 를 사용하는 상위 계층 Controller 계층을 재현함
     * -> 언체크 예외에 대해서는 throws 를 필수로 선언하지 않아도 된다
     */
    static class Controller {
        Service service = new Service();

        // 체크 예외가 발생하므로 다음과 같이 throws 로 명시해야 한다
        public void request() {
            service.logic();
        }
    }

    /**
     * Repository 와 NetworkClient 를 사용하는 Service 계층을 재현함
     * -> 언체크 예외에 대해서는 throws 를 필수로 선언하지 않아도 된다
     */
    static class Service {
        Repository repository = new Repository();
        NetworkClient networkClient = new NetworkClient();

        // 체크 예외가 발생하므로 다음과 같이 throws 로 명시해야 한다
        public void logic() {
            repository.call();
            networkClient.call();
        }
    }

    /**
     * NetworkClient 에서 언체크예외 인 RuntimeConnectException 을 발생 시킨다
     */
    static class NetworkClient {
        public void call() {
            try {
                connect();
            } catch (ConnectException e) {
                // 체크 예외를 언체크 예외로 변환함
                throw new RuntimeConnectionException(e);
            }
        }

        private void connect() throws ConnectException {
            throw new ConnectException("connect failed...");
        }
    }

    /**
     * Repository 에서 언체크예외 인 RuntimeSQLException 을 발생 시킨다
     */
    static class Repository {
        public void call() {
            try {
                runSQL();
            } catch (SQLException e) {
                // 체크 예외를 언체크 예외로 변환함
                throw new RuntimeSQLException(e);
            }
        }

        private void runSQL() throws SQLException {
            throw new SQLException("ex");
        }
    }

    static class RuntimeConnectionException extends RuntimeException {
        public RuntimeConnectionException() {
        }

        public RuntimeConnectionException(String message) {
            super(message);
        }

        public RuntimeConnectionException(Throwable cause) {
            super(cause);
        }
    }

    static class RuntimeSQLException extends RuntimeException {
        public RuntimeSQLException() {
        }

        public RuntimeSQLException(String message) {
            super(message);
        }

        public RuntimeSQLException(Throwable cause) {
            super(cause);
        }
    }

    @Test
    @DisplayName("Repository, NetworkClient 단에서 발생한 언체크 예외가 Controller 까지 도달한다")
    void unchecked_exception_test() {
        // given // when
        Controller controller = new Controller();

        // then
        Assertions.assertThatThrownBy(controller::request)
                .isInstanceOf(Exception.class);
    }

}
