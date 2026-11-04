package com.homestay.homestay.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homestay.homestay.entity.Room;
import com.homestay.homestay.entity.RoomType;
import com.homestay.homestay.entity.ServiceItem;
import com.homestay.homestay.repository.RoomRepository;
import com.homestay.homestay.repository.RoomTypeRepository;
import com.homestay.homestay.repository.ServiceItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AiChatService {

    private static final Logger log = LoggerFactory.getLogger(AiChatService.class);

    private final RoomTypeRepository roomTypeRepository;
    private final RoomRepository roomRepository;
    private final ServiceItemRepository serviceItemRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

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
            return "Xin chào! Em là Trợ lý ảo Homestay Pro. Em có thể giúp quý khách tìm loại phòng phù hợp với số người, kiểm tra phòng trống theo thời gian thực, báo giá hoặc tư vấn dịch vụ ạ!";
        }

        // Nếu có Gemini API Key thì ưu tiên gọi Gemini API
        if (geminiApiKey != null && !geminiApiKey.trim().isEmpty() && !geminiApiKey.equalsIgnoreCase("YOUR_GEMINI_API_KEY")) {
            try {
                String aiResponse = callGeminiApi(userMessage);
                if (aiResponse != null && !aiResponse.isBlank()) {
                    return aiResponse;
                }
            } catch (Throwable e) {
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
                    List<String> availableRoomNums = rooms.stream()
                            .filter(r -> r.getRoomType().getId().equals(rt.getId()) && "AVAILABLE".equalsIgnoreCase(r.getStatus()))
                            .map(Room::getRoomNumber)
                            .toList();

                    sb.append("✨ <strong>").append(rt.getName()).append("</strong><br/>")
                            .append("• Giá niêm yết: <strong>").append(cur.format(rt.getPricePerNight())).append("/đêm</strong><br/>")
                            .append("• Sức chứa tối đa: ").append(rt.getCapacity()).append(" người<br/>")
                            .append("• Tiện ích: ").append(rt.getDescription() != null ? rt.getDescription() : "Đầy đủ tiện nghi").append("<br/>")
                            .append("• Tình trạng thời gian thực: <em>")
                            .append(!availableRoomNums.isEmpty()
                                    ? "Còn " + availableRoomNums.size() + " phòng trống (Phòng " + String.join(", ", availableRoomNums) + ")"
                                    : "Tạm hết phòng ngay lúc này")
                            .append("</em><br/><br/>");
                }
            }

            sb.append("💡 Quý khách có thể bấm nút <strong>Đặt phòng ngay</strong> trên thẻ phòng hoặc chọn ngày nhận - trả ở thanh tìm kiếm để đặt phòng ạ!");
            return sb.toString();
        }

        // 2. Nếu hỏi về phòng trống / giá / danh sách phòng
        if (lower.contains("trống") || lower.contains("còn phòng") || lower.contains("giá") || lower.contains("phòng") || lower.contains("loại phòng") || lower.contains("bảng giá")) {
            sb.append("Dạ đây là bảng giá và tình trạng phòng <strong>thời gian thực</strong> tại Homestay Pro ngay lúc này ạ:<br/><br/>");
            for (RoomType rt : roomTypes) {
                List<String> availNums = rooms.stream()
                        .filter(r -> r.getRoomType().getId().equals(rt.getId()) && "AVAILABLE".equalsIgnoreCase(r.getStatus()))
                        .map(Room::getRoomNumber)
                        .toList();
                sb.append("🏨 <strong>").append(rt.getName()).append("</strong>: <strong>").append(cur.format(rt.getPricePerNight())).append("/đêm</strong>")
                        .append(" (Tối đa ").append(rt.getCapacity()).append(" người)<br/>")
                        .append("   • Tiện nghi: ").append(rt.getDescription() != null ? rt.getDescription() : "Tiện nghi cao cấp").append("<br/>")
                        .append("   • Trạng thái: ")
                        .append(!availNums.isEmpty()
                                ? "<span class='text-success fw-semibold'>Còn trống " + availNums.size() + " phòng (" + String.join(", ", availNums) + ")</span>"
                                : "<span class='text-danger'>Đang kín phòng</span>")
                        .append("<br/><br/>");
            }
            sb.append("Quý khách đi mấy người để em tư vấn phòng tối ưu chi phí nhất ạ?");
            return sb.toString();
        }

        // 3. Nếu hỏi về dịch vụ
        if (lower.contains("dịch vụ") || lower.contains("ăn") || lower.contains("xe") || lower.contains("giặt") || lower.contains("bbq") || lower.contains("cafe")) {
            sb.append("Dạ Homestay Pro có các dịch vụ tiện ích phục vụ quý khách trong kỳ nghỉ:<br/><br/>");
            for (ServiceItem s : services) {
                sb.append("• <strong>").append(s.getName()).append("</strong>: <strong>").append(cur.format(s.getUnitPrice())).append("</strong> / ").append(s.getUnit()).append("<br/>");
            }
            sb.append("<br/>Quý khách có thể đặt kèm dịch vụ ngay khi tạo đơn đặt phòng hoặc yêu cầu lễ tân phục vụ bất cứ lúc nào trong thời gian lưu trú ạ!");
            return sb.toString();
        }

        // 4. Nếu hỏi về thanh toán / QR SePay / Voucher
        if (lower.contains("thanh toán") || lower.contains("chuyển khoản") || lower.contains("qr") || lower.contains("sepay") || lower.contains("voucher") || lower.contains("giảm giá")) {
            return "Dạ tại Homestay Pro:<br/>" +
                    "• <strong>Quy trình đặt phòng:</strong> Sau khi quý khách gửi yêu cầu, Lễ tân sẽ duyệt đơn và xác nhận Check-in khi quý khách nhận phòng.<br/>" +
                    "• <strong>Thanh toán tự động:</strong> Hỗ trợ quét mã <strong>VietQR SePay (MB Bank)</strong> tự động xác nhận trạng thái <em>Đã thanh toán</em> trong 3 giây, hoặc thanh toán <strong>Tiền mặt tại quầy</strong>.<br/>" +
                    "• <strong>Ưu đãi Voucher:</strong> Quý khách có mã giảm giá (ví dụ mã tri ân <code>DENBU10-...</code>) có thể nhập trực tiếp khi đặt phòng để được giảm 5% - 10% tổng hóa đơn ạ!";
        }

        // Mặc định
        long totalAvailable = rooms.stream().filter(r -> "AVAILABLE".equalsIgnoreCase(r.getStatus())).count();
        return "Xin chào quý khách! 👋 Hiện tại Homestay Pro đang có <strong>" + totalAvailable + " phòng sẵn sàng đón khách</strong>.<br/><br/>" +
                "Em có thể hỗ trợ quý khách ngay lập tức:<br/>" +
                "1. 🛏️ <strong>Tư vấn phòng theo số người</strong> (VD: <em>'Tư vấn phòng cho 2 người'</em> hoặc <em>'Đoàn 4 người'</em>)<br/>" +
                "2. 💰 <strong>Báo giá & kiểm tra phòng trống</strong> theo thời gian thực<br/>" +
                "3. ☕ <strong>Menu dịch vụ đi kèm</strong> (Buffet sáng, thuê xe máy, BBQ, giặt ủi...)<br/>" +
                "4. 💳 <strong>Hướng dẫn thanh toán QR SePay & mã Voucher</strong><br/><br/>" +
                "Quý khách đang dự định đi nghỉ dưỡng mấy người ạ?";
    }

    private String callGeminiApi(String userMessage) throws Exception {
        String key = geminiApiKey.trim();

        List<RoomType> roomTypes = roomTypeRepository.findAll();
        List<Room> rooms = roomRepository.findAll();
        List<ServiceItem> services = serviceItemRepository.findAll();
        StringBuilder context = new StringBuilder("Dưới đây là dữ liệu thực tế theo thời gian thực của Homestay Pro:\n");
        for (RoomType rt : roomTypes) {
            long avail = rooms.stream()
                    .filter(r -> r.getRoomType().getId().equals(rt.getId()) && "AVAILABLE".equalsIgnoreCase(r.getStatus()))
                    .count();
            context.append("- Loại phòng ").append(rt.getName())
                    .append(": giá ").append(rt.getPricePerNight()).append(" VNĐ/đêm, sức chứa tối đa ")
                    .append(rt.getCapacity()).append(" người, hiện còn ").append(avail).append(" phòng trống, mô tả: ")
                    .append(rt.getDescription()).append("\n");
        }
        context.append("Danh sách từng phòng cụ thể:\n");
        for (Room r : rooms) {
            context.append("- Phòng ").append(r.getRoomNumber())
                    .append(" (").append(r.getRoomType().getName()).append(", ")
                    .append(r.getRoomType().getPricePerNight()).append(" đ/đêm): Trạng thái = ")
                    .append(r.getStatus()).append("\n");
        }
        context.append("Dịch vụ đi kèm:\n");
        for (ServiceItem s : services) {
            context.append("- ").append(s.getName()).append(": ").append(s.getUnitPrice()).append(" VNĐ/").append(s.getUnit()).append("\n");
        }
        context.append("Thanh toán: Hỗ trợ quét mã QR SePay MB Bank tự động xác nhận ngay lập tức hoặc Tiền mặt tại quầy.\n");

        String prompt = "Bạn là trợ lý lễ tân ảo thông minh, nhiệt tình của khu nghỉ dưỡng Homestay Pro. Hãy dựa vào dữ liệu thực tế sau đây để tư vấn cho khách hàng bằng tiếng Việt ngắn gọn, rõ ràng, thân thiện:\n"
                + context + "\nCâu hỏi của khách: " + userMessage;

        Map<String, Object> payload = Map.of(
                "contents", List.of(
                        Map.of(
                                "role", "user",
                                "parts", List.of(Map.of("text", prompt))
                        )
                )
        );
        String jsonBody = objectMapper.writeValueAsString(payload);

        List<String> candidateUrls = key.startsWith("AQ.")
                ? List.of(
                        "https://aiplatform.googleapis.com/v1/publishers/google/models/gemini-2.0-flash:generateContent?key=" + key,
                        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + key
                )
                : List.of(
                        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + key
                );

        for (String url : candidateUrls) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(12))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 200 && response.statusCode() < 300 && response.body() != null) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode candidates = root.path("candidates");
                if (candidates.isArray() && !candidates.isEmpty()) {
                    String rawText = candidates.get(0).path("content").path("parts").get(0).path("text").asText(null);
                    if (rawText != null && !rawText.isBlank()) {
                        return rawText
                                .replaceAll("\\*\\*(.+?)\\*\\*", "<strong>$1</strong>")
                                .replace("\n", "<br/>");
                    }
                }
            }
        }
        return null;
    }
}
