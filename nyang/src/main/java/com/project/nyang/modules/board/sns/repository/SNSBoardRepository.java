package com.project.nyang.modules.board.sns.repository;

import com.project.nyang.modules.board.entity.Board;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * sns board repository
 *
 * @author : 이은서
 * @fileName : BoardRepository
 * @since : 25. 7. 9.
 */
public interface SNSBoardRepository extends JpaRepository<Board,Long> {
    /* categoryId 가 3 이고 deletedAt 이 null 인친구들
    * 상세조회 */
    // 프로젝트 종료 후 리팩터링: 작성자(user)를 목록 쿼리에 함께 가져와 게시글마다 사용자 조회가 나가는 N+1을 막음
    @EntityGraph(attributePaths = {"user"})
    Page<Board> findByCategory_CategoryIdAndDeletedAtIsNull(Long categoryId, Pageable pageable);
    // 전체조회
    List<Board> findByCategory_CategoryIdAndDeletedAtIsNull(Long categoryId);

    @Query("""
    SELECT b FROM Board b
    WHERE b.category.categoryId = :categoryId
      AND b.deletedAt IS NULL
      AND (
            b.boardTitle LIKE %:keyword%
         OR b.boardContent LIKE %:keyword%
      )
""")/* 검색 */
    // 프로젝트 종료 후 리팩터링: 작성자를 함께 조회. 이미지(컬렉션)는 페이징과 같이 fetch join하면
    // 메모리에서 페이징(HHH90003004)되므로 default_batch_fetch_size로 IN 쿼리 한 번에 묶는다
    @EntityGraph(attributePaths = {"user"})
    Page<Board> searchKeywordSNS(
            @Param("categoryId") Long categoryId,
            @Param("keyword") String keyword,
            Pageable pageable
    );


}
