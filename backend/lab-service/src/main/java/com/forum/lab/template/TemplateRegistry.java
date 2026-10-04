package com.forum.lab.template;

import com.forum.lab.document.LabColumnDef;
import com.forum.lab.utils.Constants;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class TemplateRegistry {

    private final Map<String, TemplateDefinition> templates = new HashMap<>();

    @PostConstruct
    public void init() {
        registerLuckyWheel();
        registerFlashcard();
        registerMillionaire();
    }

    public Optional<TemplateDefinition> getTemplate(String templateType) {
        return Optional.ofNullable(templates.get(templateType));
    }

    public List<TemplateDefinition> getAllTemplates() {
        return new ArrayList<>(templates.values());
    }

    private void registerLuckyWheel() {
        List<LabColumnDef> columns = Arrays.asList(
                LabColumnDef.builder().code("label").name("Tên ô / Phần thưởng").type(Constants.COLUMN_TYPE_TEXT).align(Constants.COLUMN_ALIGN_CENTER).required(true).build(),
                LabColumnDef.builder().code("color").name("Màu nền").type(Constants.COLUMN_TYPE_TEXT).align(Constants.COLUMN_ALIGN_CENTER).required(false).build(),
                LabColumnDef.builder().code("weight").name("Tỷ lệ trúng").type(Constants.COLUMN_TYPE_NUMBER).align(Constants.COLUMN_ALIGN_CENTER).required(false).build()
        );

        Map<String, Object> spec = new HashMap<>();
        spec.put("durationSeconds", 6);
        spec.put("spinRotations", 8);
        spec.put("allowDuplicateWin", true);

        List<Map<String, Object>> mockRows = new ArrayList<>();
        mockRows.add(Map.of("label", "Giải Nhất 🌟", "color", "#f59e0b", "weight", 1));
        mockRows.add(Map.of("label", "Chúc may mắn lần sau 🍀", "color", "#64748b", "weight", 5));
        mockRows.add(Map.of("label", "Giải Nhì 🥈", "color", "#3b82f6", "weight", 2));
        mockRows.add(Map.of("label", "Thêm 1 lượt quay 🎁", "color", "#10b981", "weight", 3));
        mockRows.add(Map.of("label", "Giải Ba 🥉", "color", "#ec4899", "weight", 3));
        mockRows.add(Map.of("label", "Quà bí mật 📦", "color", "#8b5cf6", "weight", 2));

        templates.put(Constants.TEMPLATE_LUCKY_WHEEL, TemplateDefinition.builder()
                .templateType(Constants.TEMPLATE_LUCKY_WHEEL)
                .name("Vòng quay may mắn")
                .description("Vòng quay số ngẫu nhiên cho lớp học, bốc thăm sự kiện và trò chơi minigame")
                .icon("bi-pie-chart-fill")
                .defaultColumns(columns)
                .defaultSpec(spec)
                .mockRows(mockRows)
                .build());
    }

    private void registerFlashcard() {
        List<LabColumnDef> columns = Arrays.asList(
                LabColumnDef.builder().code("front").name("Mặt trước (Câu hỏi / Khái niệm)").type(Constants.COLUMN_TYPE_TEXT).align(Constants.COLUMN_ALIGN_LEFT).required(true).build(),
                LabColumnDef.builder().code("back").name("Mặt sau (Đáp án / Ý nghĩa)").type(Constants.COLUMN_TYPE_TEXT).align(Constants.COLUMN_ALIGN_LEFT).required(true).build(),
                LabColumnDef.builder().code("hint").name("Gợi ý").type(Constants.COLUMN_TYPE_TEXT).align(Constants.COLUMN_ALIGN_LEFT).required(false).build()
        );

        Map<String, Object> spec = new HashMap<>();
        spec.put("showProgress", true);
        spec.put("flipAnimation", "3d");

        List<Map<String, Object>> mockRows = new ArrayList<>();
        mockRows.add(Map.of("front", "Thủ đô của Việt Nam là gì?", "back", "Hà Nội - trung tâm chính trị, văn hóa nghìn năm văn hiến.", "hint", "Có hồ Gươm"));
        mockRows.add(Map.of("front", "Đỉnh núi cao nhất Việt Nam là đỉnh nào?", "back", "Phan Xi Păng (Fansipan) với độ cao 3.143m.", "hint", "Nóc nhà Đông Dương tại Sa Pa"));
        mockRows.add(Map.of("front", "Sông nào dài nhất chảy qua lãnh thổ Việt Nam?", "back", "Sông Mê Kông (phần chảy vào Việt Nam gọi là sông Cửu Long).", "hint", "Đồng bằng sông Cửu Long"));
        mockRows.add(Map.of("front", "Tác giả của tác phẩm 'Truyện Kiều' là ai?", "back", "Đại thi hào Nguyễn Du (1765 - 1820).", "hint", "Đoạn trường tân thanh"));

        templates.put(Constants.TEMPLATE_FLASHCARD, TemplateDefinition.builder()
                .templateType(Constants.TEMPLATE_FLASHCARD)
                .name("Thẻ ghi nhớ (Flashcard)")
                .description("Luyện tập ghi nhớ kiến thức, từ vựng và câu hỏi trắc nghiệm trực quan 2 mặt")
                .icon("bi-card-text")
                .defaultColumns(columns)
                .defaultSpec(spec)
                .mockRows(mockRows)
                .build());
    }

    private void registerMillionaire() {
        List<LabColumnDef> columns = Arrays.asList(
                LabColumnDef.builder().code("level").name("Câu số").type(Constants.COLUMN_TYPE_NUMBER).align(Constants.COLUMN_ALIGN_CENTER).required(true).build(),
                LabColumnDef.builder().code("question").name("Câu hỏi").type(Constants.COLUMN_TYPE_TEXT).align(Constants.COLUMN_ALIGN_LEFT).required(true).build(),
                LabColumnDef.builder().code("optionA").name("Lựa chọn A").type(Constants.COLUMN_TYPE_TEXT).align(Constants.COLUMN_ALIGN_LEFT).required(true).build(),
                LabColumnDef.builder().code("optionB").name("Lựa chọn B").type(Constants.COLUMN_TYPE_TEXT).align(Constants.COLUMN_ALIGN_LEFT).required(true).build(),
                LabColumnDef.builder().code("optionC").name("Lựa chọn C").type(Constants.COLUMN_TYPE_TEXT).align(Constants.COLUMN_ALIGN_LEFT).required(true).build(),
                LabColumnDef.builder().code("optionD").name("Lựa chọn D").type(Constants.COLUMN_TYPE_TEXT).align(Constants.COLUMN_ALIGN_LEFT).required(true).build(),
                LabColumnDef.builder().code("correct").name("Đáp án đúng (A/B/C/D)").type(Constants.COLUMN_TYPE_TEXT).align(Constants.COLUMN_ALIGN_CENTER).required(true).build(),
                LabColumnDef.builder().code("explanation").name("Giải thích ngắn").type(Constants.COLUMN_TYPE_TEXT).align(Constants.COLUMN_ALIGN_LEFT).required(false).build()
        );

        Map<String, Object> spec = new HashMap<>();
        spec.put("timerSeconds", 30);
        spec.put("safeMilestones", Arrays.asList(5, 10, 15));

        List<Map<String, Object>> mockRows = new ArrayList<>();
        mockRows.add(Map.of("level", 1, "question", "Thành phố nào được mệnh danh là 'Thành phố ngàn hoa' ở Việt Nam?", "optionA", "Đà Lạt", "optionB", "Nha Trang", "optionC", "Đà Nẵng", "optionD", "Sa Pa", "correct", "A", "explanation", "Đà Lạt thuộc tỉnh Lâm Đồng nổi tiếng với ngàn hoa rực rỡ."));
        mockRows.add(Map.of("level", 2, "question", "Hành tinh nào gần Mặt Trời nhất trong Hệ Mặt Trời?", "optionA", "Sao Kim", "optionB", "Sao Thủy", "optionC", "Sao Hỏa", "optionD", "Trái Đất", "correct", "B", "explanation", "Sao Thủy (Mercury) là hành tinh gần Mặt Trời nhất."));
        mockRows.add(Map.of("level", 3, "question", "Ai là tác giả của Quốc ca Việt Nam (Tiến quân ca)?", "optionA", "Trịnh Công Sơn", "optionB", "Văn Cao", "optionC", "Phạm Tuyên", "optionD", "Hoàng Việt", "correct", "B", "explanation", "Nhạc sĩ Văn Cao sáng tác Tiến quân ca vào năm 1944."));

        templates.put(Constants.TEMPLATE_MILLIONAIRE, TemplateDefinition.builder()
                .templateType(Constants.TEMPLATE_MILLIONAIRE)
                .name("Ai là triệu phú")
                .description("Gameshow đấu trí kịch tính với 15 mốc câu hỏi, quyền trợ giúp 50:50 và hỏi ý kiến khán giả")
                .icon("bi-trophy-fill")
                .defaultColumns(columns)
                .defaultSpec(spec)
                .mockRows(mockRows)
                .build());
    }
}
