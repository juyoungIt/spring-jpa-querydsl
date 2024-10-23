package hello.jdbc.exception.basic;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 언체크 예외 (UncheckedException)
 * -> 언체크 예외의 경우 해당 예외를 처리할 수 없다면 예외를 밖으로 던지는 throws 예외를 생략할 수 있다.
 * -> 이러한 특징으로 인해 다음과 같은 장단점이 발생할 수 있다.
 * -
 * 장점 : 신경쓰고 싶지 않은 언체크 예외를 무시할 수 있으며 이로 인해 신경쓰고 싶지 않은 예외로 인해 발생하는 종속성 문제가 사라진다.
 * 단점 : 언체크 예외는 체크 예외와 달리 컴파일러의 도움을 받지 못하므로 개발자가 실수로 예외 처리를 누락하게 될 가능성이 존재한다.
 */
@Slf4j
public class UncheckedTest {

    /**
     * RuntimeException 을 상속받은 예외는 언체크 예외가 된다.
     */
    static class MyUncheckedException extends RuntimeException {
        public MyUncheckedException(String message) {
            super(message);
        }
    }

    /**
     * 언체크 예외의 경우 예외를 잡아서 처리하거 던지지 않아도 된다.
     * 예외를 잡지 않으면 자동으로 예외를 던지게 된다.
     */
    static class Service {
        Repository repository = new Repository();

        /**
         * 필요한 경우 예외를 잡아서 처리할 수 있다.
         */
        public void callCatch() {
            try {
                repository.call();
            } catch (MyUncheckedException e) {
                // 예외 처리 로직이 들어간다고 가정
                // 아래와 같이 로그의 마지막 인수로 예외 객체를 넘겨주면 로그가 해당 예외의 stacktrace 를 추가로 남긴다
                log.info("예외 처리 로직 동작 = {}", e.getMessage(), e);
            }
        }

        /**
         * 예외를 처리하지 않아도 문제가 발생하지 않으며, 이 경우 자연스럽게 상위계층으로 예외가 전파된다.
         * 체크 예외와 다르게 throws 선언을 하지 않아도 문제가 발생하지 않는다.
         * -> 만약 명시적으로 하고 싶다면 언체크 예외도 throws 선언을 해도 된다.
         */
        public void callThrow() {
            repository.call();
        }
    }

    /**
     * Repository Layer 에서 체크 예외가 발생함을 가정하기 위해 작성한 코드
     */
    static class Repository {
        public void call() {
            throw new MyUncheckedException("ex");
        }
    }

    @Test
    @DisplayName("UncheckedException - 하위 계층에서 예외를 잡아서 처리하면 이를 호출했던 상위 계층은 정상흐름으로 처리된다.")
    void checked_catch_test() {
        Service service = new Service();
        service.callCatch();
    }

    @Test
    @DisplayName("UncheckedException - 하위 계층에서 발생한 예외를 외부로 던지면 이를 호출했던 상위 계층으로 예외가 넘어온다.")
    void checked_throw_test() {
        Service service = new Service();
        assertThatThrownBy(service::callThrow)
                .isInstanceOf(MyUncheckedException.class)
                .hasMessage("ex");
    }

}
