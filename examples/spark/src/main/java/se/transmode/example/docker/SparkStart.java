package se.transmode.example.docker;
import static spark.Spark.get;

public class SparkStart {
    public static void main(String... args) {
        get("/", (request, response) -> "<h1>Hello Docker, I'm Spark!</h1>");
    }
}
