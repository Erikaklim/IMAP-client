import com.google.api.client.auth.oauth2.Credential;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.LocalTime;

public class NewTokenGetter {
    String tokenEndpoint = "https://oauth2.googleapis.com/token";
    String refreshToken;
    String clientId;
    String clientSecret;
    String newAccessToken;

    LocalTime expirationTime;

    Long timeLeft;

    public NewTokenGetter(String refreshToken, String clientId, String clientSecret) {
        this.refreshToken = refreshToken;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public void getNewToken() throws IOException {

            // Create HttpClient
            HttpClient httpClient = HttpClients.createDefault();

            // Create POST request to token endpoint
            HttpPost httpPost = new HttpPost(tokenEndpoint);

            String requestBody = "refresh_token=" + refreshToken
                    + "&client_id=" + clientId
                    + "&client_secret=" + clientSecret
                    + "&grant_type=refresh_token";
            StringEntity entity = new StringEntity(requestBody);
            httpPost.setEntity(entity);
            httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");

            // Execute request and get response
            HttpResponse response = httpClient.execute(httpPost);
            String responseJson = EntityUtils.toString(response.getEntity());

            // Check response for token validity
            if (response.getStatusLine().getStatusCode() == 200) {
                // Token is valid
                System.out.println("Access token refreshed successfully.");
                JsonObject json = new Gson().fromJson(responseJson, JsonObject.class);
                newAccessToken = json.get("access_token").getAsString();
                timeLeft = json.get("expires_in").getAsLong();
                expirationTime = LocalTime.now();
                System.out.println("New accessToken: " + newAccessToken);
            } else {
                // Token refresh failed, handle error
                System.out.println("Failed to refresh access token: " + responseJson);
                // Parse and handle the response JSON as needed
            }

    }

}
