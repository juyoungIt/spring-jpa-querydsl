package hello.jdbc.exception.basic.basic;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.ConnectException;
import java.sql.SQLException;

/**
 * CheckedException 을 애플리케이션 개발 시 사용할 경우 발생할 수 있는 문제를 확인하기 위한 예제코드
 * 다음 예제 코드를 통해서 다음 두 가지 문제를 확인할 수 있다.
 * -
 * 1. 복구 불가능한 예외
 *    -> 정말 일부를 제외하면 대부분의 예외는 복구가 불가능하다
 *    -> 대부분의 이러한 문제들은 서비스나 컨트롤러 계층에서 해결할 수 없다
 *    -> 따라서 이러한 문제들은 일관성 있게 공통으로 처리해야할 필요가 있다.
 *    -> 서블릿 필터, 스프링 인터셉터, ControllerAdvice 를 사용하면 이러한 문제들을 공통으로 깔끔하게 처리할 수 있다.
 * 2. 의존 관계 문제
 *    -> 컨트롤러나 서비스 입장에서 처리할 수 없는 예외이기 때문에 의무적으로 throws 를 선언해야 함
 *    -> 그런데 이 과정에서 서비스 계층이 의존하지 않아도 되는 기술적 의존성이 발생할 수 있게 된다. (불필요한 의존성의 발생)
 *    -> 즉, OCP / DI 를 통해 클라이언트 코드 변경 없이 구현체를 변경할 수 있는 장점이 이 의존성 때문에 사라지게 된다.
 */
@Slf4j
public class CheckedAppTest {

    /**
     * Service 를 사용하는 상위 계층 Controller 계층을 재현함
     * -> 체크 예외를 처리하지 못하여 밖으로 던지기 위해 throws 를 사용했다
     */
    static class Controller {
        Service service = new Service();

        // 체크 예외가 발생하므로 다음과 같이 throws 로 명시해야 한다
        public void request() throws SQLException, ConnectException {
            service.logic();
        }
    }

    /**
     * Repository 와 NetworkClient 를 사용하는 Service 계층을 재현함
     * -> 체크 예외를 처리하지 못하여 밖으로 던지기 위해 throws 를 사용했다
     */
    static class Service {
        Repository repository = new Repository();
        NetworkClient networkClient = new NetworkClient();

        // 체크 예외가 발생하므로 다음과 같이 throws 로 명시해야 한다
        public void logic() throws SQLException, ConnectException {
            repository.call();
            networkClient.call();
        }
    }

    /**
     * NetworkClient 에서 체크예외 인 ConnectException 을 발생 시킨다
     */
    static class NetworkClient {
        public void call() throws ConnectException {
            throw new ConnectException("connect failed...");
        }
    }

    /**
     * Repository 에서 체크예외 인 SQLException 을 발생 시킨다
     */
    static class Repository {
        public void call() throws SQLException {
            throw new SQLException("ex");
        }
    }

    @Test
    @DisplayName("Repository, NetworkClient 단에서 발생한 체크 예외가 Controller 까지 도달한다")
    void checked_exception_test() {
        // given // when
        Controller controller = new Controller();

        // then
        Assertions.assertThatThrownBy(controller::request)
                .isInstanceOf(Exception.class);
    }

}
