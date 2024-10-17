package hello.jdbc.repository;

import com.zaxxer.hikari.HikariDataSource;
import hello.jdbc.domain.Member;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;
import java.util.NoSuchElementException;

import static hello.connection.ConnectionConst.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Slf4j
class MemberRepositoryV1Test {

    MemberRepositoryV1 memberRepository;

    @BeforeEach
    void init_data_source() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(URL);
        dataSource.setUsername(USERNAME);
        dataSource.setPassword(PASSWORD);
        dataSource.setMaximumPoolSize(10);
        dataSource.setPoolName("myTestPool");
        // Repository 수준에서는 DataSource 에 의존하므로 구현체를 손쉽게 교체할 수 있다.
        memberRepository = new MemberRepositoryV1(dataSource);
    }

    @AfterEach
    void clear_all_for_test() throws SQLException {
        int cleared = memberRepository.clearAll();
        log.info("{} row cleared... for next test", cleared);
    }

    @Test
    @DisplayName("테스트의 반복을 위해 현재 member table 에 저장된 모든 데이터를 삭제한다")
    void clear_all_test() throws SQLException {
        // given
        Member memberA = new Member("memberA", 10_000);
        Member memberB = new Member("memberB", 20_000);
        Member memberC = new Member("memberC", 30_000);
        memberRepository.save(memberA);
        memberRepository.save(memberB);
        memberRepository.save(memberC);

        // when
        memberRepository.clearAll();

        // then
        int rowCount = memberRepository.findAll().size();
        assertThat(rowCount).isZero();
    }

    @Test
    @DisplayName("회원정보 저장 - member_id, money 를 전달하여 회원정보를 저장한다")
    void save_success_test() throws SQLException {
        // given
        Member member = new Member("memberA", 10_000);
        Member savedMember = memberRepository.save(member);

        // when // then
        Member findMember = memberRepository.findById("memberA");
        assertThat(savedMember).isEqualTo(findMember);
    }

    @Test
    @DisplayName("회원정보 조회 - 현재 member table 에 저장된 모든 회원정보를 조회한다")
    void find_all_test() throws SQLException {
        // given
        Member memberA = new Member("memberA", 10_000);
        Member memberB = new Member("memberB", 20_000);
        Member memberC = new Member("memberC", 30_000);
        Member savedMemberA = memberRepository.save(memberA);
        Member savedMemberB = memberRepository.save(memberB);
        Member savedMemberC = memberRepository.save(memberC);

        // when
        List<Member> members = memberRepository.findAll();

        // then
        assertThat(members).hasSize(3);
        assertThat(members).containsExactly(savedMemberA, savedMemberB, savedMemberC);
    }

    @Test
    @DisplayName("회원정보 조회 - 특정 memberId 를 가진 회원을 조회한다")
    void find_by_id_success_test() throws SQLException {
        // given
        Member member = new Member("memberA", 10_000);
        Member savedMember = memberRepository.save(member);

        // when
        Member findMember = memberRepository.findById("memberA");

        // then
        assertThat(savedMember).isEqualTo(findMember);
    }

    @Test
    @DisplayName("회원정보 조회 - 특정 memberId 를 가진 회원이 없는 경우 예외를 발생시킨다")
    void find_by_id_fail_test() throws SQLException {
        // given
        Member member = new Member("memberA", 10_000);
        Member savedMember = memberRepository.save(member);

        // when // then
        assertThatThrownBy(() -> memberRepository.findById("memberB"))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("memberId = memberB does not exist...");
    }

    @Test
    @DisplayName("회원정보 수정 - 특정 memberId 를 가진 회원의 money 를 수정한다")
    void update_by_id() throws SQLException {
        // given
        Member member = new Member("memberA", 10_000);
        Member savedMember = memberRepository.save(member);

        // when
        int updated = memberRepository.updateById("memberA", 20_000);

        // then
        Member findMember = memberRepository.findById("memberA");
        assertThat(updated).isOne();
        assertThat(findMember.getMoney()).isEqualTo(20_000);
    }

    @Test
    @DisplayName("회원정보 삭제 - 특정 memberId 를 가진 회원을 삭제 한다")
    void delete_by_id() throws SQLException {
        // given
        // given
        Member memberA = new Member("memberA", 10_000);
        Member memberB = new Member("memberB", 20_000);
        Member memberC = new Member("memberC", 30_000);
        Member savedMemberA = memberRepository.save(memberA);
        Member savedMemberB = memberRepository.save(memberB);
        Member savedMemberC = memberRepository.save(memberC);

        // when
        int deleted = memberRepository.deleteById("memberC");

        // then
        assertThat(deleted).isOne();
        List<Member> members = memberRepository.findAll();
        assertThat(members)
                .hasSize(2)
                .containsExactly(savedMemberA, savedMemberB);
        assertThatThrownBy(() -> memberRepository.findById("memberC"))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("memberId = memberC does not exist...");
    }

}