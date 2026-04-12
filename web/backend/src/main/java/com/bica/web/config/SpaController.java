package com.bica.web.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

@Controller
public class SpaController {

    private static final Pattern BOT_PATTERN = Pattern.compile(
            "(?i)(facebookexternalhit|Twitterbot|LinkedInBot|Slackbot|WhatsApp|Discordbot|TelegramBot|Googlebot|bingbot)"
    );

    @RequestMapping(value = {"/", "/{path:^(?!api|static|assets|papers|.*\\..*).*$}/**"})
    public String forward(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String userAgent = request.getHeader("User-Agent");
        String type = request.getParameter("type");
        String path = request.getRequestURI();

        // Serve dynamic OG tags for social media crawlers on analyzer pages with a type param
        if (type != null && !type.isBlank() && userAgent != null && BOT_PATTERN.matcher(userAgent).find()
                && path.contains("/tools/analyzer")) {
            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().write(ogHtml(type));
            return null;
        }

        return "forward:/index.html";
    }

    private String ogHtml(String type) {
        String encodedType = URLEncoder.encode(type, StandardCharsets.UTF_8);
        String truncatedType = type.length() > 80 ? type.substring(0, 80) + "..." : type;
        String safeTitle = escapeHtml(truncatedType);
        String title = "BICA Tools — " + safeTitle;
        String imageUrl = "https://bica-tools.org/api/og-image?type=" + encodedType;
        String pageUrl = "https://bica-tools.org/tools/analyzer?type=" + encodedType;

        return """
                <!doctype html>
                <html lang="en">
                <head>
                  <meta charset="utf-8">
                  <title>%s</title>
                  <meta property="og:type" content="website">
                  <meta property="og:title" content="%s">
                  <meta property="og:description" content="Analyze this session type: lattice check, Hasse diagram, test generation.">
                  <meta property="og:image" content="%s">
                  <meta property="og:url" content="%s">
                  <meta property="og:site_name" content="BICA Tools">
                  <meta name="twitter:card" content="summary_large_image">
                  <meta name="twitter:title" content="%s">
                  <meta name="twitter:description" content="Analyze this session type: lattice check, Hasse diagram, test generation.">
                  <meta name="twitter:image" content="%s">
                  <meta http-equiv="refresh" content="0;url=%s">
                </head>
                <body><p>Redirecting...</p></body>
                </html>
                """.formatted(title, title, imageUrl, pageUrl, title, imageUrl, pageUrl);
    }

    private static String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
