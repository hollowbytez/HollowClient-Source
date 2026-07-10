package net.hollowclient.client.gui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.systems.RenderSystem;
import net.hollowclient.client.HollowClient;
import net.hollowclient.client.config.HollowConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

public class HollowAuthScreen extends Screen {
    public static boolean isSessionVerified = false;

    private TextFieldWidget keyInputField;
    private ButtonWidget activateButton;

    private String statusMessage = "Connecting to authentication server...";
    private boolean isChecking = false;
    private boolean needsInput = false;
    private boolean showDownloadProgress = false;
    private float downloadPercent = 0.0f;
    private String errorMessage = null;

    private final List<FloatingParticle> particles = new ArrayList<>();

    public HollowAuthScreen() {
        super(Text.of("Hollow Client Activation"));
    }

    private static class FloatingParticle {
        float x, y, speed, size, alpha;
        FloatingParticle(float width, float height) {
            x = (float) (Math.random() * width);
            y = (float) (Math.random() * height);
            speed = 0.1f + (float) (Math.random() * 0.4f);
            size = 1.0f + (float) (Math.random() * 1.5f);
            alpha = 0.1f + (float) (Math.random() * 0.4f);
        }
        void tick(float width, float height) {
            y -= speed;
            x += (float) Math.sin(y * 0.02) * 0.2f;
            if (y < -10) {
                y = height + 10;
                x = (float) (Math.random() * width);
            }
        }
    }

    @Override
    protected void init() {
        particles.clear();
        for (int i = 0; i < 50; i++) {
            particles.add(new FloatingParticle(this.width, this.height));
        }

        int cx = this.width / 2;
        int cy = this.height / 2;

        // Custom Key Text Field
        this.keyInputField = new TextFieldWidget(this.textRenderer, cx - 110, cy - 10, 220, 20, Text.of("License Key"));
        this.keyInputField.setMaxLength(36);
        this.keyInputField.setText(HollowConfig.INSTANCE.licenseKey);
        this.addSelectableChild(this.keyInputField);

        // Custom Activate Button
        this.activateButton = ButtonWidget.builder(Text.of("Activate Client"), b -> {
            String enteredKey = this.keyInputField.getText().trim();
            if (!enteredKey.isEmpty()) {
                startCheck(enteredKey);
            } else {
                this.errorMessage = "Please enter a license key!";
            }
        }).dimensions(cx - 110, cy + 20, 220, 20).build();
        
        this.addDrawableChild(this.activateButton);

        // Run verification on start if key exists in config
        if (!isChecking && !isSessionVerified) {
            String savedKey = HollowConfig.INSTANCE.licenseKey.trim();
            if (!savedKey.isEmpty()) {
                startCheck(savedKey);
            } else {
                this.needsInput = true;
                this.isChecking = false;
            }
        }
    }

    private void startCheck(String key) {
        this.isChecking = true;
        this.needsInput = false;
        this.errorMessage = null;
        this.statusMessage = "Verifying license key...";

        new Thread(() -> {
            try {
                String hwid = getHWID();
                String username = this.client != null && this.client.getSession() != null ? this.client.getSession().getUsername() : "Unknown";

                // 1. Verify Key & HWID
                String verifyUrl = "https://hollowclient-source.onrender.com/api/verify";
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest verifyRequest = HttpRequest.newBuilder()
                        .uri(URI.create(verifyUrl))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString("{\"key\":\"" + key + "\",\"hwid\":\"" + hwid + "\",\"username\":\"" + username + "\"}"))
                        .build();

                HttpResponse<String> verifyResponse = client.send(verifyRequest, HttpResponse.BodyHandlers.ofString());

                if (verifyResponse.statusCode() != 200) {
                    JsonObject json = JsonParser.parseString(verifyResponse.body()).getAsJsonObject();
                    errorMessage = json.has("message") ? json.get("message").getAsString() : "Activation failed!";
                    isChecking = false;
                    needsInput = true;
                    return;
                }

                // Save key to config
                HollowConfig.INSTANCE.licenseKey = key;
                HollowConfig.save();

                // 2. Check for updates
                this.statusMessage = "Checking for client updates...";
                String currentVersion = "1.0.0";
                String updateUrl = "https://hollowclient-source.onrender.com/api/check-update?version=" + currentVersion;
                HttpRequest updateRequest = HttpRequest.newBuilder()
                        .uri(URI.create(updateUrl))
                        .GET()
                        .build();

                HttpResponse<String> updateResponse = client.send(updateRequest, HttpResponse.BodyHandlers.ofString());

                if (updateResponse.statusCode() == 200) {
                    JsonObject updateJson = JsonParser.parseString(updateResponse.body()).getAsJsonObject();
                    if (updateJson.has("updateAvailable") && updateJson.get("updateAvailable").getAsBoolean()) {
                        String downloadUrl = updateJson.get("downloadUrl").getAsString();
                        String latestVersion = updateJson.get("latestVersion").getAsString();
                        boolean mandatory = updateJson.get("mandatory").getAsBoolean();

                        if (mandatory) {
                            downloadAndUpdate(downloadUrl, latestVersion);
                            return;
                        }
                    }
                }

                // 3. Go to Home Screen
                isSessionVerified = true;
                if (this.client != null) {
                    this.client.execute(() -> this.client.setScreen(new HollowHomeScreen()));
                }

            } catch (Exception e) {
                errorMessage = "Connection error! Check your network.";
                isChecking = false;
                needsInput = true;
                e.printStackTrace();
            }
        }).start();
    }

    private void downloadAndUpdate(String downloadUrl, String latestVersion) {
        this.showDownloadProgress = true;
        this.statusMessage = "Mandatory update found! Downloading version " + latestVersion + "...";

        try {
            URL url = new URI(downloadUrl).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.connect();

            int fileLength = conn.getContentLength();
            InputStream input = new BufferedInputStream(url.openStream(), 8192);

            File currentJar = new File(HollowClient.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            
            // Dev Environment Bypass
            if (!currentJar.getName().endsWith(".jar")) {
                this.statusMessage = "Development workspace detected. Skipping auto-update replace.";
                Thread.sleep(1500);
                isSessionVerified = true;
                if (this.client != null) {
                    this.client.execute(() -> this.client.setScreen(new HollowHomeScreen()));
                }
                return;
            }

            File parentDir = currentJar.getParentFile();
            File newJar = new File(parentDir, currentJar.getName() + ".new");
            OutputStream output = new FileOutputStream(newJar);

            byte[] data = new byte[4096];
            long total = 0;
            int count;
            while ((count = input.read(data)) != -1) {
                total += count;
                if (fileLength > 0) {
                    this.downloadPercent = (float) total / fileLength;
                }
                output.write(data, 0, count);
            }

            output.flush();
            output.close();
            input.close();

            this.statusMessage = "Update complete! Applying files...";
            Thread.sleep(1000);

            // Shutdown hook to swap JAR files on exit
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    String os = System.getProperty("os.name").toLowerCase();
                    if (os.contains("win")) {
                        String cmd = "cmd.exe /c ping 127.0.0.1 -n 2 > nul && del /f /q \"" + currentJar.getAbsolutePath() + "\" && move /y \"" + newJar.getAbsolutePath() + "\" \"" + currentJar.getAbsolutePath() + "\"";
                        Runtime.getRuntime().exec(cmd);
                    } else {
                        currentJar.delete();
                        newJar.renameTo(currentJar);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }));

            // Quit game to trigger restart
            System.exit(0);

        } catch (Exception e) {
            errorMessage = "Failed to download update: " + e.getMessage();
            isChecking = false;
            needsInput = true;
            showDownloadProgress = false;
            e.printStackTrace();
        }
    }

    private static String getHWID() {
        try {
            String mainString = System.getProperty("os.name") +
                                System.getProperty("os.arch") +
                                System.getProperty("user.name") +
                                System.getenv("PROCESSOR_IDENTIFIER") +
                                System.getenv("NUMBER_OF_PROCESSORS");
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(mainString.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString().substring(0, 16).toUpperCase();
        } catch (Exception e) {
            return "FALLBACK-HWID-1234";
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.needsInput) {
            if (this.keyInputField.keyPressed(keyCode, scanCode, modifiers)) return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int mod) {
        if (this.needsInput) {
            if (this.keyInputField.charTyped(chr, mod)) return true;
        }
        return super.charTyped(chr, mod);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Render custom animated eye background
        HollowHomeScreen.renderEyeBackground(context, this.width, this.height);

        // Render soft drifting particles
        for (FloatingParticle p : particles) {
            p.tick(this.width, this.height);
            int a = (int)(p.alpha * 255) << 24;
            context.fill((int)p.x, (int)p.y, (int)(p.x + p.size), (int)(p.y + p.size), a | 0x00FF3366);
        }

        // Draw title
        int centerY = this.height / 2 - 90;
        context.getMatrices().push();
        context.getMatrices().translate(this.width / 2.0f, centerY - 25, 0);
        context.getMatrices().scale(2.5f, 2.5f, 1.0f);
        Text title = Text.literal("HOLLOW CLIENT").setStyle(Style.EMPTY.withFont(Identifier.of("hollowclient", "eternalo")));
        int tw = this.textRenderer.getWidth(title);
        context.drawText(this.textRenderer, title, -tw / 2 + 1, 1, 0x80FF1A55, false);
        context.drawText(this.textRenderer, title, -tw / 2 - 1, -1, 0x809D0022, false);
        context.drawText(this.textRenderer, title, -tw / 2, 0, 0xFFFFFFFF, false);
        context.getMatrices().pop();

        this.activateButton.visible = this.needsInput;

        if (this.isChecking) {
            // Draw loading status
            context.drawCenteredTextWithShadow(this.textRenderer, this.statusMessage, this.width / 2, this.height / 2, 0xFFFFFFFF);

            if (this.showDownloadProgress) {
                // Draw a beautiful crimson progress bar
                int barWidth = 160;
                int barHeight = 8;
                int bx = this.width / 2 - barWidth / 2;
                int by = this.height / 2 + 25;
                
                // Background
                RenderUtils.drawRoundedRect(context.getMatrices(), bx, by, barWidth, barHeight, 4, 0x50110408);
                RenderUtils.drawRoundedOutline(context.getMatrices(), bx, by, barWidth, barHeight, 4, 1.0f, 0x30FFFFFF);
                
                // Progress
                int progressWidth = (int)(barWidth * this.downloadPercent);
                if (progressWidth > 0) {
                    RenderUtils.drawRoundedRect(context.getMatrices(), bx, by, progressWidth, barHeight, 4, 0xFFFF1A55);
                }
                
                String percentStr = String.format("%.1f%%", this.downloadPercent * 100);
                context.drawCenteredTextWithShadow(this.textRenderer, percentStr, this.width / 2, by + 12, 0xFFFF3366);
            }
        } else if (this.needsInput) {
            context.drawCenteredTextWithShadow(this.textRenderer, "Enter your License Key to activate the client", this.width / 2, this.height / 2 - 25, 0xFFDDDDDD);
            
            this.keyInputField.render(context, mouseX, mouseY, delta);

            if (this.errorMessage != null) {
                context.drawCenteredTextWithShadow(this.textRenderer, this.errorMessage, this.width / 2, this.height / 2 + 50, 0xFFFF3366);
            }
        }

        super.render(context, mouseX, mouseY, delta);
    }
}
