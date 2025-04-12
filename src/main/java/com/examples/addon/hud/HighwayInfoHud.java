package com.examples.addon.hud;

import com.examples.addon.AddonTemplates;
import com.examples.addon.modules.HighwayInfo;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

public class HighwayInfoHud extends HudElement {
    public static final HudElementInfo<HighwayInfoHud> INFO = new HudElementInfo<>(AddonTemplates.HUD_GROUP, "highway-info", "Displays information about the nearest highway.", HighwayInfoHud::new);

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgColors = settings.createGroup("Colors");

    // General settings
    private final Setting<Boolean> shadow = sgGeneral.add(new BoolSetting.Builder()
        .name("shadow")
        .description("Draws text shadow.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> background = sgGeneral.add(new BoolSetting.Builder()
        .name("background")
        .description("Draws background.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> borderWidth = sgGeneral.add(new IntSetting.Builder()
        .name("border-width")
        .description("Width of the border.")
        .defaultValue(0)
        .min(0)
        .sliderRange(0, 5)
        .visible(background::get)
        .build()
    );

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("scale")
        .description("Scale of the text.")
        .defaultValue(1.0)
        .min(0.5)
        .sliderRange(0.5, 3)
        .build()
    );

    // Color settings
    private final Setting<SettingColor> titleColor = sgColors.add(new ColorSetting.Builder()
        .name("title-color")
        .description("Color of the title text.")
        .defaultValue(new SettingColor(255, 255, 255))
        .build()
    );

    private final Setting<SettingColor> infoColor = sgColors.add(new ColorSetting.Builder()
        .name("info-color")
        .description("Color of the info text.")
        .defaultValue(new SettingColor(175, 175, 175))
        .build()
    );

    private final Setting<SettingColor> backgroundColor = sgColors.add(new ColorSetting.Builder()
        .name("background-color")
        .description("Color of the background.")
        .defaultValue(new SettingColor(0, 0, 0, 128))
        .visible(background::get)
        .build()
    );

    private final Setting<SettingColor> borderColor = sgColors.add(new ColorSetting.Builder()
        .name("border-color")
        .description("Color of the border.")
        .defaultValue(new SettingColor(175, 175, 175, 128))
        .visible(() -> background.get() && borderWidth.get() > 0)
        .build()
    );

    private HighwayInfo highwayInfoModule;

    public HighwayInfoHud() {
        super(INFO);
    }

    @Override
    public void render(HudRenderer renderer) {
        if (highwayInfoModule == null) {
            highwayInfoModule = Modules.get().get(HighwayInfo.class);
            if (highwayInfoModule == null) {
                String text = "Highway Info module not found";
                setSize(renderer.textWidth(text, shadow.get(), scale.get()), renderer.textHeight(shadow.get(), scale.get()));
                
                if (background.get()) {
                    renderer.quad(x, y, getWidth(), getHeight(), backgroundColor.get());
                    if (borderWidth.get() > 0) {
                        renderer.quad(x - borderWidth.get(), y - borderWidth.get(), getWidth() + 2 * borderWidth.get(), borderWidth.get(), borderColor.get()); // Top
                        renderer.quad(x - borderWidth.get(), y, borderWidth.get(), getHeight(), borderColor.get()); // Left
                        renderer.quad(x + getWidth(), y, borderWidth.get(), getHeight(), borderColor.get()); // Right
                        renderer.quad(x - borderWidth.get(), y + getHeight(), getWidth() + 2 * borderWidth.get(), borderWidth.get(), borderColor.get()); // Bottom
                    }
                }
                
                renderer.text(text, x, y, titleColor.get(), shadow.get(), scale.get());
                return;
            }
        }

        if (!highwayInfoModule.isActive()) {
            String text = "Highway Info module disabled";
            setSize(renderer.textWidth(text, shadow.get(), scale.get()), renderer.textHeight(shadow.get(), scale.get()));
            
            if (background.get()) {
                renderer.quad(x, y, getWidth(), getHeight(), backgroundColor.get());
                if (borderWidth.get() > 0) {
                    renderer.quad(x - borderWidth.get(), y - borderWidth.get(), getWidth() + 2 * borderWidth.get(), borderWidth.get(), borderColor.get()); // Top
                    renderer.quad(x - borderWidth.get(), y, borderWidth.get(), getHeight(), borderColor.get()); // Left
                    renderer.quad(x + getWidth(), y, borderWidth.get(), getHeight(), borderColor.get()); // Right
                    renderer.quad(x - borderWidth.get(), y + getHeight(), getWidth() + 2 * borderWidth.get(), borderWidth.get(), borderColor.get()); // Bottom
                }
            }
            
            renderer.text(text, x, y, titleColor.get(), shadow.get(), scale.get());
            return;
        }

        // Get highway data from the module
        HighwayInfo.Highway highway = highwayInfoModule.getNearestHighway();
        if (highway == null) {
            String text = "No highway in range";
            setSize(renderer.textWidth(text, shadow.get(), scale.get()), renderer.textHeight(shadow.get(), scale.get()));
            
            if (background.get()) {
                renderer.quad(x, y, getWidth(), getHeight(), backgroundColor.get());
                if (borderWidth.get() > 0) {
                    renderer.quad(x - borderWidth.get(), y - borderWidth.get(), getWidth() + 2 * borderWidth.get(), borderWidth.get(), borderColor.get()); // Top
                    renderer.quad(x - borderWidth.get(), y, borderWidth.get(), getHeight(), borderColor.get()); // Left
                    renderer.quad(x + getWidth(), y, borderWidth.get(), getHeight(), borderColor.get()); // Right
                    renderer.quad(x - borderWidth.get(), y + getHeight(), getWidth() + 2 * borderWidth.get(), borderWidth.get(), borderColor.get()); // Bottom
                }
            }
            
            renderer.text(text, x, y, titleColor.get(), shadow.get(), scale.get());
            return;
        }

        // Lines of text to display
        String name = highway.name;
        String state = highway.state != null ? formatState(highway.state) : "";
        String distance = String.format("%.1f blocks", highway.distance);
        String from = String.format("From: %d, %d", highway.from[0], highway.from[1]);
        String to = String.format("To: %d, %d", highway.to[0], highway.to[1]);

        // Calculate dimensions
        double nameWidth = renderer.textWidth(name, shadow.get(), scale.get());
        double stateWidth = highway.state != null ? renderer.textWidth(state, shadow.get(), scale.get()) : 0;
        double distanceWidth = renderer.textWidth(distance, shadow.get(), scale.get());
        double fromWidth = renderer.textWidth(from, shadow.get(), scale.get());
        double toWidth = renderer.textWidth(to, shadow.get(), scale.get());

        double width = Math.max(nameWidth, Math.max(stateWidth, Math.max(distanceWidth, Math.max(fromWidth, toWidth))));
        double height = renderer.textHeight(shadow.get(), scale.get()) * (highway.state != null ? 5 : 4); // 4 or 5 lines depending on state presence

        // Set size
        setSize(width, height);

        // Render background and border
        if (background.get()) {
            renderer.quad(x, y, getWidth(), getHeight(), backgroundColor.get());
            if (borderWidth.get() > 0) {
                renderer.quad(x - borderWidth.get(), y - borderWidth.get(), getWidth() + 2 * borderWidth.get(), borderWidth.get(), borderColor.get()); // Top
                renderer.quad(x - borderWidth.get(), y, borderWidth.get(), getHeight(), borderColor.get()); // Left
                renderer.quad(x + getWidth(), y, borderWidth.get(), getHeight(), borderColor.get()); // Right
                renderer.quad(x - borderWidth.get(), y + getHeight(), getWidth() + 2 * borderWidth.get(), borderWidth.get(), borderColor.get()); // Bottom
            }
        }

        // Render text
        double lineHeight = renderer.textHeight(shadow.get(), scale.get());
        double curY = y;

        renderer.text(name, x, curY, infoColor.get(), shadow.get(), scale.get());
        curY += lineHeight;

        if (highway.state != null) {
            renderer.text(state, x, curY, infoColor.get(), shadow.get(), scale.get());
            curY += lineHeight;
        }

        renderer.text(distance, x, curY, infoColor.get(), shadow.get(), scale.get());
        curY += lineHeight;

        renderer.text(from, x, curY, infoColor.get(), shadow.get(), scale.get());
        curY += lineHeight;

        renderer.text(to, x, curY, infoColor.get(), shadow.get(), scale.get());
    }

    private String formatState(String state) {
        if (state == null) return "Unknown";
        
        // Format state string (e.g., "dug_wide" -> "Dug Wide")
        String[] words = state.split("_");
        StringBuilder result = new StringBuilder();
        
        for (String word : words) {
            if (word.isEmpty()) continue;
            result.append(Character.toUpperCase(word.charAt(0)))
                  .append(word.substring(1).toLowerCase())
                  .append(" ");
        }
        
        return result.toString().trim();
    }
}
