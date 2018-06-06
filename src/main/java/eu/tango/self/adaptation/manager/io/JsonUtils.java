/**
 * Copyright 2018 University of Leeds
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 * This is being developed for the TANGO Project: http://tango-project.eu
 *
 */
package eu.tango.self.adaptation.manager.io;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * A utilities class for managing json input
 * @author Richard Kavanagh
 */
public abstract class JsonUtils {

    /**
     * This utility class is not expected to be instantiated. It is a collection
     * of static methods.
     */
    private JsonUtils() {
    }
    
    /**
     * This reads the entire contents from a reader and generates a String
     *
     * @param rd The reader to perform the full read with
     * @return The String representation of the contents of the reader.
     * @throws IOException
     */
    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    /**
     * This takes a url and parses the json from it into a Json object.
     *
     * @param url The url to parse
     * @return The json object provided by the named url
     * @throws IOException
     */
    public static JSONObject readJsonFromUrl(String url) throws IOException {
        try (InputStream is = new URL(url).openStream()) {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            String jsonText = readAll(rd);
            return new JSONObject(jsonText);
        }
    }

    /**
     * This takes a filename and parses the json from it into a Json array.
     *
     * @param filename The file to parse
     * @return The json array provided by the named file
     * @throws IOException
     */
    public static JSONArray readJsonArrayFromFile(String filename) throws IOException {
        FileReader fileReader = new FileReader(filename);
        BufferedReader rd = new BufferedReader(fileReader);
        String jsonText = readAll(rd);
        return new JSONArray(jsonText);
    }

    /**
     * This takes a filename and parses the json from it into a Json object.
     *
     * @param filename The file to parse
     * @return The json object provided by the named file
     * @throws IOException
     */
    public static JSONObject readJsonFromFile(String filename) throws IOException {
        FileReader fileReader = new FileReader(filename);
        BufferedReader rd = new BufferedReader(fileReader);
        String jsonText = readAll(rd);
        return new JSONObject(jsonText);
    }

    /**
     * This takes a url and parses the json from it into a Json object.
     *
     * @param url The url to parse
     * @param parameters The parameters to add to the query
     * @return The json object provided by the named url
     * @throws IOException
     */
    public static JSONObject readJsonFromUrl(String url, JSONObject parameters) throws IOException {
        if (parameters == null) {
            return readJsonFromUrl(url);
        } else {
            return readJsonFromUrl(url + "?q=" + parameters.toString());
        }
    }    
    
}
