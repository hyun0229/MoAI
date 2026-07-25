package com.foureyes.moai.backend.domain.ai.repository;

import com.foureyes.moai.backend.domain.ai.dto.query.SidebarRow;
import com.foureyes.moai.backend.domain.ai.entity.AiSummaryDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AiSummaryDocumentRepository extends JpaRepository<AiSummaryDocument, Integer> {
    boolean existsBySummary_IdAndDocument_Id(int summaryId, int documentId);
    long deleteBySummary_Id(int summaryId);
    long deleteByDocument_Id(int documentId);

    @Query("""
        select new com.foureyes.moai.backend.domain.ai.dto.query.SidebarRow(
            s.id, s.title, s.description, s.modelType, s.promptType, s.createdAt,
            sg.id, sg.name, sg.imageUrl
        )
        from AiSummary s
        join AiSummaryDocument sd on sd.summary.id = s.id
        join Document d on d.id = sd.document.id
        join StudyGroup sg on sg.id = d.studyGroup.id
        where s.owner.id = :ownerId
        order by s.createdAt desc
    """)
    List<SidebarRow> findSidebarRows(@Param("ownerId") int ownerId);

    List<AiSummaryDocument> findBySummary_Id(int summaryId);

}