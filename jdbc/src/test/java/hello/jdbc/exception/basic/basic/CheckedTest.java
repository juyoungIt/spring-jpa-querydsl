package hello.jdbc.exception.basic.basic;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 체크 예외 (CheckedException)
 * -> 체크 예외의 경우 해당 예외를 처리할 수 없다면 반드시 명시적으로 throws 예외를 선언해야 한다.
 * -> 그렇지 않는다면 컴파일 단계에서 오류가 발생하게 되는 데 이 때문에 다음 장단점이 존재한다.
 * -
 * 장점 : 예외 처리를 누락하지 않도록 컴파일러 수준에서 체크할 수 있으므로 훌륭한 안전장치의 개념이 된다.
 * 단점 : 모든 체크 예외를 처리해줘야 한다는 번거로움이 발생하며, throws 로 인해 서비스 계층이 특정 기술에 종속될 수 있다.
 */
@Slf4j
public class CheckedTest {

    /**
     * Exception 을 상속받은 예외는 체크 예외가 된다.
     */
    static class MyCheckedException extends Exception {
        public MyCheckedException(String message) {
            super(message);
        }
    }

    /**
     * Checked 예외의 경우 현재 위치에서 직접 처리하거나, 외부로 던져야 한다. -> 즉, 반드시 처리해야 한다.
     */
    static class Service {
        Repository repository = new Repository();

        /**
         * 예외를 잡아서 직접 처리하는 코드
         * -> 예외를 잡아서 직접 처리하므로 외부 계층은 정상흐름이 된다.
         */
        public void callCatch() {
            try {
                repository.call();
            } catch (MyCheckedException e) {
                // 예외 처리 로직이 들어간다고 가정
                // 아래와 같이 로그의 마지막 인수로 예외 객체를 넘겨주면 로그가 해당 예외의 stacktrace 를 추가로 남긴다
                log.info("예외 처리 로직 동작 = {}", e.getMessage(), e);
            }
        }

        /**
         * 체크 예외를 밖으로 던지는 코드
         * -> 체크 예외를 잡지 않고 밖으로 던지려면 throws 를 메서드에 반드시 선언해야 하며, 이는 컴파일 단계에서 체크 된다.
         * -> 체크 예외의 처리를 외부 계층으로 위임한다.
         */
        public void callThrow() throws MyCheckedException {
            repository.call();
        }
    }

    /**
     * Repository Layer 에서 체크 예외가 발생함을 가정하기 위해 작성한 코드
     */
    static class Repository {
        public void call() throws MyCheckedException {
            throw new MyCheckedException("ex");
        }
    }

    @Test
    @DisplayName("CheckedException - 하위 계층에서 예외를 잡아서 처리하면 이를 호출했던 상위 계층은 정상흐름으로 처리된다.")
    void checked_catch_test() {
        Service service = new Service();
        service.callCatch();
    }

    @Test
    @DisplayName("CheckedException - 하위 계층에서 발생한 예외를 외부로 던지면 이를 호출했던 상위 계층으로 예외가 넘어온다.")
    void checked_throw_test() {
        Service service = new Service();
        assertThatThrownBy(service::callThrow)
                .isInstanceOf(MyCheckedException.class)
                .hasMessage("ex");
    }

}
