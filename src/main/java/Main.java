import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Main {
    public static String REQUEST_STRING = "https://graphhopper.com/api/1/route?point=%s&point=%s&profile=car&locale=de&calc_points=false&key=862625ff-fd66-4884-a47d-287d0bf5f649";

    public static String VladivostokCoordinates = "43.127659494759364,131.91403434481538";
    public static String NakhodkaCoordinates = "42.816543554085506,133.00003841040606";
    public static String UssuriyskCoordinates = "43.79885610203228,131.9470496142025";

    public static String[] cityCoordinates = {
        VladivostokCoordinates, NakhodkaCoordinates, UssuriyskCoordinates
    };

    public static void main(String[] args) {
        List<String> beachCoordinates = new ArrayList<>();

        try (FileInputStream excelFile = new FileInputStream("src/main/resources/check.xlsx")) {
            Workbook workbook = new XSSFWorkbook(excelFile);
            Sheet workbookSheet = workbook.getSheetAt(0);

            for (Row row : workbookSheet) {
                beachCoordinates.add(row.getCell(5).getStringCellValue());
            }

        } catch (IOException ignored) {}

        try (BufferedWriter bufferedWriter = new BufferedWriter(
                new FileWriter("src/main/resources/result.csv")
        )) {
            bufferedWriter.write("from;to;distance\n");

            for (String city : cityCoordinates) {
                for (String coordinates : beachCoordinates) {
                    Client client = ClientBuilder.newClient();
                    Response response = client.target(String.format(REQUEST_STRING, city, coordinates)).request().get();

                    String responseBody = response.readEntity(String.class);
                    System.out.println("status: " + response.getStatus());

                    Map<String, Object> responseJSON = new ObjectMapper().readValue(responseBody, new TypeReference<>() {});

                    String pathDistance = null;
                    for (Map.Entry<String, Object> element : responseJSON.entrySet()) {
                        if (element.getKey().equals("paths")) {
                            String[] pathsArray = element.getValue().toString().split(", ");

                            for (String path : pathsArray) {
                                if (path.contains("distance")) {
                                    pathDistance = path.split("=")[1];
                                    System.out.println(pathDistance);
                                }
                            }
                        }
                    }

                    String resultString;

                    switch(city) {
                        case "43.127659494759364,131.91403434481538" ->
                                resultString = "Владивосток" + ";" + coordinates + ";" + pathDistance + "\n";
                        case "42.816543554085506,133.00003841040606" ->
                                resultString = "Находка" + ";" + coordinates + ";" + pathDistance + "\n";
                        case "43.79885610203228,131.9470496142025" ->
                                resultString = "Уссурийск" + ";" + coordinates + ";" + pathDistance + "\n";
                        default ->
                                resultString = "" + ";" + coordinates + ";" + pathDistance + "\n";
                    }
                    bufferedWriter.write(resultString);
                    System.out.println(resultString);

                    Thread.sleep(1000);
                }
            }

            bufferedWriter.flush();
        } catch (IOException ignored) {} catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
