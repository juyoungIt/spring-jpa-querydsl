package hello.connection;

/**
 * 단순히 DB 커넥션을 얻어오는 데 필요한 URL, USERNAME, PASSWORD 상수 값을 저장하는 클래스
 * -> 해당 클래스로 인스턴스를 생성할 수 없도록 abstract 로 선언한다
 */
public abstract class ConnectionConst {
    public static final String URL = "jdbc:h2:tcp://localhost/~/test";
    public static final String USERNAME = "sa";
    public static final String PASSWORD = "";
}
