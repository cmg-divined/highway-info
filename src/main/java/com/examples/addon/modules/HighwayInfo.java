package com.examples.addon.modules;

import com.examples.addon.AddonTemplates;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.Vec3d;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class HighwayInfo extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> updateFrequency = sgGeneral.add(new IntSetting.Builder()
        .name("update-frequency")
        .description("How often to update the nearest highway calculation in ticks.")
        .defaultValue(20)
        .min(1)
        .sliderRange(1, 100)
        .build()
    );


    // Data structures for highways
    public static class Highway {
        public String name;
        public String state;
        public int[] from = new int[2];
        public int[] to = new int[2];
        public double distance; // Distance from player to nearest point

        @Override
        public String toString() {
            String stateStr = state != null ? state : "unknown";
            return name + " (" + stateStr + "): " + from[0] + "," + from[1] + " → " + to[0] + "," + to[1];
        }
    }

    private List<Highway> highways = new ArrayList<>();
    private Highway nearestHighway = null;
    private int tickCounter = 0;

    public HighwayInfo() {
        super(AddonTemplates.CATEGORY, "highway-info", "Shows information about the nearest highway.");
    }

    @Override
    public void onActivate() {
        loadHighways();
        tickCounter = 0;
    }

    private void loadHighways() {
        highways.clear();
        
        try {
            // Try to load from file first
            Path highwaysPath = Paths.get("highways.json");
            BufferedReader reader;
            
            if (Files.exists(highwaysPath)) {
                reader = new BufferedReader(new FileReader(highwaysPath.toFile()));
            } else {
                // Fallback to resource
                reader = new BufferedReader(new InputStreamReader(
                    HighwayInfo.class.getResourceAsStream("/highways.json")));
            }
            
            Gson gson = new Gson();
            JsonArray jsonArray = gson.fromJson(reader, JsonArray.class);
            
            for (JsonElement element : jsonArray) {
                JsonObject obj = element.getAsJsonObject();
                Highway highway = new Highway();
                
                highway.name = obj.has("name") ? obj.get("name").getAsString() : "Unknown";
                highway.state = obj.has("state") ? obj.get("state").getAsString() : null;
                
                if (obj.has("from")) {
                    JsonArray from = obj.getAsJsonArray("from");
                    highway.from[0] = from.get(0).getAsInt();
                    highway.from[1] = from.get(1).getAsInt();
                }
                
                if (obj.has("to")) {
                    JsonArray to = obj.getAsJsonArray("to");
                    highway.to[0] = to.get(0).getAsInt();
                    highway.to[1] = to.get(1).getAsInt();
                }
                
                highways.add(highway);
            }
            
            reader.close();
            info("Loaded " + highways.size() + " highways from highways.json");
        } catch (IOException e) {
            error("Failed to load highways: " + e.getMessage());
        } catch (Exception e) {
            error("Error parsing highways.json: " + e.getMessage());
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null) return;
        
        // freq we wanna update at, suggested 20 but for realtime just do 1
        if (tickCounter++ >= updateFrequency.get()) {
            updateNearestHighway();
            tickCounter = 0;
        }
    }

    /**
     * Updates the nearest highway based on player position
     */
    private void updateNearestHighway() {
        if (highways.isEmpty() || mc.player == null) return;
        
        Vec3d playerPos = mc.player.getPos();
        double playerX = playerPos.x;
        double playerZ = playerPos.z;
        
        double minDistance = Double.MAX_VALUE;
        Highway closest = null;
        
        for (Highway highway : highways) {
            double distance = getDistanceToHighway(playerX, playerZ, highway);
            
            // Update the highway's distance
            highway.distance = distance;
            
            if (distance < minDistance) {
                minDistance = distance;
                closest = highway;
            }
        }
        
        nearestHighway = closest;
    }

    /**
     * min distance from a point to a line segment (highway)
     */
    private double getDistanceToHighway(double playerX, double playerZ, Highway highway) {
        double x1 = highway.from[0];
        double z1 = highway.from[1];
        double x2 = highway.to[0];
        double z2 = highway.to[1];
        
        // If the line is a point, return distance to the point
        if (x1 == x2 && z1 == z2) {
            return Math.sqrt(Math.pow(playerX - x1, 2) + Math.pow(playerZ - z1, 2));
        }
        
        // Calculate the projection of the point onto the line
        double lineLength = Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(z2 - z1, 2));
        double t = ((playerX - x1) * (x2 - x1) + (playerZ - z1) * (z2 - z1)) / (lineLength * lineLength);
        
        // Clamp t to the range [0, 1] to get a point on the segment
        t = Math.max(0, Math.min(1, t));
        
        // Calculate the nearest point on the segment
        double nearestX = x1 + t * (x2 - x1);
        double nearestZ = z1 + t * (z2 - z1);
        
        // Return the distance to the nearest point
        return Math.sqrt(Math.pow(playerX - nearestX, 2) + Math.pow(playerZ - nearestZ, 2));
    }

    /**
     * grab information about the nearest highway for the HUD
     */
    public Highway getNearestHighway() {
        return nearestHighway;
    }

    /**
     * return string of the nearest highway for display
     */
    @Override
    public String getInfoString() {
        if (nearestHighway == null) return "None";
        
        String distance = String.format("%.1f", nearestHighway.distance);
        return nearestHighway.name + " (" + distance + "m)";
    }
}