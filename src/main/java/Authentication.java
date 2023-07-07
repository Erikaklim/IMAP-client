import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;

public class Authentication {

    String clientId;
    String clientSecret;
    String redirectUri;
    String accessToken;
    String refreshToken;

    public Authentication(String clientId, String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = "http://localhost:3000/callback";
    }
    public void authenticate() throws IOException {
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    new NetHttpTransport(),
                    new JacksonFactory(),
                    clientId,
                    clientSecret,
                    Collections.singleton("https://mail.google.com/"))
                    .setAccessType("offline")
                    .build();

            // Generate the authorization URL
            String authorizationUrl = flow.newAuthorizationUrl().setRedirectUri(redirectUri).build();

            // Print the authorization URL and prompt the user to visit it
            System.out.println("Authorization URL: " + authorizationUrl);
            System.out.println("Please visit the above URL to authorize the application.");

            // Use a BufferedReader to read the authorization code from the user
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            System.out.print("Enter authorization code: ");
            String authorizationCode = br.readLine();

            // Exchange the authorization code for an access token and refresh token
            GoogleTokenResponse tokenResponse = flow.newTokenRequest(authorizationCode).setRedirectUri(redirectUri).execute();
            accessToken = tokenResponse.getAccessToken();
            System.out.println("Access token: " + accessToken);
            long date = tokenResponse.getExpiresInSeconds();
            System.out.println("Access token expires:" + date);
            refreshToken = tokenResponse.getRefreshToken();
            System.out.println("Refresh token: " + refreshToken);
            date = tokenResponse.getExpiresInSeconds();
            System.out.println("Refresh token expires:" + date);

    }


}
