package de.dragonrex.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class UserManager {
    private String path;
    private List<User> userList;

    public UserManager(String path) {
        this.path = path;
        this.userList = new ArrayList<>();
    }

    public void loadUser() {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            InputStream is = new FileInputStream(this.path);
            JsonNode root = objectMapper.readValue(is, JsonNode.class);
            if(root.get("users") != null) {
                for (JsonNode item : root.get("users")) {
                    this.userList.add(new User(
                       item.get("name").asText(),
                       item.get("password").asText()
                    ));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<User> getUserList() {
        return userList;
    }

    public String getPath() {
        return path;
    }
}
