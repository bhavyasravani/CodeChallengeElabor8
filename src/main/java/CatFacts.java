import io.restassured.RestAssured;
import io.restassured.http.Method;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.json.simple.parser.ParseException;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CatFacts {

    String filePath = "src\\main\\resources\\Automation Engineer - Datasheet.xlt";
    int sheetNum = 0, headerRow = 0;
    List<Map<String, String>> catFactsDataSet = new ArrayList<>();
    String baseUrl = "https://cat-fact.herokuapp.com";

    @BeforeTest
    public void readDataFromTheDatasheet() throws IOException {

        FileInputStream fileInputStream = new FileInputStream(new File(filePath));
        HSSFWorkbook workbook = new HSSFWorkbook(fileInputStream);

        // Get the first sheet
        Sheet sheet = workbook.getSheetAt(sheetNum);
        //Get the header details
        List<String> headerDetails = new ArrayList<>();
        for (Cell cellValue : sheet.getRow(headerRow)) {
            headerDetails.add(cellValue.toString());
        }
        //Read the catFacts data into a map
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Map<String, String> recordData = new HashMap<>();
            Row rowData = sheet.getRow(i);
            for (int j = 0; j < headerDetails.size(); j++) {
                Cell cell = rowData.getCell(j);
                recordData.put(headerDetails.get(j), cell.getStringCellValue());
            }
            catFactsDataSet.add(recordData);
        }
        //Print all the cat facts
        for (Map<String, String> catFact : catFactsDataSet) {
            System.out.println(catFact);
        }
        System.out.println(catFactsDataSet.get(0).get("user.name.last"));

        // Close the workbook
        workbook.close();

    }


    @Test
    public void verifyAllCatsFacts() throws ParseException {
        String getAllFactsApi = baseUrl + "/facts";

        //Call the api and read the response
        RestAssured.baseURI = getAllFactsApi;
        RequestSpecification httpRequest = RestAssured.given();
        Response response = httpRequest.request(Method.GET, "");

        // Print the status code and message body of the response received from the server
        System.out.println("Status Code Received => " + response.getStatusCode());
        System.out.println("Response =>" + response.prettyPrint());

        //Verify the status code
        int responseStatusCode = response.getStatusCode();
        Assert.assertEquals(responseStatusCode, 200, "Status codes do not match");

        //Verify each cat fact against the data specified in the spread sheet

        JsonPath jsonPathEvaluator = response.jsonPath();
        System.out.println(jsonPathEvaluator.get("").toString());

        //Read the total number of json arrays
        int responseSize = jsonPathEvaluator.getList("$").size();
        System.out.println("Total number of cat facts returned are " + responseSize);

        //Looping through each cat fact and verifying the details against the spreadsheet
        for (int i = 0; i < responseSize; i++) {
            String catFactId = jsonPathEvaluator.get("[" + i + "]._id");
            System.out.println("Comparing values for the Fact with id " + catFactId);
            int reqCatFact = 0;
            for (int j = 0; j < catFactsDataSet.size(); j++) {
                if (catFactId.equals(catFactsDataSet.get(j).get("_id")))
                    reqCatFact = j;
            }
            Assert.assertEquals((String) jsonPathEvaluator.get("[" + i + "].user"), catFactsDataSet.get(i).get("user"), "Cat Fact User does not match");
            Assert.assertEquals((String) jsonPathEvaluator.get("[" + i + "].text"), catFactsDataSet.get(i).get("text"), "Cat Fact Type does not match");

        }
    }

    @Test
    public void verifyIndividualCatFacts() {

        //Read all the ids
        ArrayList<String> catIds = new ArrayList<String>();
        for (int i = 0; i < catFactsDataSet.size(); i++) {
            catIds.add(catFactsDataSet.get(i).get("_id"));
        }
        System.out.println(catIds);

        //Verify all the details of the each cat against the values in excel

        String getFactsApi;
        int responseStatusCode;

        for (int i = 0; i < catFactsDataSet.size(); i++) {
            getFactsApi = baseUrl + "/facts/" + catFactsDataSet.get(i).get("_id");

            //Call the api and read the response
            RestAssured.baseURI = getFactsApi;
            RequestSpecification httpRequest = RestAssured.given();
            Response response = httpRequest.request(Method.GET, "");

            // Print the status code and message body of the response received from the server
            System.out.println("Cat Number " + (i + 1) + "\n  Status Code Received => " + response.getStatusCode());
            System.out.println("Response =>" + response.prettyPrint());

            //Verify the status code
            responseStatusCode = response.getStatusCode();
            Assert.assertEquals(responseStatusCode, 200, "Status codes do not match");

            //Verify the cat facts against the supplied data in the spreadsheet
            JsonPath jsonPathEvaluator = response.jsonPath();

            //Verify Cat User, Cat Text, Cat User First Name, Cat User Last Name, Cat User Type
            Assert.assertEquals((String) jsonPathEvaluator.get("user._id"), catFactsDataSet.get(i).get("user"), "Cat Fact " + (i + 1) + "User does not match");
            Assert.assertEquals((String) jsonPathEvaluator.get("text"), catFactsDataSet.get(i).get("text"), "Cat Fact \"+(i+1)+ \"Text does not match");
            Assert.assertEquals((String) jsonPathEvaluator.get("user.name.first"), catFactsDataSet.get(i).get("user.name.first"), "Cat Fact \"+(i+1)+ \"First Name does not match");
            Assert.assertEquals((String) jsonPathEvaluator.get("user.name.last"), catFactsDataSet.get(i).get("user.name.last"), "Cat Fact \"+(i+1)+ \"Last Name does not match");
            Assert.assertEquals((String) jsonPathEvaluator.get("type"), catFactsDataSet.get(i).get("type"), "Cat Fact \"+(i+1)+ \"Type does not match");
        }

    }

}
