package hello.jdbc.repository;

import hello.jdbc.domain.Member;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import javax.sql.DataSource;
import java.util.List;

/**
 * JdbcTemplate 적용
 * -
 * Repository 계층에서 JDBC 를 사용할 때 다음 패턴이 반복된다
 * 1. 커넥션 조회, 커넥션 동기화
 * 2. PreparedStatement 생성 및 파라미터 바인딩
 * 3. 쿼리 실행
 * 4. 결과 바인딩
 * 5. 예외 발생 시 스프링 예외 변환기 실행
 * 6. 리소스 종료
 * -
 * JdbcTemplate 을 사용하면 JDBC 로 개발 시 발생하는 반복을 대부분 해결해준다.
 * 또한 트랜잭션을 위한 커넥션 동기화 및 Spring 예외 변환기도 자동으로 실행해준다.
 * -> 결과적으로 매우 깔끔한 코드를 작성할 수 있다.
 */
@Slf4j
public class MemberRepositoryV5 implements MemberRepository {

    private final JdbcTemplate jdbcTemplate;

    public MemberRepositoryV5(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public Member save(Member member) {
        String sql = "insert into member(member_id, money) values (?, ?)";
        jdbcTemplate.update(sql, member.getMemberId(), member.getMoney());
        return member;
    }

    @Override
    public Member findById(String memberId) {
        String sql = "select * from member where member_id = ?";
        return jdbcTemplate.queryForObject(sql, memberRowMapper(), memberId);
    }

    @Override
    public List<Member> findAll() {
        String sql = "select * from member";
        return jdbcTemplate.query(sql, memberRowMapper());
    }

    @Override
    public int updateById(String memberId, int money) {
        String sql = "update member set money = ? where member_id = ?";
        return jdbcTemplate.update(sql, money, memberId);
    }

    @Override
    public int deleteById(String memberId) {
        String sql = "delete from member where member_id = ?";
        return jdbcTemplate.update(sql, memberId);
    }

    @Override
    public int clearAll() {
        String sql = "delete from member";
        return jdbcTemplate.update(sql);
    }

    private RowMapper<Member> memberRowMapper() {
        return (rs, rowNum) -> {
            Member member = new Member();
            member.setMemberId(rs.getString("member_id"));
            member.setMoney(rs.getInt("money"));
            return member;
        };
    }

}
