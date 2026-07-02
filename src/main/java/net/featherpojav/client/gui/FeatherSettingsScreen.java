package net.featherpojav.client.gui;

import net.featherpojav.client.config.FeatherConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class FeatherSettingsScreen extends Screen {
    private final Screen parent;
    private Category currentCategory = Category.HUD;
    private final List<ToggleOption> options = new ArrayList<>();
    
    // Sidebar geometry
    private final int sidebarWidth = 100;
    
    // Scroll state
    private double scrollY = 0;
    
    public FeatherSettingsScreen(Screen parent) {
        super(Text.of("Feather Settings"));
        this.parent = parent;
    }
    
    enum Category {
        HUD("HUD Mods"),
        GAMEPLAY("Gameplay"),
        PERFORMANCE("Performance"),
        COSMETICS("Cosmetics");
        
        final String name;
        Category(String name) { this.name = name; }
    }
    
    private static class ToggleOption {
        String name;
        String desc;
        java.util.function.BooleanSupplier getter;
        java.util.function.Consumer<Boolean> setter;
        Category category;

        ToggleOption(String name, String desc, Category category, java.util.function.BooleanSupplier getter, java.util.function.Consumer<Boolean> setter) {
            this.name = name;
            this.desc = desc;
            this.category = category;
            this.getter = getter;
            this.setter = setter;
        }
    }
    
    @Override
    protected void init() {
        options.clear();
        FeatherConfig cfg = FeatherConfig.INSTANCE;
        
        // --- Category: HUD ---
        options.add(new ToggleOption("Keystrokes", "Shows WASD and CPS clicks on screen", Category.HUD, () -> cfg.keystrokes, (v) -> cfg.keystrokes = v));
        options.add(new ToggleOption("Armor HUD", "Displays equipped armor and durability", Category.HUD, () -> cfg.armorHUD, (v) -> cfg.armorHUD = v));
        options.add(new ToggleOption("Potion HUD", "Displays active status effects", Category.HUD, () -> cfg.potionHUD, (v) -> cfg.potionHUD = v));
        options.add(new ToggleOption("Coordinates HUD", "Displays current coordinates and direction", Category.HUD, () -> cfg.coordHUD, (v) -> cfg.coordHUD = v));
        options.add(new ToggleOption("Direction HUD", "Displays a compass bar at the top", Category.HUD, () -> cfg.directionHUD, (v) -> cfg.directionHUD = v));
        options.add(new ToggleOption("FPS HUD", "Displays your current Frames Per Second", Category.HUD, () -> cfg.fpsHUD, (v) -> cfg.fpsHUD = v));
        options.add(new ToggleOption("Combo Display", "Tracks PvP combo melee hits in real time", Category.HUD, () -> cfg.comboDisplay, (v) -> cfg.comboDisplay = v));
        options.add(new ToggleOption("Ping Display", "Tracks latency relative to the server", Category.HUD, () -> cfg.pingDisplay, (v) -> cfg.pingDisplay = v));
        options.add(new ToggleOption("Playtime", "Displays session and cumulative playtime", Category.HUD, () -> cfg.playtime, (v) -> cfg.playtime = v));
        options.add(new ToggleOption("Reach Display", "Measures block-distance of PvP hits", Category.HUD, () -> cfg.reachDisplay, (v) -> cfg.reachDisplay = v));
        options.add(new ToggleOption("Server Address", "Displays the server IP or address", Category.HUD, () -> cfg.serverAddress, (v) -> cfg.serverAddress = v));
        options.add(new ToggleOption("Speed Meter", "Measures horizontal and vertical velocity", Category.HUD, () -> cfg.speedMeter, (v) -> cfg.speedMeter = v));
        options.add(new ToggleOption("Stopwatch", "Start, freeze, and clear run timers", Category.HUD, () -> cfg.stopwatch, (v) -> cfg.stopwatch = v));
        options.add(new ToggleOption("Item Counter", "Counts held inventory item count dynamically", Category.HUD, () -> cfg.itemCounter, (v) -> cfg.itemCounter = v));
        options.add(new ToggleOption("Armor Bar", "Adds extra visual metrics to hotbar", Category.HUD, () -> cfg.armorBar, (v) -> cfg.armorBar = v));
        options.add(new ToggleOption("Armor Status", "Shows durability stats on active items", Category.HUD, () -> cfg.armorStatus, (v) -> cfg.armorStatus = v));
        options.add(new ToggleOption("Boss Bar", "Lets you scale, recolor, or position boss bars", Category.HUD, () -> cfg.bossBar, (v) -> cfg.bossBar = v));
        options.add(new ToggleOption("Hearts Multiplier", "Replaces stacked hearts with a numeric counter", Category.HUD, () -> cfg.hearts, (v) -> cfg.hearts = v));
        options.add(new ToggleOption("Pack Display", "Shows loaded resource pack thumbnail/name", Category.HUD, () -> cfg.packDisplay, (v) -> cfg.packDisplay = v));
        options.add(new ToggleOption("Scoreboard", "Scaling and visual options for scoreboard", Category.HUD, () -> cfg.scoreboard, (v) -> cfg.scoreboard = v));

        // --- Category: GAMEPLAY ---
        options.add(new ToggleOption("Auto Text", "Macro tool to assign commands to quick hotkeys", Category.GAMEPLAY, () -> cfg.autoText, (v) -> cfg.autoText = v));
        options.add(new ToggleOption("Auto Perspective", "Switches camera POV depending on actions", Category.GAMEPLAY, () -> cfg.autoPerspective, (v) -> cfg.autoPerspective = v));
        options.add(new ToggleOption("Block Indicator", "Overlays context info about target block face", Category.GAMEPLAY, () -> cfg.blockIndicator, (v) -> cfg.blockIndicator = v));
        options.add(new ToggleOption("Custom Advancements", "Replaces advancement pane with a grid layout", Category.GAMEPLAY, () -> cfg.customAdvancements, (v) -> cfg.customAdvancements = v));
        options.add(new ToggleOption("Custom Chat", "Toggles chat shadows and unlimited scroll history", Category.GAMEPLAY, () -> cfg.customChat, (v) -> cfg.customChat = v));
        options.add(new ToggleOption("Death Info", "Logs exact coordinate points upon dying", Category.GAMEPLAY, () -> cfg.deathInfo, (v) -> cfg.deathInfo = v));
        options.add(new ToggleOption("Drop Prevention", "Locks hotbar items to prevent accidental drops", Category.GAMEPLAY, () -> cfg.dropPrevention, (v) -> cfg.dropPrevention = v));
        options.add(new ToggleOption("Elytras", "Utility flight indicators and armor swapping", Category.GAMEPLAY, () -> cfg.elytras, (v) -> cfg.elytras = v));
        options.add(new ToggleOption("FOV Changer", "Adjusts dynamic FOV sprint/speed distortions", Category.GAMEPLAY, () -> cfg.fovChanger, (v) -> cfg.fovChanger = v));
        options.add(new ToggleOption("Hit Indicator", "Spawns red indicators showing source of damage", Category.GAMEPLAY, () -> cfg.hitIndicator, (v) -> cfg.hitIndicator = v));
        options.add(new ToggleOption("Horses", "Overlays statistics directly above horses", Category.GAMEPLAY, () -> cfg.horses, (v) -> cfg.horses = v));
        options.add(new ToggleOption("Hypixel Utilities", "AutoGG, AutoTip, and stats tracking for Hypixel", Category.GAMEPLAY, () -> cfg.hypixelUtilities, (v) -> cfg.hypixelUtilities = v));
        options.add(new ToggleOption("Inventory Management", "Quick sorting and fast-transfer inventory tweaks", Category.GAMEPLAY, () -> cfg.inventoryManagement, (v) -> cfg.inventoryManagement = v));
        options.add(new ToggleOption("Item Info", "Visual pop-up of item enchantments on pickup", Category.GAMEPLAY, () -> cfg.itemInfo, (v) -> cfg.itemInfo = v));
        options.add(new ToggleOption("Jump Reset", "PvP feedback tracker for jump reset timing", Category.GAMEPLAY, () -> cfg.jumpReset, (v) -> cfg.jumpReset = v));
        options.add(new ToggleOption("Auto Reconnect", "Loops login attempts on server disconnects", Category.GAMEPLAY, () -> cfg.reconnect, (v) -> cfg.reconnect = v));
        options.add(new ToggleOption("Saturation Tooltips", "Extends tooltips with food saturation metrics", Category.GAMEPLAY, () -> cfg.saturation, (v) -> cfg.saturation = v));
        options.add(new ToggleOption("Screenshot Utility", "Enables clipboard copying and quick uploading", Category.GAMEPLAY, () -> cfg.screenshotUtility, (v) -> cfg.screenshotUtility = v));
        options.add(new ToggleOption("Search Keybinds", "Search overlay to look up keybind maps", Category.GAMEPLAY, () -> cfg.searchKeybind, (v) -> cfg.searchKeybind = v));
        options.add(new ToggleOption("Snaplook", "Toggle keybind to instantly snap third-person view", Category.GAMEPLAY, () -> cfg.snaplook, (v) -> cfg.snaplook = v));
        options.add(new ToggleOption("ToggleSprint", "Automatically keep sprinting", Category.GAMEPLAY, () -> cfg.toggleSprint, (v) -> cfg.toggleSprint = v));
        options.add(new ToggleOption("Zoom", "OptiFine-style zoom using customizable keybind", Category.GAMEPLAY, () -> cfg.zoom, (v) -> cfg.zoom = v));
        options.add(new ToggleOption("Perspective (Freelook)", "Decouples camera movement from player rotation", Category.GAMEPLAY, () -> cfg.freelook, (v) -> cfg.freelook = v));
        options.add(new ToggleOption("AutoGG", "Sends GG in chat after multiplayer games", Category.GAMEPLAY, () -> cfg.autoGG, (v) -> cfg.autoGG = v));

        // --- Category: PERFORMANCE ---
        options.add(new ToggleOption("Backups", "Manages world safety backups automatically", Category.PERFORMANCE, () -> cfg.backups, (v) -> cfg.backups = v));
        options.add(new ToggleOption("Cull Logs", "Purges old log files to save drive space", Category.PERFORMANCE, () -> cfg.cullLogs, (v) -> cfg.cullLogs = v));
        options.add(new ToggleOption("Custom Fog", "Allows configuration of render fog distances", Category.PERFORMANCE, () -> cfg.customFog, (v) -> cfg.customFog = v));
        options.add(new ToggleOption("Item Despawn", "Flashes items when their despawn timer is near", Category.PERFORMANCE, () -> cfg.itemDespawn, (v) -> cfg.itemDespawn = v));

        // --- Category: COSMETICS ---
        options.add(new ToggleOption("Animations Mod", "1.7 animations, block hitting tweaks", Category.COSMETICS, () -> cfg.animations, (v) -> cfg.animations = v));
        options.add(new ToggleOption("Block Highlight Overlay", "Outline or fill colors for focused blocks", Category.COSMETICS, () -> cfg.blockOverlay, (v) -> cfg.blockOverlay = v));
        options.add(new ToggleOption("Fullbright", "Forces max brightness globally", Category.COSMETICS, () -> cfg.fullbright, (v) -> cfg.fullbright = v));
        options.add(new ToggleOption("Camera Overrides", "Cinematic pan and camera smoothing parameters", Category.COSMETICS, () -> cfg.camera, (v) -> cfg.camera = v));
        options.add(new ToggleOption("Color Saturation", "Enhance or deepen world colors via slider", Category.COSMETICS, () -> cfg.colorSaturation, (v) -> cfg.colorSaturation = v));
        options.add(new ToggleOption("Custom Crosshair", "Interactive custom crosshair design layout", Category.COSMETICS, () -> cfg.customCrosshair, (v) -> cfg.customCrosshair = v));
        options.add(new ToggleOption("Custom F3 Info", "Clutters out vanilla debug overlay values", Category.COSMETICS, () -> cfg.customF3, (v) -> cfg.customF3 = v));
        options.add(new ToggleOption("Damage Indicator", "Draws health values above targeted mobs", Category.COSMETICS, () -> cfg.damageIndicator, (v) -> cfg.damageIndicator = v));
        options.add(new ToggleOption("Dark Mode", "Dark themed container screen overlay textures", Category.COSMETICS, () -> cfg.darkMode, (v) -> cfg.darkMode = v));
        options.add(new ToggleOption("Discord Rich Presence", "Custom play states on your Discord client", Category.COSMETICS, () -> cfg.discordRPC, (v) -> cfg.discordRPC = v));
        options.add(new ToggleOption("Enchantment Glint", "Custom color and velocity for items glow", Category.COSMETICS, () -> cfg.glint, (v) -> cfg.glint = v));
        options.add(new ToggleOption("Hitbox Outlines", "Draws visual hitbox outlines around entities", Category.COSMETICS, () -> cfg.hitbox, (v) -> cfg.hitbox = v));
        options.add(new ToggleOption("Light Level Overlay", "Displays light grid values to prevent spawns", Category.COSMETICS, () -> cfg.lightLevel, (v) -> cfg.lightLevel = v));
        options.add(new ToggleOption("Loot Beams", "Spawns beacons of light on dropped items", Category.COSMETICS, () -> cfg.lootBeams, (v) -> cfg.lootBeams = v));
        options.add(new ToggleOption("Mob Outlines", "Outlines targeted or nearby passive/hostile mobs", Category.COSMETICS, () -> cfg.mobOverlay, (v) -> cfg.mobOverlay = v));
        options.add(new ToggleOption("Motion Blur", "Applies smooth motion blur during movements", Category.COSMETICS, () -> cfg.motionBlur, (v) -> cfg.motionBlur = v));
        options.add(new ToggleOption("Mousestrokes", "Monitors mouse physical velocity and vector", Category.COSMETICS, () -> cfg.mousestrokes, (v) -> cfg.mousestrokes = v));
        options.add(new ToggleOption("Nametags Customizer", "Modifies player nametag scale and opacity", Category.COSMETICS, () -> cfg.nametags, (v) -> cfg.nametags = v));
        options.add(new ToggleOption("Nick Hider", "Spoofs usernames and skins locally", Category.COSMETICS, () -> cfg.nickHider, (v) -> cfg.nickHider = v));
        options.add(new ToggleOption("Pack Organizer", "Resource pack foldering and search structures", Category.COSMETICS, () -> cfg.packOrganizer, (v) -> cfg.packOrganizer = v));
        options.add(new ToggleOption("Sound Reverb Filters", "Implements echoes and low-pass underwater filter", Category.COSMETICS, () -> cfg.soundFilters, (v) -> cfg.soundFilters = v));
        options.add(new ToggleOption("Item Physics", "Dropped items fall flat on the ground realistically", Category.COSMETICS, () -> cfg.itemPhysics, (v) -> cfg.itemPhysics = v));
        
        // Setup buttons
        this.addDrawableChild(ButtonWidget.builder(Text.of("Edit HUD Layout"), button -> {
            if (this.client != null) {
                this.client.setScreen(new FeatherHudEditorScreen(this));
            }
        }).dimensions(10, this.height - 50, sidebarWidth - 20, 20).build());
        
        this.addDrawableChild(ButtonWidget.builder(Text.of("Back"), button -> {
            if (this.client != null) {
                this.client.setScreen(parent);
            }
        }).dimensions(10, this.height - 25, sidebarWidth - 20, 20).build());
    }

    private int getCategoryOptionCount() {
        int count = 0;
        for (ToggleOption opt : options) {
            if (opt.category == currentCategory) {
                count++;
            }
        }
        return count;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseX > sidebarWidth) {
            int totalHeight = getCategoryOptionCount() * 45;
            double maxScroll = Math.max(0, totalHeight - (this.height - 30));
            scrollY -= verticalAmount * 15;
            if (scrollY < 0) scrollY = 0;
            if (scrollY > maxScroll) scrollY = maxScroll;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Modern Feather Client themed background rendering
        // Left Sidebar: Darker purple
        context.fill(0, 0, sidebarWidth, this.height, 0xFF140D1A);
        
        // Right Main Area: Sleek dark grey
        context.fill(sidebarWidth, 0, this.width, this.height, 0xFF18181A);
        
        // Title
        context.drawText(this.textRenderer, "FEATHER CLIENT", 10, 15, 0xFF9C27B0, true);
        context.drawText(this.textRenderer, "Pojav Edition", 10, 27, 0xFF6A1B9A, true);
        
        // Render Sidebar Category Selection Buttons
        int categoryY = 50;
        for (Category cat : Category.values()) {
            boolean isSelected = cat == currentCategory;
            boolean isHovered = mouseX >= 5 && mouseX <= sidebarWidth - 5 && mouseY >= categoryY && mouseY <= categoryY + 20;
            
            int color = isSelected ? 0xFF9C27B0 : (isHovered ? 0xFF311B92 : 0x00000000);
            if (color != 0x00000000) {
                context.fill(5, categoryY, sidebarWidth - 5, categoryY + 20, color);
            }
            
            context.drawText(this.textRenderer, cat.name, 12, categoryY + 6, isSelected ? 0xFFFFFFFF : 0xFFCCCCCC, false);
            categoryY += 25;
        }
        
        // Render Category Contents (with Scrolling support)
        int optionY = 20 - (int) scrollY;
        int listLeft = sidebarWidth + 20;
        int listWidth = this.width - listLeft - 20;
        
        // Enable scissoring to prevent options drawing over header or footer
        context.enableScissor(listLeft - 5, 5, listLeft + listWidth + 5, this.height - 5);
        
        for (ToggleOption opt : options) {
            if (opt.category != currentCategory) continue;
            
            // Only render if visible on screen
            if (optionY + 40 >= 0 && optionY <= this.height) {
                boolean isHovered = mouseX >= listLeft && mouseX <= listLeft + listWidth && mouseY >= optionY && mouseY <= optionY + 40;
                context.fill(listLeft, optionY, listLeft + listWidth, optionY + 40, isHovered ? 0xFF2A2A2E : 0xFF222224);
                context.drawBorder(listLeft, optionY, listWidth, 40, 0xFF3E3E42);
                
                // Name & Desc
                context.drawText(this.textRenderer, opt.name, listLeft + 10, optionY + 8, 0xFFFFFFFF, false);
                context.drawText(this.textRenderer, opt.desc, listLeft + 10, optionY + 22, 0xFF888888, false);
                
                // Toggle Switch rendering (Pill shape)
                boolean enabled = opt.getter.getAsBoolean();
                int toggleX = listLeft + listWidth - 50;
                int toggleY = optionY + 12;
                
                // Draw Toggle background
                context.fill(toggleX, toggleY, toggleX + 40, toggleY + 16, enabled ? 0xFF9C27B0 : 0xFF4A4A4F);
                // Draw Toggle handle
                int handleX = enabled ? toggleX + 24 : toggleX + 2;
                context.fill(handleX, toggleY + 2, handleX + 14, toggleY + 14, 0xFFFFFFFF);
            }
            
            optionY += 45;
        }
        
        context.disableScissor();
        
        // Draw Scroll Bar indicator if list overflows
        int totalHeight = getCategoryOptionCount() * 45;
        if (totalHeight > this.height - 30) {
            int scrollBarHeight = (int) (((double) (this.height - 30) / totalHeight) * (this.height - 30));
            int scrollBarY = 15 + (int) ((scrollY / (totalHeight - (this.height - 30))) * (this.height - 30 - scrollBarHeight));
            context.fill(this.width - 6, scrollBarY, this.width - 2, scrollBarY + scrollBarHeight, 0xFF555555);
        }
        
        super.render(context, mouseX, mouseY, delta);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Handle Sidebar Category Clicks
        int categoryY = 50;
        for (Category cat : Category.values()) {
            if (mouseX >= 5 && mouseX <= sidebarWidth - 5 && mouseY >= categoryY && mouseY <= categoryY + 20) {
                currentCategory = cat;
                scrollY = 0; // Reset scroll on category change
                return true;
            }
            categoryY += 25;
        }
        
        // Handle Option Toggle clicks (incorporating scrollY offset)
        int optionY = 20 - (int) scrollY;
        int listLeft = sidebarWidth + 20;
        int listWidth = this.width - listLeft - 20;
        
        for (ToggleOption opt : options) {
            if (opt.category != currentCategory) continue;
            
            // Verify option is drawn on screen
            if (optionY + 40 >= 5 && optionY <= this.height - 5) {
                // If toggle switch area is clicked
                int toggleX = listLeft + listWidth - 50;
                int toggleY = optionY + 12;
                if (mouseX >= toggleX && mouseX <= toggleX + 40 && mouseY >= toggleY && mouseY <= toggleY + 16) {
                    opt.setter.accept(!opt.getter.getAsBoolean());
                    FeatherConfig.save();
                    return true;
                }
            }
            
            optionY += 45;
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Do nothing to prevent background blur
    }
    
    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }
}
