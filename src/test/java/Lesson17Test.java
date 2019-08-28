/*
Выгрузить проект https://github.com/ILoskutnikov/RestAssured
        Модернизировать класс Application так, чтобы вы могли использовать метод PUT.
        Имплементировать в новом тестовом файле тест, покрывающий один из путей взаимодействия пользователя с системой,
        с помощью реализованных вами методов и уже готовых.
        Пример комплексного теста:
        - Добавить пользователя
        - Добавить книгу пользователю
        - Удалить книгу у пользователя
        - Удалить пользователя
        *Примечание - вы не можете удалить пользователя, пока у него есть книги.
        Критерии оценки: +1 - Выгрузили проект с github.
        +1 - Создать новый тестовый файл в директории src/test/java.
        +1 - Разделить тесты из файла RestAssuredTest.java на отдельные @Test так, чтобы вы могли отдельно запускать:
        Добавить пользователя
        Добавить книгу
        Удалить книгу
        Удалить пользователя
        +1 - Имплементировать PUT и создать свой тестовый случай по примеру из описания.
*/


import book.Account;
import book.Book;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.commons.lang3.RandomStringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_OK;

public class Lesson17Test {
    private static Account account;

    @Test
    public void e2eTest() throws JsonProcessingException,JSONException{
        String username = "mamkinagordostb";
        postUserByName(username).then().statusCode(SC_CREATED);
        for(int i=0;i<=3;i++){
            postRandomBookToUser(username).then().statusCode(SC_CREATED);
        }
        deleteAllBooksFromUser(username);
        removeUserByName(username);
    }

    @Test
    public void postUserTest() throws JsonProcessingException {
        String userName = getRandomUserName();
        Response response = postUserByName(userName);
        response.prettyPrint();
        response.then()
                .statusCode(SC_CREATED)
                .log();
    }

    @Test
    public void removeUserTest() throws JSONException,JsonProcessingException {
        Response response = removeUserByName(getRandomExistingUserName());
        response.prettyPrint();
        response.then()
                .statusCode(SC_OK)
                .log();
    }

    @Test
    public void postBookTest() throws JsonProcessingException, JSONException {
        Response response = postRandomBookToUser(getRandomExistingUserName());
        response.prettyPrint();
        response.then()
                .statusCode(SC_CREATED)
                .log();
    }

    @Test
    public void deleteBookTest() throws JsonProcessingException, JSONException {
        JSONArray users = getUsersArr();
        JsonPath jsonPath = getUsersList().jsonPath();
        String username = "";
        String bookId = "";
        for(int i=0;i<users.length();i++){
            if (jsonPath.getList("books["+i+"]").size()>0){
                username = jsonPath.get("username["+i+"]");
                bookId = jsonPath.get("books["+i+"][0].id").toString();
                break;
            }
        }
        deleteBookFromUser(username,bookId);
    }

    private String getLastId() throws JSONException {
        return getUsersArr().getJSONObject(0).getString("id");
    }

    private String getRandomUserName(){
        return RandomStringUtils.random(8,true,false);
    }

    private String getRandomExistingUserName() throws JSONException,JsonProcessingException{
        JSONArray users = getUsersArr();
        if (users.length()<=0){
            return "¯ \\ _ (ツ) _ / ¯";
        }
        int randomIndex = new Random().nextInt(users.length());
        return users.getJSONObject(randomIndex).getString("username");
    }
    private String getUserByIndex(int index) throws JSONException{
        return getUsersArr().getJSONObject(index).getString("username");
    }

    private Book generateRandomBook() throws JsonProcessingException,JSONException{
        account = new Account(getRandomExistingUserName(), "password");
        return new Book(account,
                RandomStringUtils.random(4,true,false)
                        + RandomStringUtils.random(8,true,false),
                RandomStringUtils.random(4,true,false)
                        + RandomStringUtils.random(8,true,false),
                RandomStringUtils.random(14,true,false));
    }

    private JSONArray getUsersArr() throws JSONException{
        return new JSONArray(getUsersList().asString());
    }

    private Response getUsersList() throws JSONException{
        String url = "http://localhost:8090/user/allUsers";
        RequestSpecification requestSpecification = given().contentType(ContentType.JSON);
        return requestSpecification.get(url);
    }

    private Response postUserByName(String userName) throws JsonProcessingException{
        ObjectMapper mapper = new ObjectMapper();
        String url = "http://localhost:8090/user/addUser";
        account = new Account(userName, "password");
        RequestSpecification requestSpecification = given().contentType(ContentType.JSON).body(mapper.writeValueAsBytes(account));
        return requestSpecification.post(url);
    }

    private Response removeUserByName(String username){
        String url = "http://localhost:8090/user/removeUser/"+username;
        RequestSpecification requestSpecification = given().contentType(ContentType.JSON);
        return requestSpecification.delete(url);
    }

    private Response postRandomBookToUser(String username) throws JsonProcessingException,JSONException {
        ObjectMapper mapper = new ObjectMapper();
        String url = "http://localhost:8090/"+username+"/books";
        Book book = generateRandomBook();
        RequestSpecification requestSpecification = given().contentType(ContentType.JSON).body(mapper.writeValueAsBytes(book));
        return requestSpecification.post(url);
    }

    private void deleteBookFromUser (String username,String bookId){
        String url = "http://localhost:8090/"+username+"/books/removeBook/" + bookId;
        RequestSpecification requestSpecification = given().contentType(ContentType.JSON);
        Response response = requestSpecification.delete(url);
        response.then().statusCode(SC_OK).log();
        response.prettyPrint();
    }

    private void deleteAllBooksFromUser (String username) throws JSONException{
        String bookId;
        JSONArray users = getUsersArr();
        JsonPath jsonPath = getUsersList().jsonPath();
        ArrayList books = new ArrayList();
        for(int i=0;i<users.length();i++){
            boolean condition = jsonPath.getJsonObject("["+i+"].username").equals(username);
            if (condition){
                books = jsonPath.getJsonObject("["+i+"].books");
                break;
            }
        }
        for(int i=0;i<books.size();i++){
            bookId = ((HashMap) books.get(i)).get("id").toString();
            deleteBookFromUser(username,bookId);
        }
    }
}
