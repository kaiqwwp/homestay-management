package com.homestay.homestay.service;

import com.homestay.homestay.entity.Room;
import com.homestay.homestay.entity.RoomType;
import com.homestay.homestay.entity.ServiceItem;
import com.homestay.homestay.repository.RoomRepository;
import com.homestay.homestay.repository.RoomTypeRepository;
import com.homestay.homestay.repository.ServiceItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AiChatService {

    private static final Logger log = LoggerFactory.getLogger(AiChatService.class);

    private final RoomTypeRepository roomTypeRepository;
    private final RoomRepository roomRepository;
    private final ServiceItemRepository serviceItemRepository;

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    public AiChatService(RoomTypeRepository roomTypeRepository,
                         RoomRepository roomRepository,
                         ServiceItemRepository serviceItemRepository) {
        this.roomTypeRepository = roomTypeRepository;
        this.roomRepository = roomRepository;
        this.serviceItemRepository = serviceItemRepository;
    }

    public String reply(String userMessage) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return "Xin chào! Em là Trợ lý ảo Homestay Pro. Em có thể giúp quý khách tìm loại phòng phù hợp với số người, báo giá hoặc tư vấn dịch vụ ạ!";
        }

        // Nếu có Gemini API Key thì ưu tiên gọi Gemini API
        if (geminiApiKey != null && !geminiApiKey.trim().isEmpty() && !geminiApiKey.equalsIgnoreCase("YOUR_GEMINI_API_KEY")) {
            try {
                String aiResponse = callGeminiApi(userMessage);
                if (aiResponse != null && !aiResponse.isBlank()) {
                    return aiResponse;
                }
            } catch (Exception e) {
                log.warn("Gemini API gặp sự cố, tự động chuyển sang chế độ tư vấn chuyên gia nội bộ: {}", e.getMessage());
            }
        }

        // Chế độ tư vấn chuyên gia nội bộ (Tự động đọc dữ liệu phòng và dịch vụ thực tế từ DB)
        return generateExpertResponse(userMessage);
    }

    private String generateExpertResponse(String message) {
        String lower = message.toLowerCase();
        List<RoomType> roomTypes = roomTypeRepository.findAll();
        List<Room> rooms = roomRepository.findAll();
        List<ServiceItem> services = serviceItemRepository.findAll();
        DecimalFormat cur = new DecimalFormat("#,##0 VNĐ");

        // 1. Nhận diện số lượng người (VD: "2 người", "4 người", "3 bạn", "1 người")
        Pattern peoplePattern = Pattern.compile("(\\d+)\\s*(?:người|khách|bạn|thành viên|pax)");
        Matcher matcher = peoplePattern.matcher(lower);
        Integer peopleCount = null;
        if (matcher.find()) {
            peopleCount = Integer.parseInt(matcher.group(1));
        }

        StringBuilder sb = new StringBuilder();

        if (peopleCount != null) {
            sb.append("Dạ với đoàn <strong>").append(peopleCount).append(" người</strong>, em xin tư vấn các lựa chọn phòng phù hợp nhất tại Homestay Pro:<br/><br/>");

            List<RoomType> suitable = new ArrayList<>();
            for (RoomType rt : roomTypes) {
                if (rt.getCapacity() >= peopleCount) {
                    suitable.add(rt);
                }
            }

            if (suitable.isEmpty()) {
                sb.append("Hiện tại loại phòng lớn nhất của homestay có sức chứa tối đa cho 4 người. Quý khách có thể cân nhắc đặt <strong>2 phòng</strong> (ví dụ: kết hợp 1 phòng đơn và 1 phòng đôi) để có không gian thoải mái nhất nhé!<br/>");
            } else {
                for (RoomType rt : suitable) {
                    long availableCount = rooms.stream()
                            .filter(r -> r.getRoomType().getId().equals(rt.getId()) && "AVAILABLE".equals(r.getStatus()))
                            .count();

                    sb.append("✨ <strong>").append(rt.getName()).append("</strong><br/>")
                            .append("• Giá: <strong>").append(cur.format(rt.getPricePerNight())).append("/đêm</strong><br/>")
                            .append("• Sức chứa tối đa: ").append(rt.getCapacity()).append(" người<br/>")
                            .append("• Tiện ích: ").append(rt.getDescription() != null ? rt.getDescription() : "Đầy đủ tiện nghi").append("<br/>")
                            .append("• Tình trạng hiện tại: <em>").append(availableCount > 0 ? "Còn " + availableCount + " phòng sẵn sàng đón khách" : "Tạm hết phòng").append("</em><br/><br/>");
                }
            }

            sb.append("💡 Quý khách có thể bấm vào mục <strong>Đặt phòng</strong> trên thanh menu để chọn ngày và giữ phòng ngay ạ!");
            return sb.toString();
        }

        // 2. Nếu hỏi về giá hoặc danh sách phòng
        if (lower.contains("giá") || lower.contains("phòng") || lower.contains("loại phòng") || lower.contains("bảng giá")) {
            sb.append("Dạ đây là bảng giá và sức chứa các loại phòng hiện có tại Homestay Pro ạ:<br/><br/>");
            for (RoomType rt : roomTypes) {
                sb.append("🏨 <strong>").append(rt.getName()).append("</strong>: <strong>").append(cur.format(rt.getPricePerNight())).append("/đêm</strong>")
                        .append(" (Tối đa ").append(rt.getCapacity()).append(" người) - ").append(rt.getDescription() != null ? rt.getDescription() : "Tiện nghi cao cấp").append("<br/>");
            }
            sb.append("<br/>Quý khách đi mấy người để em tư vấn phòng tối ưu chi phí nhất ạ?");
            return sb.toString();
        }

        // 3. Nếu hỏi về dịch vụ
        if (lower.contains("dịch vụ") || lower.contains("ăn") || lower.contains("xe") || lower.contains("giặt")) {
            sb.append("Dạ Homestay Pro có các dịch vụ tiện ích phục vụ quý khách trong kỳ nghỉ:<br/><br/>");
            for (ServiceItem s : services) {
                sb.append("• <strong>").append(s.getName()).append("</strong>: ").append(cur.format(s.getUnitPrice())).append(" / ").append(s.getUnit()).append("<br/>");
            }
            sb.append("<br/>Các dịch vụ này quý khách có thể yêu cầu nhân viên lễ tân phục vụ bất cứ lúc nào trong thời gian lưu trú ạ!");
            return sb.toString();
        }

        // 4. Nếu hỏi về thanh toán
        if (lower.contains("thanh toán") || lower.contains("chuyển khoản") || lower.contains("qr") || lower.contains("sepay")) {
            return "Dạ Homestay hỗ trợ đa dạng phương thức thanh toán: <strong>Chuyển khoản quét mã VietQR tự động qua SePay</strong>, tiền mặt, thẻ ngân hàng hoặc ví điện tử. Hệ thống sẽ tự động gạch nợ và xuất hóa đơn ngay khi nhận được tiền ạ!";
        }

        // Mặc định
        return "Dạ em có thể hỗ trợ quý khách: <br/>" +
                "1. Tư vấn loại phòng phù hợp (quý khách chỉ cần nhắn số người, ví dụ: <em>'mình đi 4 người'</em>)<br/>" +
                "2. Báo giá niêm yết các loại phòng<br/>" +
                "3. Các dịch vụ đi kèm (ăn uống, thuê xe, giặt ủi...)<br/>" +
                "Quý khách đang quan tâm đến thông tin nào ạ?";
    }

    private String callGeminiApi(String userMessage) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + geminiApiKey;
        RestTemplate restTemplate = new RestTemplate();

        List<RoomType> roomTypes = roomTypeRepository.findAll();
        StringBuilder context = new StringBuilder("Dưới đây là thông tin phòng thực tế của Homestay:\n");
        for (RoomType rt : roomTypes) {
            context.append("- ").append(rt.getName()).append(": giá ").append(rt.getPricePerNight()).append(" VNĐ/đêm, sức chứa ").append(rt.getCapacity()).append(" người, mô tả: ").append(rt.getDescription()).append("\n");
        }

        String prompt = "Bạn là trợ lý lễ tân ảo thân thiện của Homestay Pro. Hãy dùng thông tin sau để tư vấn cho khách bằng tiếng Việt, ngắn gọn, lịch sự:\n"
                + context + "\nCâu hỏi của khách: " + userMessage;

        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            Map bodyMap = response.getBody();
            List candidates = (List) bodyMap.get("candidates");
            if (candidates != null && !candidates.isEmpty()) {
                Map first = (Map) candidates.get(0);
                Map content = (Map) first.get("content");
                List parts = (List) content.get("parts");
                Map part = (Map) parts.get(0);
                return (String) part.get("text");
            }
        }
        return null;
    }
}
