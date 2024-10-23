package hello.jdbc.repository.exception;

/**
 * 기존에 사용했던 MyDbException 을 상속받아 의미있는 계층을 형성
 * -> 이러한 계층을 형성하면 '데이터 베이스 관련 예외' 라는 계층을 만들 수 있게 된다
 * -> 직접 작성한 예외이기 때문에 특정 기술에 종속적이지 않다.
 * -> 따라서 이 예외를 사용하더라도 서비스 계층의 순수성을 유지할 수 있게 된다.
 */
public class MyDuplicateKeyException extends MyDbException {
    public MyDuplicateKeyException() {
    }

    public MyDuplicateKeyException(String message) {
        super(message);
    }

    public MyDuplicateKeyException(String message, Throwable cause) {
        super(message, cause);
    }

    public MyDuplicateKeyException(Throwable cause) {
        super(cause);
    }
}
