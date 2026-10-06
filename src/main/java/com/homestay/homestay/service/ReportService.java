package com.homestay.homestay.service;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ReportService {

    // Biên dịch .jrxml khá chậm nên chỉ làm 1 lần rồi lưu lại
    private final Map<String, JasperReport> cache = new ConcurrentHashMap<>();

    public byte[] exportPdf(String jrxmlName, Map<String, Object> params, Collection<?> data) {
        try {
            JasperReport report = cache.computeIfAbsent(jrxmlName, this::compile);
            JasperPrint print = JasperFillManager.fillReport(
                    report, params, new JRBeanCollectionDataSource(data));
            return JasperExportManager.exportReportToPdf(print);
        } catch (JRException e) {
            throw new IllegalStateException("Lỗi xuất báo cáo " + jrxmlName + ": " + e.getMessage(), e);
        }
    }

    private JasperReport compile(String jrxmlName) {
        // Đọc bằng stream (không dùng File) để chạy được cả khi đóng gói jar
        try (InputStream in = new ClassPathResource("reports/" + jrxmlName).getInputStream()) {
            return JasperCompileManager.compileReport(in);
        } catch (Exception e) {
            throw new IllegalStateException("Không biên dịch được " + jrxmlName + ": " + e.getMessage(), e);
        }
    }
}