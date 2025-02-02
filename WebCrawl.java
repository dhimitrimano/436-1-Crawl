// DHIMITRI MANO

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class WebCrawl {
    private static ArrayList<URL> cache = new ArrayList<URL>();
    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.out.println("ERROR: use two args, URL and number of hops!");
            return;
        }
        for (char char1 : args[1].toCharArray()) {
            if (!Character.isDigit(char1)) {
                System.out.println("ERROR: arg 2 must be an integer!");
                return;
            }
        }
        int numHops = Integer.parseInt(args[1]);
        URL start;
        try {
            start = new URL(args[0]);
        } catch(Exception e) {
            System.out.println("ERROR: arg 1 must be a full URL (with \"http://\" or \"https://\" behind it)!");
            return;
        }
        HttpURLConnection connection = (HttpURLConnection) start.openConnection();
        connection.setRequestMethod("GET");
        int responseCode;
        try {
		    responseCode = connection.getResponseCode();
        } catch(Exception e) {
            System.out.println("ERROR: arg 1 is not a valid URL!");
            return;
        }
        if (responseCode / 100 == 3) {
            System.out.println("START at " + connection.getURL());
            System.out.println("REDIRECTHOP from URL " + connection.getURL() + " to URL " + new URL(connection.getHeaderField("Location")));
            connection = recursion(new URL(connection.getHeaderField("Location")), 0);
            try {
                responseCode = connection.getResponseCode();
            } catch(Exception e) {
                System.out.println("ERROR: arg 1 does not redirect to a valid URL!");
                return;
            }
            if (responseCode / 100 != 2) {
                System.out.println("ERROR: arg 1 redirection sent back response code" + responseCode + "!");
                return;
            }
		} else if (responseCode / 100 == 5) {
            System.out.println("START at " + connection.getURL());
            connection = recursion(connection.getURL(), 0);
            try {
                responseCode = connection.getResponseCode();
            } catch(Exception e) {
                System.out.println("ERROR: arg 1 repeatedly sent back response code " + responseCode + "!");
                return;
            }
            if (responseCode / 100 != 2) {
                System.out.println("ERROR: arg 1 sent back response code " + responseCode + "!");
                return;
            }
        } else if (responseCode / 100 == 2) {
            System.out.println("START at " + connection.getURL());
            URL tempURL = connection.getURL();
            connection = recursion(connection.getURL(), 0);
            if (!cache.contains(tempURL)) {
                addURL(tempURL);
            }
        } else {
            System.out.println("ERROR: arg 1 sent back response code " + responseCode + "!");
        }
        if (connection != null && connection.getResponseCode() / 100 == 2) {
            addURL(connection.getURL());
            String line;
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            int index = 0;
            int index2 = 1;
            Boolean cont = true;
            URL newStart = start;
            HttpURLConnection newConnection = null;
            int curHops = 0;
            String newURL = "";
            while((line = reader.readLine()) != null && curHops < numHops) {
                while (line.contains("a href=\"http") || line.contains("a href=\'http")) {
                    index = line.indexOf("a href=\"http") + 8;
                    index2 = line.indexOf("a href=\'http") + 8;
                    if (index == 7 || (index > index2 && index2 != 7)) {
                        index = index2;
                        index2 = line.indexOf("\'", index);
                    } else {
                        index2 = line.indexOf("\"", index);
                    }
                    newURL = line.substring(index, index2);
                    try {
                        newStart = new URL(newURL);
                        newConnection = recursion(newStart, 0);
                    } catch(Exception e) {
                        cont = false;
                    }
                    if (newConnection != null && !cache.contains(newConnection.getURL()) && cont && curHops < numHops) {
                        if (newConnection != null) {
                            curHops += 1;
                            connection = newConnection;
                            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                            addURL(newConnection.getURL());
                            System.out.println("HOP to "+newConnection.getURL());
                            line = "NOTHING";
                        } else {
                            line = line.substring(index);
                        }
                    } else {
                        line = line.substring(index);
                    }
                cont = true;
                }
            }
        } else {
            System.out.println("ERROR: arg 1 sent back response code " + connection.getResponseCode() + "!");
            return;
        }
        System.out.println("END!");
    }
    private static HttpURLConnection recursion(URL newStart, int timesTried) throws IOException {
        HttpURLConnection newConnection = null;
        int responseCode = 0;
        newConnection = (HttpURLConnection) newStart.openConnection();
        newConnection.setRequestMethod("GET");
        try {
            responseCode = newConnection.getResponseCode();
        } catch(Exception e) {
            return null;
        }
        if (cache.contains(newConnection.getURL())) {
            return null;
        }
        if (responseCode != HttpURLConnection.HTTP_OK) {
            if (responseCode / 100 == 3) {
                newStart = new URL(newConnection.getHeaderField("Location"));
                addURL(newConnection.getURL());
                newConnection = recursion(newStart, 0);
                if (cache.contains(newConnection.getURL())) {
                    return null;
                }
                System.out.println("REDIRECTHOP from URL " + newConnection.getURL() + " to URL " + newStart);
            } else if (responseCode / 100 == 5 && timesTried < 3) {
                System.out.println("RETRYHOP as URL " + newConnection.getURL() + " returned " + responseCode + " " + (timesTried + 1) + " time(s)");
                return newConnection = recursion(newStart, timesTried + 1);
            } else if (responseCode / 100 != 2) {
                System.out.println("BADHOP as URL " + newConnection.getURL() + " sent back response code " + responseCode);
                addURL(newConnection.getURL());
                return null;
            }
        }
        return newConnection;
    }
    private static void addURL(URL newURL) throws IOException {
        String URLstring = newURL.toString();
        cache.add(newURL);
        if (URLstring.substring(URLstring.length() - 1).equals("/")) {
            cache.add(new URL(URLstring.substring(0, URLstring.length() - 1)));
        } else {
            cache.add(new URL(URLstring + "/"));
        }
    }
}