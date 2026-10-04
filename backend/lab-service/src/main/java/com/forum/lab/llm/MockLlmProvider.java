package com.forum.lab.llm;

import com.forum.lab.utils.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class MockLlmProvider implements LlmProvider {

    @Override
    public String getProviderName() {
        return "Mock-Provider-Dự-Phòng";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public Flux<String> streamChat(List<LlmMessage> messages) {
        String lastUserPrompt = "";
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (Constants.MESSAGE_ROLE_USER.equalsIgnoreCase(messages.get(i).getRole())) {
                lastUserPrompt = messages.get(i).getContent().toLowerCase();
                break;
            }
        }

        String responseText;
        if (lastUserPrompt.contains("quay") || lastUserPrompt.contains("wheel") || lastUserPrompt.contains("bốc thăm") || lastUserPrompt.contains("may mắn")) {
            responseText = "Chào bạn! Ta là **Nhà thông thái** 🧙‍♂️.\n\nTa nhận thấy bạn muốn khởi tạo một **Vòng quay may mắn**. Ta đã chuẩn bị sẵn bộ khuôn mẫu và cấu hình các ô phần thưởng/tỷ lệ trúng theo yêu cầu của bạn. Bạn hãy nhấn vào nút bên dưới để mở và trải nghiệm ngay nhé!";
        } else if (lastUserPrompt.contains("thẻ") || lastUserPrompt.contains("flashcard") || lastUserPrompt.contains("từ vựng") || lastUserPrompt.contains("ghi nhớ")) {
            responseText = "Xin chào! **Nhà thông thái** 🧙‍♂️ rất vui được đồng hành cùng bạn.\n\nBộ **Thẻ ghi nhớ (Flashcard)** đã được ta biên soạn với đầy đủ mặt câu hỏi, mặt đáp án và gợi ý tư duy trực quan 3D. Bạn có thể mở thẻ để ôn luyện ngay hoặc vào phần Cấu hình dữ liệu để bổ sung thêm các thẻ bài mới!";
        } else if (lastUserPrompt.contains("triệu phú") || lastUserPrompt.contains("millionaire") || lastUserPrompt.contains("trắc nghiệm") || lastUserPrompt.contains("game")) {
            responseText = "Chào mừng bạn đến với gameshow trí tuệ! Ta là **Nhà thông thái** 🧙‍♂️.\n\nTa đã thiết kế xong đấu trường **Ai là triệu phú** với các mốc câu hỏi kịch tính và quyền trợ giúp chuẩn mực. Hãy bấm vào đường dẫn phía dưới để bắt đầu thử thách kiến thức của mình ngay nhé!";
        } else {
            responseText = "Chào bạn, ta là **Nhà thông thái** 🧙‍♂️ của Phòng thí nghiệm diễn đàn!\n\nTa có thể giúp bạn sinh ra các sản phẩm tương tác thông minh chỉ qua trò chuyện, ví dụ như:\n- 🎡 **Vòng quay may mắn** (bốc thăm, chọn người ngẫu nhiên, minigame)\n- 🗂 **Thẻ ghi nhớ (Flashcard)** (ôn tập kiến thức, từ vựng 2 mặt)\n- 🏆 **Đấu trường Ai là triệu phú** (trắc nghiệm 15 câu kịch tính)\n\nBạn muốn tạo trò chơi hay ứng dụng nào hôm nay?";
        }

        List<String> words = new ArrayList<>();
        String[] parts = responseText.split(" ");
        for (String part : parts) {
            words.add(part + " ");
        }

        return Flux.fromIterable(words)
                .delayElements(Duration.ofMillis(35));
    }
}
