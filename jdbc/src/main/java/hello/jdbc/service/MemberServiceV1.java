package hello.jdbc.service;

import hello.jdbc.domain.Member;
import hello.jdbc.repository.MemberRepositoryV1;
import lombok.RequiredArgsConstructor;

import java.sql.SQLException;

@RequiredArgsConstructor
public class MemberServiceV1 {

    private final MemberRepositoryV1 memberRepository;

    /**
     * A가 B에게 송금하는 상황을 가정하여 트랜잭션 개념이 필요한 상황을 개념적으로 구현한다.
     * @param fromId
     * @param toId
     * @param money
     * @throws SQLException
     */
    public void accountTransfer(String fromId, String toId, int money) throws SQLException {
        Member fromMember = memberRepository.findById(fromId);
        Member toMember = memberRepository.findById(toId);

        // 다음 과정이 하나의 연결된 프로세스로서 동작해야 한다.
        memberRepository.updateById(fromId, fromMember.getMoney() - money);
        validation(toMember);
        memberRepository.updateById(toId, toMember.getMoney() + money);
    }

    /**
     * 계좌 이체 중에 예외가 발생한 상황을 만들기 위해 작성한 메서드
     * toMember 의 ID가 'ex' 인 경우 IllegalStateException 을 일으킨다.
     * @param toMember
     */
    private void validation(Member toMember) {
        if (toMember.getMemberId().equals("ex")) {
            throw new IllegalStateException("이체 중 예외 발생");
        }
    }

}
