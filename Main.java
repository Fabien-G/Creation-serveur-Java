
// Importe la classe permettant de créer un serveur HTTP.
import com.sun.net.httpserver.HttpServer;

// Permet de gérer les erreurs liées aux entrées/sorties (I/O).
import java.io.IOException;

// Permet de définir l'adresse et le port d'écoute du serveur.
import java.net.InetSocketAddress;

// Permet de décoder les données reçues dans une URL ou un formulaire.
import java.net.URLDecoder;

// Permet d'utiliser l'encodage UTF-8.
import java.nio.charset.StandardCharsets;

// Map qui conserve l'ordre dans lequel les éléments ont été ajoutés.
import java.util.LinkedHashMap;

// Interface permettant de stocker des données sous forme clé / valeur.
import java.util.Map;


public class Main {

    public static void main(String[] args) throws IOException {

        // Création d'un serveur HTTP qui écoute sur le port 3000.
        // "0" signifie que le serveur utilise la valeur par défaut pour la file d'attente.
        HttpServer server = HttpServer.create(new InetSocketAddress(3000), 0);

        // Route permettant au navigateur de récupérer le fichier CSS.
        server.createContext("/style2.css", exchange -> {

            // Lit le contenu du fichier CSS.
            byte[] css = java.nio.file.Files.readAllBytes(
                java.nio.file.Path.of("style2.css")
            );

            // Indique au navigateur qu'il s'agit d'un fichier CSS.
            exchange.getResponseHeaders().set(
                "Content-Type",
                "text/css; charset=utf-8"
            );

            // Envoie la réponse avec le contenu du fichier CSS.
            exchange.sendResponseHeaders(200, css.length);

            // Envoie le fichier CSS au navigateur.
            try (var os = exchange.getResponseBody()) {
                os.write(css);
            }
        });

        // Création d'une route "/" qui sera appelée lorsqu'un client
        // accède à http://localhost:3000/
        server.createContext("/", exchange -> {

            // Récupère les paramètres présents dans l'URL.
            // Exemple : ?nom=Fab&age=18
            // devient une Map : {nom=Fab, age=18}
            Map<String, String> query = parse(exchange.getRequestURI().getRawQuery());

            // Récupère le contenu envoyé dans le corps de la requête HTTP.
            // Ici, il s'agit notamment des données envoyées par le formulaire POST.
            String corps = new String(
                exchange.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8
            );

            // Transforme les données du formulaire POST
            // en Map clé / valeur.
            Map<String, String> post = parse(corps);

            // Génère le contenu HTML de la page à partir
            // des données GET (query) et POST.
            // Puis transforme le texte en tableau de bytes UTF-8.
            byte[] reponse = page(query, post).getBytes(StandardCharsets.UTF_8);

            // Indique au navigateur que la réponse est une page HTML
            // utilisant l'encodage UTF-8.
            exchange.getResponseHeaders().set(
                "Content-Type",
                "text/html; charset=utf-8"
            );

            // Envoie le code HTTP 200 (réponse réussie)
            // ainsi que la taille de la réponse.
            exchange.sendResponseHeaders(200, reponse.length);

            // Ouvre le flux permettant d'envoyer la réponse au navigateur.
            // Le try permet de fermer automatiquement le flux après utilisation.
            try (var os = exchange.getResponseBody()) {

                // Envoie le contenu HTML au navigateur.
                os.write(reponse);
            }
        });

        // Démarre le serveur HTTP.
        server.start();

        // Affiche dans la console l'adresse permettant d'accéder au serveur.
        System.out.println("Serveur démarré : http://localhost:3000");
    }


    // Transforme une chaîne de caractères contenant des paramètres
    // sous la forme :
    // "a=1&b=2"
    //
    // en Map :
    // {a=1, b=2}
    static Map<String, String> parse(String data) {

        // Création d'une Map qui conserve l'ordre d'insertion des éléments.
        Map<String, String> map = new LinkedHashMap<>();

        // Si aucune donnée n'est reçue ou si la chaîne est vide,
        // on retourne directement une Map vide.
        if (data == null || data.isBlank()) return map;

        // Sépare les différents paramètres grâce au caractère "&".
        // Exemple :
        // "nom=Fab&age=18"
        // devient :
        // ["nom=Fab", "age=18"]
        for (String pair : data.split("&")) {

            // Sépare chaque paramètre entre sa clé et sa valeur.
            // Le "2" signifie que la séparation est effectuée au maximum
            // une seule fois.
            String[] kv = pair.split("=", 2);

            // Décode la clé avec UTF-8.
            // Cela permet notamment de convertir les caractères encodés
            // dans une URL.
            String cle = URLDecoder.decode(
                kv[0],
                StandardCharsets.UTF_8
            );

            // Si une valeur existe, elle est également décodée.
            // Sinon, on utilise une chaîne vide.
            String valeur = kv.length > 1
                ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8)
                : "";

            // Ajoute la clé et sa valeur dans la Map.
            map.put(cle, valeur);
        }

        // Retourne toutes les données récupérées.
        return map;
    }


    // Protège les caractères spéciaux HTML présents dans une chaîne.
    // Cela évite qu'une valeur reçue soit interprétée comme du HTML.
    static String escape(String s) {

        // Remplace les caractères spéciaux par leurs équivalents HTML.
        return s
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
    }


    // Génère un tableau HTML à partir d'un titre et d'une Map de données.
    static String tableau(String titre, Map<String, String> donnees) {

        // Crée le début du tableau avec son titre.
        StringBuilder sb = new StringBuilder(
            "<h2>" + titre + "</h2>"
        );

        // Ajoute la ligne d'en-tête du tableau.
        sb.append(
            "<table border=\"1\"><tr><th>Clé</th><th>Valeur</th></tr>"
        );

        // Parcourt chaque élément de la Map.
        // Pour chaque élément, on ajoute une ligne au tableau HTML.
        donnees.forEach((k, v) ->
            sb.append("<tr><td>")
              .append(escape(k))
              .append("</td><td>")
              .append(escape(v))
              .append("</td></tr>")
        );

        // Ferme le tableau HTML et retourne le résultat sous forme de String.
        return sb.append("</table>").toString();
    }


    // Génère la page HTML complète affichée dans le navigateur.
    // "query" contient les paramètres présents dans l'URL.
    // "post" contient les données envoyées par le formulaire.
    static String page(
        Map<String, String> query,
        Map<String, String> post
    ) {

        // Utilisation d'un Text Block Java pour écrire
        // directement le HTML sur plusieurs lignes.
        //
        // %s sera remplacé par le tableau des paramètres Query-string.
        // %s sera remplacé par le tableau des données POST.
        return """
            <!DOCTYPE html>
            <html lang="fr">
            <head>
                <meta charset="utf-8">
                <title>Hello World</title>
                <link rel="stylesheet" href="/style2.css">
            </head>
            <body>

              <!-- Titre principal de la page -->
              <button class="button">
                    <span> Hello world</span>
              </button>

              <!-- Affiche les paramètres présents dans l'URL -->
              %s

              <!-- Affiche les données reçues avec POST -->
              %s

              <!-- Titre de la partie formulaire -->
              <h2>Formulaire</h2>

              <!-- Formulaire envoyé avec la méthode POST -->
              <form method="POST" action="/">

                <!-- Champ permettant de saisir le nom -->
                <input name="nom" placeholder="nom">

                <!-- Champ permettant de saisir l'âge -->
                <input name="age" placeholder="age">

                <!-- Champ permettant de saisir la ville -->
                <input name="ville" placeholder="ville">

                <!-- Bouton permettant d'envoyer le formulaire -->
                <button type="submit">Envoyer</button>

                

              </form>

            </body>
            </html>
            """.formatted(
                // Remplace le premier %s par le tableau Query-string.
                tableau("Query-string", query),

                // Remplace le deuxième %s par le tableau POST.
                tableau("POST", post)
            );
    }
}

