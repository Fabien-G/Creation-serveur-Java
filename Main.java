import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class Main {

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(3000), 0);

        server.createContext("/", exchange -> {
            Map<String, String> query = parse(exchange.getRequestURI().getRawQuery());
            String corps = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, String> post = parse(corps);

            byte[] reponse = page(query, post).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, reponse.length);
            try (var os = exchange.getResponseBody()) {
                os.write(reponse);
            }
        });

        server.start();
        System.out.println("Serveur démarré : http://localhost:3000");
    }

    // "a=1&b=2" -> {a=1, b=2}
    static Map<String, String> parse(String data) {
        Map<String, String> map = new LinkedHashMap<>();
        if (data == null || data.isBlank()) return map;
        for (String pair : data.split("&")) {
            String[] kv = pair.split("=", 2);
            String cle = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
            String valeur = kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
            map.put(cle, valeur);
        }
        return map;
    }

    static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    static String tableau(String titre, Map<String, String> donnees) {
        StringBuilder sb = new StringBuilder("<h2>" + titre + "</h2>");
        sb.append("<table border=\"1\"><tr><th>Clé</th><th>Valeur</th></tr>");
        donnees.forEach((k, v) ->
            sb.append("<tr><td>").append(escape(k)).append("</td><td>").append(escape(v)).append("</td></tr>"));
        return sb.append("</table>").toString();
    }

    static String page(Map<String, String> query, Map<String, String> post) {
        return """
            <!DOCTYPE html>
            <html lang="fr">
            <head><meta charset="utf-8"><title>Hello World</title></head>
            <body>
              <h1>Hello World</h1>
              %s
              %s
              <h2>Formulaire</h2>
              <form method="POST" action="/">
                <input name="nom" placeholder="nom">
                <input name="age" placeholder="age">
                <button type="submit">Envoyer</button>
              </form>
            </body>
            </html>
            """.formatted(tableau("Query-string", query), tableau("POST", post));
    }
}