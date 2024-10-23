package hello.jdbc.repository;

import hello.jdbc.domain.Member;

import java.util.List;

/**
 * 런타임 예외를 사용하게 되면서 인터페이스가 특정 기술에 의존하지 않는 순수한 인터페이스가 된다.
 * -> 만약 체크 예외를 사용한다면 이미 인터페이스 수준에서 throws 선언을 추가해줘야 한다.
 * -> 즉 이는 인터페이스가 특정 기술에 종속될 여지를 주기 때문에 인터페이스가 가지는 의미가 사라지게 된다.
 */
public interface MemberRepository {
    Member save(Member member);
    Member findById(String memberId);
    int updateById(String memberId, int money);
    int deleteById(String memberId);
    List<Member> findAll();
    int clearAll();
}
