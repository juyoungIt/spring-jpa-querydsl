package hello.jdbc.exception.translator;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;
import org.springframework.jdbc.support.SQLExceptionTranslator;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static hello.connection.ConnectionConst.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Spring 은 데이터 접근 계층에 대한 일관된 추상화를 제공
 * -> 예외 변환기를 통해 SQLException 의 ErrorCode 에 매핑되는 적절한 Spring 데이터 접근 예외로 변환
 * -> 따라서 서비스나 컨트롤러 계층에서 예외처리가 필요한 경우 스프링이 제공하는 데이터 접근 예외를 사용하면 됨
 * -> Spring 의 예외 추상화를 통해 데이터 접근 기술의 변경이 발생해도 서비스 계층은 영향을 받지 않게 됨
 * -> 물론 Spring 이 제공하는 것이므로 Spring Framework 에 대한 종속성을 발생한다.
 */
@Slf4j
public class SpringExceptionTranslatorTest {

    DataSource dataSource;

    @BeforeEach
    void init() {
        dataSource = new DriverManagerDataSource(URL, USERNAME, PASSWORD);
    }

    /**
     * 하지만 매번 이렇게 각 오류코드마다 대응해서 Spring 에서 지원하는 예외로 매핑하는 것은 현실적으로 불가능하다.
     * -> 이를 위해서 Spring 은 예외 변환기를 제공한다.
     */
    @Test
    @DisplayName("e.getErrorCode() 를 통해 SQLException 에 대한 구체적인 예외코드를 확인한다")
    void sql_exception_error_code_test() {
        String sql = "select bad syntax";

        Connection conn = null;
        Statement stmt = null;
        try {
            conn = dataSource.getConnection();
            stmt = conn.createStatement();
            stmt.executeQuery(sql);
        } catch (SQLException e) {
            assertThat(e.getErrorCode()).isEqualTo(42122);
            int errorCode = e.getErrorCode();
            log.info("error code = {}", errorCode);
            log.error("error", e);
        } finally {
            JdbcUtils.closeStatement(stmt);
            JdbcUtils.closeConnection(conn);
        }
    }

    @Test
    @DisplayName("Spring 에서 제공하는 예외 변환기를 통해 데이터 접근 계층에서 발생하는 오류를 일관되게 처리한다")
    void exception_translator_test() {
        String sql = "select bad syntax";

        Connection conn = null;
        Statement stmt = null;
        try {
            conn = dataSource.getConnection();
            stmt = conn.createStatement();
            stmt.executeQuery(sql);
        } catch (SQLException e) {
            assertThat(e.getErrorCode()).isEqualTo(42122);
            // org.springframework.jdbc.support.sql-error-codes.xml -> 해당 파일에서 에러코드를 매핑한다

            // Spring 에서 제공하는 예외 변환기를 사용한다
            SQLExceptionTranslator exTranslator = new SQLErrorCodeSQLExceptionTranslator(dataSource);
            // Spring 이 제공하는 데이터 접근 예외 중 적절한 것을 매핑하여 반환한다.
            // 1. param1 - 읽을 수 있는 설명
            // 2. param2 - 실행한 SQL
            // 3. param3 - 발생한 Exception
            DataAccessException resultEx = exTranslator.translate("select", sql, e);
            log.info("resultEx", resultEx);
            assertThat(resultEx.getClass()).isEqualTo(BadSqlGrammarException.class);
        } finally {
            JdbcUtils.closeStatement(stmt);
            JdbcUtils.closeConnection(conn);
        }
    }

}
